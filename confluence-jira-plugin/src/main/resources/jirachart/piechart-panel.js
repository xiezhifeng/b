AJS.Editor.JiraChart.Panels.PieChart = function() {

    var PIE_CHART_TITLE = AJS.I18n.getText('jirachart.panel.piechart.title');
    var PIE_CHART_ID = "piechart"

    return {
        title : PIE_CHART_TITLE,
        id: PIE_CHART_ID,

        init : function(panel) {
            // get content from soy template
            var contentJiraChart = Confluence.Templates.ConfluenceJiraPlugin.contentJiraChart({
                'isMultiServer' : AJS.Editor.JiraConnector.servers.length > 1,
                'chartType' : PIE_CHART_ID
            });

            panel.html(contentJiraChart);
        },

        renderChart : function(imageContainer) {
            var params = this.getMacroParamsFromDialog();
            var dataToSend = {
                "contentId" : AJS.Meta.get("page-id"),
                "macro" : {
                    "name" : "jirachart",
                    "params" : {
                        "jql" : params.jql,
                        "serverId" : params.serverId,
                        "width" : params.width,
                        "border" : params.border,
                        "showinfor" : params.showinfor,
                        "statType" : params.statType,
                        "chartType": "pie"
                    }
                }
            };

            AJS.Editor.JiraChart.previewChart(imageContainer, dataToSend);
        },

        getMacroParamsFromDialog: function() {
            var container = $("#jira-chart-content-piechart");
            var selectedServer = AJS.Editor.JiraChart.getSelectedServer(container);
            return {
                jql: encodeURIComponent(container.find('#jira-chart-inputsearch').val()),
                statType: container.find('#jira-chart-statType').val(),
                width: AJS.Editor.JiraChart.convertFormatWidth(container.find('#jira-chart-width').val()),
                border: container.find('#jira-chart-border').prop('checked'),
                showinfor: container.find('#jira-chart-show-infor').prop('checked'),
                serverId:  selectedServer.id,
                server: selectedServer.name,
                isAuthenticated: !selectedServer.authUrl
            };

        } ,

        chartImageIsExist: function() {
            return $("#jira-chart-content-piechart").find("#chart-preview-iframe").contents().find(".jira-chart-macro-img").length > 0;
        }

    };

};

AJS.Editor.JiraChart.Panels.push(new AJS.Editor.JiraChart.Panels.PieChart());