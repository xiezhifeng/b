define('confluence/jim/macro-browser/editor/dialog/jira-links-dialog-macro-view', [
    'jquery',
    'underscore',
    'ajs',
    'confluence/jim/macro-browser/editor/util/service',
    'confluence/jim/macro-browser/editor/dialog/abstract-dialog-view',
    'confluence/jim/macro-browser/editor/util/config',
    'confluence/jim/macro-browser/editor/dialog/analytic-mixin'
],
function(
    $,
    _,
    AJS,
    service,
    AbstractDialogView,
    config,
    AnalyticMixin
) {
    'use strict';

    var tinyMCE = window.tinyMCE;

    var JiraLinksDialogMacroView = AbstractDialogView.extend({
        template: Confluence.Templates.JiraIssueMacro.Dialog,

        initialize: function(options) {
            _.extend(this.events, AbstractDialogView.prototype.events);
            AbstractDialogView.prototype.initialize.apply(this, arguments);

            // there are many ways to open dialog.
            // Each way has different given parameters, so `this.openDialogOptions` will be a variable to store them.
            this.openDialogOptions = null;

            // users can open the dialog by many ways, we need to track how the users open the dialog
            // and initialize the dialog conveniently
            this.openSource = '';

            // a variable storing selected text in editor content
            this.selectionTextInEditorContent = '';

            this.on('dialog.showing.begin', this.onOpenDialog);
            this.on('reload.data', this.refresh);
        },

        /**
         * Open the dialog with macro options. There are 6 ways to open the dialog
         * 1. By short-cut
         * 2. Macro browser
         * 3. Drop-down macro list in editor content
         * 4. Double-click macro placeholder in editor content.
         * 5. Click a link in Insert menu in editor.
         * 6. Programmatically, by trigger event AJS.trigger('jira.macro.dialog.panel.sprint');
         *
         * @param {Object} options.name - name of macro, in this case
         * the dialog is opened from drop-down macro auto-complete list or macro browser
         * @param {String} options.openSource - is a one of values of "JiraLinksDialogMacroView.OPEN_DIALOG_SOURCE"
         * @param {String} options.params - macro parameters
         * which is set when users open the dialog from double-clicking macro placeholder.
         */
        open: function(options) {
            if (this.dialog && this.dialog.isVisible()) {
                return;
            }

            this.openDialogOptions = options;
            // the dialog may be opened from a list of Insert menu in editor.
            this.openSource = options.openSource;
            // get macro id
            this.macroId = options.name;
            this.macroParams = null;

            // Store the current selection and scroll position, and get the selected text.
            AJS.Editor.Adapter.storeCurrentSelectionState();

            // users open the dialog from macro placeholder in editor content.
            if (options.params) {
                this.openSource = JiraLinksDialogMacroView.OPEN_DIALOG_SOURCE.editorMacro;
            }

            // convert old jira macro id to new jira macro id
            if (this.macroId === config.macroIdJiraIssueOld) {
                this.macroId = config.macroIdJiraIssue;
            }

            // set default value if openSource is not existed.
            if (!this.openSource) {
                this.openSource = JiraLinksDialogMacroView.OPEN_DIALOG_SOURCE.macroBrowser;
            }

            this.render(options);
        },

        onOpenDialog: function() {
            this._findSelectedPanelAndActiveTab();

            if (!this.currentTabContentView) {
                return;
            }

            // after rendering active tab
            if (this.selectionTextInEditorContent &&
                this.currentTabContentView &&
                this.currentTabContentView.setSummary) {
                // it is creating issue panel
                this.currentTabContentView.setSummary(this.selectionTextInEditorContent);
            } else if (this.openSource === JiraLinksDialogMacroView.OPEN_DIALOG_SOURCE.editorMacro) {
                this._handleOpeningFromMacroPlaceholder();
            }

            this.sendOpenDialogAnalyticEvent();
        },

        /**
         * Users did double-clicking on macro placeholder.
         * @private
         */
        _handleOpeningFromMacroPlaceholder: function() {
            // for jira chart
            if (this.openDialogOptions.params &&
                    this.currentTabContentView.bindingDataFromMacroToForm) {

                var chartType = this.openDialogOptions.params.chartType;
                var mapChartTypeToPanelId = {
                    'twodimensional': 'tab-two-mentional-chart',
                    'createdvsresolved': 'tab-created-resolved-chart',
                    'pie': 'tab-pie-chart'
                };

                var tabId = mapChartTypeToPanelId[chartType];
                this.panels.setActiveForTabByTabId(tabId, true);

                this.currentTabContentView.bindingDataFromMacroToForm(this.openDialogOptions.params);
                this.currentTabContentView.doSearch();
            }

            // for search issues panel
            var macroParams = this._parseParamsFromMacro(this.openDialogOptions);
            if (macroParams.searchStr && this.currentTabContentView.setMacroParams) {
                // assign macro params to search
                this.currentTabContentView.setMacroParams(macroParams);
                var searchParams = {
                    searchValue: macroParams['searchStr'],
                    serverName: macroParams['serverName'],
                    isJqlQuery: this.openDialogOptions.params.hasOwnProperty('jqlQuery'),
                    isAutoSearch: true
                };
                this.currentTabContentView.doSearch(searchParams);
            }
        },

        /**
         * Base on opening dialog source, ex: macro id, we will have different activated panel at the beginning.
         * @private
         */
        _findSelectedPanelAndActiveTab: function() {
            this.selectionTextInEditorContent = '';

            if (this.macroId) {
                // find panel which has corresponding macroId
                this.panels.setSelectedForPanelByMacroId(this.macroId);
            }

            // handle logic when users double-click JIRA issue macro placeholder.
            if (this.macroId === config.macroIdJiraIssue ||
                this.macroId === config.macroIdJiraIssueOld) {

                var selectPanelId = 'panel-jira-issue-filter';
                var activeTabId = '';

                // if we active Create Issue Panel of Jira Issue/Filter panel, we will set summary text basing on current selected text
                if (this.openSource === JiraLinksDialogMacroView.OPEN_DIALOG_SOURCE.editorDropdownLink ||
                    this.openSource === JiraLinksDialogMacroView.OPEN_DIALOG_SOURCE.editorHotKey) {

                    // move to creating issue panel
                    if (tinyMCE.activeEditor.selection) {
                        this.selectionTextInEditorContent = tinyMCE.activeEditor.selection.getContent({format: 'text'});
                    }

                    if (this.selectionTextInEditorContent) {
                        activeTabId = 'tab-create-issue';
                        this.panels.setActiveForTab(selectPanelId, activeTabId, true);
                    }
                } else if (this.openSource === JiraLinksDialogMacroView.OPEN_DIALOG_SOURCE.editorMacro) {
                    // open dialog from double-clicking macro placeholder in editor.
                    activeTabId = 'tab-search-issue';
                    this.panels.setActiveForTab(selectPanelId, activeTabId, true);
                }
            }
        },

        _getJQLJiraIssues: function(obj) {
            if (obj.hasOwnProperty('jqlQuery')) {
                return obj['jqlQuery'];
            }

            var positiveIntegerRegex = /^([0-9]\d*)$/;
            var arrayParams = ['count','columns','title','renderMode','cache','width','height','server','serverId','anonymous','baseurl', 'showSummary'];
            for (var prop in obj) {
                if ($.inArray(prop, arrayParams) == -1 && obj.hasOwnProperty(prop)) {
                    if (positiveIntegerRegex.test(prop)) {
                        return obj[prop];
                    }

                    return prop += ' = ' + obj[prop];
                }
            }

            return '';
        },

        _getParamsJiraIssues: function(macroOptions) {
            var params = {};

            if (macroOptions.params['url']) {
                params['searchStr'] = macroOptions.params['url'];
                return params;
            }

            params['maximumIssues'] = macroOptions.params['maximumIssues'];

            // macro param is JQL | Key
            var jqlStr = macroOptions.defaultParameterValue ||
                        this._getJQLJiraIssues(macroOptions.params);

            if (typeof (jqlStr) === 'undefined') {
                params['searchStr'] = EMPTY_VALUE;
            } else {
                params['searchStr'] = jqlStr;
            }
            // macro param is server
            if (typeof (macroOptions.params['server']) !== 'undefined') {
                params['serverName'] = macroOptions.params['server'];
            } else {
                // get server primary
                for (var i = 0; i < AJS.Editor.JiraConnector.servers.length; i++) {
                    if (AJS.Editor.JiraConnector.servers[i].selected) {
                        params['serverName'] = AJS.Editor.JiraConnector.servers[i].name;
                        break;
                    }
                }
            }

            return params;
        },

        // parse params from macro data
        _parseParamsFromMacro: function(macroOptions) {
            var params = this._getParamsJiraIssues(macroOptions);

            var count = macroOptions.params['count'];
            if (!count) {
                count = 'false';
            }

            params['count'] = count;

            var columns = macroOptions.params['columns'];
            if (columns && columns.length) {
                params['columns'] = columns;
            }

            return params;
        },

        /**
         * Handle clicking insert button to submit dialog
         */
        handleClickInsertButton: function(e) {
            if (!this.currentTabContentView) {
                AJS.debug('Error: can not find current panel view object!');
                this.close();
                return;
            }

            // for new dialog
            if (this.currentTabContentView.getUserInputData) {
                var params = this.currentTabContentView.getUserInputData();

                if (AJS.Editor.inRichTextMode() && params) {
                    tinymce.confluence.macrobrowser.macroBrowserComplete({
                        name: this.macroId,
                        params: params
                    });

                    this.close();
                }
            } else {
                // for legacy code, let legacy code decide closing the dialog
                try {
                    if (this.currentTabContentView.insertLink) {
                        this.currentTabContentView.insertLink(this.currentTabContentView);
                    }
                } catch (e) {
                    AJS.debug(e);
                }
            }

            this.sendInsertDialogAnalytic();
        }

    }, {
        OPEN_DIALOG_SOURCE: {
            macroBrowser: 'macro_browser',
            editorBraceKey: 'editor_brace_key',
            editorHotKey: 'editor_hot_key',
            editorDropdownLink: 'editor_dropdown_link',
            instructionalText: 'instructional text',
            editorMacro: 'editor_macro'
        }
    });

    _.extend(JiraLinksDialogMacroView.prototype, AnalyticMixin);

    return JiraLinksDialogMacroView;
});

// for compatible with old code, we need to export this model as global var.
require('confluence/jim/amd/module-exporter').exportModuleAsGlobal(
    'confluence/jim/macro-browser/editor/dialog/jira-links-dialog-macro-view',
    'JiraLinksDialogMacroView'
);
