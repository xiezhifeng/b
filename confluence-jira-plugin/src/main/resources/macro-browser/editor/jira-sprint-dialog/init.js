require([
    'jquery',
    'ajs',
    'underscore',
    'confluence/jim/macro-browser/editor/jirasprint/sprint-panel-view',
    'confluence/jim/macro-browser/editor/jirasprint/dialog-view',
    'confluence/jim/macro-browser/editor/util/config'
],
function(
    $,
    AJS,
    _,
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
    AJS.bind('jim.jira.sprint.open', function(e, options) {
        var ops = _.extend({
            name: config.macroIdSprint
        }, options);

        jiraSprintDialogView.open(ops);
    });

    AJS.MacroBrowser.setMacroJsOverride(config.macroIdSprint, {
        opener: jiraSprintDialogView.open.bind(jiraSprintDialogView)
    });
});
