define('confluence/jim/macro-browser/editor/dialog/abstract-panel-view', [
    'jquery',
    'underscore',
    'ajs',
    'backbone',
    'confluence/jim/macro-browser/editor/util/helper',
    'confluence/jim/macro-browser/editor/util/config',
    'confluence/jim/macro-browser/editor/util/service'
],
function(
    $,
    _,
    AJS,
    Backbone,
    helper,
    config,
    service
) {
    'use strict';

    // cache some global vars
    var AppLinks = window.AppLinks;

    var FormDataModel = Backbone.Model.extend({
        defaults: {
            selectedServer: null,
            isValid: true
        },

        reset: function() {
            _.extend(this.attributes, this.defaults);
        }
    });

    var AbstractPanelView = Backbone.View.extend({
        initialize: function() {
            // id of panel
            this.panelId = '';

            // title of panel
            this.panelTitle = '';

            // essential DOM elements
            this.view = {
                $errorMessage: null,
                $server: null
            };

            this.servers = [];
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
        },

        initServerField: function() {
            this.view.$server = this.$('.jira-servers');
            this.view.$server.on('change', this._onSelectServerChanged.bind(this));

            this.listenTo(this.formData, 'change:selectedServer', this._handleServerChanged);

            // init server select
            this.setupSelect2(this.view.$server,
                    'select2-server-container',
                    'select2-server-dropdown',
                    AJS.I18n.getText('jira.server.placeholder'),
                    true);

            this._fillServersData();
        },

        onBeginInitDialog: function() {
        },

        onEndInitDialog: function() {
        },

        onOpenDialog: function(macroOptions) {
            this.formData.reset();
            if (macroOptions && macroOptions.params && macroOptions.name === this.dialogView.macroId) {
                this.macroOptions = macroOptions;

                this._fillServersData().done(function() {
                    this.removeEmptyOptionInSelect2(this.view.$server);
                    this.setSelect2Value(this.view.$server, macroOptions.params.serverId);
                }.bind(this));
            } else {
                this.macroOptions = null;
                this._fillServersData().done(function() {
                    if (this.view.$server) {
                        this.selectFirstValueInSelect2(this.view.$server);
                    }
                    this.reset();
                }.bind(this));
            }
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
            }.bind(this));

            dfd.fail(function(xhr, errorStatus) {
                this.handleRequestError($select, errorStatus);
            }.bind(this));

            return dfd;
        },

        /**
         * Load server data and fill in sprint select box.
         * @private
         */
        _fillServersData: function() {
            // TODO: do need to cache server data
            return this.fillDataInSelect2(this.view.$server, service.loadJiraServers())
                    .done(function(data) {
                        this.servers = data.servers;
                        this.primaryServer = data.primaryServer;

                        if (this.servers.length === 0) {
                            this.renderErrorNoAppLink();
                            return;
                        }

                        var templateSelect2Option = this.template.serverOptions;
                        this.fillDataSelect2(this.view.$server, templateSelect2Option, {servers: this.servers});

                        // only have one server, select it and hide server select2
                        if (this.servers.length === 1) {
                            this.view.$server.parent().addClass('hidden');
                            this.removeEmptyOptionInSelect2(this.view.$server);

                            // trigger change to load other data, such as board data
                            this.setSelect2Value(this.view.$server, this.servers[0].id);
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

            if (this.formData.get('isValid')) {
                this.dialogView.toggleEnableInsertButton(true);
            } else {
                this.dialogView.toggleEnableInsertButton(false);
            }
        },

        _onSelectServerChanged: function() {
            var serverId = this.view.$server.val();

            if (!serverId || serverId === config.DEFAULT_OPTION_VALUE) {
                this.formData.set('isValid', false);
            } else {
                this.formData.set('isValid', true);
            }

            var selectedServer = _.findWhere(this.servers, {id: serverId});
            this.formData.set('selectedServer', selectedServer);
        }
    });

    return AbstractPanelView;
});
