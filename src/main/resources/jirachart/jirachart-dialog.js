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

            //add link select macro
            popup.addLink(AJS.I18n.getText("insert.jira.issue.button.select.macro"), function() {
                popup.hide();
                AJS.MacroBrowser.open(false);
            }, "dialog-back-link");
            
            //add button insert dialog
            popup.addButton(insertText, function(){
        	alert("click button insert");
            //TODO: process insert jira chart macro
            }, 'insert-jira-chart-macro-button');
            AJS.$('#jira-chart .insert-jira-chart-macro-button').disable();
            
        //add button cancel
            popup.addCancel(cancelText, function(){
            	AJS.Editor.JiraChart.close();
            });
            
            //bind search button
            var container = $('#jira-chart #jira-chart-content');
            $('#jira-chart .jira-chart-search button').bind("click",function() {
           	 doSearch(container);
            });
            //process bind display option
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
                    expandDisplayOptPanel(container);
                    jQuery(this).addClass('jql-display-opts-close');
                    jQuery(this).removeClass('jql-display-opts-open');
                } else {
                    minimizeDisplayOptPanel(container);
                    jQuery(this).removeClass('jql-display-opts-close');
                    jQuery(this).addClass('jql-display-opts-open');
                }
            });
    	 }
         popup.show();
    }
    
    var doSearch = function(container) {
        var jql = container.find("input[name='jiraSearch']").val();
        var statType = container.find("select[name='type']").val();
        var width = container.find("input[name='width']").val().replace("px","");
        var border = container.find("input[name='border']").prop('checked');
        var url = Confluence.getContextPath() + "/plugins/servlet/jira-chart-proxy?jql=" + encodeURIComponent(jql) + "&statType=" + statType + "&width=" + width  + "&border=" + border + "&appId=" + AJS.Editor.JiraConnector.servers[0].id + "&chartType=pie"
        var img = $("<img/>").attr('src',url);
        img.error(function(){
            container.find(".jira-chart-img").empty().append(Confluence.Templates.ConfluenceJiraPlugin.showMessageRenderJiraChart());
	    AJS.$('#jira-chart .insert-jira-chart-macro-button').disable();
        }).load(function() {
	    container.find(".jira-chart-img").empty().append(img);
	    AJS.$('#jira-chart .insert-jira-chart-macro-button').enable();
        });
    }
    
    var expandDisplayOptPanel = function(container) {
        var displayOptsOverlay = container.find('.jira-chart-option.expand');
        var currentHeighOfOptsOverlay = displayOptsOverlay.height();
        var topMarginDisplayOverlay = 40;
        displayOptsOverlay.css("top", "");
        var currentBottomPosition =  -(currentHeighOfOptsOverlay - topMarginDisplayOverlay);
        displayOptsOverlay.css("bottom", currentBottomPosition + "px");
        displayOptsOverlay.animate({
            bottom: 2
        }, 500 );
    };
    
    var minimizeDisplayOptPanel = function(container) {
        var displayOptsOverlay = container.find('.jira-chart-option.expand');
        displayOptsOverlay.css("top", displayOptsOverlay.position().top + "px");
        displayOptsOverlay.css("bottom", "");
        displayOptsOverlay.animate({
            top: 445
        }, 500 );
    }
    
    return {
        open: function() {
            openJiraChartDialog();
        },
        
        close: function() {
          popup.hide();
          tinymce.confluence.macrobrowser.macroBrowserCancel();
        }
    }
})(AJS.$);

AJS.MacroBrowser.setMacroJsOverride('jirachart', {opener: AJS.Editor.JiraChart.open});
