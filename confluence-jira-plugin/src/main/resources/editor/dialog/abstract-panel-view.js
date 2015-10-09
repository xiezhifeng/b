define('confluence/jim/editor/dialog/abstract-panel-view', [
    'jquery',
    'underscore',
    'ajs',
    'backbone',
    'confluence/jim/editor/util/helper',
    'confluence/jim/editor/util/config'
],
function(
    $,
    _,
    AJS,
    Backbone,
    helper,
    config
) {
    'use strict';

    // cache some global vars
    var AppLinks = window.AppLinks;

    var AbstractPanelView = Backbone.View.extend({

        initialize: function() {
            // id of panel
            this.panelId = '';

            // title of panel
            this.panelTitle = '';

            // essential DOM elements
            this.view = {
                $errorMessage: null
            };

            this.servers = [];
        },

        render: function(options) {
            this.dialogView = options.dialog;
            this.panelDialog = options.panelDialog;

            // reset container
            this.$el = this.panelDialog.body;
            this.el = this.$el[0];
            this.delegateEvents();

            this.listenTo(this.dialogView, 'dialog.process.begin', this.onBeginInitDialog);
            this.listenTo(this.dialogView, 'dialog.process.finish', this.onEndInitDialog);
            this.listenTo(this.dialogView, 'dialog.showing.begin', this.onOpenDialog);
        },

        onBeginInitDialog: function() {
        },

        onEndInitDialog: function() {
        },

        onOpenDialog: function(options) {
        },

        toggleEnablePanel: function(isEnabled) {
            if (isEnabled) {
                this.$('input, area').removeAttr('disabled');
            } else {
                this.$('input, area').attr('disabled', 'disabled');
            }

            this.dialogView.toggleEnableInsertButton(isEnabled);
        },

        toggleSiblingErrorMessage: function($el, isShowing, message) {
            var $error = $el.siblings('.error');

            if (isShowing) {
                $el.focus();
                $error
                        .removeClass('hidden')
                        .text(message);
            } else {
                $error
                        .addClass('hidden')
                        .text('');
            }
        },

        handleRequestError: function($el, errorStatus) {
            var errorMessage = AJS.I18n.getText('jira.sprint.validation.cannot.connect');

            if (errorStatus === 'timeout') {
                errorMessage = AJS.I18n.getText('jira.sprint.validation.connection.timeout');
            }

            var $markup = AJS.messages.error({
                body: errorMessage
            });

            this.view.$errorMessage
                    .empty()
                    .append($markup)
                    .removeClass('hidden');

            this.resetSelect2Options($el);
            this.toggleSiblingErrorMessage($el, false);
            this.toggleEnableSelect2($el, true);
        },

        renderErrorNoAppLink: function() {
            var isAdministrator = AJS.Meta.get('is-confluence-admin');
            var errorMessage = '';

            if (isAdministrator) {
                errorMessage = AJS.I18n.getText('jira.sprint.noapplink.admin.message', AJS.contextPath());
            } else {
                errorMessage = AJS.I18n.getText('jira.sprint.noapplink.user.message', AJS.contextPath());
            }

            var $markup = AJS.messages.error({
                body: errorMessage
            });

            this.view.$errorMessage
                    .empty()
                    .append($markup)
                    .removeClass('hidden');
            this.toggleCreateButton(false);
        },

        resetAllTextBoxes: function() {
            this.$('input[type=text]')
                    .val('')
                    .siblings('.error').empty().hide();
        },

        validateServer: function(server) {
            var _this = this;

            var isValid = this.validateJiraServerSupported(server);

            if (isValid && server && server.authUrl) {
                var markup = this.template.errorMessageOauth();
                this.view.$errorMessage
                        .removeClass('hidden')
                        .append(markup);

                this.view.$errorMessage.find('a').click(function(e) {
                    e.preventDefault();

                    AppLinks.authenticateRemoteCredentials(server.authUrl, function() {
                        server.authUrl = null;
                        _this.view.$errorMessage.empty().addClass('hidden');
                        _this.trigger('reload.data');
                    });
                });

                isValid = false;
            }

            return isValid;
        },

        validateRequiredFields: function($el, message) {
            var val = $.trim($el.val());

            if (!val || val === config.DEFAULT_OPTION_VALUE) {
                this.toggleSiblingErrorMessage($el, true, message);
                return false;
            }

            this.toggleSiblingErrorMessage($el, false);

            return true;
        },

        validateJiraServerSupported: function(server) {
            if (helper.isJiraUnSupportedVersion(server)) {
                var markup = Confluence.Templates.ConfluenceJiraPlugin.showJiraUnsupportedVersion({});

                this.view.$errorMessage
                        .html(markup)
                        .removeClass('hidden');

                this.toggleEnablePanel(false);

                return false;
            }

            this.view.$errorMessage.empty().addClass('hidden');
            this.toggleEnablePanel(true);

            return true;
        },


        /**
         * Get all user input as JSON object.
         * @returns {Object} json object
         */
        getUserInputData: function() {
            return {};
        }
    });

    return AbstractPanelView;
});
