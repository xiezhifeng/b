(function() {
    AJS.bind("init.rte", function() {
        AJS.Editor.JiraConnector.Paste = {
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

            pasteHandler : function(uri, node, done) {

                var macro = {
                    name : 'smart-quote',
                    params : {
                        quoteId: uri.queryKey.quoteId
                    }
                };
                tinymce.plugins.Autoconvert.convertMacroToDom(macro, done, done);
                return;


                var servers = AJS.Editor.JiraConnector.servers;
                var jiraAnalytics = AJS.Editor.JiraAnalytics;
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
                    
                    var jql = AJS.Editor.JiraConnector.Paste.jqlRegEx.exec(uri.source)
                                || AJS.Editor.JiraConnector.Paste.jqlRegExAlternateFormat.exec(uri.source);
                    
                    var personalFilter = AJS.JQLHelper.isFilterUrl(uri.source);
                    
                    var singleKey = AJS.Editor.JiraConnector.Paste.issueKeyOnlyRegEx.exec(uri.source)
                    || AJS.Editor.JiraConnector.Paste.issueKeyWithinRegex.exec(uri.source);
                    if (singleKey) {
                        singleKey = singleKey[2];
                        if (jiraAnalytics) {
                            pasteEventProperties.type = jiraAnalytics.linkTypes.jql;
                        }
                    } else {
                        singleKey = AJS.Editor.JiraConnector.Paste.singleTicketXMLEx.exec(uri.source);
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
                                 name : 'jira',
                                 params : {
                                     server : matchedServer.name,
                                     serverId : matchedServer.id,
                                     jqlQuery : decodeURIComponent(jql[1].replace(/\+/g, '%20'))
                                 }
                        };
                    } else if (personalFilter) {
                        var url = uri.source;
                        pasteEventProperties.is_single_issue = false;
                        pasteEventProperties.type = AJS.JQLHelper.checkQueryType(url);
                        macro = {
                                name : 'jira',
                                params : {
                                    server : matchedServer.name,
                                    serverId : matchedServer.id,
                                    jqlQuery : AJS.JQLHelper.getFilterFromFilterUrl(url)
                                }
                       };
                    } else if (singleKey) {
                        pasteEventProperties.is_single_issue = true;
                        macro = {
                                 name : 'jira',
                                 params : {
                                     server : matchedServer.name,
                                     serverId : matchedServer.id,
                                     key : singleKey
                                 }
                        };
                    }
                }
                if (macro) {
                    tinymce.plugins.Autoconvert.convertMacroToDom(macro, done, done);
                    if (jiraAnalytics) {
                        jiraAnalytics.triggerPasteEvent(pasteEventProperties);
                    }
                } else {
                    done();
                }
            }
        };
        tinymce.plugins.Autoconvert.autoConvert.addHandler(AJS.Editor.JiraConnector.Paste.pasteHandler);
    });
})();
