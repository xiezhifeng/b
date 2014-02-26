AJS.Editor.Timeline = (function($) {

    var NOT_SUPPORTED_BUILD_NUMBER = -1;
    var START_JIRA_UNSUPPORTED_BUILD_NUMBER = 6109; //jira version 6.0.8
    var END_JIRA_UNSUPPORTED_BUILD_NUMBER = 6155; //jira version 6.1.1

    var insertText = AJS.I18n.getText("insert.jira.issue.button.insert");
    var cancelText = AJS.I18n.getText("insert.jira.issue.button.cancel");
    var EMPTY_VALUE = "";
    var jqlWhenEnterKeyPress;
    var intRegex = /^\d+$/;
    var popup;

    var openJiraTimelineDialog = function() {
        /*if (!popup) {
            popup = new AJS.ConfluenceDialog({width:840, height: 590, id: "timeline"});
            popup.addHeader("Jira Timeline Macro");
            
            var panels = AJS.Editor.Timeline.Panels;
            
            for (var i = 0; i < panels.length; i++) {
                popup.addPanel(panels[i].title);
                var dlgPanel = popup.getCurrentPanel();
                panels[i].init(dlgPanel);
            }
            
            var $container = $('#jira-chart-content');

            //add link select macro
            popup.addLink(AJS.I18n.getText("insert.jira.issue.button.select.macro"), function () {
                popup.hide();
                AJS.MacroBrowser.open(false);
            }, "dialog-back-link");
            
            //add button insert dialog
            popup.addButton(insertText, function() {
                    var macroInputParams = getMacroParamsFromDialog($container);

                    //if wrong format width, set width is default
                    if (!AJS.Editor.JiraChart.validateWidth(macroInputParams.width)) {
                        macroInputParams.width = EMPTY_VALUE;
                    }

                    insertJiraChartMacroWithParams(macroInputParams);
                    AJS.Editor.JiraChart.close();

            }, 'insert-jira-timeline-macro-button');
            
            //add button cancel
            popup.addCancel(cancelText, function() {
                AJS.Editor.Timeline.close();
            });

        }
        // default to pie chart
        popup.gotoPanel(0);
        popup.show();*/

        if (!popup) {
            popup = new AJS.ConfluenceDialog({width:750, height: 585, id: "jira-timeline"});

            popup.addHeader("Jira Timeline Macro");
            var panels = AJS.Editor.Timeline.Panels;

            for (var i = 0; i < panels.length; i++){
                popup.addPanel(panels[i].title);
                var dlgPanel = popup.getCurrentPanel();
                panels[i].init(dlgPanel);
            }

            popup.addButton(insertText, function(){
                var panelIndex = popup.getCurrentPanel().id;
                var panel = panels[panelIndex];
                panel.insertLinkFromForm();
            }, 'insert-issue-button');

            popup.addCancel(cancelText, function() {
                AJS.Editor.Timeline.close();
            });
            // default to search panel
            popup.gotoPanel(0);

        }
        popup.show();
        popup.gotoPanel(0);
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
    

    var disableInsert = function() {
        AJS.$('#jira-chart').find('.insert-jira-chart-macro-button').disable();
    };

    var enableInsert = function() {
        var $insertButton = AJS.$('#jira-chart').find('.insert-jira-chart-macro-button');
        if ($insertButton.is(":disabled")) {
            $insertButton.enable();
        }
    };


    return {

        close: function() {
          popup.hide();
          tinymce.confluence.macrobrowser.macroBrowserCancel();
        },
        
        edit: function(macro) {
            openJiraTimelineDialog();
        },

        convertFormatWidth : convertFormatWidth,

        disableInsert : disableInsert,

        enableInsert : enableInsert
    };
})(AJS.$);

AJS.Editor.Timeline.Panels = [];
AJS.MacroBrowser.setMacroJsOverride('timeline', {opener: AJS.Editor.Timeline.edit});




