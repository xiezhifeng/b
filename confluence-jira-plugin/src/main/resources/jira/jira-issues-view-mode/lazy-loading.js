define('confluence/jim/jira/jira-issues-view-mode/lazy-loading', [
    'jquery',
    'ajs',
    'underscore',
    'confluence/jim/jira/jira-issues-view-mode/fetching-job',
    'confluence/jim/jira/jira-issues-view-mode/refresh-table',
    'confluence/jim/jira/jira-issues-view-mode/history-handling'
], function(
    $,
    AJS,
    _,
    FetchingJob,
    jiraRefreshTableMacro,
    historyHandling
) {
    'use strict';

    // list of jQuery DOM object of all single JIM in page
    var $jiraIssuesEls = null;

    var ui = {
        renderUISingleJIMFromMacroHTML: function(htmlMacros, $elsGroupByServerKey) {
            _.each(
                htmlMacros,
                function(htmlPlaceHolders, issueKey) {
                    var $elsGroupByIssueKey = $elsGroupByServerKey.filter('[data-jira-key=' + issueKey + ']');
                    $elsGroupByIssueKey.each(function(index, jiraIssueEl) {
                        var $jiraElement = $(jiraIssueEl);
                        if ($jiraElement.hasClass('jira-table')) {
                            jiraRefreshTableMacro.updateRefreshedElement($jiraElement, htmlPlaceHolders[index]);
                        } else {
                            $jiraElement.replaceWith(htmlPlaceHolders[index]);
                        }
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

    var ajaxHandlers = {
        handleAjaxSuccess: function(data, status, promise) {
            _.each(data, function(clientData) {
                var $elsGroupByServerKey = $jiraIssuesEls.filter('[data-client-id=' + clientData.clientId + ']');
                if (clientData.status === 200) {
                    ui.renderUISingleJIMFromMacroHTML(JSON.parse(clientData.data).htmlMacro, $elsGroupByServerKey);
                } else if (clientData.status !== 202) {
                    ui.renderUISingleJIMInErrorCase($elsGroupByServerKey, clientData.data);
                }
            });
        },

        /**
         * Callback for error ajax
         * @param promise
         * @param ajaxErrorMessage
         */
        handleAjaxError: function(promise, ajaxErrorMessage) {
            var clientIdErrors = promise.clientIds.split(',');
            _.each(clientIdErrors, function(clientId) {
                var $elsGroupByServerKey = $jiraIssuesEls.filter('[data-client-id=' + clientId + ']');
                ui.renderUISingleJIMInErrorCase($elsGroupByServerKey, ajaxErrorMessage);
            });
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

            var job = new FetchingJob({
                clientIds: clientIds.join(',')
            });
            jobs.push(job);

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
                var HistoryState = historyHandling.getJiraHistoryData(job.clientIds);
                if (HistoryState) {
                    ajaxHandlers.handleAjaxSuccess(HistoryState.data.macroHtml);
                    return false;
                }

                job.startJobWithRetry()
                    .fail(function(promise, error, ajaxErrorMessage) {
                        ajaxHandlers.handleAjaxError(promise, ajaxErrorMessage);
                    })
                    .always(function() {
                        if (++counter === totalNumberOfRequests) {
                            mainDefer.resolve();
                        }
                    })
                    .progress(function(data, status, promise) {
                        historyHandling.setState(promise.clientIds, data, promise.status);
                        ajaxHandlers.handleAjaxSuccess.apply(this, arguments);
                        var remainingClientIds = _.reduce(data, function(clientIds, item) {
                            if(item.status == 202) {
                                clientIds.push(item.clientId)
                            }
                            return clientIds;
                        }, []);
                        job.clientIds = remainingClientIds.join(',');
                    });
            });

            return mainDefer.promise();
        }
    };

    var exportModule = {
        /**
         * Initialize the module
         * @return {Object} a Promise object
         */
        init: function() {
            $jiraIssuesEls = $('.wiki-content [data-jira-key][data-client-id]');
            return core.loadOneByOneJiraServerStrategy();
        }
    };

    return exportModule;
});
