require([
    'jquery',
    'ajs',
    'confluence/jim/editor/jirasprint/sprint-panel-view',
    'confluence/jim/editor/jirasprint/dialog-view',
    'confluence/jim/editor/util/config'
],
function(
    $,
    AJS,
    JiraSprintPanelView,
    JiraSprintDialogView,
    config
) {
    'use strict';

    var jiraSprintDialogView = new JiraSprintDialogView({
        panels: [new JiraSprintPanelView()],
        macroId: config.macroIdSprint,
        externalLinks: [
            {
                id: 'open-jira-issue-dialog',
                label: AJS.I18n.getText('jira.issue'),
                callBack: function() {
                    if (AJS.Editor.JiraConnector) {
                        this.close();
                        AJS.Editor.JiraConnector.openCleanDialog(false);
                    }
                }
            },
            {
                id: 'open-jira-chart-dialog',
                label: AJS.I18n.getText('confluence.extra.jira.jirachart.label'),
                callBack: function() {
                    if (AJS.Editor.JiraChart) {
                        this.close();
                        AJS.Editor.JiraChart.open();
                    }
                }
            }
        ]
    });

    // support other dialogs can send trigger to open jira-sprint dialog
    AJS.bind('jim.jira.sprint.open', function() {
        jiraSprintDialogView.open();
    });

    AJS.MacroBrowser.setMacroJsOverride(config.macroIdSprint, {
        opener: jiraSprintDialogView.open.bind(jiraSprintDialogView)
    });
});
