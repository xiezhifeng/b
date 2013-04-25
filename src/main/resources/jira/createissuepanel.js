AJS.Editor.JiraConnector.Panel.Create = function(){
    
}
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
    	AJS.$('.oauth-message', this.container).remove();
    	AJS.$('div.field-group', this.container).show();
    	this.resetForm();
        this.loadProjects();
    },
    showOauthChallenge: function(){
    	 AJS.$('div.field-group', this.container).not('.servers').hide();
    	 AJS.$('.oauth-message', this.container).remove();
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
        this.startLoading();
        var thiz = this;
        var container = this.container;
        populateRequest = AppLinks.makeRequest({
            appId: thiz.selectedServer.id,
            type: 'GET',
            url: '/secure/CreateIssue.jspa?pid=' + pid + '&issuetype=' + issuetype,
            dataType: 'html',
            success: function(data){
                thiz.endLoading();
                var createForm = AJS.$('form[action$="CreateIssueDetails.jspa"]', data);
                var versions = AJS.$('select[name="versions"] option', createForm).not('[value="-1"]');
                var components = AJS.$('select[name="components"] option', createForm).not('[value="-1"]');
                var reporter = AJS.$('input[name="reporter"],select[name="reporter"]', createForm).val();
                var priority = AJS.$('select[name="priority"]', createForm).val();
                
                var typeSelectParent = AJS.$('div.type-select-parent', container);
                if (versions.length){
                    var select = AJS.$('.version-select', container);
                    select.parent().show();
                    select.append(versions);
                }
                if (components.length){
                    var select = AJS.$('.component-select', container);
                    select.parent().show();
                    select.append(components);
                }
                
                if (reporter){
                    AJS.$('form', container).append('<input type="hidden" name="reporter" value="' + reporter + '" />');
                }
                AJS.$('form', container).append('<input type="hidden" name="assignee" value="-1" />');
                if (priority){
                    AJS.$('form', container).append('<input type="hidden" name="priority" value="' + priority + '" />');
                }
            },
            error:function(xhr){
                thiz.ajaxAuthCheck(xhr);
            }
        });
    },
    
    loadProjects: function(){
        this.startLoading();
        this.disableInsert();
        
        var thiz = this;
        var issueTypes = {};
        var projectsById = {};
        var schemes = {};
        
        AppLinks.makeRequest({
                appId: thiz.selectedServer.id,
                type: 'GET',
                url: '/rest/api/1.0/admin/issuetypeschemes.json',
                dataType: 'json',
                success: function(data){
                    var container = thiz.container;
                   
                    AJS.$(data.types).each(function(){
                        issueTypes[this.id] = this;
                    });
                    
                    var projects = AJS.$('.project-select', container);
                    
                    // there is a slight chance of multiple requests being made via selenium or bad programming.
                    // this prevents the project list from getting duplicate options
                    projects.children().remove();
                    
                    AJS.$(data.projects).each(function(){
                        projectsById[this.id] = this;
                        var project = AJS.$('<option value="' + this.id + '"></option>').appendTo(projects);
                        project.text(this.name);
                    });
                    projects.prepend('<option value="-1" selected>Select a Project</option>');
                    AJS.$(data.schemes).each(function(){
                        schemes[this.id] = this;
                    });
                    
                    AJS.$('.type-select', container).disable();
                    projects.unbind();
                    projects.change(function(){
                        var project = AJS.$('option:selected', projects);
                        if (project.val() != "-1"){
                            AJS.$('option[value="-1"]', projects).remove();
                            
                            var projectScheme = schemes[projectsById[project.val()].scheme];
                            AJS.$('.type-select option', container).remove();

                            var types = AJS.$('select.type-select', container);
                            types.unbind();
                            AJS.$(projectScheme.types).each(function(){
                                var issueType = issueTypes[this];
                                if (issueType){
                                    var opt = AJS.$('<option value="' + issueType.id + '"></option>').appendTo(types);
                                    opt.text(issueType.name);
                                }
                                    
                            });
                            AJS.$('option:first', types).attr('selected', 'selected');
                            
                            var pid = project.val();
                            
                            var updateForType = function(){
                                var issuetype = AJS.$('option:selected', types).val();
                                thiz.populateForm(pid, issuetype);
                            }
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
        
        container.append(
                 '<form action="#" method="post" class="aui">' + 
                 '<div class="loading-blanket" style="display:none"><div class="loading-data"></div></div>' + 
                 '<div class="field-group servers"><label>Server</label>' + 
                 '<select class="select server-select"></select>' + 
                 '</div>' +
                 '<div class="field-group project-select-parent" ><label>Project</label>' + 
                 '<select class="select project-select" name="pid"></select>' + 
                 '</div>' +
                 '<div class="field-group type-select-parent" ><label>Issue Type</label>' + 
                 '<select class="select type-select" name="issuetype"></select></div>' + 
                 '<div class="field-group"><label>Summary</label>' + 
                 '<input class="text issue-summary" type="text" name="summary"/></div>' + 
                 '<div style="display:none" class="field-group component-parent" ><label>Component/s</label>' + 
                 '<select class="select component-select" multiple="multiple" size="3" name="components" ></select></div>' +
                 '<div style="display:none" class="field-group version-parent" ><label>Version/s</label>' + 
                 '<select class="select version-select" multiple="multiple" size="3" name="versions"></select></div>'+
                 '<div class="field-group"><label>Description</label>' + 
                 '<textarea class="issue-description textarea" rows="5" name="description"/>' + 
                 '</div></form>');
        
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
        })
       
        this.showSpinner(AJS.$('.loading-data', container)[0], 50, true, true);
        
        var insertClick = function(){
        	AJS.$('.insert-issue-button:enabled').click();
        }
        
        this.setActionOnEnter(summary, insertClick);
        
        panel.onselect=function(){
            thiz.onselect();
        }
    },
    insertLink: function(){
        var myform = AJS.$('div.create-issue-container form');
        var createIssueUrl = '/secure/CreateIssueDetails.jspa?' + myform.serialize();
        this.startLoading();
        var thiz = this;
        AppLinks.makeRequest({
            appId: this.selectedServer.id,
            type: 'GET',
            url: createIssueUrl,
            dataType: 'html',
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
