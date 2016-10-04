define('confluence/jim/jira/jira-issues-view-mode/lazy-loading', [
    'jquery',
    'ajs',
    'underscore',
    'confluence/jim/jira/jira-issues-view-mode/fetching-job'
], function(
    $,
    AJS,
    _,
    FetchingJob
) {
    'use strict';
    var WEB_RESOURCE_TABLE = "wr!confluence.extra.jira:refresh-resources";
    var jiraRefreshTableMacro;

    var ui = {
        /**
         * CONF-39871: we can not cache these elements because they may be updated/replaced some times.
         * @returns {jQuery} - List of jQuery DOM object of all single JIM in page
         */
        queryJiraIssuesEls: function() {
            return $('.wiki-content [data-jira-key][data-client-id]');
        },

        renderUISingleJIMFromMacroHTML: function(htmlMacros, $elsGroupByServerKey) {
            _.each(
                htmlMacros,
                function(htmlPlaceHolders, issueKey) {
                    var $elsGroupByIssueKey = $elsGroupByServerKey.filter('[data-jira-key="' + issueKey + '"]');
                    $elsGroupByIssueKey.each(function(index, jiraIssueEl) {
                        var $jiraElement = $(jiraIssueEl);
                        if ($jiraElement.hasClass('jira-table')) {
                            jiraRefreshTableMacro.updateRefreshedElement($jiraElement, htmlPlaceHolders[0]);
                        } else {
                            $jiraElement.replaceWith(htmlPlaceHolders[index] || htmlPlaceHolders[0]);
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
            var errorJimClass = ['aui-message', 'aui-message-warning', 'jim-error-message'];
            var errorMessage = AJS.I18n.getText('jiraissues.unexpected.error') + ' ' + ajaxErrorMessage;

            if ($elsGroupByServerKey.hasClass('jira-table')) {
                errorJimClass.push('jim-error-message-table');
                jiraRefreshTableMacro.updateRefreshedElement($elsGroupByServerKey, errorMessage);
            } else {
                errorJimClass.push('jim-error-message-single');
                $elsGroupByServerKey.find('.summary').text(errorMessage);
                $elsGroupByServerKey.find('.jira-status').remove();
                $elsGroupByServerKey.find('.issue-placeholder').remove();
                $elsGroupByServerKey.find('.aui-icon-wait').remove();
            }
            $elsGroupByServerKey
                .removeClass('jira-issue jira-table')
                .addClass(errorJimClass.join(' '));
        }
    };

    var ajaxHandlers = {
        handleAjaxSuccess: function(data, status, promise) {
            var $jiraIssuesEls = ui.queryJiraIssuesEls();
            _.each(data, function(clientData) {
                var $elsGroupByServerKey = $jiraIssuesEls.filter('[data-client-id="' + clientData.clientId + '"]');
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
            var $jiraIssuesEls = ui.queryJiraIssuesEls();
            _.each(clientIdErrors, function(clientId) {
                var $elsGroupByServerKey = $jiraIssuesEls.filter('[data-client-id="' + clientId + '"]');
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
            var $jiraIssuesEls = ui.queryJiraIssuesEls();
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
            if (job.clientIds) {
                jobs.push(job);
            }

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
                    .fail(function(promise, error, ajaxErrorMessage) {
                        ajaxHandlers.handleAjaxError(promise, ajaxErrorMessage);
                    })
                    .always(function() {
                        if (++counter === totalNumberOfRequests) {
                            mainDefer.resolve();
                        }
                    })
                    .progress(function(data, status, promise) {
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

    var jimResource = {
        loadTableResourceIfNeeded: function() {
            var resDefer = $.Deferred();
            var $jiraTableIssues = $('.wiki-content .jira-table[data-jira-key]');

            if ($jiraTableIssues.length) {
                WRM.require(WEB_RESOURCE_TABLE, function() {
                    jiraRefreshTableMacro = require('confluence/jim/jira/jira-issues-view-mode/refresh-table');
                    jiraRefreshTableMacro.init();
                    resDefer.resolve();
                });
            } else {
                resDefer.resolve();
            }

            return resDefer.promise();
        }
    };

    var exportModule = {
        /**
         * Initialize the module
         * @return {Object} a Promise object
         */
        init: function($jiraIssuesElement) {
            return $.when(jimResource.loadTableResourceIfNeeded())
                .pipe(core.loadOneByOneJiraServerStrategy);
        }
    };

    return exportModule;
});
