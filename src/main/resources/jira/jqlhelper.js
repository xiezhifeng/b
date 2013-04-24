AJS.Editor.JiraConnector.JQL = (function() {
    var issueKey = /\s*([A-Z][A-Z]+)-[0-9]+\s*/;
    // http://localhost/si/jira.issueviews:issue-xml/TST-1/TST-1.xml
    var xmlUrlRegEx = /(issue|searchrequest)-xml/i;
    // singleKey - http://localhost/browse/TST-1
    var issueUrlRegEx = /\/browse\/([\x00-\x19\x21-\x22\x24\x27-\x3E\x40-\x7F]+-[0-9]+$)/i;
    // http://localhost/sr/jira.issueviews:searchrequest-xml/temp/SearchRequest.xml?jqlQuery=summary+~+%22test%22+OR+description+~+%22test%22
    var jqlRegEx = /(jqlQuery|jql)\=([^&]+)/i; 

    // get server index from servers array has url match with beginning of url
    var findServerFromUrl = function(url, servers) {
        if (typeof (servers) !== 'undefined' || servers.length > 0) {
            var urlLowerCase = url.toLowerCase();
            for (i = 0; i < servers.length; i++) {
                if (urlLowerCase.indexOf(servers[i].url.toLowerCase()) == 0) {
                    if(url.charAt(servers[i].url.length) == '/') {
                        return i;
                    }
                }
            }
        }
        return -1;
    };

    // get jql base on url by matched with pattern of singleKey, xml url,
    // jqlQuery string existing
    var getJqlQuery = function(url) {
        var jqlQuery = "";
        // singleKey
        var singleKey = issueUrlRegEx.exec(url);
        if (singleKey) {
            jqlQuery = "key=" + singleKey[1];
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
        // check queryTxt input match with one of issue url, xml url, jql url
        // patterns
        isIssueUrlOrXmlUrl : function(queryTxt) {
            if (issueUrlRegEx.test(queryTxt) || xmlUrlRegEx.test(queryTxt)
                    || jqlRegEx.test(queryTxt)) {
                return true;
            }
            return false;
        },

        getJqlQueryFromUrl : getJqlQuery,

        // convert url to Jql, find server index in servers ( array list of
        // JiraServerBean ) has url match with url input
        getJqlAndServerIndexFromUrl : function(url, servers) {
            var jiraParams = {};
            jiraParams["serverIndex"] = findServerFromUrl(url, servers);
            jiraParams["jqlQuery"] = getJqlQuery(url);
            return jiraParams;
        }
    }
})();
