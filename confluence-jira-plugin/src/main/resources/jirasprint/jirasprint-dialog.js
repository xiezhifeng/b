AJS.Editor.JiraSprint = (function($) {

    var NOT_SUPPORTED_BUILD_NUMBER = -1;
    var START_JIRA_UNSUPPORTED_BUILD_NUMBER = 6109; //jira version 6.0.8
    var END_JIRA_UNSUPPORTED_BUILD_NUMBER = 6155; //jira version 6.1.1

    var insertText = AJS.I18n.getText("insert.jira.issue.button.insert");
    var cancelText = AJS.I18n.getText("insert.jira.issue.button.cancel");
    var SPRINT_TITLE = AJS.I18n.getText("jirasprint.macro.popup.title");
    var popup;
    var panels;

    var openJiraSprintDialog = function(macro) {
        if (!popup) {
            popup = new AJS.ConfluenceDialog({width:840, height: 590, id: "jira-sprint"});
            popup.addHeader(SPRINT_TITLE);
            
            panels = AJS.Editor.JiraSprint.Panels;

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
            $('#jira-sprint ul.dialog-page-menu').show()
                .append(Confluence.Templates.ConfluenceJiraPlugin.addCrossMacroLink({'id': 'open-jira-issue-dialog', 'label' : AJS.I18n.getText("jira.issue")}));
            $('#jira-sprint ul.dialog-page-menu').show()
                .append(Confluence.Templates.ConfluenceJiraPlugin.addCrossMacroLink({'id': 'open-jira-sprint-dialog', 'label' : AJS.I18n.getText("jira.issue")}));

            popup.addButton(insertText, function() {
                var currentSprint = panels[popup.getCurrentPanel().id];
                var macroInputParams = currentSprint.getMacroParamsFromDialog();
                insertJiraSprintMacroWithParams(macroInputParams);
                AJS.Editor.JiraSprint.close();
            }, 'insert-jira-sprint-macro-button');

            //add link select macro
            popup.addLink(AJS.I18n.getText("insert.jira.issue.button.select.macro"), function () {
                popup.hide();
                AJS.MacroBrowser.open(false);
            }, "dialog-back-link");

            //add button cancel
            popup.addCancel(cancelText, function() {
                AJS.Editor.JiraSprint.close();
            });
        }

        AJS.$('#jira-sprint .dialog-page-menu button').click(function() {
            var currentPanel = panels[popup.getCurrentPanel().id];
            var $container = popup.getCurrentPanel().body;
            var selectedServer = AJS.Editor.JiraChart.Helper.getSelectedServer($container);
            checkOau($container, selectedServer);
            validateServerSupportedSprint($container);
            currentPanel.handleInsertButton();
            currentPanel.focusForm();
            currentPanel.resetDisplayOption();
        });

        //fix for switch between JIM and JCM
        var $container = popup.getCurrentPanel().body;
        validateServerSupportedSprint($container);

        var jirachartsIndexes = jirachartsIndexes || function(panels) {
            var _jirachartsIndexes = {};
            _.each(panels, function(panel, index) {
                _jirachartsIndexes[panel.sprintType] = index;
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
            return jirachartsIndexes[macro.params.sprintType];
        }
        return 0;
    };

    /**
     * Call pre binding for the whole dialog.
     * For each panel, call preBinding if any
     */
    var processPreBinding = function() {
        _.each(AJS.Editor.JiraSprint.Panels, function(panel){
            panel.preBinding && typeof panel.preBinding === 'function' && panel.preBinding();
        });
    };

    var processPostPopup = function() {
        $('#open-jira-issue-dialog').click(function() {
            AJS.Editor.JiraSprint.close();
            if (AJS.Editor.JiraConnector) {
                AJS.Editor.JiraConnector.openCleanDialog(false);
            }
        });
    };

    var loadServers = function(container) {
        if (AJS.Editor.JiraConnector.servers.length > 0) {
            AJS.Editor.JiraConnector.Panel.prototype.applinkServerSelect(container.find('#jira-sprint-servers'),
                function(server) {
                    clearSprintContent(container);
                    if (validateServerSupportedSprint(container)) {
                        checkOau(container,server);
                        enableSprintDialog(container);
                    }
                }
            );
        }
    };

    var getCurrentChart = function(executor){
        executor(panels[popup.getCurrentPanel().id]);

    };

    var doSearch = function(container) {
        if (AJS.Editor.JiraSprint.Helper.convertSearchTextToJQL(container) === undefined) {
            return;
        }

        getCurrentChart(function(chart) {
            chart.renderChart();
        });

    };

    var validateServerSupportedSprint = function(container) {

        if (container.find("#jira-sprint-support-all-version").length) return true;

        var selectedServer = AJS.Editor.JiraSprint.Helper.getSelectedServer(container);
        if (isJiraUnSupportedVersion(selectedServer)) {
            showJiraUnsupportedVersion(container);
            disableSprintDialog(container);
            return false;
        }
        return true;
    };
    
    var insertJiraSprintMacroWithParams = function(params) {
        
        var insertMacroAtSelectionFromMarkup = function (macro) {
            tinymce.confluence.macrobrowser.macroBrowserComplete(macro);
        };

        if (AJS.Editor.inRichTextMode()) {
            insertMacroAtSelectionFromMarkup({name: 'jirasprint', "params": params});
        }
    };

    var resetDialogValue = function(jirachartsIndexes, macro) {
         for (var i = 0; i < panels.length; i++) {
            panels[i].resetDialogValue();
        }
        processPreBinding();
        if (macro && macro.params) {
            macro.params.sprintType='pie';
            var currentPanel = panels[jirachartsIndexes[macro.params.sprintType]];
            currentPanel.bindingDataFromMacroToForm(macro.params);
        }

    };

    var checkNoApplinkConfig = function() {
        if (AJS.Editor.JiraConnector.servers === undefined || AJS.Editor.JiraConnector.servers.length === 0) {
            AJS.Editor.JiraConnector.warningPopup(AJS.Meta.get("is-admin"));
            return false;
        }
        return true;
    };

    var clearSprintContent = function(container) {
        container.find(".jira-oauth-message-marker").remove();
        container.find(".jira-sprint-img").empty();
        container.find("#jira-sprint-search-input").empty();

    };

    var disableInsert = function() {
        $('#jira-sprint').find('.insert-sprint-chart-macro-button').disable();
    };

    var enableInsert = function() {
        var $insertButton = AJS.$('#jira-sprint').find('.insert-jira-sprint-macro-button');
        if ($insertButton.is(":disabled")) {
            $insertButton.enable();
        }
    };

    var disableSearch = function(container) {
        container.find('#jira-sprint-search-button').disable();
    };

    var enableSearch = function(container) {
        if (container.find('#jira-sprint-search-button').is(":disabled")) {
            container.find('#jira-sprint-search-button').enable();
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
                AJS.Editor.JiraSprint.search($container);
            });
            $container.find('div.jira-sprint-search').append(oauForm);
        }
    };

    var showJiraUnsupportedVersion = function($container) {
        $container.find('.jira-sprint-img').html(Confluence.Templates.ConfluenceJiraPlugin.showJiraUnsupportedVersion());
    };

    var disableSprintDialog = function($container) {
        $container.find('#jira-sprint-search-input').attr('disabled','disabled');
        $container.find("#jira-sprint-search-button").attr('disabled','disabled');
        var $displayOptsBtn = $container.find('.jirasprint-display-opts-close, .jirasprint-display-opts-open');
        if ($displayOptsBtn.hasClass("jirasprint-display-opts-close")) {
            $displayOptsBtn.click();
        }
        $displayOptsBtn.addClass("disabled");
        disableInsert();
    };

    var enableSprintDialog = function($container) {
        $container.find('#jira-sprint-search-input').removeAttr('disabled');
        $container.find("#jira-sprint-search-button").removeAttr('disabled');
        $container.find('.jirasprint-display-opts-open').removeClass('disabled');
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
            openJiraSprintDialog(macro);

            //check for show custom dialog when click in other macro
            var $container = popup.getCurrentPanel().body;

            if (!validateServerSupportedSprint($container)) {
                return;
            }

            enableSprintDialog($container);
            if (macro.params !== undefined && macro.params.serverId !== undefined) {
                doSearch($container);
            }
            var selectedServer = AJS.Editor.JiraSprint.Helper.getSelectedServer($container);
            checkOau($container, selectedServer);

        },

        search: doSearch,
        
        disableInsert : disableInsert,

        enableInsert : enableInsert,

        disableSearch : disableSearch,

        enableSearch : enableSearch,

        insertJiraSprintMacroWithParams : insertJiraSprintMacroWithParams,
        
        open: openJiraSprintDialog,

        clearSprintContent : clearSprintContent,

        loadServers : loadServers,

        validateServerSupportedChart : validateServerSupportedSprint
    };
})(AJS.$);

AJS.Editor.JiraSprint.Panels = [];
AJS.MacroBrowser.setMacroJsOverride('jirasprint', {opener: AJS.Editor.JiraSprint.edit});




