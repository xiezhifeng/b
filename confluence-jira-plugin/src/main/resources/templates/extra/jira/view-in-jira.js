
AJS.bind("add-handler.property-panel", function(event, panel) {
    AJS.log("add-handler.property-panel: panel name = " + panel.name);
    if (panel.name != "macro")
        return;

    panel.registerButtonHandler('view-in-jira', function(event, macroElement) {
        AJS.Editor.JiraConnector.serversAjax.done(function() {
            if (!AJS.Editor.JiraConnector.servers) {
                return;
            }
            var servers = AJS.Editor.JiraConnector.servers;
            //var macroHtml = AJS.Rte.getEditor().serializer.serialize(AJS.$(macroElement).clone()[0]);
            var $macroNode = AJS.$(macroElement);

            //var windowName = (AJS.$.browser && AJS.$.browser.msie) ? "_blank" : "confluence-goto-link-include-macro-" + macroElement.id;

            var defaultParam = $macroNode.attr("data-macro-default-parameter");
            var macroParametersString = $macroNode.attr("data-macro-parameters") || "";
            var parameters = Confluence.MacroParameterSerializer.deserialize(macroParametersString);

            //var macro = AJS.$.secureEvalJSON(macroData);
            var jql_operators = /=|!=|~|>|<|!~| is | in /i;

            var parseUglyMacro = function (macroTxt) {
                //get first macro parameter and assume its a jql query
                var bar = macroTxt.indexOf("|");
                if (bar >= 0) {
                    return macroTxt.substring(0, bar);
                }
                return macroTxt;
            };
            var searchStr = defaultParam || parameters["jqlQuery"] || parameters["key"] || parseUglyMacro(macroParametersString);
            var serverName = parameters["server"];
            var serverId = parameters["serverId"];

            var isJQL = searchStr.match(jql_operators);
            var server = null;
            for (var i = 0; i < servers.length; i++) {
                if ((serverId && servers[i].id == serverId)) {
                    server = servers[i];
                    break;
                }
                if ((serverName && servers[i].name == serverName) || (!serverName && servers[i].selected)) {
                    server = servers[i];
                    break;
                }
            }

            if (server != null) {
                var baseURL = (typeof server.displayUrl !== "undefined") ? server.displayUrl : server.url;
                if (baseURL.charAt(baseURL.length - 1) == '/') {
                    baseURL = baseURL.substr(0, baseURL.length - 1);
                }

                var windowName = tinymce.isIE ? "_blank" : "confluence-goto-jiralink-" + AJS.params.pageId;
                if (!isJQL)
                    window.open(baseURL + '/browse/' + encodeURIComponent(searchStr), windowName);
                else
                    window.open(baseURL + '/secure/IssueNavigator.jspa?reset=true&jqlQuery=' + encodeURIComponent(searchStr), windowName);
            }
        });
    });
});