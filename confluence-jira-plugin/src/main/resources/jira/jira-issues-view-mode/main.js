require([
    'jquery',
    'confluence/jim/jira/jira-issues-view-mode/lazy-loading',
    'confluence/jim/jira/jira-issues-view-mode/fix-ui'
], function(
    $,
    JiraIssuesLazyLoading,
    JiraIssuesFixUI
){
    'use strict';

    $(document).ready(function() {
        JiraIssuesLazyLoading.init().done(function() {
            JiraIssuesFixUI.fixBreakIconInOldConf();
        });
    });
});