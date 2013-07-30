AJS.Editor.JiraChart = (function($){
    var insertText = AJS.I18n.getText("insert.jira.issue.button.insert");
    var cancelText = AJS.I18n.getText("insert.jira.issue.button.cancel");
    var popup;
    
    var openJiraChartDialog = function() {
     if (!popup) {
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
            
            var container = $('#jira-chart #jira-chart-content');

            //process bind display option
            bindSelectOption(container);
    	 }
         popup.show();
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
