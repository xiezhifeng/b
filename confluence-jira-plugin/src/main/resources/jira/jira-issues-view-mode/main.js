require([
    'ajs',
    'jquery',
    'confluence/jim/jira/jira-issues-view-mode/lazy-loading',
    'confluence/jim/jira/jira-issues-view-mode/fix-ui',
    'confluence/jim/jira/jira-issues-view-mode/refresh-table'
], function(
    AJS,
    $,
    JiraIssuesLazyLoading,
    JiraIssuesFixUI,
    JiraRefreshTableMacro
){
    'use strict';

    $(document).ready(function() {
        JiraIssuesLazyLoading.init().done(function() {
            JiraIssuesFixUI.fixBreakIconInOldConf();
        });

        // start a module
        JiraRefreshTableMacro.init();
    });
});