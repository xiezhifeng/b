AJS.Editor.JiraChart.Panels.AreaChart = function () {
};

AJS.Editor.JiraChart.Panels.AreaChart.prototype = AJS.$.extend(AJS.Editor.JiraChart.Panels.AreaChart.prototype, AJS.Editor.JiraChart.Panels.prototype);
AJS.Editor.JiraChart.Panels.AreaChart.prototype = AJS.$.extend(AJS.Editor.JiraChart.Panels.AreaChart.prototype, {
    title: function() {
        return Confluence.Templates.ConfluenceJiraPlugin.areaChartTitle();
    },
    init: function(panel){
    }
});
AJS.Editor.JiraChart.Panels.push(new AJS.Editor.JiraChart.Panels.AreaChart());