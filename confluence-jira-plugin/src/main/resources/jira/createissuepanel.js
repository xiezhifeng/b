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

    renderElement: function(field, key) {
        var defaultFields = ["project", "summary", "issuetype", "reporter"];
        var allowFields = ["versions", "components", "assignee"];
        var acceptedFieldsConfig = [{
            name: 'Epic',
            fieldPath: 'schema.custom',
            value: 'com.pyxis.greenhopper.jira:gh-epic-label',
            afterElement: '.type-select'
        },
        {
            name: 'Priority',
            fieldPath: 'schema.system',
            value: 'priority',
            afterElement: '.issue-summary'
        },
        {
            name: 'Versions',
            key: 'versions',
            afterElement: '.issue-summary'
        },
        {
            name: 'Assignee',
            key: 'assignee',
            afterElement: '.issue-summary'
        },
        {
            name: 'Components',
            key: 'components',
            afterElement: '.issue-summary'
        }];

        var getAcceptedFieldConfig = function() {
            var config;
            for(var i=0; i <acceptedFieldsConfig.length; i++) {
                config = acceptedFieldsConfig[i];
                if(config.key === key || (config.value && eval('field.' + config.fieldPath) === config.value)) {
                    return acceptedFieldsConfig[i];
                }
            }
        };

        if((field.required || _.contains(allowFields, key)) && !_.contains(defaultFields, key) && jiraIntegration.fields.canRender(field)) {
            var fieldConfig = getAcceptedFieldConfig();
            if(fieldConfig) {
                $(jiraIntegration.fields.renderField(null, field)).insertAfter($(fieldConfig.afterElement, this.container).parent());
            }
        }
    },

    renderCreateIssuesForm: function(container, fields) {
        var thiz = this;
        $('.jira-field', container).remove();
        $.each(fields, function(key, field) {
            if ((field.name === 'Assignee') || (field.name === 'DueDate')) {
                field.required = true;
            }
            thiz.renderElement(field, key)
            jiraIntegration.fields.attachFieldBehaviors(container, {serverId: '8835b6b9-5676-3de4-ad59-bbe987416662', projectKey: 'TP'}, null);
        });
    },

    fillProjectOptions: function(projectValues) {
        var projects = AJS.$('.project-select', this.container);
        projects.empty();
        var defaultOption = {
            id: -1,
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
            var issueType = AJS.$(Confluence.Templates.ConfluenceJiraPlugin.renderOption({"option": this})).appendTo(issuesType);
            issueType.data("fields", this.fields);
        });
        AJS.$('option:first', issuesType).attr('selected', 'selected');
    },

    bindEvent: function() {
        var thiz = this;
        var projects = AJS.$('.project-select', this.container);
        var types = AJS.$('select.type-select', this.container);

        projects.change(function(){
            var project = AJS.$('option:selected', projects);
            if (project.val() != "-1"){
                AJS.$('option[value="-1"]', projects).remove();
                thiz.appLinkRequest('expand=projects.issuetypes.fields&projectIds=' + project.val(), function(data) {
                    thiz.fillIssuesTypeOptions(types, data.projects[0].issuetypes);
//                    thiz.renderCreateIssuesForm(thiz.container, types.find("option:selected").data("fields"));
                    jiraIntegration.fields.renderFields(
                        thiz.container.find('#jira-required-fields-panel'),
                        $('.issue-summary'),
                        {
                            serverId: '8835b6b9-5676-3de4-ad59-bbe987416662',
                            projectKey: 'TP',
                            issueType: 'Bug'
                        }, 
                        {
                            excludedFields: ['Project', 'Issue Type', 'Summary']
                        },
                        null
                    );
                    if (thiz.summaryOk()){
                        thiz.enableInsert();
                    }
                    thiz.endLoading();
                })
            }
        });

        types.change(function() {
//            thiz.renderCreateIssuesForm(thiz.container, types.find("option:selected").data("fields"));
            jiraIntegration.fields.renderFields(
                thiz.container.find('#jira-required-fields-panel'),
                $('.issue-summary'),
                {
                    serverId: '8835b6b9-5676-3de4-ad59-bbe987416662',
                    projectKey: 'TP',
                    issueType: 'Bug'
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

    //AllowedValuesHandler not support check allowedValues have value or not to render field
    //So add canRender function to AllowedValuesHandler. If have values will render field
    updateAllowedValuesHandler: function() {
        if ( jiraIntegration && jiraIntegration.fields && jiraIntegration.fields.getFieldHandler("priority")){
            jiraIntegration.fields.getFieldHandler("priority")["canRender"]=function(field){
                return field.allowedValues.length > 0;
            }
        }
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
        this.updateAllowedValuesHandler();
        this.bindEvent();
    },

    convertFormToJSON: function($myform){
        if (!jiraIntegration) {
            AJS.logError("Jira integration plugin is missing!");
            return "";
        }

        var createIssuesObj = {};
        createIssuesObj.issues = [];
        
        var issue = {};
        issue.fields = {
            project: {
                id: AJS.$('.project-select option:selected', $myform).val() 
            },
            issuetype: {
                id: AJS.$('.type-select option:selected', $myform).val() 
            },
            summary: AJS.$('.issue-summary', $myform).val(),
            description:  AJS.$('.issue-description', $myform).val()
        }

        $myform.children('#jira-required-fields-panel')
                   .children('.jira-field')
                   .find('input,select,textarea').not(".select2-input")
                   .each(function(index, formElement) {
                var field = AJS.$(formElement);
                issue.fields[field.attr("name")] = jiraIntegration.fields.getJSON(field);
        });

        createIssuesObj.issues.push(issue);
        return JSON.stringify(createIssuesObj);
    },

    insertLink: function(){
        var JIRA_REST_URL = Confluence.getContextPath() + "/rest/jira-integration/1.0/issues";
        var myform = AJS.$('div.create-issue-container form');

        this.startLoading();
        var thiz = this;
        $.ajax({
            type : "POST",
            contentType : "application/json",
            url : JIRA_REST_URL + "?applicationId=" + this.selectedServer.id,
            data : this.convertFormToJSON(myform),
            success: function(data) {
                var key = data && data.issues && data.issues[0] && data.issues[0].issue && data.issues[0].issue.key;
                if (!key) {
                    var errors = AJS.$('.errMsg, .error', data);
                    var ul = AJS.$("<ul></ul>");
                    errors.each(function() {
                        AJS.$('<li></li>').appendTo(ul).text(AJS.$(this).text());
                    });

                    thiz.errorMsg(AJS.$('div.create-issue-container'), AJS.$('<div>' + AJS.I18n.getText("insert.jira.issue.create.error") + ' <a target="_blank" href="' + thiz.selectedServer.url + '" >JIRA</a></div>').append(ul));
                } else {
                    thiz.insertIssueLink(key, thiz.selectedServer.url + '/browse/' + key);
                    thiz.resetIssue();
                }
                thiz.endLoading();
            },
            error: function(xhr, status) {
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
