define('confluence/jim/jira/jira-issues-view-mode/lazy-loading', [
    'jquery',
    'ajs',
    'confluence',
    'underscore',
    'confluence/jim/util/retry-caller',
    'confluence/jim/util/deferred-utils'
], function(
    $,
    AJS,
    Confluence,
    _,
    retryCaller,
    deferredUtils
) {
    'use strict';

    var DARK_FEATURE_KEY = 'jim.enable.strategy.fetch.and.wait.all.jira.servers.in.once';
    var $jiraIssuesEls = null;
    var ONE_MINUTE = 1000 * 60;
    var TIMER_RETRIES = [0, 0.5 * ONE_MINUTE, 2 * ONE_MINUTE, 3 * ONE_MINUTE, 4 * ONE_MINUTE];
    var RETRY_HTTP_CODE = 202;

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
        renderUISingleJIMInErrorCase: function($elsGroupByServerKey, ajaxErrorMessage) {
        var errorMessage = AJS.I18n.getText('jiraissues.unexpected.error');

        $elsGroupByServerKey.find('.summary').text(errorMessage + ' ' + ajaxErrorMessage);
        $elsGroupByServerKey.find('.jira-status').remove();
        $elsGroupByServerKey.find('.icon').remove();
        $elsGroupByServerKey.find('.icon').remove();

        var errorJimClass = 'aui-message aui-message-warning ' +
                'jim-error-message jim-error-message-single ' +
                'conf-macro output-block';

        $elsGroupByServerKey
                .removeClass('jira-issue')
                .addClass(errorJimClass);
    }
    };

    var handlersAjax = {
        handleSuccessAjaxCB: function(dataOfAServer) {
            var $elsGroupByServerKey = $jiraIssuesEls.filter('[data-server-id=' + dataOfAServer.serverId + ']');
            ui.renderUISingleJIMFromMacroHTML(dataOfAServer.htmlMacro, $elsGroupByServerKey);
        },

        handleErrorAjaxCB: function(promise, ajaxErrorMessage) {
            var $elsGroupByServerKey = $jiraIssuesEls.filter('[data-server-id=' + promise.jimJiraServerId + ']');
            ui.renderUISingleJIMInErrorCase($elsGroupByServerKey, ajaxErrorMessage);
        }
    };

    var util = {
        /**
         * Begin to featch data from a Jira Server
         * @param jiraServerId
         * @returns {Object} a jQuery Deferred object
         */
        fetchSingeJiraServer: function(jiraServerId) {
            var clientId = _.find($jiraIssuesEls, function(item) {
                return $(item).attr('data-server-id') == jiraServerId;
            });
            var jimUrl = [
                AJS.contextPath(),
                '/rest/jiraanywhere/1.0/jira/page/',
                Confluence.getContentId(),
                '/server/', jiraServerId,
                '/', $(clientId).attr('data-client-id')
            ];

            var promise = $.ajax({
                type: 'GET',
                url: jimUrl.join(''),
                cache: true
            });

            // we need to cache jira server id so that we know which Promise object is rejected later
            // and render error message
            promise.jimJiraServerId = jiraServerId;
            return promise;
        },

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
         * Begin to fetch data from server for per Jira Server we got.
         * @returns {Array} array of deferred object.
         */
        startAjaxFetching: function() {
            var deferreds = [];
            var servers = util.findAllJiraServersInPageContent();

            _.each(servers, function(serverId) {
                var defer = util.fetchSingeJiraServer(serverId);
                deferreds.push(defer);
            });

            return deferreds;
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
            var deferreds = util.startAjaxFetching();
            var totalNumberOfRequests = deferreds.length;

            // we need to know when all request are solved.
            var defer = $.Deferred();
            var promise = defer.promise();

            var counter = 0;
            deferreds.forEach(function(defer) {

                var retryFunc = function() {
                    return defer;
                };

                retryCaller(retryFunc, {
                    delays: TIMER_RETRIES,
                    tester: function(dataOfAServer, successMessage, promise) {
                        // if status is 202, we need to retry to call the same ajax again
                        return promise && promise.status === RETRY_HTTP_CODE;
                    }
                })
                .done(handlersAjax.handleSuccessAjaxCB)
                .fail(function(promise, error, ajaxErrorMessage) {
                    handlersAjax.handleErrorAjaxCB(promise, ajaxErrorMessage);
                })
                .always(function() {
                    ++counter;

                    if (counter === totalNumberOfRequests) {
                        defer.resolve();
                    }
                });

            });

            return promise;
        },

        /**
         * When enable dark feature key: "jim.enable.strategy.fetch.and.wait.all.jira.servers.in.once " and
         * loading data as following steps:
         * - Each JIRA server is one ajax request call.
         * - Wait for all ajax called done (success and error)
         * - Render UI basing on returned data from server (in success case) and error message (in error case)
         */
        loadAllJiraServersInOnceStrategy: function() {
            var deferreds = util.startAjaxFetching();

            // convert all current Deferred objects to new Deferred objects which have retrying ability.
            deferreds = _.map(deferreds, function(defer) {
                var retryFunc = function() {
                    return defer;
                };

                var newDfd = retryCaller(retryFunc, {
                                delays: TIMER_RETRIES,
                                tester: function(dataOfAServer, successMessage, promise) {
                                    // if status is 202, we need to retry to call the same ajax again
                                    return promise && promise.status === RETRY_HTTP_CODE;
                                }
                            })
                            .done(handlersAjax.handleSuccessAjaxCB)
                            .fail(function(promise, error, ajaxErrorMessage) {
                                handlersAjax.handleErrorAjaxCB(promise, ajaxErrorMessage);
                            });

                return newDfd;
            });

            // convert all current Deferred objects to new Deferred objects which are always resolved.
            deferreds = _.map(deferreds, function(defer) {
                return deferredUtils.convertPromiseToAlwaysResolvedDeferred(defer);
            });

            // fetch all ajax calls and wait for them all.
            return $.when.apply($, deferreds)
                .done(function() {
                    var returnedDataByServers = _.toArray(arguments);

                    _.each(returnedDataByServers, function(dataOfAServer) {
                        var $elsGroupByServerKey;

                        if (dataOfAServer.serverId) {
                            handlersAjax.handleSuccessAjaxCB(dataOfAServer);
                        } else {
                            var promise = dataOfAServer[0];
                            var ajaxErrorMessage = dataOfAServer[2];
                            handlersAjax.handleErrorAjaxCB(promise, ajaxErrorMessage);
                        }
                    });
                });
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
