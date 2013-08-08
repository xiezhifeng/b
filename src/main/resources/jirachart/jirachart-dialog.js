AJS.Editor.JiraChart = (function($){
    var insertText = AJS.I18n.getText("insert.jira.issue.button.insert");
    var cancelText = AJS.I18n.getText("insert.jira.issue.button.cancel");
    var JQL_OPERATORS = /=|!=|~|>|<|!~| is | in | was | changed /i;
    var KEY_ISSUE_OPERATORS = /\s*([A-Z][A-Z]+)-[0-9]+\s*/;
    
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
        //bind search button
        container.find('.jira-chart-search button').bind("click",function() {
            doSearch(container);
        });
        
        //bind action enter for input field
        setActionOnEnter(container.find("input[type='text']"), doSearch, container);

        //bind out focus in width field
        container.find("#jira-chart-width").focusout(function() {
            doSearch(container);
         });

        //bind click in border
        container.find("#jira-chart-border").click(function(){
            doSearch(container);
        });

        //for auto convert when paste url
        container.find("#jira-chart-inputsearch").bind('paste', function() {
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

        if(typeof convertToJQL(container) === 'undefined') {
            return;
        }
        
        var imageContainer = container.find(".jira-chart-img"); 

        //load image loading
        imageContainer.empty().append('<div class="loading-data"></div>');
        var imageLoading = imageContainer.find(".loading-data")[0];
        AJS.$.data(imageLoading, "spinner", Raphael.spinner(imageLoading, 50, "#666"));
    
        var params = getMacroParamsFromDialog(container);
        var url = Confluence.getContextPath() + "/plugins/servlet/jira-chart-proxy?jql=" + params.jql + "&statType=" + params.statType + "&width=" + params.width  + "&appId=" + params.serverId + "&chartType=" + params.chartType;
        if(params.width !== '') {
            url += "&height=" + parseInt(params.width * 2/3); 
        }
        var img = $("<img />").attr('src',url);
        
        if(params.border === true) {
            img.addClass('img-border');
        } 
        
        img.error(function(){
            imageContainer.empty().append(Confluence.Templates.ConfluenceJiraPlugin.showMessageRenderJiraChart());
            AJS.$('#jira-chart').find('.insert-jira-chart-macro-button').disable();
        }).load(function() {
            var chartImg =  $("<div class='chart-img'></div>").append(img);
            imageContainer.empty().append(chartImg);
            AJS.$('#jira-chart').find('.insert-jira-chart-macro-button').enable();
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
    
    var convertFilterToJQL = function (textSearch, container) {
        var jql;
        var url = decodeURIComponent(textSearch);
        var serverIndex = AJS.JQLHelper.findServerIndexFromUrl(url, AJS.Editor.JiraConnector.servers);
        if (serverIndex !== -1) {
            container.find("#jira-chart-servers").val(AJS.Editor.JiraConnector.servers[serverIndex].id);
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
            container.find("#jira-chart-servers").val(AJS.Editor.JiraConnector.servers[jiraParams.serverIndex].id);
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
        var textSearch = container.find("#jira-chart-inputsearch").val();
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
            container.find("#jira-chart-inputsearch").val(jql);
        }
        return jql;
    };
    
    var getMacroParamsFromDialog = function(container) {
        var servers = AJS.Editor.JiraConnector.servers;
        var serverId =  servers[0].id;
        var server = servers[0].name;
        if (servers.length > 1) {
            serverId = container.find('#jira-chart-servers').val();
            server = container.find('#jira-chart-servers').find("option:selected").text();
        }

        return {
            jql: encodeURIComponent(container.find('#jira-chart-inputsearch').val()),
            statType: container.find('#jira-chart-statType').val(),
            width: container.find('#jira-chart-width').val().replace("px",""),
            border: container.find('#jira-chart-border').prop('checked'),
            serverId:  serverId,
            server: server,
            chartType: 'pie'
        };
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
        doSearch(container);
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
                return;
            }
            
            var params = macro.params;
            
            openJiraChartDialog();
            popup.gotoPanel(0);
            setValueAndDoSearchInDialog(params);
        }
    };
})(AJS.$);

AJS.Editor.JiraChart.Panels = [];
AJS.MacroBrowser.setMacroJsOverride('jirachart', {opener: AJS.Editor.JiraChart.edit});
