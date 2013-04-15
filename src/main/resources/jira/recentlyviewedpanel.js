AJS.Editor.JiraConnector.Panel.Recent = function(){ };
AJS.Editor.JiraConnector.Panel.Recent.prototype = AJS.$.extend(AJS.Editor.JiraConnector.Panel.Recent.prototype, AJS.Editor.JiraConnector.Panel.prototype);
AJS.Editor.JiraConnector.Panel.Recent.prototype = AJS.$.extend(AJS.Editor.JiraConnector.Panel.Recent.prototype, {

        title: function(){
            return AJS.I18n.getText("insert.jira.issue.recent");
        },
        init: function(panel){
            var servers = AJS.Editor.JiraConnector.servers;
            panel.html('<div id="my-recent-issues" ' + (servers.length > 1 ? 'class="multi-server" ' : '') + '></div>');
            var thiz = this;
            this.selectedServer = servers[0];
            if (servers.length > 1){
                var serverForm = AJS.$('<div class="jira-server-select"><form action="#" method="post" class="aui">' + 
                             '<div class="field-group"><label>Server</label>' + 
                             '<select class="select" ></select>' + 
                             '</div>' +
                             '</form></div>').appendTo('div#my-recent-issues');
                
               
                this.applinkServerSelect(AJS.$('.select', serverForm), function(server){
                	thiz.selectedServer = server;
                	thiz.onselect();
                });
            }
            panel.onselect = function(){
                thiz.onselect();
            }
        },
        insertLink: function(){
            this.insertSelected();
        },
        onselect: function(){
            var thiz = this;
            var container = AJS.$('div#my-recent-issues');
            this.container = container;
            
            var clearPanel = function(){
                container.children().not('.jira-server-select').remove();
            };

            var loadRecentIssues;
            var authCheck = function(){
                if (thiz.selectedServer.authUrl){
                    clearPanel();
                    var oauthForm = thiz.createOauthForm(function(){
                        loadRecentIssues();
                    });
                    container.append(oauthForm);
                }
                else{
                    loadRecentIssues();
                }
            };
            
            loadRecentIssues = function(){
                if (thiz.currentXhr && thiz.currentXhr.readyState != 4){
                    return;
                }
                AJS.$('.select', container).disable();
                clearPanel();
                thiz.createIssueTableFromUrl(container, 
                        thiz.selectedServer.id, 
                        '/sr/jira.issueviews:searchrequest-xml/temp/SearchRequest.xml?jqlQuery=key+in+issueHistory()&tempMax=50&field=summary&field=type&field=link', 
                        thiz.setSelectedIssue, 
                        thiz.insertLink,
                        thiz.disableInsert, null, function(xhr){
                            AJS.$('div.data-table', container).remove();
                            thiz.ajaxError(xhr, authCheck);
                        });
            };
            authCheck();
        }
    });
AJS.Editor.JiraConnector.Panels[2] =new AJS.Editor.JiraConnector.Panel.Recent();