AJS.Editor.JiraChart = (function($) {

    var NOT_SUPPORTED_BUILD_NUMBER = -1;
    var START_JIRA_UNSUPPORTED_BUILD_NUMBER = 6109; //jira version 6.0.8
    var END_JIRA_UNSUPPORTED_BUILD_NUMBER = 6155; //jira version 6.1.1

    var insertText = AJS.I18n.getText("insert.jira.issue.button.insert");
    var cancelText = AJS.I18n.getText("insert.jira.issue.button.cancel");
    var CHART_TITLE = AJS.I18n.getText("jirachart.macro.popup.title");
    var popup;
    var panels;

    var openJiraChartDialog = function(macro) {
        if (!popup) {
            popup = new AJS.ConfluenceDialog({width:840, height: 590, id: "jira-chart"});
            popup.addHeader(CHART_TITLE);
            
            panels = AJS.Editor.JiraChart.Panels;
            
            for (var i = 0; i < panels.length; i++) {
                if (typeof (panels[i].title) === "function")
                {
                    popup.addPanel(panels[i].title());
                }
                else if (panels[i].title !== undefined)
                {
                    popup.addPanel(panels[i].title);
                }
                var dlgPanel = popup.getCurrentPanel();
                panels[i].init(dlgPanel);
            }
            
            // add button for opening JIRA Issue dialog
            $('#jira-chart ul.dialog-page-menu').show()
                .append(Confluence.Templates.ConfluenceJiraPlugin.addCrossMacroLink({'id': 'open-jira-issue-dialog', 'label' : AJS.I18n.getText("jira.issue")}));

            popup.addButton(insertText, function() {
                var currentChart = panels[popup.getCurrentPanel().id];
                if (chartTypeExists(currentChart.chartType) && currentChart.isImageChartExisted()) {
                    var macroInputParams = currentChart.getMacroParamsFromDialog();
                    insertJiraChartMacroWithParams(macroInputParams);
                    AJS.Editor.JiraChart.close();
                } else {
                    doSearch($("#jira-chart-content-" + currentChart.chartType));
                }
            }, 'insert-jira-chart-macro-button');

            //add link select macro
            popup.addLink(AJS.I18n.getText("insert.jira.issue.button.select.macro"), function () {
                popup.hide();
                AJS.MacroBrowser.open(false);
            }, "dialog-back-link");

            //add button cancel
            popup.addCancel(cancelText, function() {
                AJS.Editor.JiraChart.close();
            });

        }


        AJS.$('#jira-chart .dialog-page-menu button').click(function() {
            var currentPanel = panels[popup.getCurrentPanel().id];
            var $container = popup.getCurrentPanel().body;
            var selectedServer = getSelectedServer($container);
            checkOau($container, selectedServer);
            currentPanel.handleInsertButton();
            currentPanel.focusForm();
            currentPanel.resetDisplayOption();
        });


        var jirachartsIndexes = jirachartsIndexes || function(panels) {
            var _jirachartsIndexes = {};
            _.each(panels, function(panel, index) {
                _jirachartsIndexes[panel.chartType] = index;
            });
            return _jirachartsIndexes;
        }(panels);

        resetDialogValue(jirachartsIndexes, macro);
        disableInsert();
        popup.gotoPanel(getIndexPanel(jirachartsIndexes, macro));
        popup.overrideLastTab();
        popup.show();
        processPostPopup();
    };

    var getIndexPanel = function (jirachartsIndexes, macro) {
        if (macro && macro.params) {
            return jirachartsIndexes[macro.params.chartType];
        }
        return 0;
    };

    var processPostPopup = function() {
        $('#open-jira-issue-dialog').click(function() {
            AJS.Editor.JiraChart.close();
            if (AJS.Editor.JiraConnector) {
                AJS.Editor.JiraConnector.openCleanDialog(false);
            }
        });
    };

    var loadServers = function(container) {

        if (AJS.Editor.JiraConnector.servers.length > 0) {
            AJS.Editor.JiraConnector.Panel.prototype.applinkServerSelect(container.find('#jira-chart-servers'),
                function(server) {
                    clearChartContent(container);
                    if (isJiraUnSupportedVersion(server)) {
                        showJiraUnsupportedVersion(container);
                        disableChartDialog(container);
                    } else {
                        checkOau(container,server);
                        enableChartDialog(container);
                    }
                }
            );
        }

    };

    var chartTypeExists = function(chartType) {
        var panel = popup.getCurrentPanel().body;
        return panel.find("#jira-chart-content-" + chartType).length > 0;
    };

    var getCurrentChart = function(executor){
        executor(panels[popup.getCurrentPanel().id]);

    };

    var showSpinner = function(element, radius) {
        AJS.$.data(element, "spinner", Raphael.spinner(element, radius, "#666"));
    };

    var hideSpinner =  function (element) {
        var spinner = AJS.$.data(element, "spinner");
        if (spinner) {
            spinner();
            delete spinner;
            AJS.$.data(element, "spinner", null);
        }

    };
    
    var doSearch = function(container) {
        if (AJS.Editor.JiraChart.Helper.convertSearchTextToJQL(container) === undefined) {
            return;
        }
        getCurrentChart(function(chart) {
            chart.renderChart();
        });

    };
    
     var insertJiraChartMacroWithParams = function(params) {
        
        var insertMacroAtSelectionFromMarkup = function (macro) {
            tinymce.confluence.macrobrowser.macroBrowserComplete(macro);
        };

        if (AJS.Editor.inRichTextMode()) {
            insertMacroAtSelectionFromMarkup({name: 'jirachart', "params": params});
        }
    };

    var resetDialogValue = function(jirachartsIndexes, macro) {
         for (var i = 0; i < panels.length; i++) {
            panels[i].resetDialogValue();
        }

        if (macro && macro.params) {
            var currentPanel = panels[jirachartsIndexes[macro.params.chartType]];
            currentPanel.bindingDataFromMacroToForm(macro.params);
        }

    };

    var getSelectedServer = function($container) {
        var servers = AJS.Editor.JiraConnector.servers;
        if (servers.length > 1) {
            return $container.find('#jira-chart-servers option:selected').data('jiraapplink');
        }
        return servers[0];
    };

    var checkNoApplinkConfig = function() {
        if (AJS.Editor.JiraConnector.servers === undefined || AJS.Editor.JiraConnector.servers.length === 0) {
            AJS.Editor.JiraConnector.warningPopup(AJS.Meta.get("is-admin"));
            return false;
        }
        return true;
    };

    var clearChartContent = function(container) {
        container.find(".jira-oauth-message-marker").remove();
        container.find(".jira-chart-img").empty();
        container.find("#jira-chart-search-input").empty();

    };

    var disableInsert = function() {
        $('#jira-chart').find('.insert-jira-chart-macro-button').disable();
    };

    var enableInsert = function() {
        var $insertButton = AJS.$('#jira-chart').find('.insert-jira-chart-macro-button');
        if ($insertButton.is(":disabled")) {
            $insertButton.enable();
        }
    };

    var disableSearch = function(container) {
        container.find('#jira-chart-search-button').disable();
    };

    var enableSearch = function(container) {
        if (container.find('#jira-chart-search-button').is(":disabled")) {
            container.find('#jira-chart-search-button').enable();
        }
    };

    var checkOau = function($container, server) {
        $('.jira-oauth-message-marker', $container).remove();
        var oauObject = {
            selectedServer : server,
            msg : AJS.Editor.JiraConnector.Panel.prototype.msg
        };

        if (server && server.authUrl) {
            var oauForm = AJS.Editor.JiraConnector.Panel.prototype.createOauthForm.call(oauObject, function() {
                $('.jira-oauth-message-marker', $container).remove();
                AJS.Editor.JiraChart.search($container);
            });
            $container.find('div.jira-chart-search').append(oauForm);
        }
    };

    var showJiraUnsupportedVersion = function($container) {
        $container.find('.jira-chart-img').html(Confluence.Templates.ConfluenceJiraPlugin.showJiraUnsupportedVersion());
    };

    var disableChartDialog = function($container) {
        $container.find('.jira-chart-search .jira-chart-search-input').attr('disabled','disabled');
        $container.find(".jira-chart-search button").attr('disabled','disabled');
        var $displayOptsBtn = $container.find('.jirachart-display-opts-close, .jirachart-display-opts-open');
        if ($displayOptsBtn.hasClass("jirachart-display-opts-close")) {
            $displayOptsBtn.click();
        }
        $displayOptsBtn.addClass("disabled");
        disableInsert();
    };

    var enableChartDialog = function($container) {
        $container.find('#jira-chart-search-input').removeAttr('disabled');
        $container.find("#jira-chart-search-button").removeAttr('disabled');
        $container.find('.jirachart-display-opts-open').removeClass('disabled');
    };

    var isJiraUnSupportedVersion = function(server) {
        var buildNumber = server.buildNumber;
        return  buildNumber == NOT_SUPPORTED_BUILD_NUMBER ||
            (buildNumber >= START_JIRA_UNSUPPORTED_BUILD_NUMBER && buildNumber < END_JIRA_UNSUPPORTED_BUILD_NUMBER);
    };

    return {

        close: function() {
            popup.hide();
            tinymce.confluence.macrobrowser.macroBrowserCancel();
        },
        
        edit: function(macro) {

            if (!checkNoApplinkConfig()) {
                return;
            }

            openJiraChartDialog(macro);

            //check for show custom dialog when click in other macro
            var $container = popup.getCurrentPanel().body;
            var selectedServer = getSelectedServer($container);
            if (isJiraUnSupportedVersion(selectedServer)) {
                showJiraUnsupportedVersion($container);
                disableChartDialog($container);
                return;
            }

            enableChartDialog($container);
            if (macro.params !== undefined && macro.params.serverId !== undefined) {
                doSearch($container);
            }
            checkOau($container, selectedServer);

        },

        search: doSearch,
        
        disableInsert : disableInsert,

        enableInsert : enableInsert,

        disableSearch : disableSearch,

        enableSearch : enableSearch,
        
        insertJiraChartMacroWithParams : insertJiraChartMacroWithParams,
        
        getSelectedServer : getSelectedServer,

        open: openJiraChartDialog,

        clearChartContent : clearChartContent,

        loadServers : loadServers
    };
})(AJS.$);

AJS.Editor.JiraChart.Panels = [];
AJS.MacroBrowser.setMacroJsOverride('jirachart', {opener: AJS.Editor.JiraChart.edit});




