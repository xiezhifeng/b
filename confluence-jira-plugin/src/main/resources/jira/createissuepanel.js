AJS.Editor.JiraConnector.Panel.Create = function(){};

AJS.Editor.JiraConnector.Panel.Create.prototype = AJS.$.extend(AJS.Editor.JiraConnector.Panel.Create.prototype, AJS.Editor.JiraConnector.Panel.prototype);
AJS.Editor.JiraConnector.Panel.Create.prototype = AJS.$.extend(AJS.Editor.JiraConnector.Panel.Create.prototype, {
    resetProject: function(){
        var components = AJS.$('.component-select', this.container);
        var versions = AJS.$('.version-select', this.container);
        components.children().remove();
        versions.children().remove();
        components.parent().hide();
        versions.parent().hide();
        AJS.$('input[type="hidden"]', this.container).remove();
    },
    setSummary: function(summary){
    	AJS.$('.issue-summary', this.container).val(summary);
    },
    resetIssue: function(){
    	AJS.$('.issue-summary', this.container).val('').focus();
    	AJS.$('.issue-description', this.container).val('');
    },
    resetForm: function(){
    	var container = this.container;
        AJS.$('.project-select', container).children().remove();
        AJS.$('.type-select', container).children().remove();
        this.resetProject();
    },
    authCheck: function(server){
    	this.selectedServer = server;
        if (this.selectedServer.authUrl){
            this.showOauthChallenge();
        }
        else{
        	this.serverSelect();
        }
    },
    ajaxAuthCheck: function(xhr){
        var thiz = this;
        this.endLoading();
        this.ajaxError(xhr, function(){thiz.authCheck(thiz.selectedServer);});
    },
    serverSelect: function(){
    	AJS.$('.jira-oauth-message-marker', this.container).remove();
    	AJS.$('div.field-group', this.container).show();
    	this.resetForm();
        this.loadProjects();
    },
    showOauthChallenge: function(){
    	 AJS.$('div.field-group', this.container).not('.servers').hide();
    	 AJS.$('.jira-oauth-message-marker', this.container).remove();
    	 var thiz = this;
    	 var oauthForm = this.createOauthForm(function(){
    		 thiz.serverSelect();
         });
         this.container.append(oauthForm);
    },
    summaryOk: function(){
        return AJS.$('.issue-summary', this.container).val().replace('\\s', '').length > 0;
    },
    projectOk: function(){
        var project = AJS.$('.project-select option:selected', this.container).val();
        return project && project.length && project != "-1";
    },
    setButtonState: function(){
        if (this.summaryOk() && this.projectOk()){
            this.enableInsert();
            return true;
        }
        else{
            this.disableInsert();
            return false;
        }
    },
    
    startLoading: function(){
        this.removeError(this.container);
        AJS.$('.loading-blanket', this.container).show();
        AJS.$('input,select,textarea', this.container).disable();
        this.disableInsert();
    },
    endLoading: function(){
        AJS.$('.loading-blanket', this.container).hide();
        AJS.$('input,select,textarea', this.container).enable();
        this.setButtonState();
    },
    
    populateForm: function(pid, issuetype){
        this.resetProject();
      
        var thiz = this;
        var container = this.container;
      
        var versions = issuetype.fields.versions.allowedvalues;
        var components = issuetype.fields.components.allowedvalues;

        if (versions.length){
            var select = AJS.$('.version-select', container);
            select.parent().show();
            select.append(Confluence.Templates.ConfluenceJiraPlugin.renderOptions(versions));
        }
        if (components.length){
            var select = AJS.$('.component-select', container);
            select.parent().show();
            select.append(Confluence.Templates.ConfluenceJiraPlugin.renderOptions(components));
        }
    },
    
    loadProjects: function(){
        this.startLoading();
        this.disableInsert();

        var thiz = this;
        var projectsById = {};

        AppLinks.makeRequest({
                appId: thiz.selectedServer.id,
                type: 'GET',
                url: '/rest/api/2/issue/createmeta?expand=projects.issuetypes.fields',
                dataType: 'json',
                success: function(data){
                    var container = thiz.container;

                    var projects = AJS.$('.project-select', container);
                    
                    // there is a slight chance of multiple requests being made via selenium or bad programming.
                    // this prevents the project list from getting duplicate options
                    projects.children().remove();
                    
                    AJS.$(data.projects).each(function(){
                        projectsById[this.id] = this;
                        var project = AJS.$('<option value="' + this.id + '"></option>').appendTo(projects);
                        project.text(this.name);
                    });
                    projects.prepend('<option value="-1" selected>'+AJS.I18n.getText("insert.jira.issue.create.select.project.hint")+'</option>');
                    
                    AJS.$('.type-select', container).disable();
                    projects.unbind();
                    projects.change(function(){
                        var project = AJS.$('option:selected', projects);
                        if (project.val() != "-1"){
                            AJS.$('option[value="-1"]', projects).remove();

                            var issuetypes = projectsById[project.val()].issuetypes;
                            AJS.$('.type-select option', container).remove();

                            var types = AJS.$('select.type-select', container);
                            types.unbind();
                            AJS.$(issuetypes).each(function(){
                                    var issueType = this;
                                    var opt = AJS.$('<option value="' + issueType.id + '"></option>').appendTo(types);
                                    opt.text(issueType.name);
                                });

                            AJS.$('option:first', types).attr('selected', 'selected');
                            
                            var pid = project.val();
                            
                            var updateForType = function(){
                                var issuetype = projectsById[pid].issueTypes[AJS.$('option:selected', types).val()];
                                thiz.populateForm(pid, issuetype);
                            };
                            AJS.$('.type-select', container).enable();
                            updateForType();
                            types.change(updateForType);
                            
                            if (thiz.summaryOk()){
                                thiz.enableInsert();
                            }
                        }
                    });
                    thiz.endLoading();
                    projects.focus();
                },
                error:function(xhr){
                    thiz.ajaxAuthCheck(xhr);
                }
        });
    },
    
    title: function(){
        return AJS.I18n.getText("insert.jira.issue.create");
    },
    
    init: function(panel){
        
        panel.html('<div class="create-issue-container"></div>');
        this.container = AJS.$('div.create-issue-container');
        var container = this.container;
        var servers = AJS.Editor.JiraConnector.servers;
        this.selectedServer = servers[0];
        container.append(Confluence.Templates.ConfluenceJiraPlugin.createIssuesForm());
        
        var thiz = this;
        var serverSelect = AJS.$('select.server-select', container);                     
        if (servers.length > 1){
            this.applinkServerSelect(serverSelect, function(server){thiz.authCheck(server);});
        }
        else{
            serverSelect.parent().remove();
        }
        
        var summary = AJS.$('.issue-summary', container);
        summary.keyup(function(){
            thiz.setButtonState();
        });
       
        this.showSpinner(AJS.$('.loading-data', container)[0], 50, true, true);
        
        var insertClick = function(){
            AJS.$('.insert-issue-button:enabled').click();
        };
        
        this.setActionOnEnter(summary, insertClick);
        
        panel.onselect=function(){
            thiz.onselect();
        };
    },

    insertLink: function(){
        var myform = AJS.$('div.create-issue-container form');
        
        var createIssueUrl = '/rest/api/2/issue';
        this.startLoading();
        var thiz = this;
        $.ajax({
            type : "POST",
            contentType : "application/json",
            url : JIRA_REST_URL + "/jira-issue/create-jira-issues/" + this.selectedServer.id,
            data : JSON.stringify(myform.serializeObject()),
            success: function(data){
                var key = AJS.$('#key-val', data);
                if (!key.length){
                    key = AJS.$('#issuedetails a[id^="issue_key"]', data);
                }
                if (!key.length){
                    var errors = AJS.$('.errMsg, .error', data);
                    var ul = AJS.$("<ul></ul>");
                    errors.each(function(){
                        AJS.$('<li></li>').appendTo(ul).text(AJS.$(this).text());
                    });
                    
                    thiz.errorMsg(AJS.$('div.create-issue-container'), AJS.$('<div>' + AJS.I18n.getText("insert.jira.issue.create.error") + ' <a target="_blank" href="' + thiz.selectedServer.url + '" >JIRA</a></div>').append(ul));
                }
                else{
                    thiz.insertIssueLink(key.text(), thiz.selectedServer.url + '/browse/' + key.text());
                    thiz.resetIssue();
                }
                thiz.endLoading();
            },
            error:function(xhr, status){
                thiz.ajaxAuthCheck(xhr);
            }
        });
    },
    onselect: function(){
        var container = this.container;
    
        // first time viewing panel or they may have authed on a different panel
        if (!AJS.$('.project-select option', container).length || AJS.$('.oauth-message', container).length){
            this.authCheck(this.selectedServer);
        }
        if (this.setButtonState() || this.projectOk()){
            // added the timeout because chrome is too fast. It calls this before the form appears. 
            window.setTimeout(function(){
                AJS.$('.project-select', this.container).focus();
            	AJS.$('.issue-summary', this.container).focus();
            }, 0);
        }
        
    }
});
AJS.Editor.JiraConnector.Panels.push(new AJS.Editor.JiraConnector.Panel.Create());
