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

    renderCreateIssuesForm: function(container, fields) {

        if(fields.versions && fields.versions.allowedValues && fields.versions.allowedValues.length > 0)
        {

        }

        if(fields.components && fields.components.allowedValues && fields.components.allowedValues.length > 0)
        {

        }

        var defaultFields = ["project", "summary", "issuetype", "reporter", "assignee", "priority"];
        var acceptedRequiredFields = [{
            name: 'Epic',
            fieldPath: 'schema.custom',
            value: 'com.pyxis.greenhopper.jira:gh-epic-label'
        }]
        $.each(fields, function(key, field) {
            if(field.required && !_.contains(defaultFields, key) && field.schema.custom === 'com.pyxis.greenhopper.jira:gh-epic-label') {
                $("#create-issues-form", container).append(jiraIntegration.fields.renderField(null, field));
            }
        });
    },

    renderProjectsSelect: function() {

    },

    loadProjects: function(){
        this.startLoading();
        this.disableInsert();
        var thiz = this;
        AppLinks.makeRequest({
                appId: thiz.selectedServer.id,
                type: 'GET',
                url: '/rest/api/2/issue/createmeta?expand=projects.issuetypes.fields',
                dataType: 'json',
                success: function(data){
                    var container = thiz.container;
                    var projects = AJS.$('.project-select', container);
                    projects.children().remove();
                    
                    AJS.$(data.projects).each(function(){
                        var project = AJS.$(Confluence.Templates.ConfluenceJiraPlugin.renderOption({"option": this})).appendTo(projects);
                        project.data("issuesType", this.issuetypes);
                    });
                    projects.prepend('<option value="-1" selected>'+AJS.I18n.getText("insert.jira.issue.create.select.project.hint")+'</option>');
                    
                    AJS.$('.type-select', container).disable();
                    projects.unbind();
                    projects.change(function(){
                        var project = AJS.$('option:selected', projects);
                        if (project.val() != "-1"){
                            AJS.$('option[value="-1"]', projects).remove();
                            AJS.$('.type-select option', container).remove();

                            var types = AJS.$('select.type-select', container);
                            types.unbind();

                            AJS.$(project.data("issuesType")).each(function(){
                                var issueType = AJS.$(Confluence.Templates.ConfluenceJiraPlugin.renderOption({"option": this})).appendTo(types);
                                issueType.data("fields", this.fields);
                            });

                            AJS.$('option:first', types).attr('selected', 'selected');
                            
                            var updateForType = function(){
                                var issueTypeOption = AJS.$('option:selected', types);
                                thiz.renderCreateIssuesForm(container, issueTypeOption.data("fields"));
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

    convertFormToJSON: function($myform){
        var data = {};
        data.summary = AJS.$('.issue-summary', $myform).val();
        data.projectId = AJS.$('.project-select option:selected', $myform).val();
        data.issueTypeId = AJS.$('.type-select option:selected', $myform).val();
        data.description = AJS.$('.issue-description', $myform).val();

        if (jiraIntegration){
            $myform.children('.jira-field').each(function(index, formElement){
                var fieldParent = AJS.$(formElement);
                var fieldId = fieldParent.data('jira-type');
                var field = AJS.$("#"+fieldId, fieldParent);
                if (field){
                    if(!data.fields){
                        data.fields = {};
                    }
                    var json = JSON.stringify(jiraIntegration.fields.getJSON(field));
                    data.fields[fieldId] = JSON.stringify(jiraIntegration.fields.getJSON(field));
                }
            });
        }
        var list = [];
        list.push(data);
        return JSON.stringify(list);
    },

    insertLink: function(){

        var JIRA_REST_URL = Confluence.getContextPath() + "/rest/jiraanywhere/1.0";
        var myform = AJS.$('div.create-issue-container form');
        
        var createIssueUrl = '/rest/api/2/issue';
        this.startLoading();
        var thiz = this;
        $.ajax({
            type : "POST",
            contentType : "application/json",
            url : JIRA_REST_URL + "/jira-issue/create-jira-issues/" + this.selectedServer.id,
            data : this.convertFormToJSON(myform),
            success: function(data){

                if (!data || !data[0] || !data[0].key){
                    var errors = AJS.$('.errMsg, .error', data);
                    var ul = AJS.$("<ul></ul>");
                    errors.each(function(){
                        AJS.$('<li></li>').appendTo(ul).text(AJS.$(this).text());
                    });
                    
                    thiz.errorMsg(AJS.$('div.create-issue-container'), AJS.$('<div>' + AJS.I18n.getText("insert.jira.issue.create.error") + ' <a target="_blank" href="' + thiz.selectedServer.url + '" >JIRA</a></div>').append(ul));
                }
                else{
                    var key = data[0].key;
                    thiz.insertIssueLink(key, thiz.selectedServer.url + '/browse/' + key);
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
