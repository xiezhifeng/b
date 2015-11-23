require([
    'jquery',
    'ajs',
    'confluence/jim/macro-browser/editor/dialog/jira-links-dialog-macro-view',
    'confluence/jim/macro-browser/editor/jirasprint/sprint-panel-view',
    'confluence/jim/macro-browser/editor/util/config',
    'confluence/jim/macro-browser/editor/dialog/panel-collection'
],
function(
    $,
    AJS,
    JiraLinksDialogMacroView,
    JiraSprintPanelView,
    config,
    PanelCollection
) {
    'use strict';

    // Although we have AMD module with key='confluence-macro-browser/macro-browser', but we want to support  CONF. old version.
    var macroBrowser = AJS.MacroBrowser;
    var isMac = navigator.platform.toLowerCase().indexOf('mac') !== -1;

    // Because of the compatibility with old code, we need to some global variables, ex: AJS.Editor.JiraConnector.Panel.Recent.
    var panels = new PanelCollection([
        // jira issue/filter panel.
        {
            id: 'panel-jira-issue-filter',
            name: AJS.I18n.getText('jira.macro.dialog.panel.issue'),
            macroId: config.macroIdJiraIssue,
            tooltipFooter: AJS.I18n.getText('insert.jira.issue.dialog.help.shortcut', isMac ? 'Cmd' : 'Ctrl'),
            isSelect: true,
            tabs: [
                {
                    id: 'tab-search-issue',
                    isActive: true,
                    name: AJS.I18n.getText('insert.jira.issue.search'),
                    PanelContentView: AJS.Editor.JiraConnector.Panel.Search
                },
                {
                    id: 'tab-create-issue',
                    isActive: false,
                    name: AJS.I18n.getText('insert.jira.issue.create'),
                    PanelContentView: AJS.Editor.JiraConnector.Panel.Create
                },
                {
                    id: 'tab-recent-issue',
                    isActive: false,
                    name: AJS.I18n.getText('insert.jira.issue.recent'),
                    PanelContentView: AJS.Editor.JiraConnector.Panel.Recent
                }
            ]
        },
        // jira sprint panel.
        {
            id: 'panel-jira-sprint',
            isSelect: false,
            name: AJS.I18n.getText('jira.macro.dialog.panel.sprint'),
            macroId: config.macroIdJiraSprint,
            tabs: [{
                id: 'tab-sprint',
                isActive: true,
                name: AJS.I18n.getText('jira.macro.dialog.panel.sprint'),
                PanelContentView: JiraSprintPanelView
            }]
        },
        {
            id: 'panel-jira-chart',
            isSelect: false,
            name: AJS.I18n.getText('jira.macro.dialog.panel.chart'),
            macroId: config.macroIdJiraChart,
            tabs: [
                {
                    id: 'tab-pie-chart',
                    isActive: true,
                    name: AJS.I18n.getText('jirachart.panel.piechart.title'),
                    PanelContentView: AJS.Editor.JiraChart.Panel.PieChart
                },
                {
                    id: 'tab-created-resolved-chart',
                    isActive: false,
                    name: AJS.I18n.getText('jirachart.panel.createdvsresolvedchart.title'),
                    PanelContentView: AJS.Editor.JiraChart.Panel.CreatedVsResolvedChart
                },
                {
                    id: 'tab-two-mentional-chart',
                    isActive: false,
                    name: AJS.I18n.getText('jirachart.panel.twodimensionalchart.title'),
                    PanelContentView: AJS.Editor.JiraChart.Panel.TwoDimensionalChart
                }
            ]
        }
    ]);

    var jiraDialogView = new JiraLinksDialogMacroView({panels: panels});

    // support other dialogs can send trigger to open jira-sprint dialog
    AJS.bind('jira.links.macro.dialog.open', function(e, options) {
        jiraDialogView.open(options);
    });

    // support other dialogs can send trigger to close jira-sprint dialog
    AJS.bind('jira.links.macro.dialog.close', function() {
        jiraDialogView.close();
    });

    // support legacy code to enable insert button state of the dialog
    AJS.bind('jira.links.macro.dialog.enabled', function() {
        jiraDialogView.toggleEnableInsertButton(true);
    });

    // support legacy code to disable insert button state of the dialog
    AJS.bind('jira.links.macro.dialog.disabled', function() {
        jiraDialogView.toggleEnableInsertButton(false);
    });

    // support legacy code to refresh the new dialog
    // when users do authentication, we need to refresh all tabs of current panels
    AJS.bind('jira.links.macro.dialog.refresh', function() {
        jiraDialogView.refresh();
    });

    macroBrowser.setMacroJsOverride(config.macroIdJiraSprint, {
        opener: jiraDialogView.open.bind(jiraDialogView)
    });

    macroBrowser.setMacroJsOverride(config.macroIdJiraIssue, {
        opener: jiraDialogView.open.bind(jiraDialogView)
    });

    macroBrowser.setMacroJsOverride(config.macroIdJiraIssueOld, {
        opener: jiraDialogView.open.bind(jiraDialogView)
    });

    macroBrowser.setMacroJsOverride(config.macroIdJiraChart, {
        opener: jiraDialogView.open.bind(jiraDialogView)
    });
});
