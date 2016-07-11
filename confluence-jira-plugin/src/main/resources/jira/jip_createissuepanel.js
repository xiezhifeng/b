AJS.Editor.JiraConnector.Panel.Create = function(){};

AJS.Editor.JiraConnector.Panel.Create.prototype = AJS.$.extend(AJS.Editor.JiraConnector.Panel.Create.prototype, AJS.Editor.JiraConnector.Panel.prototype);
AJS.Editor.JiraConnector.Panel.Create.prototype = AJS.$.extend(AJS.Editor.JiraConnector.Panel.Create.prototype, {
    DEFAULT_PROJECT_VALUE: "-1",
    SHOW_MESSAGE_ON_TOP: true,
    EXCLUDED_FIELDS: ['project', 'issuetype', 'summary', 'description'],
    PROJECTS_META: {},
    setSummary: function(summary) {
        var $summaryField = AJS.$('.field-group [name="summary"]', this.jipForm.formEl);
        $summaryField.length && $summaryField.val(summary);
    },
    resetIssue: function() {
        AJS.$('.issue-summary', this.container).empty();
        AJS.$('.issue-description', this.container).empty();
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
    authCheck: function(){
        this.selectedServer = this.jipForm.getCurrentServer();
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
            thiz.authCheck(thiz.jipForm.getCurrentServer());
        });
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
        if (this.formHasError === false && this.projectOk()) {
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
        this.handleInsertWaiting(true);
    },
    endLoading: function() {
        AJS.$('.loading-blanket', this.container).addClass("hidden");
        AJS.$('input,select,textarea', this.container).enable();

        // Disable issue type select box
        if (AJS.$('.project-select', this.container).val() === this.DEFAULT_PROJECT_VALUE) {
             AJS.$('.issuetype-select', this.container).disable();
        }
        this.setInsertButtonState();
        this.handleInsertWaiting(false);
    },

    bindEvent: function() {
        var thiz = this;

        var $summaryField = AJS.$('.field-group [name="summary"]', this.jipForm.formEl);
        $summaryField.keyup(function() {
            thiz.setInsertButtonState();
        });

        /**
         * The fix adds custom class to AUI Inline Dialog only in case of AUI Datepicker
         * The incidient caused by the conflicts between jQuery UI Datepicker stylesheet and AUI Datepicker stylesheet
         *
         * The fix may be removed if AUI Datepicker updates to fix the z-index of its Inline Dialog (always below Dialog if the DatePicker is on the Dialog)
         */
        this.container.on('focus', 'input[data-aui-dp-uuid]', function() {
           var uuid = AJS.$(this).attr('data-aui-dp-uuid');
           setTimeout(function() {
               AJS.$('[data-aui-dp-popup-uuid=' + uuid + ']')
                   .parents('.aui-inline-dialog')
                   .addClass('datepicker-patch')
           }, 0);
        });
    },

    title: function() {
        return AJS.I18n.getText("insert.jira.issue.create");
    },

    init: function(panel) {
        var thiz = this;
        panel.html('<div class="create-issue-container"></div>');
        this.container = AJS.$('div.create-issue-container');
        var container = this.container;
        AJS.Editor.JiraConnector.serversAjax.done(function(){
            var servers = AJS.Editor.JiraConnector.servers;
            this.selectedServer = servers[0];
        });

        this.jipForm = new jiraIntegration.JiraCreateIssueForm({
            container: '.create-issue-container',
            renderSummaryAndDescription: true,
            onError: function() {
                AJS.$('.field-group .error', this.container).remove();
                thiz.formHasError = true;
                thiz.disableInsert();
            },
            onServerChanged: function() {
                AJS.$('.field-group .error', this.container).remove();
                thiz.setInsertButtonState();

                thiz.selectedServer = this.getCurrentServer();
            },
            onRequiredFieldsRendered: function(undefined, unsupportedFields) {
                AJS.$('.field-group .error', this.container).remove();
                thiz.formHasError = !!unsupportedFields.length;
                thiz.setInsertButtonState();
            }
        });

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
                id: AJS.$('.issuetype-select option:selected', $myform).val()
            },
            summary: AJS.$('.field-group [name="summary"]', $myform).val(),
            description:  AJS.$('.field-group [name="description"]', $myform).val()
        };

        $myform.children('.create-issue-required-fields')
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
        var isPassed = true;
        var isPlaceholderSupported = 'placeholder' in document.createElement('input');
        var $requiredFields = $createIssueForm.find('.field-group .icon-required, .field-group .aui-icon-required');

        $requiredFields.each(function(index, requiredElement) {
            var $requiredFieldLabel = AJS.$(requiredElement).parent();
            var fieldLabel = $requiredFieldLabel.text();
            var $field = $requiredFieldLabel.nextAll('input,select,textarea');
            var fieldValue = AJS.$.trim($field.val());

            if (!fieldValue || (!isPlaceholderSupported && fieldValue == $field.attr('placeholder'))) {
                isPassed = false;
                var $fieldContainer = $requiredFieldLabel.parent();
                var requiredMessage = AJS.I18n.getText("jiraissues.error.field.required", fieldLabel);
                $fieldContainer.append(aui.form.fieldError({
                    message: requiredMessage
                }));
            }
        });
        return isPassed;
    },
    clearFieldErrors: function() {
        AJS.$("form div.error", this.container).remove();
    },
    insertLink: function() {
        var thiz = this;
        var JIRA_REST_URL = Confluence.getContextPath() + "/rest/jira-integration/1.0/issues";
        var $form = AJS.$("div.create-issue-container form");
        var currentServer = this.jipForm.getCurrentServer();

        thiz.clearFieldErrors();
        if (!thiz.validateRequiredFieldInForm($form)) {
            return;
        }

        this.startLoading();
        AJS.Editor.JiraConnector.serversAjax.done(function() {
            AJS.$.ajax({
                type : "POST",
                contentType : "application/json",
                url : JIRA_REST_URL + "?applicationId=" + this.selectedServer.id,
                data : this.convertFormToJSON($form),
                success: function(data) {
                    var key = data && data.issues && data.issues[0] && data.issues[0].issue && data.issues[0].issue.key;
                    if (!key) {
                        if (!_.isEmpty(data.errors[0].elementErrors.errorMessages)) {
                            var formErrors = data.errors[0].elementErrors.errorMessages;
                            var errorPanelHTML = Confluence.Templates.ConfluenceJiraPlugin.renderCreateErrorPanel({errors: formErrors, serverUrl: currentServer.displayUrl});
                            thiz.errorMsg(AJS.$('div.create-issue-container'), errorPanelHTML);
                        }

                        var fieldErrors = data.errors[0].elementErrors.errors;

                        _.each(fieldErrors, function(errorMessage, errorKey) {
                            var errorElement = aui.form.fieldError({
                                message: errorMessage
                            });
                            AJS.$(AJS.format('.field-group [name={0}]', errorKey), $form).after(errorElement);
                        });
                    } else {
                        thiz.insertIssueLink(key, currentServer.displayUrl + '/browse/' + key);
                        thiz.resetIssue();
                    }
                    thiz.endLoading();
                },
                error: function(xhr, status) {
                    thiz.ajaxAuthCheck(xhr);
                }
            });
        });
    },
    onselect: function() {
        // We will reload project list if:
        // - Current server doesn't require authorise
        // - There is an existing oauth message.
        var hasOAuthMessage = !!AJS.$('.aui-message > .oauth-init',  this.container).length;
        if (this.selectedServer && !this.selectedServer.authUrl && hasOAuthMessage) {
            this.jipForm.defaultFields.server.trigger('change');
        } else {
            this.setInsertButtonState();
        }
    },

    analyticPanelActionName: "confluence.jira.plugin.issuecreated"
});
AJS.Editor.JiraConnector.Panels.push(new AJS.Editor.JiraConnector.Panel.Create());
