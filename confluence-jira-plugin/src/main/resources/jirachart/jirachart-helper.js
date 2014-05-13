AJS.Editor.JiraChart.Helper = (function($) {

    var intRegex = /^\d+$/;

    var convertFormatWidth = function(val) {
        val = (val && typeof val === 'string') ? val.replace("px", "") : "";
        if (val === "auto") {
            val = "";
        }

        if (val.indexOf("%") > 0) {
            val = val.replace("%", "") * 4; //default image is width = 400px;
        }
        return val;
    };

    var isNumber = function(val) {
        return intRegex.test(val);
    };

    //convert URL/XML/Filter/Text/Key to JQL
    var convertSearchTextToJQL = function (container) {
        var servers = AJS.Editor.JiraConnector.servers;
        var serverId;
        var textSearch = container.find("#jira-chart-search-input").val();
        if (textSearch.indexOf('http') === 0) {
            var serverIndex = AJS.JQLHelper.findServerIndexFromUrl(textSearch, servers);
            if (serverIndex !== -1) {
                serverId = servers[serverIndex].id;
                container.find("#jira-chart-servers").val(serverId);
            } else {
                var message = Confluence.Templates.ConfluenceJiraPlugin.noServerWarning(
                    {
                        'isAdministrator': AJS.Meta.get("is-admin"),
                        'contextPath': Confluence.getContextPath()
                    }
                );
                container.find(".jira-chart-img").html(message);
                return;
            }
        }

        var jql = AJS.JQLHelper.convertToJQL(textSearch, serverId);
        if (jql) {
            container.find("#jira-chart-search-input").val(jql);
        } else {
            container.find(".jira-chart-img").html(Confluence.Templates.ConfluenceJiraPlugin.jqlInvalid());
            AJS.Editor.JiraChart.disableInsert();
        }
        return jql;
    };

    var isChartWidthValid = function($element) {

        // remove error message if have
        $element.next().next('.width-error').remove();

        var width = convertFormatWidth($element.val());

        //min and max for width value: [100,9000]
        if (width !== "" && !(isNumber(width) &&  width >= 100 && width <= 9000)) {

            var inforErrorWidth = "wrongFormat";

            if (intRegex.test(width)) {
                inforErrorWidth = "wrongNumber";
            }

            $element.next().after(Confluence.Templates.ConfluenceJiraPlugin.warningValWidthColumn({'error': inforErrorWidth}));
            AJS.Editor.JiraChart.disableInsert();
            return false;
        }
        return true;
    };

    /**
     * Binding chart element
     */
    var bindingCommonChartElements = function(container) {
        return {
            jql: container.find('#jira-chart-search-input'),
            width: container.find('#jira-chart-width'),
            border: container.find('.jira-chart-show-border'),
            showinfor: container.find('.jira-chart-show-infor'),
            server: container.find('#jira-chart-servers')
        };
    };

    var bindingCommonDataFromMacroToForm = function(chartElements, params) {
        chartElements.jql.val(decodeURIComponent(params['jql']));
        chartElements.width.val(params['width']);
        chartElements.border.attr('checked', (params['border'] === 'true'));
        chartElements.showinfor.attr('checked', (params['showinfor'] === 'true'));
        if (AJS.Editor.JiraConnector.servers.length > 1) {
            chartElements.server.val(params['serverId']);
        }
    };

    var getCommonMacroParamsFromDialog = function(chartElements, container) {
        var selectedServer = AJS.Editor.JiraChart.getSelectedServer(container);
        return {
            jql: encodeURIComponent(chartElements.jql.val()),
            width: convertFormatWidth(chartElements.width.val()),
            border: chartElements.border.prop('checked'),
            showinfor: chartElements.showinfor.prop('checked'),
            serverId:  selectedServer.id,
            server: selectedServer.name,
            isAuthenticated: !selectedServer.authUrl
        };
    };

    var getCommonChartParamsRequest = function(params, chartType) {
        return {
            "contentId" : AJS.Meta.get("page-id"),
            "macro" : {
                "name" : "jirachart",
                "params" : {
                    "jql" : params.jql,
                    "serverId" : params.serverId,
                    "width" : params.width,
                    "border" : params.border,
                    "showinfor" : params.showinfor,
                    "chartType": chartType
                }
            }
        };
    };

    return {
        bindingCommonChartElements: bindingCommonChartElements,
        bindingCommonDataFromMacroToForm: bindingCommonDataFromMacroToForm,
        getCommonMacroParamsFromDialog: getCommonMacroParamsFromDialog,
        getCommonChartParamsRequest: getCommonChartParamsRequest,
        convertSearchTextToJQL: convertSearchTextToJQL,
        convertFormatWidth: convertFormatWidth,
        isChartWidthValid: isChartWidthValid,
        isNumber: isNumber
    };
})(AJS.$);





