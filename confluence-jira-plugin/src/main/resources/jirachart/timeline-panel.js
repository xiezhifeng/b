AJS.Editor.JiraChart.Panels.Timeline = function() {

    var TIMELINE_TITLE = AJS.I18n.getText('jirachart.panel.timeline.title');

    return {
        title : TIMELINE_TITLE,

        init : function(panel) {
            // get content from soy template
            var contentJiraChart = Confluence.Templates.ConfluenceJiraPlugin.contentJiraTimeline({
                'isMultiServer' : AJS.Editor.JiraConnector.servers.length > 1
            });
            panel.html(contentJiraChart);
        }
    };
};

AJS.Editor.JiraChart.Panels.push(new AJS.Editor.JiraChart.Panels.Timeline());