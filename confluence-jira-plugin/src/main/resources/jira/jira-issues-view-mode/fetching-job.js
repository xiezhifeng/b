define('confluence/jim/jira/jira-issues-view-mode/fetching-job', [
    'jquery',
    'ajs',
    'confluence/jim/confluence-shim',
    'confluence/jim/util/retry-caller'
], function(
    $,
    AJS,
    Confluence,
    retryCaller
) {
    'use strict';

     /**
     * Fetching Job object - abstract of ajax call to fetch content
     * @param options
     * @constructor
     */
    var FetchingJob = function(options) {
        this.clientIds = options.clientIds;

        var TICK = 1000;

        // we need total ~120 seconds in server side to render timeout error.
        this.TICK_RETRIES = [
            0,
            1 * TICK,
            1 * TICK,
            2 * TICK,
            3 * TICK,
            5 * TICK,
            8 * TICK,
            13 * TICK,
            13 * TICK,
            13 * TICK
        ];

        // returned HTTP code which will help to detect whether reloading data.
        this.RETRY_HTTP_CODE = 202;
    };

    FetchingJob.prototype.startJob = function() {
         return this.fetchSingeJiraServer();
    };

    /**
     * Begin to featch data from a Jira Server
     * @param jiraServerId
     * @returns {Object} a jQuery Deferred object
     */
    FetchingJob.prototype.fetchSingeJiraServer = function() {
        var jimUrl = AJS.contextPath() + '/rest/jiraanywhere/1.0/jira/clientIds/{0}';
        var promise = $.ajax({
            type: 'GET',
            url: AJS.format(jimUrl, this.clientIds),
            cache: true
        });

        // we need to cache jira server id so that we know which Promise object is rejected later
        // and render error message
        promise.clientIds = this.clientIds;

        return promise;
    };

    /**
     * Start the job with having retry ability
     * @returns {Object} a Promise object
     */
    FetchingJob.prototype.startJobWithRetry = function() {
        return retryCaller(
                this.startJob, {
                    name: this.clientIds, // for logging
                    delays: this.TICK_RETRIES,
                    context: this,
                    tester: function(dataOfAServer, successMessage, promise) {
                        // if status is 202, we need to retry to call the same ajax again
                        return promise && promise.status === this.RETRY_HTTP_CODE;
                    }
                }
        );
    };

    return FetchingJob;
});
