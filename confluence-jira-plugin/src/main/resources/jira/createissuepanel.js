AJS.Editor.JiraConnector.Panel.Create = function(){};

AJS.Editor.JiraConnector.Panel.Create.prototype = AJS.$.extend(AJS.Editor.JiraConnector.Panel.Create.prototype, AJS.Editor.JiraConnector.Panel.prototype);
AJS.Editor.JiraConnector.Panel.Create.prototype = AJS.$.extend(AJS.Editor.JiraConnector.Panel.Create.prototype, {
    setSummary: function(summary){
        AJS.$('.issue-summary', this.container).val(summary);
    },
    resetIssue: function(){
        AJS.$('.issue-summary', this.container).empty();
        AJS.$('.issue-description', this.container).empty();
    },
    resetForm: function(){
        var container = this.container;
        AJS.$('.project-select', container).empty();
        AJS.$('.type-select', container).empty();
        $('.jira-field', container).remove();
    },
    authCheck: function(server){
        this.selectedServer = server;
        if (this.selectedServer.authUrl){
            this.showOauthChallenge();
        } else {
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
        AJS.$('.loading-blanket', this.container).removeClass("hidden");
        AJS.$('input,select,textarea', this.container).disable();
        this.disableInsert();
    },
    endLoading: function(){
        AJS.$('.loading-blanket', this.container).addClass("hidden");
        AJS.$('input,select,textarea', this.container).enable();
        this.setButtonState();
    },

    fillProjectOptions: function(projectValues) {
        var projects = AJS.$('.project-select', this.container);
        projects.empty();
        var defaultOption = {
            id: -1,
            key: '',
            name: AJS.I18n.getText("insert.jira.issue.create.select.project.hint")
        };
        projects.append(Confluence.Templates.ConfluenceJiraPlugin.renderOption({"option": defaultOption}));
        AJS.$(projectValues).each(function(){
            var project = AJS.$(Confluence.Templates.ConfluenceJiraPlugin.renderOption({"option": this})).appendTo(projects);
            project.data("issuesType", this.issuetypes);
        });

        this.endLoading();
        projects.focus();
    },

    fillIssuesTypeOptions: function(issuesType, issuesTypeValues) {
        issuesType.empty();
        AJS.$(issuesTypeValues).each(function(){
            var option = $.extend({key: this.name}, this);
            var issueType = AJS.$(Confluence.Templates.ConfluenceJiraPlugin.renderOption({"option": option})).appendTo(issuesType);
            issueType.data("fields", this.fields);
        });
        AJS.$('option:first', issuesType).attr('selected', 'selected');
    },

    bindEvent: function() {
        var thiz = this;
        var serverId = this.selectedServer.id;
        var projects = AJS.$('.project-select', this.container);
        var types = AJS.$('select.type-select', this.container);

        projects.change(function(){
            var project = AJS.$('option:selected', projects);
            if (project.val() != "-1"){
                AJS.$('option[value="-1"]', projects).remove();
                thiz.appLinkRequest('expand=projects.issuetypes.fields&projectIds=' + project.val(), function(data) {
                    var firstProject = data.projects[0];
                    thiz.fillIssuesTypeOptions(types, firstProject.issuetypes);
                    jiraIntegration.fields.renderFields(
                        thiz.container.find('#jira-required-fields-panel'),
                        $('.issue-summary'),
                        {
                            serverId: serverId,
                            projectKey: firstProject.key,
                            issueType: firstProject.issuetypes[0].name
                        }, 
                        {
                            excludedFields: ['Project', 'Issue Type', 'Summary']
                        },
                        null
                    );
                    if (thiz.summaryOk()) {
                        thiz.enableInsert();
                    }
                    thiz.endLoading();
                })
            }
        });

        types.change(function() {
            jiraIntegration.fields.renderFields(
                thiz.container.find('#jira-required-fields-panel'),
                $('.issue-summary'),
                {
                    serverId: serverId,
                    projectKey: $(projects.find("option:selected")[0]).attr('jira-data-option-key'),
                    issueType: $(types.find("option:selected")[0]).attr('jira-data-option-key')
                }, 
                {
                    excludedFields: ['Project', 'Issue Type', 'Summary']
                },
                null
            );
        });
    },

    appLinkRequest: function(queryParam, success) {
        var thiz = this;
        thiz.startLoading();
        AppLinks.makeRequest({
            appId: this.selectedServer.id,
            type: 'GET',
            url: '/rest/api/2/issue/createmeta?' + queryParam,
            dataType: 'json',
            success: success,
            error:function(xhr){
                thiz.ajaxAuthCheck(xhr);
            }
        });
    },

    loadProjects: function(){
        var thiz = this;
        this.appLinkRequest('expand=projects', function(data) {
            thiz.fillProjectOptions(data.projects);
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
        this.bindEvent();
    },

    convertFormToJSON: function($myform){
        var data = {};
        data.summary = AJS.$('.issue-summary', $myform).val();
        data.projectId = AJS.$('.project-select option:selected', $myform).val();
        data.issueTypeId = AJS.$('.type-select option:selected', $myform).val();
        data.description = AJS.$('.issue-description', $myform).val();

        if (jiraIntegration){
            $myform.children('.jira-field').find('input,select,textarea').each(function(index, formElement){
                var field = AJS.$(formElement);
                if (field){
                    if(!data.fields){
                        data.fields = {};
                    }
                    var jsonString = jiraIntegration.fields.getJSON(field);
                    if (jsonString instanceof Object)
                    {
                        jsonString = JSON.stringify(jiraIntegration.fields.getJSON(field));
                    }
                    data.fields[field.attr("name")] = jsonString;
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
    }
});
AJS.Editor.JiraConnector.Panels.push(new AJS.Editor.JiraConnector.Panel.Create());
