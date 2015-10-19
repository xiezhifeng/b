define('confluence/jim/macro-browser/editor/dialog/validation-mixin', [
    'jquery',
    'underscore',
    'ajs',
    'confluence/jim/macro-browser/editor/util/config',
    'confluence/jim/macro-browser/editor/util/helper'
],
function(
    $,
    _,
    AJS,
    config,
    helper
) {
    'use strict';

    /**
     * Contains all mixin methods regarding to validation.
     * These methods will extend other object, such as a Backbone View.
     */
    var JiraDialogValidationMixin = {
        showingWarningPopupToSetUpAppLink: function() {
            var isAdmin = AJS.Meta.get('is-admin');

            // create new dialog
            var dialogMarkup = this.template.warningDialogNoAppLink({
                isAdmin: isAdmin
            });
            var warningDialog = AJS.dialog2(dialogMarkup);

            // add button cancel
            warningDialog.$el.find('.dialog-close-button').on('click', function() {
                warningDialog.hide();
                window.tinymce.confluence.macrobrowser.macroBrowserCancel();
            });

            warningDialog.$el.find('.dialog-submit-button').on('click', function() {
                var url = AJS.contextPath();

                if (isAdmin) {
                    url += '/admin/listapplicationlinks.action';
                } else {
                    url += '/wiki/contactadministrators.action';
                }

                window.open(url, '_blank');
                window.tinymce.confluence.macrobrowser.macroBrowserCancel();
                warningDialog.hide();
            });

            warningDialog.show();
        },

        validateServers: function(servers) {
            var isValid = true;

            // check no app link config
            if (!servers || servers.length === 0) {
                this.showingWarningPopupToSetUpAppLink();
                return false;
            }

            return isValid;
        },

        validateServer: function(server) {
            var _this = this;

            var isValid = this.validateJiraServerSupported(server);

            if (isValid && this.isServerNotAuthenticated(server)) {
                var $errorMessage = AJS.messages.warning({
                    body: AJS.I18n.getText('jira.sprint.oauth.message.text') +
                    ' <a href=#>' + AJS.I18n.getText('jira.sprint.oauth.link.text') + '</a>'
                });
                $errorMessage.addClass('oauth-form');

                this.view.$errorMessage
                    .empty()
                    .append($errorMessage)
                    .removeClass('hidden');

                this.view.$errorMessage.find('a').on('click', function(e) {
                    e.preventDefault();

                    window.AppLinks.authenticateRemoteCredentials(server.authUrl, function() {
                        server.authUrl = null;

                        // for legacy code: update global server list as well
                        // so that users don't need to re-authenticate when switching to Issue/Filter and Chart panels
                        var serverObject = _.findWhere(AJS.Editor.JiraConnector.servers, {
                            id: server.id
                        });
                        if (serverObject) {
                            serverObject.authUrl = null;
                        }

                        _this.trigger('reload.data');
                    }, function() {});
                });

                isValid = false;
            }

            return isValid;
        },

        isServerNotAuthenticated: function(server) {
            return server && server.authUrl;
        },

        validateJiraServerSupported: function(server) {
            if (helper.isJiraUnSupportedVersion(server)) {
                var $errorMessage = AJS.messages.error({
                    body: AJS.I18n.getText('jirachart.version.unsupported')
                });

                $errorMessage.addClass('jira-unsupported-version');

                this.view.$errorMessage
                    .empty()
                    .append($errorMessage)
                    .removeClass('hidden');

                this.toggleEnablePanel(false);

                return false;
            }

            this.view.$errorMessage.empty().addClass('hidden');
            this.toggleEnablePanel(true);

            return true;
        }
    };

    return JiraDialogValidationMixin;
});

