define('confluence/jim/util/analytic', [
    'ajs',
    'confluence/analytics-support'
],
function(
    AJS,
    analyticSupport
) {
    'use strict';

    return {
        sendPasteSprintLinkEvent: function() {
            var eventName = 'confluence.macro.sprint.paste';
            analyticSupport.publish(eventName);
        },

        sendInsertSprintMacroToEdtiorContentEvent: function() {
            var eventName = 'confluence.macro.sprint.insert';
            analyticSupport.publish(eventName);
        },

        sendOpenSprintDialogEvent: function() {
            var eventName = 'jira.version.sprint.dialog.open';
            analyticSupport.publish(eventName);
        }
    };
});
