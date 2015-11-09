define('confluence/jim/jira/jira-issues-view-mode/history-handling', [
    'jquery',
    'ajs',
    'underscore',
    'confluence/jim/jira/jira-issues-view-mode/refresh-table'
], function(
    $,
    AJS,
    _,
    jiraRefreshTableMacro
) {
    'use strict';

    var core = {
        getJiraHistoryData: function(clientIds) {
            var HistoryState = _.find(History.store.idToState, function(state) {
                return state.data.clientIds == clientIds;
            });
            if (HistoryState) {
                return HistoryState;
            }
            return null;
        },

        setState: function(clientIds, data, statusCode) {
            if (statusCode == 200) {
                History.replaceState({clientIds: clientIds, macroHtml: data}, undefined, window.location.href);
            }
        }
    };

    return core;
});
