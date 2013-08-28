AJS.Editor.JiraChart = (function($){
    var insertText = AJS.I18n.getText("insert.jira.issue.button.insert");
    var cancelText = AJS.I18n.getText("insert.jira.issue.button.cancel");

    var popup;
    
    var openJiraChartDialog = function() {
        if (!popup) {
            popup = new AJS.ConfluenceDialog({width:840, height: 590, id: "jira-chart"});
            var jiraChartTitle = AJS.I18n.getText("jirachart.macro.popup.title");
            popup.addHeader(jiraChartTitle);
            
            var panels = AJS.Editor.JiraChart.Panels;
            
            for (var i = 0; i < panels.length; i++){
                popup.addPanel(panels[i].title());
                var dlgPanel = popup.getCurrentPanel();
                var panelObj = panels[i];
                panelObj.init(dlgPanel);
            }
            
            var container = $('#jira-chart-content');

            //add link select macro
            popup.addLink(AJS.I18n.getText("insert.jira.issue.button.select.macro"), function () {
                popup.hide();
                AJS.MacroBrowser.open(false);
            }, "dialog-back-link");
            
            //add button insert dialog
            popup.addButton(insertText, function() {
                var macroInputParams = getMacroParamsFromDialog(container);
                insertJiraChartMacroWithParams(macroInputParams);
                //reset form after insert macro to RTE
                resetDialog(container);
                AJS.Editor.JiraChart.close();
            }, 'insert-jira-chart-macro-button');
            
            //add button cancel
            popup.addCancel(cancelText, function() {
                AJS.Editor.JiraChart.close();
            });
            
            //bind Action in Dialog
            bindActionInDialog(container);
            
         }
         // default to pie chart
         popup.gotoPanel(0);
         popup.show();
         AJS.$('#jira-chart').find('.insert-jira-chart-macro-button').disable();
    };
    
    var bindActionInDialog = function(container) {
        var bindElementClick = container.find(".jira-chart-search button, #jira-chart-border");
        //bind search button, click in border
        bindElementClick.click(function() {
            doSearch(container);
        });
        
        //bind action enter for input field
        setActionOnEnter(container.find("input[type='text']"), doSearch, container);

        //bind out focus in width field
        container.find("#jira-chart-width").focusout(function() {
            doSearch(container);
         });

        //for auto convert when paste url
        container.find("#jira-chart-inputsearch").bind("paste", function() {
            autoConvert(container);
        });

        //process bind display option
        bindSelectOption(container);
    };
    
    var bindSelectOption = function(container) {
        var displayOptsOverlay = container.find('.jira-chart-option');
        displayOptsOverlay.css("top", "440px");
        var displayOptsBtn = container.find('.jirachart-display-opts-close, .jirachart-display-opts-open');
        displayOptsBtn.click(function(e) {
            var thiz = $(this);
            e.preventDefault();
            if(thiz.hasClass("disabled")) {
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
    
    var doSearch = function(container) {

        if(typeof convertInputSearchToJQL(container) === 'undefined') {
            return;
        }

        var imageContainer = container.find(".jira-chart-img");

        //load image loading
        imageContainer.html('<div class="loading-data"></div>');
        var imageLoading = imageContainer.find(".loading-data")[0];
        AJS.$.data(imageLoading, "spinner", Raphael.spinner(imageLoading, 50, "#666"));

        var params = getMacroParamsFromDialog(container);
        if(params.chartType === "pie") {
            var pieChart = AJS.Editor.JiraChart.Panels[0];
            pieChart.renderChart(imageContainer, params);
        }
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
        var bottom =  "";
        var animateConfig = {top: 440};
        
        if(open) {
            top = "";
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
            if(serverIndex !== -1) {
                serverId = servers[serverIndex].id;
                container.find("#jira-chart-servers").val(serverId);
            } else {
                var message = Confluence.Templates.ConfluenceJiraPlugin.noServerWarning({'isAdministrator': AJS.Meta.get("is-admin"), 'contextPath': Confluence.getContextPath()});
                container.find(".jira-chart-img").html(message);
                return;
            }
        }
        
        var jql = AJS.JQLHelper.convertToJQL(textSearch, serverId);
        if(jql) {
            container.find("#jira-chart-inputsearch").val(jql);
        } else {
            container.find(".jira-chart-img").html(Confluence.Templates.ConfluenceJiraPlugin.showMessageRenderJiraChart());
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
            serverId:  selectedServer.id,
            server: selectedServer.name,
            isAuthenticated: !selectedServer.authUrl,
            chartType: 'pie'
        };
    };
    
    var convertFormatWidth = function(val) {
        val = val.replace("px","");
        if(val === "auto") {
            val="";
        }
        if(val.indexOf("%") > 0) {
            val = val.replace("%","")*4; //default image is width = 400px;
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
    
    var setActionOnEnter = function(input, func, source) {
        input.unbind('keydown').keydown(function(e){
            if (e.which == 13){
                var keyup = function(e){
                    input.unbind('keyup', keyup);
                    func(source);
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
        var servers = AJS.Editor.JiraConnector.servers;
        if (servers.length > 1) {
            container.find('#jira-chart-servers').val(params['serverId']);
        }
        AJS.Editor.JiraChart.Panels[0].checkOau(container, getSelectedServer(container));
        doSearch(container);
    };

    var getSelectedServer = function(container) {
        var servers = AJS.Editor.JiraConnector.servers;
        if(servers.length > 1) {
            return container.find('#jira-chart-servers option:selected').data('jiraapplink');
        }
        return servers[0];
    };
    
    return {
        open: openJiraChartDialog,
    
        close: function() {
          popup.hide();
          tinymce.confluence.macrobrowser.macroBrowserCancel();
        },
        
        edit: function(macro) {
            //check for show custom dialog when click in other macro
            if (typeof(macro.params) === 'undefined' || typeof(macro.params.serverId) === 'undefined') {
                AJS.Editor.JiraChart.open();
                var container = $('#jira-chart-content');
                resetDialog(container);
                AJS.Editor.JiraChart.Panels[0].checkOau(container, getSelectedServer(container));
                return;
            }
            
            var params = macro.params;
            
            openJiraChartDialog();
            popup.gotoPanel(0);
            setValueAndDoSearchInDialog(params, container);
        },

        search: function(container) {
            doSearch(container);
        }
    };
})(AJS.$);

AJS.Editor.JiraChart.Panels = [];
AJS.MacroBrowser.setMacroJsOverride('jirachart', {opener: AJS.Editor.JiraChart.edit});
