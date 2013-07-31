AJS.JQLHelper = (function() {
    var singleKeyJQLExp = /^\s*(key\s*=\s*)?([A-Z]+)-([0-9]+)\s*$/i;
    var issueKey = /\s*([A-Z][A-Z]+)-[0-9]+\s*/;
    // http://localhost/si/jira.issueviews:issue-xml/TST-1/TST-1.xml
    var xmlUrlRegEx = /(issue|searchrequest)-xml/i;
    // singleKey - http://localhost/browse/TST-1
    var issueUrlRegEx = /\/(i#)?browse\/([\x00-\x19\x21-\x22\x24\x27-\x3E\x40-\x7F]+-[0-9]+$)/i;
    var singleTicketXMLEx = /\/jira\.issueviews:issue-xml\/([\x00-\x19\x21-\x22\x24\x27-\x3E\x40-\x7F]+-[0-9]+)\//;
    // http://localhost/sr/jira.issueviews:searchrequest-xml/temp/SearchRequest.xml?jqlQuery=summary+~+%22test%22+OR+description+~+%22test%22
    var jqlRegEx = /(jqlQuery|jql)\=([^&]+)/i;
    // http://localhost/jira/secure/IssueNavigator.jspa?mode=hide&requestId=10406 OR site.com/issues/?filter=10001
    var filterUrlRegEx = /(requestId|filter)\=([^&]+)/i;
    // http://localhost/jira/jira.issueviews:searchrequest-xml/10100/SearchRequest-10100.xml?tempMax=1000
    var filterXmlRegEx = /(searchrequest-xml\/)([0-9]+)\/SearchRequest/i;

    // get jql base on url by matched with pattern of singleKey, xml url,
    // jqlQuery string existing
    var getJqlQuery = function(url) {
        var jqlQuery = "";
        // singleKey
        var singleKey = issueUrlRegEx.exec(url);
        if (singleKey) {
            jqlQuery = "key=" + singleKey[2];
        } else {
            // jql
            var jql = jqlRegEx.exec(url);
            if (jql) {
                jqlQuery = jql[2];
            } else {
                // xml key
                var xmlKey = issueKey.exec(url);
                if (xmlKey) {
                    jqlQuery = "key=" + xmlKey[0];
                }
            }
        }
        jqlQuery = jqlQuery.replace(/\+/g, " ");
        return jqlQuery;
    };

    return {
        /*
         * check if query is single key query, for eg: " key = XXX-111 "
         * */
        isSingleKeyJQLExp : function(query) {
            return singleKeyJQLExp.exec(query);
        },
        // check queryTxt input match with one of issue url, xml url, jql url
        // patterns
        isIssueUrlOrXmlUrl : function(queryTxt) {
            if (issueUrlRegEx.test(queryTxt) 
                    || xmlUrlRegEx.test(queryTxt)
                    || jqlRegEx.test(queryTxt)
                    || singleTicketXMLEx.test(queryTxt)
                    ) {
                return true;
            }
            return false;
        },

        isFilterUrl : function(queryTxt) {
            return filterUrlRegEx.test(queryTxt) || filterXmlRegEx.test(queryTxt);
        },

        getJqlQueryFromJiraFilter : function(url, appLinkId, success, error) {
            var filterId = (filterUrlRegEx.exec(url) || filterXmlRegEx.exec(url))[2];
            var restUrl = '/rest/jiraanywhere/1.0/jira/appLink/' + appLinkId + '/filter/' + filterId;
            AJS.$.ajax({
                async: false,
                dataType: 'json',
                url: Confluence.getContextPath() + restUrl,
                success: success,
                error: error
            });
        },

        // get server index from servers array has url match with beginning of url
        findServerIndexFromUrl : function(url, servers) {
            if (typeof (servers) !== 'undefined' || servers.length > 0) {
                var urlLowerCase = url.toLowerCase();
                for (var i = 0; i < servers.length; i++) {
                    if (urlLowerCase.indexOf(servers[i].url.toLowerCase()) == 0) {
                        if(url.charAt(servers[i].url.length) == '/') {
                            return i;
                        }
                    }
                }
            }
            return -1;
        },

        getJqlQueryFromUrl : getJqlQuery,

        // convert url to Jql, find server index in servers ( array list of
        // JiraServerBean ) has url match with url input
        getJqlAndServerIndexFromUrl : function(url, servers) {
            var jiraParams = {};
            jiraParams["serverIndex"] = this.findServerIndexFromUrl(url, servers);
            jiraParams["jqlQuery"] = getJqlQuery(url);
            return jiraParams;
        },

        // Return one of AJS.Editor.JiraAnalytics.linkTypes
        checkQueryType : function (queryTxt) {
            if (!AJS.Editor.JiraAnalytics) {
                return undefined;
            }
            /*
            queryTxt example:

            // direct_jql
            status = open

            // jql_link
            http://localhost:11990/jira/issues/?jql=status%3DOpen
            http://localhost:11990/jira/browse/TSTT-7

            // xml_link
            http://localhost:11990/jira/sr/jira.issueviews:searchrequest-xml/temp/SearchRequest.xml?jqlQuery=status%3DOpen&tempMax=1000
            http://localhost:11990/jira/si/jira.issueviews:issue-xml/TSTT-7/TSTT-7.xml

            // rss_link
            http://localhost:11990/jira/sr/jira.issueviews:searchrequest-rss/temp/SearchRequest.xml?jqlQuery=status%3DOpen&tempMax=1000

            // filter_link
            http://localhost:11990/jira/issues/?filter=10001

            */
            if (!queryTxt || $.trim(queryTxt).length == 0) {
                return;
            }
            if (queryTxt.indexOf('http') != 0) {
                return AJS.Editor.JiraAnalytics.linkTypes.jqlDirect;
            } else if (queryTxt.indexOf('jira.issueviews:searchrequest-xml') != -1 || queryTxt.indexOf('jira.issueviews:issue-xml') != -1) {
                return AJS.Editor.JiraAnalytics.linkTypes.xml;
            } else if (queryTxt.indexOf('jira.issueviews:searchrequest-rss') != -1) {
                return AJS.Editor.JiraAnalytics.linkTypes.rss;
            } else if (queryTxt.indexOf('filter=') != -1 || queryTxt.indexOf('filter\\=') != -1) {
                return AJS.Editor.JiraAnalytics.linkTypes.filter;
            } else {
                return AJS.Editor.JiraAnalytics.linkTypes.jql;
            }
        }
    };
})();
