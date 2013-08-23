AJS.Editor.JiraChart.Panels.PieChart = function () {
    
    var getTotalIssue = function(serverId, jql) {
        var totalIssue;
        AJS.$.ajax({
            dataType : 'text',
            url: Confluence.getContextPath() + '/rest/jiraanywhere/1.0/servers/applink/' + serverId + '/jql/'  + jql + '/totalissue',
            async: false
        }).done(function (result) {
            totalIssue = result;
        }).fail(function(jqXHR, textStatus) {
            console.log( "Request failed: " + textStatus );
        });
        if(!totalIssue) {
            totalIssue = "X issue";
        }
        return totalIssue;
    };
    
    var getUrlServerById = function(appLinkId) {
        var servers = AJS.Editor.JiraConnector.servers;
        for (var i = 0; i < servers.length; i++) {
           if(servers[i].id === appLinkId) {
               return servers[i].url;
           } 
        };
    }
    
    return {
        title: function() {
            return Confluence.Templates.ConfluenceJiraPlugin.pieChartTitle();
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
            var url = Confluence.getContextPath() + "/plugins/servlet/jira-chart-proxy?jql=" + params.jql + "&statType="
                + params.statType + "&width=" + params.width  + "&appId=" + params.serverId + "&authenticated=" + params.isAuthenticated + "&chartType=pie";
            if(params.width !== '') {
                url += "&height=" + parseInt(params.width * 2/3); 
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
                if(params.showinfor == true) {
                    var urlIssue = getUrlServerById(params.serverId) + '/issues/?jql=' + params.jql;
                    var totalIssue = getTotalIssue(params.serverId, params.jql);
                    showInfor =  Confluence.Templates.ConfluenceJiraPlugin.showInforInJiraChart({'urlIssue': urlIssue, 'totalIssue': totalIssue, 'staticType': params.statType});
                    if(params.width > 900) {
                        showInfor = $(showInfor).width(params.width + 'px')
                    } 
                    chartImg.append(showInfor);
                }
                imageContainer.html(chartImg);
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