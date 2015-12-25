define('confluence/jim/editor-plugins/jira-paste-link', [
    'ajs',
    'confluence/jsUri',
    'underscore',
    'confluence/jim/util/analytic'
], function(
    AJS,
    Uri,
    _,
    analytic
) {
    'use strict';

    var JiraPasteLink = {
        // match almost any ASCII character and allow for issue URLS with query parameters or anchors (e.g. ? or #)
        // (http://confluence.atlassian.com/display/JIRA044/Configuring+Project+Keys specifies that project keys must be ASCII)
        // These aren't full-proof Regex given the flexibility allowed in configuration of project keys but it will
        // cover just about all real-world cases.

        // matches a browse URL ending in the project key e.g. http://localhost/browse/TST-1
        issueKeyOnlyRegEx: /\/(i#)?browse\/([\x00-\x19\x21-\x22\x24\x27-\x3E\x40-\x7F]+-[0-9]+$)/,

        // matches a single XML link, for e.g: http://localhost:11990/jira/si/jira.issueviews:issue-xml/TSTT-2/TSTT-2.xml
        singleTicketXMLEx: /\/jira\.issueviews:issue-xml\/([\x00-\x19\x21-\x22\x24\x27-\x3E\x40-\x7F]+-[0-9]+)\//,

        // matches a browse URL with query parameters or an anchor link e.g. http://localhost:11990/browse/TST-1?jql...
        issueKeyWithinRegex: /\/(i#)?browse\/([\x00-\x19\x21-\x22\x24\x27-\x3E\x40-\x7F]+-[0-9]+)(?:\?|#)/i,

        jqlRegEx: /jqlQuery\=([^&]+)/,

        jqlRegExAlternateFormat: /jql\=([^&]+)/,

        pasteHandler: function(uri, node, done) {
            var context = JiraPasteLink;
            var jiraAnalytics = AJS.Editor.JiraAnalytics;
            var pasteEventProperties = {};
            var matchedServer = context._getMatchedServerFromLink(uri.source, done);

            // see if we had a hit
            var macro = null;

            if (matchedServer) {

                var jql = context.jqlRegEx.exec(uri.source) ||
                            context.jqlRegExAlternateFormat.exec(uri.source);

                var personalFilter = AJS.JQLHelper.isFilterUrl(uri.source);

                var singleKey = context.issueKeyOnlyRegEx.exec(uri.source) ||
                                context.issueKeyWithinRegex.exec(uri.source);

                if (singleKey) {
                    singleKey = singleKey[2];
                    if (jiraAnalytics) {
                        pasteEventProperties.type = jiraAnalytics.linkTypes.jql;
                    }
                } else {
                    singleKey = context.singleTicketXMLEx.exec(uri.source);
                    if (singleKey) {
                        singleKey = singleKey[1];
                        if (jiraAnalytics) {
                            pasteEventProperties.type = jiraAnalytics.linkTypes.xml;
                        }
                    }
                }

                if (jql) {
                    pasteEventProperties.is_single_issue = false;
                    pasteEventProperties.type = AJS.JQLHelper.checkQueryType(uri.source);
                    macro = {
                         name: 'jira',
                         params: {
                             server: matchedServer.name,
                             serverId: matchedServer.id,
                             jqlQuery: decodeURIComponent(jql[1].replace(/\+/g, '%20'))
                         }
                    };
                } else if (personalFilter) {
                    var url = uri.source;
                    pasteEventProperties.is_single_issue = false;
                    pasteEventProperties.type = AJS.JQLHelper.checkQueryType(url);
                    macro = {
                        name: 'jira',
                        params: {
                            server: matchedServer.name,
                            serverId: matchedServer.id,
                            jqlQuery: AJS.JQLHelper.getFilterFromFilterUrl(url)
                        }
                   };
                } else if (singleKey) {
                    pasteEventProperties.is_single_issue = true;
                    macro = {
                         name: 'jira',
                         params: {
                             server: matchedServer.name,
                             serverId: matchedServer.id,
                             key: singleKey
                         }
                    };
                } else {
                    macro = context._getSprintMacroParamsIfHave(uri.source, matchedServer.id);
                }
            }

            if (macro) {
                window.tinymce.plugins.Autoconvert.convertMacroToDom(macro, done, done);
                if (jiraAnalytics) {
                    jiraAnalytics.triggerPasteEvent(pasteEventProperties);
                }
            } else {
                done();
            }
        },

        /**
         * Find a matched server from a url.
         * @param {string} url
         * @param {Function} done
         * @returns {object} matched server object
         * @private
         */
        _getMatchedServerFromLink: function(url, done) {
            var matchedServer = null;
            var servers = AJS.Editor.JiraConnector.servers;

            if (!servers) {
                done();
                return null;
            }

            for (var i in servers) {
                if (servers.hasOwnProperty(i)) {
                    var server = servers[i];
                    if (url.indexOf(server.url) === 0) {
                        matchedServer = server;
                        break;
                    }
                }
            }

            return matchedServer;
        },

        /**
         * Convert a pasting url into Jira Sprint macro
         * @param {string} url - example: https://jira.atlassian.com/secure/RapidBoard.jspa?rapidView=877&sprint=1722
         * @param {string} serverId
         * @returns {object} - a Jira Sprint macro parameter
         * @private
         */
        _getSprintMacroParamsIfHave: function(url, serverId) {
            // matches a rapid view JIRA link, ex: https://jira.atlassian.com/secure/RapidBoard.jspa?...
            var rapidViewJiraLink = /\/RapidBoard\.jspa\?{1}/;

            if (!rapidViewJiraLink.test(url)) {
                return null;
            }

            var uri = new Uri(url.toLowerCase());
            var boardId = uri.getQueryParamValue('rapidview');
            var sprintId = uri.getQueryParamValue('sprint');

            var macro = null;

            if (boardId && sprintId) {

                macro = {
                    name: 'jirasprint',
                    params: {
                        serverId: serverId,
                        boardId: boardId,
                        sprintId: sprintId
                    }
                };

                var SUPPORT_OTHER_PARAMS = ['view', 'chart', 'projectKey'];
                _.each(SUPPORT_OTHER_PARAMS, function(paramName) {
                    var paramValue = uri.getQueryParamValue(paramName.toLowerCase());
                    if (paramValue) {
                        macro.params[paramName] = paramValue;
                    }
                });

                analytic.sendPasteSprintLinkEvent();
            }

            return macro;
        }
    };

    return JiraPasteLink;
});

require('confluence/jim/amd/module-exporter')
.safeRequire('confluence/jim/editor-plugins/jira-paste-link', function(JiraPasteLink) {

    var AJS = require('ajs');

    AJS.bind('init.rte', function() {
        window.tinymce.plugins.Autoconvert.autoConvert.addHandler(JiraPasteLink.pasteHandler);
    });

});
