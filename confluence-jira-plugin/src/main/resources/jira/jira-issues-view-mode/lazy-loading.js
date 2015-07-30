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

    // list of jQuery DOM object of all single JIM in page
    var $jiraIssuesEls = null;

    var ONE_SECOND = 1000;
    var TIMER_RETRIES = [0, 2 * ONE_SECOND, 5 * ONE_SECOND, 8 * ONE_SECOND, 10 * ONE_SECOND];

    // returned HTTP code which will help to detect whether reloading data.
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
        fetchSingeJiraServer: function(jiraServerId, clientId) {
            var jimUrl = [
                AJS.contextPath(),
                '/rest/jiraanywhere/1.0/jira/page/',
                Confluence.getContentId(),
                '/server/', jiraServerId,
                '/', clientId
            ];

            var promise = $.ajax({
                type: 'GET',
                url: jimUrl.join(''),
                cache: true
            });

            // we need to cache jira server id so that we know which Promise object is rejected later
            // and render error message
            promise.jimJiraServerId = jiraServerId;
            promise.jimClientId = clientId;
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
        startAjaxFetching: function() {
            var promises = [];
            var servers = util.findAllJiraServersInPageContent();

            _.each(servers, function(serverId) {
                var clientId = util.getClientIdFromJIMMacro(serverId);
                var promise = util.fetchSingeJiraServer(serverId, clientId);

                // supply a ability of retry itself for promise
                promise.retry = function() {
                    return util.fetchSingeJiraServer(serverId, clientId);
                };

                promises.push(promise);
            });

            return promises;
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
            var jQAjaxPromises = util.startAjaxFetching();
            var totalNumberOfRequests = jQAjaxPromises.length;

            // we need to know when all request are solved.
            var mainDefer = $.Deferred();

            var counter = 0;

            var beginLoading = function(ajaxPromise) {
                retryCaller(ajaxPromise.retry, {
                        delays: TIMER_RETRIES,
                        context: ajaxPromise,
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
                            mainDefer.resolve();
                        }
                    });

            };

            jQAjaxPromises.forEach(beginLoading);

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
            var jQAjaxPromises = util.startAjaxFetching();

            // convert all current Deferred objects to new Deferred objects which have retrying ability.
            jQAjaxPromises = _.map(jQAjaxPromises, function(ajaxPromise) {
                var newDfd = retryCaller(ajaxPromise.retry, {
                                delays: TIMER_RETRIES,
                                context: ajaxPromise,
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
            jQAjaxPromises = _.map(jQAjaxPromises, function(promise) {
                return deferredUtils.convertPromiseToAlwaysResolvedDeferred(promise);
            });

            // fetch all ajax calls and wait for them all.
            var mainDefer = $.when.apply($, jQAjaxPromises)
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
