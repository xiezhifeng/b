define('confluence/jim/macro-browser/editor/dialog/abstract-panel-view', [
    'jquery',
    'underscore',
    'ajs',
    'backbone',
    'confluence/jim/macro-browser/editor/util/helper',
    'confluence/jim/macro-browser/editor/util/config',
    'confluence/jim/macro-browser/editor/util/service',
    'confluence/jim/macro-browser/editor/util/select2-mixin'
],
function(
    $,
    _,
    AJS,
    Backbone,
    helper,
    config,
    service,
    Select2Mixin
) {
    'use strict';

    var FormDataModel = Backbone.Model.extend({
        defaults: {
            selectedServer: null,
            isValid: false
        },

        reset: function() {
            _.extend(this.attributes, this.defaults);
        }
    });

    var AbstractPanelView = Backbone.View.extend({
        template: Confluence.Templates.JiraSprints.Dialog,

        initialize: function() {
            // id of panel
            this.panelId = '';

            // title of panel
            this.panelTitle = '';

            // essential DOM elements should be initialized in child panel view
            this.view = {
                $errorMessage: null,
                $servers: null
            };

            this.servers = [];

            this.on('reload.data', function() {
                this.toggleEnablePanel(true);
                this._fillServersData();
            }, this);
        },

        render: function(options) {
            this.dialogView = options.dialog;
            this.panelDialog = options.panelDialog;

            // reset container
            if (options.$el) {
                this.$el = options.$el;
            } else {
                this.$el = this.panelDialog.body;
            }
            this.el = this.$el[0];
            this.delegateEvents();

            this.listenTo(this.dialogView, 'dialog.process.begin', this.onBeginInitDialog);
            this.listenTo(this.dialogView, 'dialog.process.finish', this.onEndInitDialog);
            this.listenTo(this.dialogView, 'dialog.showing.begin', this.onOpenDialog);

            this.formData = new FormDataModel();
            this.listenTo(this.formData, 'change:selectedServer', this._handleServerChanged);
        },

        /**
         * This method should be called by child panel view
         * because child panel view will know exactly when it is ready for DOM stucture.
         */
        initServerField: function() {
            this.view.$servers = this.$('.jira-servers');
            this.view.$servers.on('change', this._onSelectServerChanged.bind(this));

            // init server select
            this.setupSelect2({
                $el: this.view.$servers,
                placeholderText: AJS.I18n.getText('jira.server.placeholder'),
                isRequired: true
            });
        },

        onBeginInitDialog: function() {
        },

        onEndInitDialog: function() {
        },

        onOpenDialog: function(macroOptions) {
            // don't need to re-render dialog if switching from other dialogs and form is valid
            if (macroOptions.isOpenFromOtherDialog &&
                this.formData.get('isValid')) {
                return;
            }

            // open from other dialogs, we need to merge new `macroOptions` with current `macroOptions` of current dialog
            if (macroOptions.isOpenFromOtherDialog &&
                this.macroOptions) {
                _.extend(macroOptions, this.macroOptions);
            }

            this.formData.reset();

            if (macroOptions && macroOptions.params && macroOptions.name === this.dialogView.macroId) {
                this.macroOptions = macroOptions;
            } else {
                this.macroOptions = null;
            }

            this.init();
        },

        init: function() {
            this._fillServersData().done(function() {
                this.reset();
            }.bind(this));
        },

        reset: function() {
            // reset all text boxes
            var _this = this;
            this.$('input[type=text]').each(function() {
                var $this = $(this);
                $this.val('');
                _this.toggleSiblingErrorMessage($this, false);
            });
        },

        toggleEnablePanel: function(isEnabled) {
            var $formControl = this.$('input, area, select');

            // should not disable server select in all cases because we want users to select other server.
            $formControl = $formControl.not('[name=jira-server]');

            if (isEnabled) {
                $formControl.enable();
            } else {
                $formControl.disable();
            }

            $formControl.select2('enable', isEnabled);
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

        /**
         * Show error message when there is an error in ajax request.
         * Insert button of dialog will be disabled in case there is an error.
         * @param $el
         * @param errorStatus
         */
        handleAjaxRequestError: function($el, errorStatus) {
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
            this.dialogView.toggleEnableInsertButton(false);
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

        validateServer: function(server) {
            var isValid = this.validateJiraServerSupported(server);

            if (isValid && server && server.authUrl) {
                this._renderErrorMessageUnauthentication(server);
                isValid = false;
            }

            return isValid;
        },

        _renderErrorMessageUnauthentication: function(server) {
            var _this = this;

            var hasAppLinksUtilResources = (window.AppLinks && window.AppLinks.authenticateRemoteCredentials);
            var markup = this.template.errorMessageOauth({
                forceUserToReload: !hasAppLinksUtilResources
            });
            this.view.$errorMessage
                    .removeClass('hidden')
                    .append(markup);

            this.view.$errorMessage.find('a').click(function(e) {
                e.preventDefault();

                if (hasAppLinksUtilResources) {
                    window.AppLinks.authenticateRemoteCredentials(server.authUrl, function() {
                        server.authUrl = null;

                        // reset data for global variable so that other dialogs don't need to do authentication again.
                        var findedServer = _.findWhere(window.AJS.Editor.JiraConnector.servers, {id: server.id});
                        if (findedServer) {
                            findedServer.authUrl = null;
                        }

                        _this.view.$errorMessage.empty().addClass('hidden');
                        _this.trigger('reload.data');
                    });
                } else {
                    // in some special pages, ex: Upload Add-on page,
                    // somehow we can not load async applinks resources.
                    // Users must reload page manually after finishing authentication.
                    window.open(server.authUrl, 'com_atlassian_applinks_authentication');
                }
            });
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
                var markup = this.template.showJiraUnsupportedVersion({});

                this.view.$errorMessage
                        .html(markup)
                        .removeClass('hidden');

                return false;
            }

            this.view.$errorMessage.empty().addClass('hidden');

            return true;
        },

        /**
         * Get all user input as a plain object.
         * @returns {Object}
         */
        getUserInputData: function() {
            return {};
        },

        fillDataInSelect2: function($select, dfd) {
            this.toggleSelect2Loading($select, true);

            dfd.done(function() {
                this.toggleSelect2Loading($select, false);
                this.view.$errorMessage.empty().addClass('hidden');
                this.dialogView.toggleEnableInsertButton(true);
            }.bind(this));

            dfd.fail(function(xhr, errorStatus) {
                this.handleAjaxRequestError($select, errorStatus);
            }.bind(this));

            return dfd;
        },

        /**
         * Load server data and fill in sprint select box.
         * @private
         */
        _fillServersData: function() {
            return this.fillDataInSelect2(this.view.$servers, service.loadJiraServers())
                    .done(function(data) {
                        this.servers = data.servers;
                        this.primaryServer = data.primaryServer;

                        if (this.servers.length === 0) {
                            this.renderErrorNoAppLink();
                            return;
                        }

                        this.fillDataSelect2(this.view.$servers, this.servers);

                        // only have one server, select it and hide server select2
                        if (this.servers.length === 1) {
                            this.view.$servers.parent().addClass('hidden');
                            this.removeEmptyOptionInSelect2(this.view.$servers);
                        }

                        // after loading server data, this is initial value to set as selected.
                        var selectedServerId = this.macroOptions && this.macroOptions.params
                                                    ? this.macroOptions.params.serverId
                                                    : null;

                        // choose a server as selected by default.
                        if (selectedServerId) {
                            this.setSelect2Value(this.view.$servers, selectedServerId);
                        } else if (this.primaryServer) {
                            this.setSelect2Value(this.view.$servers, this.primaryServer.id);
                        } else {
                            this.selectFirstValueInSelect2(this.view.$servers);
                        }

                    }.bind(this));
        },

        _handleServerChanged: function() {
            var selectedServer = this.formData.get('selectedServer');

            if (!selectedServer) {
                this.formData.set('isValid', false);
                return;
            }

            this.formData.set('isValid', this.validateServer(selectedServer));

            var isValid = this.formData.get('isValid');

            // if there is any error regarding to server,
            // we will disable all form controls, ex: input, select, Insert button dialog...
            this.toggleEnablePanel(isValid);

            // should not cache any data if there is any error regarding to server
            if (!isValid) {
                service.resetCachingData();
            }
        },

        _onSelectServerChanged: function() {
            var serverId = this.view.$servers.val();

            if (!serverId || serverId === config.DEFAULT_OPTION_VALUE) {
                this.formData.set('isValid', false);
            } else {
                this.formData.set('isValid', true);
            }

            var selectedServer = _.findWhere(this.servers, {id: serverId});
            this.formData.set('selectedServer', selectedServer);
        }
    });

    // extend with some mixins
    _.extend(AbstractPanelView.prototype, Select2Mixin);

    return AbstractPanelView;
});
