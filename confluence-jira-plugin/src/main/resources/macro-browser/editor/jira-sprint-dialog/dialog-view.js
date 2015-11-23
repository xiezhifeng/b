define('confluence/jim/macro-browser/editor/jirasprint/dialog-view', [
    'ajs',
    'confluence/jim/macro-browser/editor/dialog/abstract-dialog-view',
    'confluence/jim/macro-browser/editor/util/config'
],
function(
    AJS,
    AbstractDialogView,
    config
) {
    'use strict';

    var JiraSprintDialog = AbstractDialogView.extend({
        initialize: function(options) {
            AbstractDialogView.prototype.initialize.apply(this, arguments);

            this.dialogId = 'jira-sprint';
            this.macroId = config.macroIdSprint;
            this.cssClassInsertButton = 'insert-jira-sprint-macro-button';
            this.dialogTitle = AJS.I18n.getText('jira.sprint.macro.popup.title');
        }
    });

    return JiraSprintDialog;
});