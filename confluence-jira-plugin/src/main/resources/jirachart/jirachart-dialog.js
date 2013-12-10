AJS.Editor.JiraChart = (function($){
    var insertText = AJS.I18n.getText("insert.jira.issue.button.insert");
    var cancelText = AJS.I18n.getText("insert.jira.issue.button.cancel");
    var CHART_TITLE = AJS.I18n.getText("jirachart.macro.popup.title");
    var EMPTY_VALUE = "";
    var previousJiraChartWidth = EMPTY_VALUE;
    var jqlWhenEnterKeyPress;
    var intRegex = /^\d+$/;
    var popup;

    var openJiraChartDialog = function() {
        if (!popup) {
            popup = new AJS.ConfluenceDialog({width:840, height: 590, id: "jira-chart"});
            popup.addHeader(CHART_TITLE);
            
            var panels = AJS.Editor.JiraChart.Panels;
            
            for (var i = 0; i < panels.length; i++) {
                popup.addPanel(panels[i].title);
                var dlgPanel = popup.getCurrentPanel();
                panels[i].init(dlgPanel);
            }
            
            //add link more to come
            $('#jira-chart ul.dialog-page-menu').show().append(Confluence.Templates.ConfluenceJiraPlugin.addMoreToComeLink());
            
            var $container = $('#jira-chart-content');

            //add link select macro
            popup.addLink(AJS.I18n.getText("insert.jira.issue.button.select.macro"), function () {
                popup.hide();
                AJS.MacroBrowser.open(false);
            }, "dialog-back-link");
            
            //add button insert dialog
            popup.addButton(insertText, function() {
                if (isHaveChartImage()) {
                    var macroInputParams = getMacroParamsFromDialog($container);

                    //if wrong format width, set width is default
                    if (!AJS.Editor.JiraChart.validateWidth(macroInputParams.width)) {
                        macroInputParams.width = EMPTY_VALUE;
                    }

                    insertJiraChartMacroWithParams(macroInputParams);
                    //reset form after insert macro to RTE
                    resetDialog($container);
                    AJS.Editor.JiraChart.close();
                } else {
                    doSearch($container);
                }

            }, 'insert-jira-chart-macro-button');
            
            //add button cancel
            popup.addCancel(cancelText, function() {
                AJS.Editor.JiraChart.close();
            });
            
            //bind Action in Dialog
            bindActionInDialog($container);
            
        }
        // default to pie chart
        popup.gotoPanel(0);
        popup.show();
    };
    
    var bindActionInDialog = function(container) {
        var bindElementClick = container.find(".jira-chart-search button, #jira-chart-border, #jira-chart-show-infor");
        //bind search button, click in border
        bindElementClick.click(function() {
            doSearch(container);
        });
        
        //bind action enter for input field
        setActionOnEnter(container.find("input[type='text']"), doSearch, container);

        //bind out focus in width field
        container.find("#jira-chart-width").focusout(function(event) {
            var jiraChartWidth = convertFormatWidth(this.value);
            if (jiraChartWidth != previousJiraChartWidth)
            {
                previousJiraChartWidth = jiraChartWidth;
                doSearch(container);
            }
         });

        //for auto convert when paste url
        container.find("#jira-chart-inputsearch").change(function() {
            if (this.value !== jqlWhenEnterKeyPress) {
                clearChartContent();
                enableInsert();
            }
            jqlWhenEnterKeyPress = EMPTY_VALUE;
        }).bind("paste", function() {
            autoConvert(container);
        });

        // bind change event on stat type
        container.find("#jira-chart-statType").change(function(event) {
            doSearch(container);
        });

        //process bind display option
        bindSelectOption(container);

        //bind change event on server select
        if (AJS.Editor.JiraConnector.servers.length > 0) {
            AJS.Editor.JiraConnector.Panel.prototype.applinkServerSelect(AJS.$('#jira-chart-servers'),
                function(server) {
                    checkOau(container,server);
                    clearChartContent();
                    enableInsert();
                }
            );
        }
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
    
    var getCurrentChart = function(executor){
        var params = getMacroParamsFromDialog(AJS.$('#jira-chart-content'));
        if (params.chartType === "pie") {
            var pieChart = AJS.Editor.JiraChart.Panels[0];
            
            executor(pieChart, params);
        }
    };
    
    var doSearch = function(container) {
        var elementToValidate = AJS.$('#jira-chart-width');
        getCurrentChart(function(chart, params){
            if (chart.validate(elementToValidate))
            {
                doSearchInternal(container);
            }
        });
    };
    
    var doSearchInternal = function(container) {
        if (convertInputSearchToJQL(container) === undefined) {
            return;
        }
        
        var imageContainer = container.find(".jira-chart-img");

        //load image loading
        imageContainer.html('<div class="loading-data"></div>');
        var imageLoading = imageContainer.find(".loading-data")[0];
        AJS.$.data(imageLoading, "spinner", Raphael.spinner(imageLoading, 50, "#666"));

        getCurrentChart(function(chart, params){
            chart.renderChart(imageContainer, params);
        });
    };

    var resetDialog = function (container) {
        $(':input',container)
            .not(':button, :submit')
            .val('')
            .removeAttr('checked')
            .removeAttr('selected');
        container.find(".jira-chart-img").empty();
    };
    
    var displayOptPanel = function(container, open) {
        var jiraChartOption = container.find('.jira-chart-option');
        var topMargin = 40;
        var top = jiraChartOption.position().top + "px";
        var bottom =  EMPTY_VALUE;
        var animateConfig = {top: 430};
        
        if (open) {
            top = EMPTY_VALUE;
            bottom =  topMargin - jiraChartOption.height() + "px";
            animateConfig = {bottom: 0};
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
        var textSearch = container.find("#jira-chart-inputsearch").val();
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
            container.find("#jira-chart-inputsearch").val(jql);
        } else {
            container.find(".jira-chart-img").html(Confluence.Templates.ConfluenceJiraPlugin.jqlInvalid());
            disableInsert();
        }
        return jql;
    };
    
    var getMacroParamsFromDialog = function(container) {
        var selectedServer = getSelectedServer(container);
        return {
            jql: encodeURIComponent(container.find('#jira-chart-inputsearch').val()),
            statType: container.find('#jira-chart-statType').val(),
            width: convertFormatWidth(container.find('#jira-chart-width').val()),
            border: container.find('#jira-chart-border').prop('checked'),
            showinfor: container.find('#jira-chart-show-infor').prop('checked'),
            serverId:  selectedServer.id,
            server: selectedServer.name,
            isAuthenticated: !selectedServer.authUrl,
            chartType: 'pie'
        };
    };
    
    var convertFormatWidth = function(val) {
        val = val.replace("px", EMPTY_VALUE);
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
        if ($input.attr('id') === 'jira-chart-inputsearch') {
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
    
    var setValueAndDoSearchInDialog = function(params) {
        var container = $('#jira-chart-content');
        container.find('#jira-chart-inputsearch').val(decodeURIComponent(params['jql']));
        container.find('#jira-chart-statType').val(params['statType']);
        container.find('#jira-chart-width').val(params['width']);
        container.find('#jira-chart-border').attr('checked', (params['border'] === 'true'));
        container.find('#jira-chart-show-infor').attr('checked', (params['showinfor'] === 'true'));
        var servers = AJS.Editor.JiraConnector.servers;
        if (servers.length > 1) {
            container.find('#jira-chart-servers').val(params['serverId']);
        }
        checkOau(container, getSelectedServer(container));
        doSearch(container);
    };

    var getSelectedServer = function(container) {
        var servers = AJS.Editor.JiraConnector.servers;
        if (servers.length > 1) {
            return container.find('#jira-chart-servers option:selected').data('jiraapplink');
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

    var clearChartContent = function() {
        $('#chart-preview-iframe').contents().find('.jira-chart-macro-preview-container').empty();
        $('#jira-chart-content').find(".jira-chart-img .aui-message-container").remove();
    };

    var disableInsert = function() {
        AJS.$('#jira-chart').find('.insert-jira-chart-macro-button').disable();
    };

    var enableInsert = function() {
        var $insertButton = AJS.$('#jira-chart').find('.insert-jira-chart-macro-button');
        if ($insertButton.is(":disabled")) {
            $insertButton.enable();
        }
    };

    var checkOau = function($container, server) {
        AJS.$('.jira-oauth-message-marker', $container).remove();
        var oauObject = {
            selectedServer : server,
            msg : AJS.Editor.JiraConnector.Panel.prototype.msg
        };

        if (server && server.authUrl) {
            var oauForm = AJS.Editor.JiraConnector.Panel.prototype.createOauthForm.call(oauObject, function() {
                AJS.$('.jira-oauth-message-marker', $container).remove();
                AJS.Editor.JiraChart.search($container);
            });
            $container.find('div.jira-chart-search').append(oauForm);
        }
    };

    var isHaveChartImage = function() {
        return $('#chart-preview-iframe').contents().find(".jira-chart-macro-img").length > 0
    };
    
    return {
        open: openJiraChartDialog,
    
        close: function() {
          popup.hide();
          tinymce.confluence.macrobrowser.macroBrowserCancel();
        },
        
        edit: function(macro) {
            if (!checkNoApplinkConfig()) {
                return;
            }

            //check for show custom dialog when click in other macro
            if (macro.params === undefined || macro.params.serverId === undefined) {
                openJiraChartDialog();
                var container = $('#jira-chart-content');
                resetDialog(container);
                checkOau(container, getSelectedServer(container));
                return;
            }
            
            var params = macro.params;

            openJiraChartDialog();
            popup.gotoPanel(0);
            setValueAndDoSearchInDialog(params);
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

        clearChartContent : clearChartContent,

        disableInsert : disableInsert,

        enableInsert : enableInsert
    };
})(AJS.$);

AJS.Editor.JiraChart.Panels = [];
AJS.MacroBrowser.setMacroJsOverride('jirachart', {opener: AJS.Editor.JiraChart.edit});




