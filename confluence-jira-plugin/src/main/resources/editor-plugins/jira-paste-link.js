define('confluence/jim/editor-plugins/jira-paste-link', [
    'ajs',
], function(
    AJS
) {
    'use strict';

    var jiraPasteLink = {
            // match almost any ASCII character and allow for issue URLS with query parameters or anchors (e.g. ? or #)
            // (http://confluence.atlassian.com/display/JIRA044/Configuring+Project+Keys specifies that project keys must be ASCII)
            // These aren't full-proof Regex given the flexibility allowed in configuration of project keys but it will
            // cover just about all real-world cases.

            // matches a browse URL ending in the project key e.g. http://localhost/browse/TST-1
            issueKeyOnlyRegEx : /\/(i#)?browse\/([\x00-\x19\x21-\x22\x24\x27-\x3E\x40-\x7F]+-[0-9]+$)/,

            // matches a single XML link, for e.g: http://localhost:11990/jira/si/jira.issueviews:issue-xml/TSTT-2/TSTT-2.xml
            singleTicketXMLEx : /\/jira\.issueviews:issue-xml\/([\x00-\x19\x21-\x22\x24\x27-\x3E\x40-\x7F]+-[0-9]+)\//,

            // matches a browse URL with query parameters or an anchor link e.g. http://localhost:11990/browse/TST-1?jql...
            issueKeyWithinRegex : /\/(i#)?browse\/([\x00-\x19\x21-\x22\x24\x27-\x3E\x40-\x7F]+-[0-9]+)(?:\?|#)/,

            jqlRegEx : /jqlQuery\=([^&]+)/,
            jqlRegExAlternateFormat: /jql\=([^&]+)/,

            /**
             * Find a matched server from a url.
             * @param {string} url
             * @param {Function} done
             * @returns {object} matched server object
             * @private
             */
            _getMatchedServerFromLink: function(url, servers) {
                var matchedServer = null;

                for (var i in servers) {
                    if (servers.hasOwnProperty(i)) {
                        var server = servers[i];

                        // CONF-39419: add '/' into server url to make sure we are strictly comparing full base url.
                        var serverUrlWithContextPath = server.url;
                        var lastChar = server.url[server.url.length - 1];
                        if (lastChar !== '/') {
                            serverUrlWithContextPath += '/';
                        }

                        if (url.indexOf(serverUrlWithContextPath) === 0) {
                            matchedServer = server;
                            break;
                        }
                    }
                }

                return matchedServer;
            },

            pasteHandler : function(uri, node, done) {
                var isLinkedInLink = uri.source && uri.source.indexOf('https://www.linkedin.com') == 0;

                if (isLinkedInLink) {
                    $.ajax({
                        crossDomain: true,
                        url: uri.source,
                        success: function(data) {
                            console.log(/<title>(.*)<\/title>/.exec(data));

                        }
                    });

                }

                done();
            }
        };

    return jiraPasteLink;
});
