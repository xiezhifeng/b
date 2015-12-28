require([
    'ajs',
    'jquery',
    'confluence/jim/jira/jira-issues-view-mode/lazy-loading',
    'confluence/jim/jira/jira-issues-view-mode/fix-ui'
], function(
    AJS,
    $,
    JiraIssuesLazyLoading,
    JiraIssuesFixUI
){
    'use strict';

    $(document).ready(function() {
        JiraIssuesLazyLoading.init().pipe(function() {
            JiraIssuesFixUI.fixBreakIconInOldConf();
        });
    });
});