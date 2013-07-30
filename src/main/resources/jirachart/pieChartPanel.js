AJS.Editor.JiraChart.Panels.PieChart = function () {
    
};

AJS.Editor.JiraChart.Panels.PieChart.prototype = AJS.$.extend(AJS.Editor.JiraChart.Panels.PieChart.prototype, AJS.Editor.JiraChart.Panels.prototype);
AJS.Editor.JiraChart.Panels.PieChart.prototype = AJS.$.extend(AJS.Editor.JiraChart.Panels.PieChart.prototype, {
    title: function() {
        return Confluence.Templates.ConfluenceJiraPlugin.pieChartTitle();
    },
    init: function(panel){
        //add body content
        var servers = AJS.Editor.JiraConnector.servers;
        var isMultiServer = false;
        if (servers.length > 1) {
            isMultiServer = true;
        }
        //get content from soy template
        var contentJiraChart = Confluence.Templates.ConfluenceJiraPlugin.contentJiraChart({'isMultiServer':isMultiServer, 'servers':servers});
        panel.html(contentJiraChart);
    }
});

AJS.Editor.JiraChart.Panels.push(new AJS.Editor.JiraChart.Panels.PieChart());