define('confluence/jim/editor-plugins/view-in-jira-button', [
    'ajs',
    'underscore'
], function(
    AJS,
    _
) {

    'use strict';

    var HandleViewInJiraButton = {
        _parseUglyMacro: function(macroTxt) {
            // get first macro parameter and assume its a jql query
            var bar = macroTxt.indexOf('|');
            if (bar >= 0) {
                return macroTxt.substring(0, bar);
            }
            return macroTxt;
        },

        _getMatchedServer: function(serverId, serverName) {
            var servers = AJS.Editor.JiraConnector.servers;
            var server = null;

            for (var i = 0; i < servers.length; i++) {
                if ((serverId && servers[i].id === serverId)) {
                    server = servers[i];
                    break;
                }

                if ((serverName && servers[i].name === serverName) ||
                        (!serverName && servers[i].selected)) {
                    server = servers[i];
                    break;
                }
            }

            return server;
        },

        /**
         * Get base url from a server object
         * @param {Object} server
         * @returns {string} base url of server object
         * @private
         */
        _getBaseUrl: function(server) {
            var baseURL = (typeof server.displayUrl !== 'undefined') ? server.displayUrl : server.url;

            if (baseURL.charAt(baseURL.length - 1) === '/') {
                baseURL = baseURL.substr(0, baseURL.length - 1);
            }

            return baseURL;
        },

        /**
         * Get redirect url when click View in jira button in property panel of sprint macro
         * @param $macroNode
         * @param parameters
         * @returns {*}
         */
        getUrlIfSprintMacro: function($macroNode, parameters) {
            var EXCLUDE_PARAMS = ['boardId', 'sprintId', 'serverId', 'sprintName'];
            var macroName = $macroNode.attr('data-macro-name');

            // make sure it is sprint macro.
            if (macroName !== 'jirasprint') {
                return;
            }

            var boardId = parameters['boardId'];
            var sprintId = parameters['sprintId'];

            // make sure macro has both board and sprint id
            if (boardId && sprintId) {
                var url = AJS.format('/secure/RapidBoard.jspa?rapidView={0}&sprint={1}', boardId, sprintId);

                var otherSupportParams  = _.omit(parameters, EXCLUDE_PARAMS);

                _.each(_.keys(otherSupportParams), function(paramName) {
                    var value = parameters[paramName];
                    if (value) {
                        url += '&' + encodeURIComponent(paramName) + '=' + encodeURIComponent(value);
                    }
                });

                return url;
            }

            return null;
        },

        init: function(event, macroElement) {
            if (!AJS.Editor.JiraConnector.servers) {
                return;
            }

            var tinymce = window.tinymce;
            var context = HandleViewInJiraButton;

            var $macroNode = AJS.$(macroElement);
            var defaultParam = $macroNode.attr('data-macro-default-parameter');
            var macroParametersString = $macroNode.attr('data-macro-parameters') || '';
            var parameters = Confluence.MacroParameterSerializer.deserialize(macroParametersString);

            var serverName = parameters['server'];
            var serverId = parameters['serverId'];
            var server = context._getMatchedServer(serverId, serverName);

            if (server) {
                var baseURL = context._getBaseUrl(server);
                var windowName = tinymce.isIE ? '_blank' : 'confluence-goto-jiralink-' + AJS.params.pageId;

                var url = context.getUrlIfSprintMacro($macroNode, parameters);
                if (url) {
                    window.open(baseURL + url, windowName);
                    return;
                }

                var jql_operators = /=|!=|~|>|<|!~| is | in /i;
                var searchStr = defaultParam || parameters['jqlQuery'] || parameters['key'] || context._parseUglyMacro(macroParametersString);
                var isJQL = searchStr.match(jql_operators);

                if (!isJQL) {
                    window.open(baseURL + '/browse/' + encodeURIComponent(searchStr), windowName);
                } else {
                    window.open(baseURL + '/secure/IssueNavigator.jspa?reset=true&jqlQuery=' + encodeURIComponent(searchStr), windowName);
                }
            }
        }
    };

    return HandleViewInJiraButton;
});

require('confluence/jim/amd/module-exporter')
.safeRequire('confluence/jim/editor-plugins/view-in-jira-button', function(HandleViewInJiraButton) {

    AJS.bind('add-handler.property-panel', function(event, panel) {
        if (panel.name !== 'macro') {
            return;
        }

        panel.registerButtonHandler('view-in-jira', HandleViewInJiraButton.init);

    });
});
