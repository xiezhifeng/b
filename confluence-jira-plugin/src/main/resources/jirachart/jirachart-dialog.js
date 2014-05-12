AJS.Editor.JiraChart = (function($) {

    var NOT_SUPPORTED_BUILD_NUMBER = -1;
    var START_JIRA_UNSUPPORTED_BUILD_NUMBER = 6109; //jira version 6.0.8
    var END_JIRA_UNSUPPORTED_BUILD_NUMBER = 6155; //jira version 6.1.1

    var insertText = AJS.I18n.getText("insert.jira.issue.button.insert");
    var cancelText = AJS.I18n.getText("insert.jira.issue.button.cancel");
    var CHART_TITLE = AJS.I18n.getText("jirachart.macro.popup.title");
    var EMPTY_VALUE = "";
    var jqlWhenEnterKeyPress;
    var intRegex = /^\d+$/;
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
                panels[i].init(dlgPanel, panels[i].id);
                var container = $('#jira-chart-content-' + panels[i].id);
                setActionOnEnter(container.find("input[type='text']"), doSearch, container);

            }
            
            // add button for opening JIRA Issue dialog
            $('#jira-chart ul.dialog-page-menu').show()
                .append(Confluence.Templates.ConfluenceJiraPlugin.addCrossMacroLink({'id': 'open-jira-issue-dialog', 'label' : AJS.I18n.getText("jira.issue")}));

            popup.addButton(insertText, function() {

                var currentChart = panels[popup.getCurrentPanel().id];

                if (chartTypeExists(currentChart.id) && currentChart.isExistImageChart()) {


                    var macroInputParams = currentChart.getMacroParamsFromDialog();


                    //if wrong format width, set width is default
                    if (!AJS.Editor.JiraChart.validateWidth(macroInputParams.width) && macroInputParams.width !== "") {
                        macroInputParams.width = EMPTY_VALUE;
                    }

                    insertJiraChartMacroWithParams(macroInputParams);
                    AJS.Editor.JiraChart.close();

                } else {
                    doSearch($("#jira-chart-content-" + currentChart.id));
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
            resetDisplayOption($container);

        });


        var jirachartsIndexes = jirachartsIndexes || function(panels) {
            var _jirachartsIndexes = {};
            _.each(panels, function(panel, index) {
                _jirachartsIndexes[panel.id] = index;
            })
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

    var chartTypeExists = function(chartId) {
        var panel = popup.getCurrentPanel().body;
        return panel.find("#jira-chart-content-" + chartId).length > 0;
    };

    var resetDisplayOption = function(container) {

        var displayOption = container.find('.jirachart-display-opts-close, .jirachart-display-opts-open');
        displayOption.addClass('jirachart-display-opts-open');
        displayOption.removeClass('jirachart-display-opts-close');
        setTimeout(function() {
            var jiraChartOption = container.find('.jira-chart-option');
            jiraChartOption.scrollTop(0);
            jiraChartOption.css({
                overflow: 'hidden',
                top: '430px'
            });
        }, 0);
    };

    var validate = function(element) {

        var $element = AJS.$(element);
        // remove error message if have
        $element.parent().find('#jira-chart-macro-dialog-validation-error').remove();

        var width = convertFormatWidth($element.val());
        // do the validation logic

        if (!AJS.Editor.JiraChart.validateWidth(width) && width !== "") {

            var inforErrorWidth = "wrongFormat";

            if (AJS.Editor.JiraChart.isNumber(width)) {
                inforErrorWidth = "wrongNumber";
            }

            $element.next().after(Confluence.Templates.ConfluenceJiraPlugin.warningValWidthColumn({'error': inforErrorWidth}));
            disableInsert();
            return false;
        }
        return true;

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

    var previewChart = function (dataToSend) {
        var previewUrl = Confluence.getContextPath() + "/rest/tinymce/1/macro/preview";

        var imageContainer = popup.getCurrentPanel().body.find(".jira-chart-img");

        //load image loading
        imageContainer.html('<div class="loading-data"></div>');
        var imageLoading = imageContainer.find(".loading-data")[0];
        showSpinner(imageLoading, 50);

        AJS.$.ajax({
            url : previewUrl,
            type : "POST",
            contentType : "application/json",
            data : JSON.stringify(dataToSend)
        })
        .done(
        function(data) {

            imageContainer.html('').hide(); // this will be re-show right after iframe is loaded
            var $iframe = AJS.$('<iframe frameborder="0" id="chart-preview-iframe"></iframe>');
            $iframe.appendTo(imageContainer);

            // window and document belong to iframe
            var win = $iframe[0].contentWindow,
                doc = win.document;

            //prevent call AJS.MacroBrowser.previewOnload when onload.
            //business of this function is not any effect to my function
            data = data.replace("window.onload", "var chartTest");

            // write data into iframe
            doc.open();
            doc.write(data);
            doc.close();
            hideSpinner(imageLoading);
            // make sure everyting has loaded completely
            $iframe.on('load', function() {
                win.AJS.$('#main').addClass('chart-preview-main');
                imageContainer.show();
                setupInsertButton(AJS.$(this));
            });
        })
        .error(
        function(jqXHR, textStatus, errorThrown) {
            AJS.log("Jira Chart Macro - Fail to get data from macro preview");
            hideSpinner(imageLoading);
            imageContainer.html(Confluence.Templates.ConfluenceJiraPlugin.showMessageRenderJiraChart());
            disableInsert();
        });

    };

    var setupInsertButton = function($iframe) {
        if ($iframe.contents().find(".jira-chart-macro-img").length > 0) {
            enableInsert();
        } else {
            disableInsert();
        }
    };
    
    var getCurrentChart = function(executor){
        executor(panels[popup.getCurrentPanel().id]);

    };
    
    var doSearch = function(container) {
        if (convertInputSearchToJQL(container) === undefined) {
            return;
        }
        getCurrentChart(function(chart){
            chart.renderChart();
        });

    };
    

    var displayOptPanel = function(container, open) {
        var jiraChartOption = container.find('.jira-chart-option');
        var topMargin = 40;
        var top = jiraChartOption.position().top + "px";
        var bottom =  EMPTY_VALUE;
        var animateConfig = {top: 430};
        
        if (open) {
            top = EMPTY_VALUE;
            bottom =  topMargin - jiraChartOption.find("#jiraChartMacroOption").height() + "px";
            animateConfig = {bottom: 0};
            jiraChartOption.css("overflow", "auto");
        } else {
            jiraChartOption.css("overflow", "hidden");
        }
        jiraChartOption.css("top", top);
        jiraChartOption.css("bottom", bottom);
        jiraChartOption.animate(animateConfig, 500 );
    };
    
    //auto convert URL/XML/Filter/Text/Key to JQL
    var autoConvert = function (container) {
        setTimeout(function () {
            doSearch(container);
        }, 100);
    };
    
    //convert URL/XML/Filter/Text/Key to JQL
    var convertInputSearchToJQL = function (container) {
        var servers = AJS.Editor.JiraConnector.servers;
        var serverId;
        var textSearch = container.find("#jira-chart-search-input").val();
        if (textSearch.indexOf('http') === 0) {
            var serverIndex = AJS.JQLHelper.findServerIndexFromUrl(textSearch, servers);
            if (serverIndex !== -1) {
                serverId = servers[serverIndex].id;
                container.find("#jira-chart-servers").val(serverId);
            } else {
                var message = Confluence.Templates.ConfluenceJiraPlugin.noServerWarning(
                    {
                        'isAdministrator': AJS.Meta.get("is-admin"),
                        'contextPath': Confluence.getContextPath()
                    }
                );
                container.find(".jira-chart-img").html(message);
                return;
            }
        }
        
        var jql = AJS.JQLHelper.convertToJQL(textSearch, serverId);
        if (jql) {
            container.find("#jira-chart-search-input").val(jql);
        } else {
            container.find(".jira-chart-img").html(Confluence.Templates.ConfluenceJiraPlugin.jqlInvalid());
            disableInsert();
        }
        return jql;
    };
    

    var convertFormatWidth = function(val) {
        val = (val && typeof val === 'string') ? val.replace("px", EMPTY_VALUE) : EMPTY_VALUE;
        if (val === "auto") {
            val = EMPTY_VALUE;
        }

        if (val.indexOf("%") > 0) {
            val = val.replace("%",EMPTY_VALUE) * 4; //default image is width = 400px;
        }
        return val;
    };
    
    var insertJiraChartMacroWithParams = function(params) {
        
        var insertMacroAtSelectionFromMarkup = function (macro) {
            tinymce.confluence.macrobrowser.macroBrowserComplete(macro);
        };

        if (AJS.Editor.inRichTextMode()) {
            insertMacroAtSelectionFromMarkup({name: 'jirachart', "params": params});
        }
    };

    var setJQLWhenEnterPress = function($input) {
        if ($input.attr('id') === 'jira-chart-search-input') {
            jqlWhenEnterKeyPress = $input.val();
        }
    };
    
    var setActionOnEnter = function(input, func, source) {
        input.unbind('keydown').keydown(function(e){
            if (e.which == 13){
                var keyup = function(e) {
                    input.unbind('keyup', keyup);
                    func(source);
                    setJQLWhenEnterPress(input);
                    return AJS.stopEvent(e);
                };
                input.keyup(keyup);
                return AJS.stopEvent(e);
            }
        });
    };

    var bindSelectOption = function(container) {
        var displayOptsOverlay = container.find('.jira-chart-option');
        displayOptsOverlay.css("top", "430px");
        var displayOptsBtn = container.find('.jirachart-display-opts-close, .jirachart-display-opts-open');
        displayOptsBtn.click(function(e) {
            var thiz = $(this);
            e.preventDefault();
            if (thiz.hasClass("disabled")) {
                return;
            }
            var isOpenButton = thiz.hasClass('jirachart-display-opts-open');

            if (isOpenButton) {
                displayOptPanel(container, true);
                thiz.addClass('jirachart-display-opts-close');
                thiz.removeClass('jirachart-display-opts-open');
            } else {
                displayOptPanel(container);
                thiz.removeClass('jirachart-display-opts-close');
                thiz.addClass('jirachart-display-opts-open');

            }
        });
    };

    var bindSelectedServer = function(container) {
        container.find("#jira-chart-servers").change(function(event) {
            container.find(".jira-chart-img").empty();
            AJS.Editor.JiraChart.disableInsert();
        });

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
        
        validateWidth: function(val) {
            //min and max for width value: [100,9000]
            return this.isNumber(val) &&  val >= 100 && val <= 9000;
        },
        
        isNumber: function(val) {
            return intRegex.test(val);
        },

        convertFormatWidth : convertFormatWidth,

        disableInsert : disableInsert,

        enableInsert : enableInsert,

        disableSearch : disableSearch,

        enableSearch : enableSearch,
        
        insertJiraChartMacroWithParams : insertJiraChartMacroWithParams,
        
        getSelectedServer : getSelectedServer,

        open: openJiraChartDialog,

        previewChart : previewChart,

        clearChartContent : clearChartContent,

        autoConvert : autoConvert,

        displayOptPanel : displayOptPanel,

        loadServers : loadServers,

        setActionOnEnter : setActionOnEnter,

        validate : validate,

        resetDisplayOption : resetDisplayOption,

        bindSelectOption : bindSelectOption,

        bindSelectedServer : bindSelectedServer

    };
})(AJS.$);

AJS.Editor.JiraChart.Panels = [];
AJS.MacroBrowser.setMacroJsOverride('jirachart', {opener: AJS.Editor.JiraChart.edit});




