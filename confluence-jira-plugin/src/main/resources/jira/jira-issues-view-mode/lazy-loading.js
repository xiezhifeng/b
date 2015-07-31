define('confluence/jim/jira/jira-issues-view-mode/lazy-loading', [
    'jquery',
    'ajs',
    'underscore',
    'confluence/jim/util/deferred-utils',
    'confluence/jim/jira/jira-issues-view-mode/fetching-job'
], function(
    $,
    AJS,
    _,
    deferredUtils,
    FetchingJob
) {
    'use strict';

    var DARK_FEATURE_KEY = 'jim.enable.strategy.fetch.and.wait.all.jira.servers.in.once';

    // list of jQuery DOM object of all single JIM in page
    var $jiraIssuesEls = null;

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
            $jiraIssuesEls = $('.wiki-content .jira-issue');

            if (AJS.DarkFeatures.isEnabled(DARK_FEATURE_KEY)) {
                return core.loadAllJiraServersInOnceStrategy();
            } else {
                return core.loadOneByOneJiraServerStrategy();
            }
        }
    };

    return exportModule;
});
