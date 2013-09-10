AJS.Editor.JiraChart.Panels.PieChart = function () {
    
    var checkWidthField = function(val){
        return AJS.Editor.JiraChart.validateWidth(val);
    };
    
    return {
        title: function() {
            return AJS.I18n.getText('jirachart.panel.piechart.title');
        },
        init: function(panel){
            //add body content
            var thiz = this;
            var servers = AJS.Editor.JiraConnector.servers;
            var isMultiServer =  (servers.length > 1);
            //get content from soy template
            var contentJiraChart = Confluence.Templates.ConfluenceJiraPlugin.contentJiraChart({'isMultiServer':isMultiServer});
            panel.html(contentJiraChart);

            if (isMultiServer) {
                AJS.Editor.JiraConnector.Panel.prototype.applinkServerSelect(AJS.$('#jira-chart-servers'), function(server) {
                    thiz.checkOau(AJS.$('#jira-chart-content'), server);
                });
            }
        },
        renderChart: function(imageContainer, params) {
            
            var urlTemplate = AJS.template("{contextPath}/plugins/servlet/jira-chart-proxy?jql={jql}&statType={statType}&appId={serverId}&authenticated={authenticated}&chartType=pie");
                urlTemplate.fill({
                "contextPath": Confluence.getContextPath(), 
                "jql": params.jql,
                "statType": params.statType,
                "serverId": params.serverId,
                "authenticated": params.isAuthenticated
                });
            
            var url = urlTemplate.toString();
            var booleanWidth = checkWidthField(params.width);
            if(booleanWidth) {
                url += "&width=" + params.width + "&height=" + parseInt(params.width * 2/3);
            }

            var img = $("<img />").attr('src',url);
            
            if(params.border === true) {
                img.addClass('jirachart-border');
            } 
            
            img.error(function(){
                imageContainer.html(Confluence.Templates.ConfluenceJiraPlugin.showMessageRenderJiraChart());
                AJS.$('#jira-chart').find('.insert-jira-chart-macro-button').disable();
            }).load(function() {
                var chartImg =  $("<div class='chart-img'></div>").append(img);
                imageContainer.html(chartImg);
                if(!booleanWidth && params.width !== "") {
                    imageContainer.prepend(Confluence.Templates.ConfluenceJiraPlugin.warningValWidthColumn());
                }
                AJS.$('#jira-chart').find('.insert-jira-chart-macro-button').enable();
            });
        },

        checkOau: function(container, server) {
            AJS.$('.jira-oauth-message-marker', container).remove();
            var oauObject = {
                selectedServer: server,
                msg: AJS.Editor.JiraConnector.Panel.prototype.msg
            };

            if(server && server.authUrl) {
                var oauForm = AJS.Editor.JiraConnector.Panel.prototype.createOauthForm.call(oauObject, function() {
                    AJS.$('.jira-oauth-message-marker', container).remove();
                    AJS.Editor.JiraChart.search(container);
                });
                container.find('div.jira-chart-search').append(oauForm);
            }
        }
    };
};

AJS.Editor.JiraChart.Panels.push(new AJS.Editor.JiraChart.Panels.PieChart());