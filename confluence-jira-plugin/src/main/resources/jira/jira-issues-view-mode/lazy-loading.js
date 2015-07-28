define('confluence/jim/jira/jira-issues-view-mode/lazy-loading', [
    'jquery',
    'ajs',
    'confluence',
    'underscore'
], function(
    $,
    AJS,
    Confluence,
    _
) {
    'use strict';

    var DARK_FEATURE_KEY = 'jim.enable.strategy.fetch.and.wait.all.jira.servers.in.once';
    var $jiraIssuesEls = null;

    var util = {
        convertPromiseToAlwaysResolvedDeferred: function(promise) {
            var newDfd = $.Deferred();

            promise.done(function(data) {
                newDfd.resolve(data);
            });

            promise.fail(function() {
                newDfd.resolve(_.toArray(arguments));
            });

            newDfd.jimJiraServerId = promise.jimJiraServerId;

            return newDfd;
        }
    };

    /**
     * Begin to featch data from a Jira Server
     * @param jiraServerId
     * @returns {Object} a jQuery Deferred object
     */
    var fetchSingeJiraServer = function(jiraServerId) {
        var jimUrl = [
            AJS.contextPath(),
            '/rest/jiraanywhere/1.0/jira/page/',
            Confluence.getContentId(),
            '/server/', jiraServerId
        ];

        var promise = $.ajax({
            type: 'GET',
            url: jimUrl.join(''),
            cache: true
        });

        // we need to cache jira server id so that we know which Promise object is rejected later and render error message
        promise.jimJiraServerId = jiraServerId;
        return promise;
    };

    /**
     * Scan single Jira Issues Macro DOM to get all unique Jira Servers
     * @returns {Array}
     */
    var findAllJiraServersInPageContent = function() {
        var servers = _.map($jiraIssuesEls, function(item) {
            return $(item).attr('data-server-id');
        });

        return _.uniq(servers);
    };

    /**
     * Begin to fetch data from server for per Jira Server we got.
     * @returns {Array} array of deferred object.
     */
    var startFetching = function() {
        var deferreds = [];
        var servers = findAllJiraServersInPageContent();

        _.each(servers, function(serverId) {
            var dfd = fetchSingeJiraServer(serverId);
            deferreds.push(dfd);
        });

        return deferreds;
    };

    var renderSingleJIMFromMacroHTML = function(htmlMacros, $elsGroupByServerKey) {
        _.each(
            htmlMacros,
            function(htmlPlaceHolders, issueKey) {

                var $elsGroupByIssueKey = $elsGroupByServerKey.filter('[data-issue-key=' + issueKey + ']');

                $elsGroupByIssueKey.each(function(index, jiraIssueEl) {
                    $(jiraIssueEl).replaceWith(htmlPlaceHolders[index]);
                });
        });
    };

    var renderSingleJIMInErrorCase = function($elsGroupByServerKey, ajaxErrorMessage) {
        var errorMessage = AJS.I18n.getText('jiraissues.unexpected.error');

        $elsGroupByServerKey.find('.summary').text(errorMessage + ' ' + ajaxErrorMessage);
        $elsGroupByServerKey.find('.jira-status').remove();
        $elsGroupByServerKey.find('.icon').remove();
        $elsGroupByServerKey.find('.icon').remove();

        $elsGroupByServerKey
                .removeClass('jira-issue')
                .addClass('aui-message aui-message-warning jim-error-message jim-error-message-single conf-macro output-block');
    };

    /**
     * When disable dark feature key: "jim.enable.strategy.fetch.and.wait.all.jira.servers.in.once " and
     * loading data as following steps:
     * - Each JIRA server is one ajax request call.
     * - Wait for all ajax called done (success and error)
     * - Render UI basing on returned data from server (in success case) and error message (in error case)
     */
    var loadOneByOneJiraServerStrategy = function() {
        var deferreds = startFetching();

        deferreds.forEach(function(defer) {
            defer
                .done(function(dataOfAServer) {
                    var $elsGroupByServerKey = $jiraIssuesEls.filter('[data-server-id=' + dataOfAServer.serverId + ']');
                    renderSingleJIMFromMacroHTML(dataOfAServer.htmlMacro, $elsGroupByServerKey);
                })
                .fail(function(promise, error, ajaxErrorMessage) {
                    var $elsGroupByServerKey = $jiraIssuesEls.filter('[data-server-id=' + promise.jimJiraServerId + ']');
                    renderSingleJIMInErrorCase($elsGroupByServerKey, ajaxErrorMessage);
                });
        });
    };

    /**
     * When enable dark feature key: "jim.enable.strategy.fetch.and.wait.all.jira.servers.in.once " and
     * loading data as following steps:
     * - Each JIRA server is one ajax request call.
     * - Wait for all ajax called done (success and error)
     * - Render UI basing on returned data from server (in success case) and error message (in error case)
     */
    var loadAllJiraServersInOnceStrategy = function() {
        var deferreds = startFetching();

        // convert all current Deferred objects to new Deferred objects which are always resolved.
        deferreds = _.map(deferreds, function(dfd) {
            return util.convertPromiseToAlwaysResolvedDeferred(dfd);
        });

        // fetch all ajax calls and wait for them all.
        $.when.apply($, deferreds)
            .done(function() {
                var returnedDataByServers = _.toArray(arguments);

                _.each(returnedDataByServers, function(dataOfAServer) {
                    var $elsGroupByServerKey;

                    if (dataOfAServer.serverId) {
                        $elsGroupByServerKey = $jiraIssuesEls.filter('[data-server-id=' + dataOfAServer.serverId + ']');
                        renderSingleJIMFromMacroHTML(dataOfAServer.htmlMacro, $elsGroupByServerKey);
                    } else {
                        var promise = dataOfAServer[0];
                        var ajaxErrorMessage = dataOfAServer[2];
                        $elsGroupByServerKey = $jiraIssuesEls.filter('[data-server-id=' + promise.jimJiraServerId + ']');
                        renderSingleJIMInErrorCase($elsGroupByServerKey, ajaxErrorMessage);
                    }
                });
            });
    };

    var exportModule = {
        /**
         * Initialize the module
         * @return {Object} a Promise object
         */
        init: function() {
            $jiraIssuesEls = $('.jira-issue');

            if (AJS.DarkFeatures.isEnabled(DARK_FEATURE_KEY)) {
                return loadAllJiraServersInOnceStrategy();
            } else {
                return loadOneByOneJiraServerStrategy();
            }
        }
    };

    return exportModule;
});
