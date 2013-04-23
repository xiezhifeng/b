AJS.JiraConnector = AJS.JiraConnector || {};
AJS.JiraConnector.JQL = (function(){
	
	var issueKey = /\s*([A-Z][A-Z]+)-[0-9]+\s*/;
	var xmlUrlRegEx = /(issue|searchrequest)-xml/i; // http://localhost/si/jira.issueviews:issue-xml/TST-1/TST-1.xml
	var urlIssueRegEx = /\/browse\/([\x00-\x19\x21-\x22\x24\x27-\x3E\x40-\x7F]+-[0-9]+$)/i; //http://localhost/browse/TST-1	
	var jqlRegEx = /(jqlQuery|jql)\=([^&]+)/i; //http://localhost/sr/jira.issueviews:searchrequest-xml/temp/SearchRequest.xml?jqlQuery=summary+~+%22test%22+OR+description+~+%22test%22

	// get server index from servers array has url match with beginning of url
	var findServerFromUrl = function(url, servers) {
		if(typeof(servers) !== 'undefined' || servers.length > 0) {
			var urlLowerCase = url.toLowerCase();                		
			for(i = 0;i< servers.length; i++) {
				if(urlLowerCase.indexOf(servers[i].url.toLowerCase()) == 0 ) {
					return i;
				}
			}	
		}
		return -1;
    };
 
    var getJqlQuery= function(url) {
		var jqlQuery = "";
		// singleKey 
		var singleKey = urlIssueRegEx.exec(url);                    	
		if(singleKey) {
			jqlQuery = "key=" + singleKey[1];
		}
		else {
			// jql
			var jql = jqlRegEx.exec(url);                    		
			if (jql){
				jqlQuery = jql[2];
			}
			else {
			// xml key
				var xmlKey = issueKey.exec(url);            					
				if(xmlKey) {
					jqlQuery = "key=" + xmlKey[0];            					            						
				}
			}
		}
		jqlQuery = jqlQuery.replace(/\+/g, " ");                    	
		return jqlQuery;
	};
	
	return {
		// check queryTxt input match with one of issue url, xml url, jql url patterns  
		isIssueUrlOrXmlUrl: function(queryTxt) {                    	
			if(urlIssueRegEx.test(queryTxt) || xmlUrlRegEx.test(queryTxt) 
	        		|| jqlRegEx.test(queryTxt) ) {
				return true;
			}
			return false;
		},
		
		getJqlQueryFromUrl: getJqlQuery,
		
		getJQLAndServerIndexFromUrl: function(url, servers) {			
			var jiraParams = {};
			jiraParams["serverIndex"] = findServerFromUrl(url, servers);
			jiraParams["jqlQuery"] = getJqlQuery(url);			                    	
			return jiraParams;
		}
	}
})();
