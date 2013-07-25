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

var addListServer = function(container){
    var servers = AJS.Editor.JiraConnector.servers;
    AJS.$(servers).each(function(){
        var option = '<option ';
        if (this.selected){
            selectedServer = this;
            option += 'selected="selected"';
        }
        option += 'value="' + this.id + '"></option>';
        option = AJS.$(option);
        option.text(this.name);
        $(container).find("#servers").append(option);
        option.data('jiraapplink', this);
    });
};

AJS.Editor.JiraChart.Panels.push(new AJS.Editor.JiraChart.Panels.PieChart());