define('confluence/jim/macro-browser/editor/dialog/abstract-dialog-view', [
    'jquery',
    'underscore',
    'ajs',
    'backbone',
    'confluence/jim/macro-browser/editor/dialog/validation-mixin',
    'confluence/jim/macro-browser/editor/dialog/abstract-panel-view'
],
function(
    $,
    _,
    AJS,
    Backbone,
    JiraDialogValidationMixin,
    AbstractPanelView
) {
    'use strict';

    var AbstractDialogView = Backbone.View.extend({
        events: {
            'click .dialog-close-button': 'close',
            'click .dialog-submit-button': 'handleClickInsertButton',
            'click .dialog-back-link': 'openMacroBrowserDialog',
            'click .panels .page-menu-item': '_handleClickPanel'
        },

        template: Confluence.Templates.JiraIssueMacro.Dialog,

        initialize: function(options) {
            // id of dialog
            this.dialogId = 'jira-issue-dialog';

            // macro id of dialog
            this.macroId = '';

            // an instance of AUI Dialog 2
            this.dialog = null;

            // instance of active tab
            this.currentTabContentView = null;

            // list of panel objects
            this.panels = options && options.panels ? options.panels : [];

            // essential DOM elements
            this.view = {
                // insert button of the dialog
                $insertButton: null,
                // left panel of the dialog
                $panel: null
            };

            this.isValid = true;

            this.on('dialog.process.finish', this.preRenderValidation, this);
            this.on('dialog.showing.begin', this._renderTabContainers, this);

            this.listenTo(this.panels, 'selected.tab.changed', this._updateUISelectedTabChanged);
            this.listenTo(this.panels, 'selected.panel.changed', this._updateUISelectedPanelChanged);
        },

        render: function(macroOptions) {
            this._initDialog();

            // do not show the dialog if it is invalid state.
            if (this.isValid) {
                this.dialog.show();
                this.trigger('dialog.showing.begin', macroOptions);
            } else {
                this.toggleEnableInsertButton(false);
            }
        },

        /**
         * Re-render current panel UI
         */
        refresh: function() {
            // remove all tab content
            var $containerContent = this.$el.find('.dialog-main-content');
            this.panels.each(function(panel) {
                _.each(panel.get('tabs'), function(tab) {
                    var $tabContainer = $containerContent.find('#' + tab.id);
                    $tabContainer.empty();
                }, this);
            }, this);

            this._renderTabContainers();

            this._updateUISelectedPanelChanged();
            this._updateUISelectedTabChanged();
        },

        /**
         * Init Confluence Dialog once
         * When the dialog 2 is close, it will be destroyed completely, so we need to create Dialog2 instance again.
         */
        _initDialog: function() {
            this.trigger('dialog.process.begin');

            var dialogMarkup = this.template.dialog({
                panels: this.panels.toJSON()
            });
            this.dialog = AJS.dialog2(dialogMarkup);

            this.$el =  this.dialog.$el;
            this.el = this.$el[0];

            this.delegateEvents();

            this.view.$panel = this.$('.panels');
            this.view.$insertButton = this.$('.dialog-submit-button');

            this.trigger('dialog.process.finish');
        },

        /**
         * Some some validation on early state.
         * If the dialog is in invalid state after validation, the dialog should not appear.
         */
        preRenderValidation: function() {
            this._fetchServersData();

            // TODO: because legacy code is storing servers data in global variable which is populated when editor is opening.
            this.servers = AJS.Editor.JiraConnector.servers;
            this.isValid = this.validateServers(this.servers);
        },

        /**
         * Render all tabs content view of all panels in DOM because
         * we all to keep state of tab.
         * @private
         */
        _renderTabContainers: function() {
            var $containerContent = this.$el.find('.dialog-main-content');

            AJS.tabs.setup();

            // render all tabs content
            this.panels.each(function(panel) {
                _.each(panel.get('tabs'), function(tab) {
                    var $tabContainer = $containerContent.find('#' + tab.id);

                    var PanelContentView = tab.PanelContentView;
                    var tabContentView = new PanelContentView({
                        macroId: panel.get('macroId')
                    });

                    // legacy code
                    if (tabContentView.init) {
                        tabContentView.init($tabContainer);
                    } else {
                        // new dialog/panel
                        tabContentView.render({
                            dialog: this
                        });
                        $tabContainer.append(tabContentView.$el);
                    }

                    tab.cachedView = tabContentView;
                }, this);
            }, this);

            $containerContent.find('.menu-item a').on('tabSelect', this._handleTabSelect.bind(this));
        },

        _fetchServersData: function() {
            // prefetch server columns for autocompletion feature
            if (!AJS.Editor.JiraConnector.servers) {
                return;
            }

            var len = AJS.Editor.JiraConnector.servers.length;
            for ( var i = 0; i < len; i++) {
                var server = AJS.Editor.JiraConnector.servers[i];

                window.AppLinks.makeRequest({
                    appId: server.id,
                    type: 'GET',
                    url: '/rest/api/2/field',
                    dataType: 'json',
                    serverIndex: i,
                    success: function(data) {
                        if (data && data.length) {
                            AJS.Editor.JiraConnector.servers[this.serverIndex].columns = data;
                        }
                    },
                    error: function() {
                        AJS.log('Jira Issues Macro: unable to retrieve fields from AppLink: ' + server.id);
                    }
                });
            }
        },

        /**
         * Close dialog
         */
        close: function() {
            this.panels.resetCachedView();
            this.dialog.hide();
            window.tinymce.confluence.macrobrowser.macroBrowserCancel();
        },

        openMacroBrowserDialog: function(e) {
            e.preventDefault();

            this.close();
            AJS.MacroBrowser.open(false);
        },

        /**
         * Turn on and off enable state for insert button
         * @param isEnabled
         */
        toggleEnableInsertButton: function(isEnabled) {
            if (isEnabled) {
                this.view.$insertButton.enable();
            } else {
                this.view.$insertButton.disable();
            }
        },

        /**
         * Handle clicking Insert button of the dialog.
         */
        handleClickInsertButton: function() {
        },

        /**
         * Handle logic when users click on panel title
         * @param e
         * @private
         */
        _handleClickPanel: function(e) {
            var $this = $(e.target).closest('.page-menu-item');

            $this.parent().find('.page-menu-item').removeClass('selected');
            $this.addClass('selected');

            this.currentTabContentView = null;

            // update selected panel without trigger events.
            var newPanelId = $this.attr('data-panel-id');
            var currentPanel = this.panels.setSelectedForPanelById(newPanelId, false);

            // update current macro id in dialog
            this.macroId = currentPanel.get('macroId');

            // update tooltip for dialog
            this.$('.aui-dialog2-footer-hint .other-footer-hint').html(currentPanel.get('tooltipFooter'));

            // show tab content
            var $containerContent = this.$el.find('.dialog-main-content');
            $containerContent.find('.dialog-main-content-inner')
                    .addClass('hidden')
                    .filter('.' + currentPanel.id).removeClass('hidden');

            AJS.tabs.change($containerContent.find('.menu-item[data-tab-id=' + this.panels.getActiveTab().id + '] a'));
        },

        _handleTabSelect: function(event, data) {
            // update new active tab
            var newTabId = data.tab.closest('.menu-item').attr('data-tab-id');
            this.panels.setActiveForTabByTabId(newTabId, false);

            var currentNewTab = this.panels.getActiveTab();
            this.currentTabContentView = currentNewTab.cachedView;

            // for dialog2
            if (currentNewTab.cachedView instanceof AbstractPanelView) {
                var isOpenFromMacro =  this.openDialogOptions && this.openDialogOptions.params && this.openDialogOptions.params.serverId;
                this.currentTabContentView.initWithMacroOption(this.openDialogOptions, isOpenFromMacro);
            } else {
                // for legacy code:
                // because legacy code does not have good APIs, so this is workaround to be compatiable with old code.
                if (this.currentTabContentView.focusForm) {
                    this.currentTabContentView.focusForm();
                }

                if (this.currentTabContentView.setInsertButtonState) {
                    this.currentTabContentView.setInsertButtonState();
                }

                if (this.currentTabContentView.validateSelectedServer) {
                    this.currentTabContentView.validateSelectedServer();
                }

                if (this.currentTabContentView.handleInsertButton) {
                    this.currentTabContentView.handleInsertButton();
                }
            }
        },

        _updateUISelectedPanelChanged: function() {
            // find default selected panel
            var selectedPanel = this.panels.getSelectedPanel();

            if (selectedPanel) {
                this.view.$panel.find('.page-menu-item[data-panel-id=' + selectedPanel.get('id') + ']').trigger('click');
            }
        },

        _updateUISelectedTabChanged: function() {
            // find default selected panel
            var selectActive = this.panels.getActiveTab();

            if (selectActive) {
                AJS.tabs.change(this.$el.find('.menu-item[data-tab-id=' + selectActive.id + '] a'));
            }
        }
    }, {
        OPEN_DIALOG_SOURCE: {
            macroBrowser: 'macro_browser',
            editorBraceKey: 'editor_brace_key',
            editorHotKey: 'editor_hot_key',
            editorDropdownLink: 'editor_dropdown_link',
            instructionalText: 'instructional text'
        }
    });

    _.extend(AbstractDialogView.prototype, JiraDialogValidationMixin);
    return AbstractDialogView;
});
