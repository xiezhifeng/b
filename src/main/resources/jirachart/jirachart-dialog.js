AJS.Editor.JiraChart = (function($){
    var insertText = AJS.I18n.getText("insert.jira.issue.button.insert");
    var cancelText = AJS.I18n.getText("insert.jira.issue.button.cancel");
    var popup;
    
    var openJiraChartDialog = function() {
     if (!popup){
            popup = new AJS.ConfluenceDialog({width:840, height: 590, id: "jira-chart"});
            var jiraChartTitle = AJS.I18n.getText("jirachart.macro.popup.title");
            popup.addHeader(jiraChartTitle);
  
            //add body content
            var servers = AJS.Editor.JiraConnector.servers;
            var isMultiServer = false;
            if (servers.length > 1) {
                isMultiServer = true;
            }
            //get content from soy template
            var contentJiraChart = Confluence.Templates.ConfluenceJiraPlugin.contentJiraChart({'isMultiServer':isMultiServer});
            
            popup.addPanel("Panel 1", contentJiraChart);
            popup.get("panel:0").setPadding(0);

            var container = $('#jira-chart #jira-chart-content');

            //add link select macro
            popup.addLink(AJS.I18n.getText("insert.jira.issue.button.select.macro"), function() {
                popup.hide();
                AJS.MacroBrowser.open(false);
            }, "dialog-back-link");
            
            //add button insert dialog
            popup.addButton(insertText, function(){
        	var macroInputParams = getMacroParamsFromDialog(container);
        	insertJiraChartMacroWithParams(macroInputParams);
            }, 'insert-jira-chart-macro-button');
            AJS.$('#jira-chart .insert-jira-chart-macro-button').disable();
            
        //add button cancel
            popup.addCancel(cancelText, function(){
            	AJS.Editor.JiraChart.close();
            });
            
            //bind search button
            $('#jira-chart .jira-chart-search button').bind("click",function() {
           	 doSearch(container);
            });
	     //set action enter for input field
            setActionOnEnter($("input[type='text']", container), doSearch, container);

            //process bind display option
            bindSelectOption(container);
            
    	 }
         popup.show();
    };
    
    var bindSelectOption = function(container) {
	var displayOptsOverlay = container.find('.jira-chart-option.expand');
        displayOptsOverlay.css("top", "445px");
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
    }
    
    var doSearch = function(container) {
	var params = getMacroParamsFromDialog(container);
	
        var url = Confluence.getContextPath() + "/plugins/servlet/jira-chart-proxy?jql=" + params.jql + "&statType=" + params.statType + "&width=" + params.width  + "&border=" + params.border + "&appId=" + params.serverId + "&chartType=" + params.chartType;
        
        var img = $("<img/>").attr('src',url);
        img.error(function(){
            container.find(".jira-chart-img").empty().append(Confluence.Templates.ConfluenceJiraPlugin.showMessageRenderJiraChart());
	    AJS.$('#jira-chart .insert-jira-chart-macro-button').disable();
        }).load(function() {
	    container.find(".jira-chart-img").empty().append(img);
	    AJS.$('#jira-chart .insert-jira-chart-macro-button').enable();
        });
    };
    
    var displayOptPanel = function(container, open) {
        var displayOptsOverlay = container.find('.jira-chart-option.expand');
        if(open) {
            var currentHeighOfOptsOverlay = displayOptsOverlay.height();
            var topMarginDisplayOverlay = 40;
            var currentBottomPosition =  -(currentHeighOfOptsOverlay - topMarginDisplayOverlay);
            displayOptsOverlay.css("top", "");
            displayOptsOverlay.css("bottom", currentBottomPosition + "px");
            displayOptsOverlay.animate({
        	bottom: 2
            }, 500 );
        } else {
            displayOptsOverlay.css("top", displayOptsOverlay.position().top + "px");
            displayOptsOverlay.css("bottom", "");
            displayOptsOverlay.animate({
        	top: 445
            }, 500 );
        }
    };
    
    var getMacroParamsFromDialog = function(container) {
	var jql = container.find("input[name='jiraSearch']").val();
	var statType = container.find("select[name='type']").val();
	var width = container.find("input[name='width']").val().replace("px","");
	var border = container.find("input[name='border']").prop('checked');

	var macroParams = {
		jql: encodeURIComponent(jql),
		statType: statType,
		width: width,
		border: border,
		serverId:  AJS.Editor.JiraConnector.servers[0].id,
		server: AJS.Editor.JiraConnector.servers[0].name,
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
        AJS.Editor.JiraChart.close();
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
    
    return {
        open: openJiraChartDialog,
        
        close: function() {
          popup.hide();
          tinymce.confluence.macrobrowser.macroBrowserCancel();
        },
        
        edit: function() {
            openJiraChartDialog();
        }
    }
})(AJS.$);

AJS.MacroBrowser.setMacroJsOverride('jirachart', {opener: AJS.Editor.JiraChart.edit});
