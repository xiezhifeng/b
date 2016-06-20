require([
    'ajs',
    'jquery'
], function(
    AJS,
    $
){
    'use strict';

    var WEB_JIM_RESOURCE = "wr!confluence.extra.jira:jira-issues-view-mode-async-resource";

    var initAsyncProcessPlaceHolder = function () {
        var $jiraIssuesEls = $('.wiki-content [data-jira-key][data-client-id]');
        if ($jiraIssuesEls.length == 0) {
            return false;
        }

        WRM.require(WEB_JIM_RESOURCE, function() {
            require([
                'confluence/jim/jira/jira-issues-view-mode/lazy-loading',
                'confluence/jim/jira/jira-issues-view-mode/fix-ui'
            ], function(JiraIssuesLazyLoading, JiraIssuesFixUI) {
                JiraIssuesLazyLoading.init($jiraIssuesEls).done(function() {
                    JiraIssuesFixUI.fixBreakIconInOldConf();
                });
            });
        });
    };

    AJS.toInit(initAsyncProcessPlaceHolder);

    //This a hack at rendering Inline Comments asynchronously
    AJS.bind("ic-jim-async-supported", initAsyncProcessPlaceHolder);
});