define('confluence/jim/util/analytic', [
    'ajs'
],
function(
    AJS
) {
    'use strict';

    var analyticSupport;

    // support for old version CONF, ex: 5.7
    if (AJS.Confluence.Analytics) {
        analyticSupport = AJS.Confluence.Analytics;
    } else {
        analyticSupport = require('confluence/analytics-support');
    }

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
