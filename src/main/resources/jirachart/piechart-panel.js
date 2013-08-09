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
        var isMultiServer =  (servers.length > 1);
        //get content from soy template
        var contentJiraChart = Confluence.Templates.ConfluenceJiraPlugin.contentJiraChart({'isMultiServer':isMultiServer, 'servers':servers});
        panel.html(contentJiraChart);
    },
    renderChart: function(imageContainer, params) {
        var url = Confluence.getContextPath() + "/plugins/servlet/jira-chart-proxy?jql=" + params.jql + "&statType=" + params.statType + "&width=" + params.width  + "&appId=" + params.serverId + "&chartType=pie";
        if(params.width !== '') {
            url += "&height=" + parseInt(params.width * 2/3); 
        }
        var img = $("<img />").attr('src',url);
        
        if(params.border === true) {
            img.addClass('img-border');
        } 
        
        img.error(function(){
            imageContainer.html(Confluence.Templates.ConfluenceJiraPlugin.showMessageRenderJiraChart());
            AJS.$('#jira-chart').find('.insert-jira-chart-macro-button').disable();
        }).load(function() {
            var chartImg =  $("<div class='chart-img'></div>").append(img);
            imageContainer.html(chartImg);
            AJS.$('#jira-chart').find('.insert-jira-chart-macro-button').enable();
        });
    }
});

AJS.Editor.JiraChart.Panels.push(new AJS.Editor.JiraChart.Panels.PieChart());