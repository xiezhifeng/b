AJS.Editor.JiraConnector.Panel.Create = function(){};

AJS.Editor.JiraConnector.Panel.Create.prototype = AJS.$.extend(AJS.Editor.JiraConnector.Panel.Create.prototype, AJS.Editor.JiraConnector.Panel.prototype);
AJS.Editor.JiraConnector.Panel.Create.prototype = AJS.$.extend(AJS.Editor.JiraConnector.Panel.Create.prototype, {
    DEFAULT_PROJECT_VALUE: "-1",
    SHOW_MESSAGE_ON_TOP: true,
    EXCLUDED_FIELDS: ['project', 'issuetype', 'summary', 'description'],
    PROJECTS_META: {},
    hasUnsupportedFields: false,
    setSummary: function(summary) {
        AJS.$('.issue-summary', this.container).val(summary);
    },
    resetIssue: function() {
        AJS.$('.issue-summary', this.container).empty();
        AJS.$('.issue-description', this.container).empty();
    },
    resetForm: function() {
        var container = this.container;
        AJS.$('.project-select', container).empty();
        AJS.$('.type-select', container).empty();
        AJS.$('.jira-field', container).remove();
        this.removeError(container);
    },
    focusForm: function() {
        var $server = AJS.$('select.server-select', this.container);
        if($server.length) {
            $server.focus();
        } else {
            var $projects = AJS.$('.project-select', this.container);
            $projects.length && $projects.focus();
        }
    },
    authCheck: function(server){
        this.selectedServer = server;
        if (this.selectedServer.authUrl){
            this.showOauthChallenge();
        } else {
            this.serverSelect();
        }
    },
    ajaxAuthCheck: function(xhr) {
        var thiz = this;
        this.endLoading();
        this.ajaxError(xhr, function() {
            thiz.authCheck(thiz.selectedServer);
        });
    },
    serverSelect: function() {
        AJS.$('.jira-oauth-message-marker', this.container).remove();
        AJS.$('div.field-group', this.container).show();
        this.loadProjects();
    },
    showOauthChallenge: function() {
        AJS.$('div.field-group', this.container).not('.servers').hide();
        AJS.$('.jira-oauth-message-marker', this.container).remove();
        var thiz = this;
        var oauthForm = this.createOauthForm(function() {
            thiz.serverSelect();
         });
        this.container.append(oauthForm);
    },
    projectOk: function() {
        var project = AJS.$('.project-select option:selected', this.container).val();
        return project && project.length && project != this.DEFAULT_PROJECT_VALUE;
    },
    setInsertButtonState: function() {
        if (!this.hasUnsupportedFields && this.projectOk()) {
            this.enableInsert();
            return true;
        } else {
            this.disableInsert();
            return false;
        }
    },

    startLoading: function() {
        this.removeError(this.container);
        AJS.$('.loading-blanket', this.container).removeClass("hidden");
        AJS.$('input,select,textarea', this.container).disable();
        this.disableInsert();
    },
    endLoading: function() {
        AJS.$('.loading-blanket', this.container).addClass("hidden");
        AJS.$('input,select,textarea', this.container).enable();

        // Disable issue type select box
        if (AJS.$('.project-select', this.container).val() === this.DEFAULT_PROJECT_VALUE) {
             AJS.$('.type-select', this.container).disable();
        }

        this.setInsertButtonState();
    },

    fillProjectOptions: function(projectValues) {
        var thiz = this;
        var $projects = AJS.$('.project-select', this.container);
        $projects.empty();
        var defaultOption = {
            id: thiz.DEFAULT_PROJECT_VALUE,
            key: '',
            name: AJS.I18n.getText("insert.jira.issue.create.select.project.hint")
        };
        $projects.append(Confluence.Templates.ConfluenceJiraPlugin.renderOption({"option": defaultOption}));
        AJS.$(projectValues).each(function() {
            var project = AJS.$(Confluence.Templates.ConfluenceJiraPlugin.renderOption({"option": this})).appendTo($projects);
            project.data("issuesType", this.issuetypes);
        });

        this.endLoading();
        $projects.focus();
    },

    fillIssuesTypeOptions: function(issuesType, issuesTypeValues) {
        issuesType.empty();
        AJS.$(issuesTypeValues).each(function() {
            if (!this.subtask) {
                var option = $.extend({key: this.name}, this);
                var issueType = AJS.$(Confluence.Templates.ConfluenceJiraPlugin.renderOption({"option": option})).appendTo(issuesType);
                issueType.data("fields", this.fields);
            }
        });
        AJS.$('option:first', issuesType).attr('selected', 'selected');
    },

    getCurrentJiraCreateIssueUrl: function() {
        var $projects = AJS.$('.project-select', this.container);
        var $types = AJS.$('select.type-select', this.container);
        var projectId = $projects.find("option:selected").first().val();
        var issueTypeId = $types.find("option:selected").first().val();
        return this.selectedServer.url + "/secure/CreateIssueDetails!Init.jspa?pid=" + projectId + "&issuetype=" + issueTypeId;
    },

    showUnsupportedFieldsMessage: function(unsupportedFields) {
        this.hasUnsupportedFields = true;
        this.disableInsert();
        var unsupportedFieldsPanelHTML = Confluence.Templates.ConfluenceJiraPlugin.renderUnsupportedFieldsErrorPanel({
            unsupportedFields: _.map(unsupportedFields, function(item) { return item.name; }),
            createIssueUrl: this.getCurrentJiraCreateIssueUrl()
        });
        this.warningMsg(AJS.$('div.create-issue-container'), unsupportedFieldsPanelHTML);
    },

    renderCreateRequiredFields: function(serverId, projectKey, issueType) {
        this.enableInsert();
        this.hasUnsupportedFields = false;
        var $requiredFieldsPanel = this.container.find('#jira-required-fields-panel');
        $requiredFieldsPanel.empty();
        var thiz = this;
        jiraIntegration.fields.renderCreateRequiredFields(
            $requiredFieldsPanel,
            AJS.$('.issue-summary'),
            {
                serverId: serverId,
                projectKey: projectKey,
                issueType: issueType
            },
            {
                excludedFields: thiz.EXCLUDED_FIELDS,
                ignoreFieldsWithDefaultValue: true
            },
            _.bind(thiz.showUnsupportedFieldsMessage, thiz) // provide current scope for this function
        );
    },

    bindEvent: function() {
        var thiz = this;
        var $projects = AJS.$('.project-select', this.container);
        var $types = AJS.$('select.type-select', this.container);

        $projects.change(function() {
            var projectId = AJS.$('option:selected', $projects).val();
            if (projectId != thiz.DEFAULT_PROJECT_VALUE) {
                thiz.removeError(thiz.container);
                AJS.$('option[value="-1"]', $projects).remove();
                $types.enable();

                var project = thiz.PROJECTS_META[projectId];
                thiz.fillIssuesTypeOptions($types, project.issuetypes);
                thiz.renderCreateRequiredFields(thiz.selectedServer.id, project.key, project.issuetypes[0].id);
            }
        });

        $types.change(function() {
            thiz.startLoading();
            var projectKey = $projects.find("option:selected").first().attr('data-jira-option-key');
            var issueType = $types.find("option:selected").first().val(); // use issue type id to avoid multiple languages problem
            thiz.renderCreateRequiredFields(thiz.selectedServer.id, projectKey, issueType);
            thiz.endLoading();
        });

        /**
         * The fix adds custom class to AUI Inline Dialog only in case of AUI Datepicker
         * The incidient caused by the conflicts between jQuery UI Datepicker stylesheet and AUI Datepicker stylesheet
         *
         * The fix may be removed if
         * - Confluence business blueprint - Decision Blueprint moved away from jQuery UI Datepicker
         * - AUI Datepicker updates to fix the z-index of its Inline Dialog (always below Dialog if the DatePicker is on the Dialog)
         */
       this.container.on('focus', 'input[data-aui-dp-uuid]', function() {
           var uuid = AJS.$(this).attr('data-aui-dp-uuid');
           setTimeout(function(){
               AJS.$('[data-aui-dp-popup-uuid=' + uuid + ']')
                   .parents('.aui-inline-dialog')
                   .addClass('datepicker-patch')
           }, 0);
       });
    },

    /**
     * Get project meta data to fill in project and issue type drop box
     * 
     * @param params Parameters object contains information we need to get project metadata: serverId, projectId and sucessHandler
     */
    getProjectMeta: function(params) {
        if (!params.sucessHandler) {
            AJS.logError("JIRA Issues Macro : Error occurs when getting project meta, no success handler found !");
            return;
        }

        var thiz = this;
        thiz.startLoading();
        var url = Confluence.getContextPath() + '/rest/jira-integration/1.0/servers/' + params.serverId + '/projects';
        var $ajx = $.ajax({
            type : 'GET',
            url : url
        }).pipe(function(projects) {
            return params.projectId ? _.find(projects, function(project) {return project.id === params.projectId}) : projects;
        });

        $ajx.done(params.sucessHandler)
            .fail(_.bind(thiz.ajaxAuthCheck, thiz))
            .always(_.bind(thiz.endLoading, thiz));
    },

    loadProjects: function() {
        var thiz = this;
        thiz.getProjectMeta({
            serverId: thiz.selectedServer.id,
            sucessHandler: function(projects) {
                // Clean the cache
                thiz.PROJECT_META = {};

                _.each(projects, function(project) {
                    thiz.PROJECTS_META[project.id] = project;
                });
                thiz.fillProjectOptions(projects);
            }
        });
    },

    title: function() {
        return AJS.I18n.getText("insert.jira.issue.create");
    },

    init: function(panel) {
        panel.html('<div class="create-issue-container"></div>');
        this.container = AJS.$('div.create-issue-container');
        var container = this.container;
        var servers = AJS.Editor.JiraConnector.servers;
        this.selectedServer = servers[0];
        container.append(Confluence.Templates.ConfluenceJiraPlugin.createIssuesForm());

        var thiz = this;
        var serverSelect = AJS.$('select.server-select', container);
        if (servers.length > 1) {
            this.applinkServerSelect(serverSelect, function(server) {
                thiz.resetForm();
                thiz.authCheck(server);
            });
        }
        else{
            serverSelect.parent().remove();
        }

        var $summary = AJS.$('.issue-summary', container);
        $summary.keyup(function() {
            thiz.setInsertButtonState();
        });

        this.showSpinner(AJS.$('.loading-data', container)[0], 50, true, true);

        var insertClick = function() {
            AJS.$('.insert-issue-button:enabled').click();
        };

        this.setActionOnEnter($summary, insertClick);

        panel.onselect=function() {
            thiz.onselect();
        };
        this.bindEvent();
    },

    convertFormToJSON: function($myform) {
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
        };

        $myform.children('#jira-required-fields-panel')
                   .children('.jira-field')
                   .children('input,select,textarea').not(".select2-input")
                   .each(function(index, formElement) {
                var field = AJS.$(formElement);
                issue.fields[field.attr("name")] = jiraIntegration.fields.getJSON(field);
        });

        createIssuesObj.issues.push(issue);
        return JSON.stringify(createIssuesObj);
    },
    validateRequiredFieldInForm: function($createIssueForm) {
        var invalidRequiredFields = [];
        var $requiredFields = $createIssueForm.find('.field-group .icon-required');
        $requiredFields.each(function(index, requiredElement) {
            var $requiredFieldLabel = AJS.$(requiredElement).parent();
            var fieldLabel = $requiredFieldLabel.text();
            var fieldValue = $requiredFieldLabel.nextAll('input,select,textarea').val();
            if (typeof fieldValue === 'string') {
                fieldValue = $.trim(fieldValue);
            }
            if (!fieldValue) {
                invalidRequiredFields.push(fieldLabel);
            }
        });
        return invalidRequiredFields;
    },

    insertLink: function() {
        var thiz = this;
        var JIRA_REST_URL = Confluence.getContextPath() + "/rest/jira-integration/1.0/issues";
        var myform = AJS.$('div.create-issue-container form');
        
        var invalidRequiredFields = this.validateRequiredFieldInForm(myform);
        if (invalidRequiredFields.length) {
            var error = AJS.I18n.getText("jiraissues.error.field.required", invalidRequiredFields.join(', '));
            var errorPanelHTML = Confluence.Templates.ConfluenceJiraPlugin.renderCreateErrorPanel({errors: [error], serverUrl: thiz.selectedServer.url});
            this.errorMsg(AJS.$('div.create-issue-container'), errorPanelHTML);
            return;
        }
        this.startLoading();
        AJS.$.ajax({
            type : "POST",
            contentType : "application/json",
            url : JIRA_REST_URL + "?applicationId=" + this.selectedServer.id,
            data : this.convertFormToJSON(myform),
            success: function(data) {
                var key = data && data.issues && data.issues[0] && data.issues[0].issue && data.issues[0].issue.key;
                if (!key) {
                    var errors = data.errors[0].elementErrors.errors;
                    var errorPanelHTML = Confluence.Templates.ConfluenceJiraPlugin.renderCreateErrorPanel({errors: _.values(errors), serverUrl: thiz.selectedServer.url});
                    thiz.errorMsg(AJS.$('div.create-issue-container'), errorPanelHTML);
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
    onselect: function() {
        var container = this.container;
        // first time viewing panel or they may have authed on a different panel
        if (!AJS.$('.project-select option', container).length || AJS.$('.oauth-message', container).length) {
            this.authCheck(this.selectedServer);
        }
    },

    analyticName: "create_new"
});
AJS.Editor.JiraConnector.Panels.push(new AJS.Editor.JiraConnector.Panel.Create());
