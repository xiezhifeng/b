define('confluence/jim/jira/jira-issues-view-mode/lazy-loading', [
    'jquery',
    'ajs',
    'underscore',
    'confluence/jim/util/deferred-utils',
    'confluence/jim/jira/jira-issues-view-mode/fetching-job'
], function($,
        AJS,
        _,
        deferredUtils,
        FetchingJob) {
    'use strict';

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
        handleSuccessAjaxCB: function(dataOfAServer, status, promise) {
            var $elsGroupByServerKey = $jiraIssuesEls.filter('[data-client-id=' + promise.clientId + ']');
            ui.renderUISingleJIMFromMacroHTML(dataOfAServer.htmlMacro, $elsGroupByServerKey);
        },

        /**
         * Callback for error ajax
         * @param promise
         * @param ajaxErrorMessage
         */
        handleErrorAjaxCB: function(promise, ajaxErrorMessage) {
            var $elsGroupByServerKey = $jiraIssuesEls.filter('[data-client-id=' + promise.clientId + ']');
            ui.renderUISingleJIMInErrorCase($elsGroupByServerKey, ajaxErrorMessage);
        }
    };

    var util = {
        /**
         * Scan single Jira Issues Macro DOM to get all unique Jira Servers
         * @returns {Array}
         */
        findAllClientIdInPageContent: function() {
            var clientIds = _.map($jiraIssuesEls, function(item) {
                var clientId = $(item).attr('data-client-id');
                if (clientId) {
                    return clientId;
                }
            });

            return _.uniq(clientIds);
        },

        /**
         * Begin to fetch data from server for per Jira Server we got.
         * @returns {Array} array of Promise object.
         */
        collectFetchingJobs: function() {
            var clientIds = util.findAllClientIdInPageContent();
            var jobs = [];

            _.each(clientIds, function(clientId) {

                var job = new FetchingJob({
                    clientId: clientId
                });

                jobs.push(job);
            });

            return jobs;
        }
    };

    var core = {
        /**
         * loading data as following steps:
         * - Each JIRA server is one ajax request call.
         * - Wait for all ajax called done (success and error)
         * - Render UI basing on returned data from server (in success case) and error message (in error case)
         */
        loadOneByOneJiraServerStrategy: function() {
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
        }
    };

    var timers = {};

    function timer(name) {
        timers[name + '_start'] = window.performance.now();
    }

    function timerEnd(name) {
        if (!timers[name + '_start']) return undefined;
        var time = window.performance.now() - timers[name + '_start'];
        var amount = timers[name + '_amount'] = timers[name + '_amount'] ? timers[name + '_amount'] + 1 : 1;
        var sum = timers[name + '_sum'] = timers[name + '_sum'] ? timers[name + '_sum'] + time : time;
        timers[name + '_avg'] = sum / amount;
        delete timers[name + '_start'];
        return time;
    }

    var exportModule = {
        /**
         * Initialize the module
         * @return {Object} a Promise object
         */
        init: function() {
            timer('asyn.jim');

            $jiraIssuesEls = $('.wiki-content .jira-issue[data-client-id]');

            var dfd = core.loadOneByOneJiraServerStrategy();

            dfd.done(function() {
                var totalDuration = timerEnd('asyn.jim');

                totalDuration = totalDuration / 1000;
                totalDuration = totalDuration.toFixed(2);


                AJS.trigger('analyticsEvent', {
                    name: 'asyn.jim',
                    data: {
                        pageId: Confluence.getContentId(),
                        numberOfJIM: $jiraIssuesEls.length,
                        totalDuration: totalDuration + 's'
                    }
                });
            }.bind(this));

            return dfd;
        }
    };

    return exportModule;
});
