AJS.Editor.JiraChart.Helper = (function($) {

    var intRegex = /^\d+$/;

    /**
     * Convert width to right format (px, %)
     * @public
     * @param val
     * @returns {string}
     */
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

    /**
     * Convert URL/XML/Filter/Text/Key to JQL
     * @public
     * @param container
     * @returns {String} jql
     */
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

    /**
     * Validate width value and show error when it is not valid.
     * @public
     * @param $element width element
     * @returns {boolean}
     */
    var isChartWidthValid = function($element) {

        // remove error message if have
        $element.next().next('.width-error').remove();

        var width = convertFormatWidth($element.val());

        //min and max for width value: [100,9000]
        var inforErrorWidth;
        if (width) {
            if (isNumber(width)) {
                if (width < 100 || width > 9000) {
                    inforErrorWidth = "wrongNumber";
                }
            } else {
                inforErrorWidth = "wrongFormat";
            }
        }
        if (inforErrorWidth) {
            $element.next().after(Confluence.Templates.ConfluenceJiraPlugin.warningValWidthColumn({'error': inforErrorWidth}));
            AJS.Editor.JiraChart.disableInsert();
            return false;
        }
        return true;
    };

    /**
     * Binding common chart elements.
     * @public
     * @param container
     * @returns {{jql: *, width: *, border: *, showinfor: *, server: *}}
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

    /**
     * Binding common chart params to chart form.
     * @public
     * @param chartElements
     * @param params
     */
    var bindingCommonDataFromMacroToForm = function(chartElements, params) {
        chartElements.jql.val(decodeURIComponent(params['jql']));
        chartElements.width.val(params['width']);
        chartElements.border.attr('checked', (params['border'] === 'true'));
        chartElements.showinfor.attr('checked', (params['showinfor'] === 'true'));
        if (AJS.Editor.JiraConnector.servers.length > 1) {
            chartElements.server.val(params['serverId']);
        }
    };

    /**
     * Get common chart macro params from dialog
     * @public
     * @param chartElements
     * @param container
     * @returns {{jql: string, width: string, border: boolean, showinfor: boolean, serverId: string, server: string, isAuthenticated: boolean}}
     */
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

    /**
     * Get common object pass to server
     * @public
     * @param params
     * @param chartType
     * @returns {{contentId: string, macro: {name: string, params: jql: string, width: string, border: boolean, showinfor: boolean, chartType: string}}}
     */
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





