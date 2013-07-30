AJS.Editor.JiraChart.Panels.PieChart = function () {

};

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
        var isMultiServer = false;
        if (servers.length > 1) {
            isMultiServer = true;
        }
        //get content from soy template
        var contentJiraChart = Confluence.Templates.ConfluenceJiraPlugin.contentJiraChart({'isMultiServer':isMultiServer});
        panel.html(contentJiraChart);

        if (servers.length > 1) {
            thiz.applinkServerSelect($('#servers'), function(server) {
                this.selectedServer = server;
                this.msg = AJS.Editor.JiraConnector.Panel.prototype.msg;
                if(server.authUrl) {
                    var oauForm = thiz.createOauthForm.call(this, function() {
                        console.log('success authenticate');
                    });
                    $('form.aui').append(oauForm);
                }
            });
        }
        var server = $('#servers option:selected').data('jiraapplink');
        this.selectedServer = server;
        //this.msg = AJS.Editor.JiraConnector.Panel.prototype.msg;
        if(server.authUrl) {
            var oauForm = thiz.createOauthForm.call(this, function() {
                console.log('success authenticate');
            });
            $('form.aui').append(oauForm);
        }
    }
});

var authCheck = function(server) {
    clearPanel();
    // disable insert when authCheck
    thiz.disableInsert();
    if (server)
        thiz.selectedServer = server;
    if (thiz.selectedServer.authUrl) {
        disableSearch();
        var oauthForm = thiz.createOauthForm(function() {
            clearPanel();
            enableSearch();
        });
        container.append(oauthForm);
    }
    else{
        enableSearch();
        $('.search-help').show();
    }
};

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