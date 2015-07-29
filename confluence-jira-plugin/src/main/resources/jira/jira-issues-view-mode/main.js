require([
    'jquery',
    'confluence/jim/jira/jira-issues-view-mode/lazy-loading'
], function(
    $,
    JiraIssuesLazyLoading
){
    'use strict';

    $(document).ready(function() {
        JiraIssuesLazyLoading.init().done(function() {
            // TODO: do some works after finished rendering single JIM.
        });
    });
});