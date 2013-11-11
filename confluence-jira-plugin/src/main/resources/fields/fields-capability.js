
var jiraIntegration = window.jiraIntegration || {};

jiraIntegration.contextHandler = (function($, _) {
 function userContextHandler(serverId, renderField, restField) {
    renderField.children().eq(1).select2({
        minimumInputLength: 1,
        query: function (query) {
            function onsuccess(datas) {
                var data = {results: []};
                if(datas.users) {
                    $.each(datas.users, function() { //custom userpicker
                        data.results.push({
                            id: this.name,
                            text: this.displayName
                        });
                    });
                } else {
                    $.each(datas, function() { //reporter, assignee
                        data.results.push({
                            id: this.key,
                            text: this.name
                        });
                    });
                }
                query.callback(data);
            }
            appLinkAutocompleteRequest(serverId, restField.autoCompleteUrl, query.term, onsuccess);
        }
    });
}

function appLinkAutocompleteRequest (serverId, url, term, success) {
    AppLinks.makeRequest({
        appId: serverId,
        type: 'GET',
        url: url + term,
        dataType: 'json',
        contentType: "application/json; charset=utf-8",
        success: success,
        error:function(xhr){
            AJS.logError(xhr.status);
        }
    });
}


    return {
        userContextHandler : userContextHandler
    };
    
}(AJS.$, window._));

