define('confluence/jim/macro-browser/editor/dialog/abstract-dialog-view', [
    'jquery',
    'underscore',
    'ajs',
    'backbone',
    'confluence/jim/util/analytic',
    'confluence/jim/macro-browser/editor/util/config'
],
function(
    $,
    _,
    AJS,
    Backbone,
    analytic,
    config
) {
    'use strict';

    var tinymce = window.tinymce;

    var AbstractDialogView = Backbone.View.extend({

        initialize: function(options) {

            // id of dialog
            this.dialogId = '';

            // title of dialog
            this.dialogTitle = '';

            // macro id of dialog
            this.macroId = '';

            // CSS class for primary Insert button
            this.cssClassInsertButton = '';

            // an instance of `AJS.ConfluenceDialog`
            this.popup = null;

            // list of panel objects
            this.panels = options && options.panels ? options.panels : [];

            // macroId is used to insert content to editor
            this.macroId = options && options.macroId ? options.macroId : '';

            this.externalLinks = options && options.externalLinks ? options.externalLinks : [];

            // essential DOM elements
            this.view = {
                $insertButton: null,
                $searchButton: null
            };

            this.isValid = true;

            this.on('dialog.process.finish', this.renderExternalLinks, this);
            this.on('dialog.process.finish', this.preRenderValidation, this);
        },

        /**
         * Init Confluence Dialog once
         */
        _initDialog: function() {
            if (this.popup) {
                // do not need to initialize dialog
                return;
            }

            this.trigger('dialog.process.begin');

            this.popup = new AJS.ConfluenceDialog({
                width: 840,
                height: 590,
                id: this.dialogId
            });

            this.popup.addHeader(this.dialogTitle);

            // add insert button
            var insertText = AJS.I18n.getText('insert.jira.issue.button.insert');
            this.popup.addButton(insertText, this.handleClickInsertButton.bind(this), this.cssClassInsertButton);

            // add button cancel
            var cancelText = AJS.I18n.getText('insert.jira.issue.button.cancel');
            this.popup.addCancel(cancelText, this.close.bind(this));

            // add link select macro
            this.popup.addLink(AJS.I18n.getText('insert.jira.issue.button.select.macro'), function() {
                this.popup.hide();
                AJS.MacroBrowser.open(false);
            }.bind(this), 'dialog-back-link');

            // add panels
            var panels = this.panels;
            _.each(panels, function(panel) {
                if (typeof (panel.panelTitle) === 'function') {
                    this.popup.addPanel(panel.panelTitle());
                } else if (panel.panelTitle) {
                    this.popup.addPanel(panel.panelTitle);
                }

                var panelDialog = this.popup.getCurrentPanel();
                panel.render({
                    dialog: this,
                    panelDialog: panelDialog
                });

            }.bind(this));

            var $container = this.popup.popup.element;

            this.$el = $container;
            this.el = this.$el[0];
            this.delegateEvents();

            this.view.$insertButton = this.$('.' + this.cssClassInsertButton);

            // go to first panel
            this.popup.gotoPanel(0);
            this.popup.overrideLastTab();

            this.trigger('dialog.process.finish');
        },

        render: function(macroOptions) {
            this._initDialog();

            // do not show the dialog if it is invalid state.
            if (this.isValid) {
                this.popup.show();
                this.trigger('dialog.showing.begin', macroOptions);
            } else {
                this.toggleEnableInsertButton(false);
            }
        },

        /**
         * Some some validation on early state.
         * If the dialog is in invalid state after validation, the dialog should not appear.
         */
        preRenderValidation: function() {
            // TODO: because we are storing servers data in global variable which is populated when editor is opening.
            this.servers = AJS.Editor.JiraConnector.servers;

            // check no app link config
            if (!this.servers || this.servers.length === 0) {
                this.showingWarningPopupToSetUpAppLink();
                this.isValid = false;
                return;
            }

            this.isValid = true;
        },

        showingWarningPopupToSetUpAppLink: function() {
            var isAdmin = AJS.Meta.get('is-admin');

            // create new dialog
            var warningDialog = new AJS.ConfluenceDialog({width: 600, height: 400, id: 'warning-applink-dialog'});

            // add title dialog
            var warningDialogTitle = AJS.I18n.getText('applink.connector.jira.popup.title');
            warningDialog.addHeader(warningDialogTitle);

            // add button cancel
            warningDialog.addLink(AJS.I18n.getText('insert.jira.issue.button.cancel'), function() {
                warningDialog.hide();
                tinymce.confluence.macrobrowser.macroBrowserCancel();
            });

            // add body content in panel
            var bodyContent = Confluence.Templates.ConfluenceJiraPlugin.warningDialog({
                isAdministrator: isAdmin
            });

            if (isAdmin) {
                // add button set connect
                warningDialog.addButton(AJS.I18n.getText('applink.connector.jira.popup.button.admin'), function() {
                    AJS.Editor.JiraConnector.clickConfigApplink = true;
                    warningDialog.hide();
                    tinymce.confluence.macrobrowser.macroBrowserCancel();
                    window.open(AJS.contextPath() + '/admin/listapplicationlinks.action', '_blank');
                }, 'create-dialog-create-button app_link');

                // apply class css of form
                warningDialog.popup.element
                        .find('.create-dialog-create-button')
                        .removeClass('button-panel-button')
                        .addClass('aui-button aui-button-primary');

            } else {
                // add button contact admin
                warningDialog.addButton(AJS.I18n.getText('applink.connector.jira.popup.button.contact.admin'), function() {
                    warningDialog.hide();

                    tinymce.confluence.macrobrowser.macroBrowserCancel();
                    window.open(AJS.contextPath() + '/wiki/contactadministrators.action', '_blank');
                });
            }

            warningDialog.addPanel('Panel 1', bodyContent);
            warningDialog.get('panel:0').setPadding(0);
            warningDialog.show();
            warningDialog.gotoPanel(0);
        },

        /**
         * Open the dialog with macro options
         * @param {Object} macroOptions
         */
        open: function(macroOptions) {
            this.render(macroOptions);

            if (macroOptions && macroOptions.name === config.macroIdSprint) {
                analytic.sendOpenSprintDialogEvent();
            }
        },

        /**
         * Close dialog
         */
        close: function() {
            this.popup.hide();
            tinymce.confluence.macrobrowser.macroBrowserCancel();
        },

        renderExternalLinks: function() {
            if (!this.externalLinks.length) {
                return;
            }

            var linksMarkup = Confluence.Templates.ConfluenceJiraPlugin.addCrossMacroLinks({
                links: this.externalLinks
            });

            var $menu = this.$('ul.dialog-page-menu');
            $menu.append(linksMarkup).show();

            _.each(this.externalLinks, function(link) {
                if (typeof link.callBack === 'function') {
                    $menu.find('#' + link.id).on('click', link.callBack.bind(this));
                }
            }, this);
        },

        /**
         * Handle clicking insert button to submit dialog
         */
        handleClickInsertButton: function() {
            var panel = this.panels[this.popup.getCurrentPanel().id];
            var params = panel.getUserInputData();

            if (AJS.Editor.inRichTextMode() && params) {

                if (this.macroId === config.macroIdSprint) {
                    analytic.sendInsertSprintMacroToEdtiorContentEvent();
                }

                tinymce.confluence.macrobrowser.macroBrowserComplete({
                    name: this.macroId,
                    params: params
                });
                this.close();
            }
        },

        /**
         * Turn on and off enable state for insert button
         * @param isEnabled
         */
        toggleEnableInsertButton: function(isEnabled) {
            if (isEnabled) {
                this.view.$insertButton.removeAttr('disabled');
            } else {
                this.view.$insertButton.attr('disabled', 'disabled');
            }
        }
    });

    return AbstractDialogView;
});
