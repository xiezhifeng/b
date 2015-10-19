define('confluence/jim/macro-browser/editor/dialog/abstract-panel-view', [
    'jquery',
    'underscore',
    'ajs',
    'backbone',
    'confluence/jim/macro-browser/editor/util/helper',
    'confluence/jim/macro-browser/editor/util/config',
    'confluence/jim/macro-browser/editor/util/service',
    'confluence/jim/macro-browser/editor/util/select2-mixin',
    'confluence/jim/macro-browser/editor/dialog/validation-mixin'
],
function(
    $,
    _,
    AJS,
    Backbone,
    helper,
    config,
    service,
    Select2Mixin,
    JiraDialogValidationMixin
) {
    'use strict';

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
        template: Confluence.Templates.JiraIssueMacro.Dialog,

        initialize: function(options) {
            // essential DOM elements should be initialized in child panel view
            this.view = {
                $errorMessage: null,
                $servers: null
            };

            this.servers = [];
            this.macroId = options && options.macroId ? options.macroId : ''; // macroIf of current panel.

            // instance of dialog view.
            this.dialogView = null;

            this.on('reload.data', function() {
                this.dialogView.refresh();
            }, this);

            this.isAlreadyInit = false;
        },

        render: function(options) {
            this.dialogView = options.dialog;

            // reset container
            if (options.$el) {
                this.$el = options.$el;
                this.el = this.$el[0];
                this.delegateEvents();
            }

            this.listenTo(this.dialogView, 'dialog.process.begin', this.onBeginInitDialog);
            this.listenTo(this.dialogView, 'dialog.process.finish', this.onEndInitDialog);

            this.formData = new FormDataModel();
            this.listenTo(this.formData, 'change:selectedServer', this._handleServerChanged);
        },

        init: function() {
            this.initWithMacroOption({}, false);
        },

        initWithMacroOption: function(openDialogOptions, isOpenFromMacro) {
            this.dialogView.toggleEnableInsertButton(true);

            // just init once time because we want to keep view state when switching between panels/views
            if (this.isAlreadyInit) {
                return;
            }

            this.isAlreadyInit = true;

            this.macroOptions = isOpenFromMacro ? openDialogOptions : null;
            this.formData.reset();
            this._fillServersData().done(function() {
                if (isOpenFromMacro) {
                    this.setSelect2Value(this.view.$servers, openDialogOptions.params.serverId);
                }
            }.bind(this));
        },

        /**
         * This method should be called by child panel view
         * because child panel view will know exactly when it is ready for DOM stucture.
         */
        initServerField: function() {
            this.view.$servers = this.$('.jira-servers');
            this.view.$servers.on('change', this._onSelectServerChanged.bind(this));

            // init server select
            this.setupSelect2(this.view.$servers,
                    'select2-server-container',
                    'select2-server-dropdown',
                    AJS.I18n.getText('jira.server.placeholder'),
                    true);
        },

        onBeginInitDialog: function() {
        },

        onEndInitDialog: function() {
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
                this.$('input, area, button').enable();
            } else {
                this.$('input, area, button').disable();
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

        validateRequiredFields: function($el, message) {
            var val = AJS.trim($el.val());

            if (!val || val === config.DEFAULT_OPTION_VALUE) {
                this.toggleSiblingErrorMessage($el, true, message);
                return false;
            }

            this.toggleSiblingErrorMessage($el, false);

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
            return this.fillDataInSelect2(this.view.$servers, service.loadJiraServers())
                    .done(function(data) {
                        this.servers = data.servers;
                        this.primaryServer = data.primaryServer;

                        if (this.servers.length === 0) {
                            this.renderErrorNoAppLink();
                            return;
                        }

                        this.fillDataSelect2(this.view.$servers, this.servers);

                        // only have one server, hide server select2
                        if (this.servers.length === 1) {
                            this.view.$servers.parent().addClass('hidden');
                            this.removeEmptyOptionInSelect2(this.view.$servers);
                        }

                        this.selectFirstValueInSelect2(this.view.$servers);
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
                // avoid "formData" model trigger change event
                this.formData.attributes.selectedServer = null;
                this.dialogView.toggleEnableInsertButton(false);
            }
        },

        _onSelectServerChanged: function() {
            var serverId = this.view.$servers.val();
            var selectedServer = _.findWhere(this.servers, {id: serverId});

            if (!selectedServer) {
                this.formData.set('isValid', false);
            } else {
                this.formData.set('isValid', true);
            }

            this.formData.set('selectedServer', selectedServer);
        },

        destroy: function() {
            this.remove();
        },

        validate: function() {
            AJS.debug('Child view must implement "validate" method.');
        }
    });

    // extend with some mixins
    _.extend(AbstractPanelView.prototype, Select2Mixin);
    _.extend(AbstractPanelView.prototype, JiraDialogValidationMixin);

    return AbstractPanelView;
});
