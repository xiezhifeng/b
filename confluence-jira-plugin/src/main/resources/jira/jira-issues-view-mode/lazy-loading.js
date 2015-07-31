define('confluence/jim/jira/jira-issues-view-mode/lazy-loading', [
    'jquery',
    'ajs',
    'underscore',
    'confluence/jim/confluence-shim',
    'confluence/jim/util/retry-caller',
    'confluence/jim/util/deferred-utils'
], function(
    $,
    AJS,
    _,
    Confluence,
    retryCaller,
    deferredUtils
) {
    'use strict';

    var DARK_FEATURE_KEY = 'jim.enable.strategy.fetch.and.wait.all.jira.servers.in.once';

    // list of jQuery DOM object of all single JIM in page
    var $jiraIssuesEls = null;

    /**
     * Fetching Job object - abstract of ajax call to fetch content
     * @param options
     * @constructor
     */
    var FetchingJob = function(options) {
        this.jiraServerId = options.jiraServerId;
        this.clientId = options.clientId;

        var ONE_SECOND = 1000;
        this.TIMER_RETRIES = [
            0,
            2 * ONE_SECOND,
            5 * ONE_SECOND,
            8 * ONE_SECOND,
            10 * ONE_SECOND
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
        var jimUrl = [
            AJS.contextPath(),
            '/rest/jiraanywhere/1.0/jira/page/',
            Confluence.getContentId(),
            '/server/', this.jiraServerId,
            '/', this.clientId
        ];

        var promise = $.ajax({
            type: 'GET',
            url: jimUrl.join(''),
            cache: true
        });

        // we need to cache jira server id so that we know which Promise object is rejected later
        // and render error message
        promise.jiraServerId = this.jiraServerId;
        promise.clientId = this.clientId;

        return promise;
    };

    FetchingJob.prototype.startJobWithRetry = function() {
        return retryCaller(
                this.startJob, {
                    name: this.jiraServerId, // for logging
                    delays: this.TIMER_RETRIES,
                    context: this,
                    tester: function(dataOfAServer, successMessage, promise) {
                        // if status is 202, we need to retry to call the same ajax again
                        return promise && promise.status === this.RETRY_HTTP_CODE;
                    }
                }
        );
    };

    /*end fetching job object*/

    var ui = {
        renderUISingleJIMFromMacroHTML: function(htmlMacros, $elsGroupByServerKey) {
            _.each(
                htmlMacros,
                function(htmlPlaceHolders, issueKey) {

                    var $elsGroupByIssueKey = $elsGroupByServerKey.filter('[data-issue-key=' + issueKey + ']');

                    $elsGroupByIssueKey.each(function(index, jiraIssueEl) {
                        $(jiraIssueEl).replaceWith(htmlPlaceHolders[index]);
                    });
            });
        },

        /**
         * Convert JIM placeholder to the AUI compatible warning message JIM
         * @param $elsGroupByServerKey
         * @param ajaxErrorMessage
         */
        renderUISingleJIMInErrorCase: function($elsGroupByServerKey, ajaxErrorMessage) {
            var errorMessage = AJS.I18n.getText('jiraissues.unexpected.error');

            $elsGroupByServerKey.find('.summary').text(errorMessage + ' ' + ajaxErrorMessage);
            $elsGroupByServerKey.find('.jira-status').remove();
            $elsGroupByServerKey.find('.issue-placeholder').remove();
            $elsGroupByServerKey.find('.aui-icon-wait').remove();

            var errorJimClass = 'aui-message aui-message-warning ' +
                    'jim-error-message jim-error-message-single ';

            $elsGroupByServerKey
                    .removeClass('jira-issue')
                    .addClass(errorJimClass);
        }
    };

    var handlersAjax = {
        /**
         * Callback for success ajax
         * @param dataOfAServer
         */
        handleSuccessAjaxCB: function(dataOfAServer) {
            var $elsGroupByServerKey = $jiraIssuesEls.filter('[data-server-id=' + dataOfAServer.serverId + ']');
            ui.renderUISingleJIMFromMacroHTML(dataOfAServer.htmlMacro, $elsGroupByServerKey);
        },

        /**
         * Callback for error ajax
         * @param promise
         * @param ajaxErrorMessage
         */
        handleErrorAjaxCB: function(promise, ajaxErrorMessage) {
            var $elsGroupByServerKey = $jiraIssuesEls.filter('[data-server-id=' + promise.jiraServerId + ']');
            ui.renderUISingleJIMInErrorCase($elsGroupByServerKey, ajaxErrorMessage);
        }
    };

    var util = {
        /**
         * Scan single Jira Issues Macro DOM to get all unique Jira Servers
         * @returns {Array}
         */
        findAllJiraServersInPageContent: function() {
            var servers = _.map($jiraIssuesEls, function(item) {
                return $(item).attr('data-server-id');
            });

            return _.uniq(servers);
        },

        /**
         * Get client id by server id.
         * Each server id has a client id.
         * @param serverId
         * @returns {*}
         */
        getClientIdFromJIMMacro: function(serverId) {
            return $jiraIssuesEls
                    .filter('[data-server-id=' + serverId + ']')
                    .first()
                    .attr('data-client-id');
        },

        /**
         * Begin to fetch data from server for per Jira Server we got.
         * @returns {Array} array of Promise object.
         */
        collectFetchingJobs: function() {
            var promises = [];
            var servers = util.findAllJiraServersInPageContent();
            var jobs = [];

            _.each(servers, function(jiraServerId) {
                var clientId = util.getClientIdFromJIMMacro(jiraServerId);

                var job = new FetchingJob({
                    clientId: clientId,
                    jiraServerId: jiraServerId
                });

                jobs.push(job);
            });

            return jobs;
        }
    };

    var core = {
        /**
         * When disable dark feature key: "jim.enable.strategy.fetch.and.wait.all.jira.servers.in.once " and
         * loading data as following steps:
         * - Each JIRA server is one ajax request call.
         * - Wait for all ajax called done (success and error)
         * - Render UI basing on returned data from server (in success case) and error message (in error case)
         */
        loadOneByOneJiraServerStrategy: function() {
            AJS.debug('JIM lazy lading: waiting returned data from one by one serverr');

            var jobs = util.collectFetchingJobs();
            var totalNumberOfRequests = jobs.length;

            // we need to know when all request are solved.
            var mainDefer = $.Deferred();
            var counter = 0;

            jobs.forEach(function(job) {
                job.startJobWithRetry()
                    .done(handlersAjax.handleSuccessAjaxCB)
                    .fail(function(promise, error, ajaxErrorMessage) {
                        handlersAjax.handleErrorAjaxCB(promise, ajaxErrorMessage);
                    })
                    .always(function() {
                        ++counter;

                        if (counter === totalNumberOfRequests) {
                            mainDefer.resolve();
                        }
                    });
            });

            return mainDefer.promise();
        },

        /**
         * When enable dark feature key: "jim.enable.strategy.fetch.and.wait.all.jira.servers.in.once " and
         * loading data as following steps:
         * - Each JIRA server is one ajax request call.
         * - Wait for all ajax called done (success and error)
         * - Render UI basing on returned data from server (in success case) and error message (in error case)
         */
        loadAllJiraServersInOnceStrategy: function() {
            AJS.debug('JIM lazy lading: waiting returned data from all serverrs at once');
            var jobs = util.collectFetchingJobs();

            // convert all Deferred objects to new Deferred objects which are always resolved.
            var promises = _.map(jobs, function(job) {
                return job.startJobWithRetry();
            });

            promises = _.map(promises, function(promise) {
                return deferredUtils.convertPromiseToAlwaysResolvedDeferred(promise);
            });

            // fetch all ajax calls and wait for them all.
            var mainDefer = $.when.apply($, promises)
                .done(function() {
                    var returnedDataByServers = _.toArray(arguments);

                    _.each(returnedDataByServers, function(dataOfAServer) {
                        var $elsGroupByServerKey;

                        if (dataOfAServer && dataOfAServer.serverId) {
                            handlersAjax.handleSuccessAjaxCB(dataOfAServer);
                        } else {
                            var promise = dataOfAServer[0];
                            var ajaxErrorMessage = dataOfAServer[2];
                            handlersAjax.handleErrorAjaxCB(promise, ajaxErrorMessage);
                        }
                    });
                });

            return mainDefer;
        }
    };

    var exportModule = {
        /**
         * Initialize the module
         * @return {Object} a Promise object
         */
        init: function() {
            $jiraIssuesEls = $('.jira-issue');

            if (AJS.DarkFeatures.isEnabled(DARK_FEATURE_KEY)) {
                return core.loadAllJiraServersInOnceStrategy();
            } else {
                return core.loadOneByOneJiraServerStrategy();
            }
        }
    };

    return exportModule;
});
