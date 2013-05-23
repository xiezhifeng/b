(function() {
    AJS.bind("init.rte", function() {
        AJS.Editor.JiraConnector.Paste = {
            // match almost any ASCII character and allow for issue URLS with query parameters or anchors (e.g. ? or #)
            // (http://confluence.atlassian.com/display/JIRA044/Configuring+Project+Keys specifies that project keys must be ASCII)
            // These aren't full-proof Regex given the flexibility allowed in configuration of project keys but it will
            // cover just about all real-world cases.

            // matches a browse URL ending in the project key e.g. http://localhost/browse/TST-1
            issueKeyOnlyRegEx : /\/browse\/([\x00-\x19\x21-\x22\x24\x27-\x3E\x40-\x7F]+-[0-9]+$)/,

            // matches a single XML link, for e.g: http://localhost:11990/jira/si/jira.issueviews:issue-xml/TSTT-2/TSTT-2.xml
            singleTicketXMLEx : /\/jira\.issueviews:issue-xml\/([\x00-\x19\x21-\x22\x24\x27-\x3E\x40-\x7F]+-[0-9]+)\//,

            // matches a browse URL with query parameters or an anchor link e.g. http://localhost:11990/browse/TST-1?addcomment...
            issueKeyWithinRegex : /\/browse\/([\x00-\x19\x21-\x22\x24\x27-\x3E\x40-\x7F]+-[0-9]+)(?:\?|#)/,

            jqlRegEx : /jqlQuery\=([^&]+)/,
            jqlRegExAlternateFormat: /jql\=([^&]+)/,

            pasteHandler : function(uri, node, done) {
                var servers = AJS.Editor.JiraConnector.servers;
                var jiraAnalytics = AJS.Editor.JiraConnector.Analytics;
                var pasteEventProperties = {};
                var matchedServer;
                if (!servers) {
                    done();
                    return;
                }

                for (var i in servers) {
                    var server = servers[i];
                    if (uri.source.indexOf(server.url) == 0) {
                        matchedServer = server;
                        break;
                    }
                }
                // see if we had a hit
                var macro;
                if (matchedServer) {
                    var singleKey = AJS.Editor.JiraConnector.Paste.issueKeyOnlyRegEx.exec(uri.source)
                                    || AJS.Editor.JiraConnector.Paste.issueKeyWithinRegex.exec(uri.source);
                    if (singleKey) {
                        pasteEventProperties.type = jiraAnalytics.linkTypes.jql;
                    } else {
                        singleKey = AJS.Editor.JiraConnector.Paste.singleTicketXMLEx.exec(uri.source);
                        if (singleKey) {
                            pasteEventProperties.type = jiraAnalytics.linkTypes.xml;
                        }
                    }
                    if (singleKey) {
                        pasteEventProperties.is_single_issue = true;
                        macro = {
                            name : 'jira',
                            params : {
                                server : matchedServer.name,
                                key : singleKey[1]
                            }
                        };
                    } else {
                        pasteEventProperties.is_single_issue = false;
                        jql = AJS.Editor.JiraConnector.Paste.jqlRegEx.exec(uri.query)
                                || AJS.Editor.JiraConnector.Paste.jqlRegExAlternateFormat.exec(uri.query);
                        if (jql) {
                            macro = {
                                name : 'jira',
                                params : {
                                    server : matchedServer.name,
                                    jqlQuery : decodeURIComponent(jql[1].replace(/\+/g, '%20'))
                                }
                            };
                            pasteEventProperties.type = AJS.Editor.JiraConnector.JQL.checkQueryType(uri.source);
                        } else if (AJS.Editor.JiraConnector.JQL.isFilterUrl(uri.source)){
                            AJS.Editor.JiraConnector.JQL.getJqlQueryFromJiraFilter(uri.source, matchedServer.id,
                                function(data) {
                                    if(data.jql) {
                                        macro = {
                                            name : 'jira',
                                            params : {
                                                server : matchedServer.name,
                                                jqlQuery : decodeURIComponent(data.jql)
                                            }
                                        };
                                    }
                                }
                            )
                        }
                    }
                }
                if (macro) {
                    tinymce.plugins.Autoconvert.convertMacroToDom(macro, done, done);
                    jiraAnalytics.triggerPasteEvent(pasteEventProperties);
                } else {
                    done();
                }
            }
        };
        tinymce.plugins.Autoconvert.autoConvert.addHandler(AJS.Editor.JiraConnector.Paste.pasteHandler);
    });
})();
