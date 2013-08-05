AJS.Editor.JiraChart.Panels.PieChart = function () {
};


AJS.Editor.JiraChart.Panels.PieChart.prototype = AJS.$.extend(AJS.Editor.JiraChart.Panels.PieChart.prototype, AJS.JiraIssues);
AJS.Editor.JiraChart.Panels.PieChart.prototype = AJS.$.extend(AJS.Editor.JiraChart.Panels.PieChart.prototype, AJS.Editor.JiraConnector.Panel.prototype);
AJS.Editor.JiraChart.Panels.PieChart.prototype = AJS.$.extend(AJS.Editor.JiraChart.Panels.PieChart.prototype, AJS.Editor.JiraChart.Panels.prototype);
AJS.Editor.JiraChart.Panels.PieChart.prototype = AJS.$.extend(AJS.Editor.JiraChart.Panels.PieChart.prototype, {
    title: function() {
        return Confluence.Templates.ConfluenceJiraPlugin.pieChartTitle();
    },
    init: function(panel){
        var thiz = this;
        //add body content
        var servers = AJS.Editor.JiraConnector.servers;
        var isMultiServer =  (servers.length > 1);
        //get content from soy template
        var contentJiraChart = Confluence.Templates.ConfluenceJiraPlugin.contentJiraChart({'isMultiServer':isMultiServer});
        panel.html(contentJiraChart);

        var container = $('#jira-chart #jira-chart-content');
        if (servers.length > 1) {
            thiz.applinkServerSelect($('#servers'), function(server) {
                AJS.$('.jira-oauth-message-marker', container).remove();
                this.selectedServer = server;
                this.msg = AJS.Editor.JiraConnector.Panel.prototype.msg;
                if(server.authUrl) {
                    var oauForm = thiz.createOauthForm.call(this, function() {
                        AJS.$('.jira-oauth-message-marker', container).remove();
                        AJS.Editor.JiraChart.doSearch(container, true);
                    });
                    container.find('div.jira-chart-search').append(oauForm);
                }
                AJS.Editor.JiraChart.doSearch(container);
            });
        }
    },

    checkOau: function(server, container) {
        var thiz = this;
        container.find('div.jira-oauth-message-marker').remove();
        this.selectedServer = server;
        if(server.authUrl) {
            var oauForm = thiz.createOauthForm.call(this, function() {
                AJS.$('.jira-oauth-message-marker', container).remove();
                AJS.Editor.JiraChart.doSearch(container, true);
            });
            container.find('div.jira-chart-search').append(oauForm);
        }
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