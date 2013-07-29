AJS.Editor.JiraChart = (function($){
    var insertText = AJS.I18n.getText("insert.jira.issue.button.insert");
    var cancelText = AJS.I18n.getText("insert.jira.issue.button.cancel");
    var JQL_OPERATORS = /=|!=|~|>|<|!~| is | in | was | changed /i;
    var KEY_ISSUE_OPERATORS = /\s*([A-Z][A-Z]+)-[0-9]+\s*/;
    
    var popup;
    
    var openJiraChartDialog = function () {
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
            
            //popup.addPanel("Panel 1", contentJiraChart);
            //popup.get("panel:0").setPadding(0);

            var container = $('#jira-chart #jira-chart-content');

            //add link select macro
            popup.addLink(AJS.I18n.getText("insert.jira.issue.button.select.macro"), function () {
                popup.hide();
                AJS.MacroBrowser.open(false);
            }, "dialog-back-link");
            
            //add button insert dialog
            popup.addButton(insertText, function () {
                var macroInputParams = getMacroParamsFromDialog(container);
                insertJiraChartMacroWithParams(macroInputParams);
                //reset form after insert macro to RTE
                resetDialog(container);
                AJS.Editor.JiraChart.close();
            }, 'insert-jira-chart-macro-button');
            
            //add button cancel
            popup.addCancel(cancelText, function(){
                AJS.Editor.JiraChart.close();
            });
            
            //bind search button
            $('#jira-chart .jira-chart-search button').bind("click",function() {
                doSearch(container);
            });
            //set action enter for input field
            setActionOnEnter(container.find("input[type='text']"), doSearch, container);
            
            //for auto convert when paste url
            container.find("input[name='jiraSearch']").bind('paste', function() {
                autoConvert(container);
            });

            //process bind display option
            bindSelectOption(container);
         }
         // default to search panel
         popup.gotoPanel(0);
         popup.show();
         AJS.$('#jira-chart .insert-jira-chart-macro-button').disable();
    };
    
    var bindSelectOption = function(container) {
        var displayOptsOverlay = container.find('.jira-chart-option');
            displayOptsOverlay.css("top", "440px");
            var displayOptsBtn = container.find('.jql-display-opts-close, .jql-display-opts-open');
            displayOptsBtn.bind("click", function(e) {
                e.preventDefault();
                if($(this).hasClass("disabled")) {
                    return;
                }
                var isOpenButton = $(this).hasClass('jql-display-opts-open');
                
                if (isOpenButton) {
                    displayOptPanel(container, true);
                    jQuery(this).addClass('jql-display-opts-close');
                    jQuery(this).removeClass('jql-display-opts-open');
                } else {
                    displayOptPanel(container);
                    jQuery(this).removeClass('jql-display-opts-close');
                    jQuery(this).addClass('jql-display-opts-open');
                }
            });
    };
    
    var showSpinner = function (element, radius, centerWidth, centerHeight) {
        AJS.$.data(element, "spinner", Raphael.spinner(element, radius, "#666"));
        // helps with centering the spinner
        if (centerWidth) AJS.$(element).css('marginLeft', radius * 7);
        if (centerHeight) AJS.$(element).css('marginTop', radius * 1.2);
    };
    
    var doSearch = function(container) {
        
        if(typeof convertToJQL(container) === 'undefined') {
            return;    
        }
        
        var params = getMacroParamsFromDialog(container);
        container.find(".jira-chart-img").empty().append('<div class="loading-data"></div>');
        showSpinner(container.find(".jira-chart-img .loading-data")[0], 50, true, true);
    
        var url = Confluence.getContextPath() + "/plugins/servlet/jira-chart-proxy?jql=" + params.jql + "&statType=" + params.statType + "&width=" + params.width  + "&border=" + params.border + "&appId=" + params.serverId + "&chartType=" + params.chartType;
        
        var img = $("<img />").attr('src',url);
        img.error(function(){
            container.find(".jira-chart-img").empty().append(Confluence.Templates.ConfluenceJiraPlugin.showMessageRenderJiraChart());
            AJS.$('#jira-chart .insert-jira-chart-macro-button').disable();
        }).load(function() {
            var chartImg =  $("<div class='chart-img'></div>").append(img);
            container.find(".jira-chart-img").empty().append(chartImg);
            AJS.$('#jira-chart .insert-jira-chart-macro-button').enable();
        });
    };
    
    var resetDialog = function (container) {
        $(':input',container)
            .not(':button, :submit, :reset, :hidden')
            .val('')
            .removeAttr('checked')
            .removeAttr('selected');
        container.find(".jira-chart-img").empty();
    };
    
    var displayOptPanel = function(container, open) {
        var displayOptsOverlay = container.find('.jira-chart-option');
        if(open) {
            var currentHeighOfOptsOverlay = displayOptsOverlay.height();
            var topMarginDisplayOverlay = 40;
            var currentBottomPosition =  -(currentHeighOfOptsOverlay - topMarginDisplayOverlay);
            displayOptsOverlay.css("top", "");
            displayOptsOverlay.css("bottom", currentBottomPosition + "px");
            displayOptsOverlay.animate({
                bottom: 0
            }, 500 );
        } else {
            displayOptsOverlay.css("top", displayOptsOverlay.position().top + "px");
            displayOptsOverlay.css("bottom", "");
            displayOptsOverlay.animate({
                top: 440
            }, 500 );
        }
    };
    
    //auto convert URL/XML/Filter/Text/Key to JQL
    var autoConvert = function (container) {
        setTimeout(function () {
            doSearch(container);
        }, 100);
    };
    
    var convertFilterToJQL = function (textSearch, container) {
        var jql;
        var url = decodeURIComponent(textSearch);
        var serverIndex = AJS.JQLHelper.findServerIndexFromUrl(url, AJS.Editor.JiraConnector.servers);
        if (serverIndex !== -1) {
            container.find("select[name='server']").val(AJS.Editor.JiraConnector.servers[serverIndex].id);
            container.find("select[name='server']").trigger("change");
            //error when convert filter to JQL
            var onError = function (xhr) {
                //show error
                container.find(".jira-chart-img").empty().append(Confluence.Templates.ConfluenceJiraPlugin.showMessageRenderJiraChart());
            };
            //success when convert filter to JQL
            var onSuccess = function (responseData) {
                if (responseData.jql) {
                    jql = responseData.jql;
                }
                else {
                    onError();
                }
            };
            AJS.JQLHelper.getJqlQueryFromJiraFilter(url, AJS.Editor.JiraConnector.servers[serverIndex].id, onSuccess, onError);
        }
        else {
            var message = Confluence.Templates.ConfluenceJiraPlugin.noServerWarning({'isAdministrator': AJS.Meta.get("is-admin"), 'contentPath': Confluence.getContextPath()});
            container.find(".jira-chart-img").empty().append(message);
        }
        return jql;
    };
    
    var processJiraParams = function (jiraParams, container) {
        var jql;
        if (jiraParams.serverIndex !== -1) {
            container.find("select[name='server']").val(AJS.Editor.JiraConnector.servers[jiraParams.serverIndex].id);
            container.find("select[name='server']").trigger("change");
            if (jiraParams.jqlQuery.length > 0) {
                jql = jiraParams.jqlQuery;
            }
        }
        else {
            var message = Confluence.Templates.ConfluenceJiraPlugin.noServerWarning({'isAdministrator': AJS.Meta.get("is-admin"), 'contentPath': Confluence.getContextPath()});
            container.find(".jira-chart-img").empty().append(message);
        }
        return jql;
    };
    
    var convertToJQL = function (container) {
        var jql;
        var textSearch = container.find("input[name='jiraSearch']").val();
        if ($.trim(textSearch) !== "") {
            //convert Filter to JQL
            if (textSearch.indexOf('http') === 0 && AJS.JQLHelper.isFilterUrl(textSearch)) {
                jql = convertFilterToJQL(textSearch, container);
            //convert URL/XML to JQL
            } else if (textSearch.indexOf('http') === 0 && AJS.JQLHelper.isIssueUrlOrXmlUrl(textSearch)) {
                var jiraParams = AJS.JQLHelper.getJqlAndServerIndexFromUrl(decodeURIComponent(textSearch), AJS.Editor.JiraConnector.servers);
                if (processJiraParams(jiraParams, container)) {
                    jql = jiraParams.jqlQuery;
                } 
            }  else {
                //check is JQL
                if (textSearch.indexOf('http') !== 0 && textSearch.match(JQL_OPERATORS)) {
                    jql = textSearch;
                //convert key to JQL
                } else if (textSearch.match(KEY_ISSUE_OPERATORS)) {
                    jql = "key=" + textSearch;
                //convert text to JQL
                } else {
                    jql = 'summary ~ "' + textSearch + '" OR description ~ "' + textSearch + '"';
                }
            }
        }
        if(jql) {
            container.find("input[name='jiraSearch']").val(jql);
        }
        return jql;
    };
    
    var getMacroParamsFromDialog = function(container) {
        var jql = container.find("input[name='jiraSearch']").val();
        var statType = container.find("select[name='type']").val();
        var width = container.find("input[name='width']").val().replace("px","");
        var border = container.find("input[name='border']").prop('checked');
        var serverId, server;
        var servers = AJS.Editor.JiraConnector.servers;
        if (servers.length === 1) {
            serverId = servers[0].id;
            server = servers[0].name;
        } else {
            serverId = container.find("select[name='server']").val();
            server = container.find("select[name='server']").find("option:selected").text();
        }
        var macroParams = {
            jql: encodeURIComponent(jql),
            statType: statType,
            width: width,
            border: border,
            serverId:  serverId,
            server: server,
            chartType: 'pie'
        };
        return macroParams;
    };
    
    var insertJiraChartMacroWithParams = function(params) {
        
        var insertMacroAtSelectionFromMarkup = function (macro){
            tinymce.confluence.macrobrowser.macroBrowserComplete(macro);
        };

        if (AJS.Editor.inRichTextMode()) {
            insertMacroAtSelectionFromMarkup({name: 'jirachart', "params": params});
        } else {
            var markup = '{jirachart:';
            for (var key in params) {
                markup = markup + key + '=' + params[key] + '|';
            }
            
            if (markup.charAt(markup.length - 1) == '|') {
                markup = markup.substr(0, markup.length - 1);
            }
            
            var textArea = $("#markupTextarea");
            var selection = textArea.selectionRange();
            textArea.selectionRange(selection.start, selection.end);
            textArea.selection(markup);
            selection = textArea.selectionRange();
            textArea.selectionRange(selection.end, selection.end);
        }
    };
    
    var setActionOnEnter = function(input, f, source){
        input.unbind('keydown').keydown(function(e){
            if (e.which == 13){
                var keyup = function(e){
                    input.unbind('keyup', keyup);
                    f(source);
                    return AJS.stopEvent(e);
                };
                input.keyup(keyup);
                return AJS.stopEvent(e);
            }
        });
    };
    
    var setValueDialog = function(params) {
        var container = $('#jira-chart #jira-chart-content');
        container.find("input[name='jiraSearch']").val(decodeURIComponent(params['jql']));
        container.find("select[name='type']").val(params['statType']);
        container.find("input[name='width']").val(params['width']);
        container.find("input[name='border']").attr('checked', params['border'] === 'true');
        var servers = AJS.Editor.JiraConnector.servers;
        if (servers.length > 1) {
            container.find("select[name='server']").val(params['serverId']);
        }
        doSearch(container);
    };
    
    return {
        open: function() {
            openJiraChartDialog();
        },
        
        close: function() {
          popup.hide();
          tinymce.confluence.macrobrowser.macroBrowserCancel();
        },
        
        edit: function(macro) {
            //check for show custom dialog when click in other macro
            if (typeof(macro.params) === 'undefined' || typeof(macro.params.serverId) === 'undefined') {
                AJS.Editor.JiraChart.open();
                var container = $('#jira-chart #jira-chart-content');
                resetDialog(container);
                return;
            }
            
            var params = macro.params;
            
            if (macro && !AJS.Editor.inRichTextMode()) { // select and replace the current macro markup
                $("#markupTextarea").selectionRange(macro.startIndex, macro.startIndex + macro.markup.length);
            }
            openJiraChartDialog();
            popup.gotoPanel(0);
            setValueDialog(params);
        }
    }
})(AJS.$);

AJS.Editor.JiraChart.Panels = [];
AJS.MacroBrowser.setMacroJsOverride('jirachart', {opener: AJS.Editor.JiraChart.edit});
