AJS.Editor.JiraChart.Panels.CreatedVsResolvedChart = function($) {
    var CREATED_VS_RESOLVED_CHART_TITLE = AJS.I18n.getText('jirachart.panel.createdvsresolvedchart.title');
    var CREATED_VS_RESOLVED_CHART_ID = "createdvsresolvedchart";
    var container;
    var validateDayPrevious = function() {
            container = $("#jira-chart-content-createdvsresolvedchart");
            var periodName  = container.find("#periodName").val();
            var dayprevious = container.find("#daysprevious").val();
            if (dayprevious === "") {
                container.find(".days-previous-error").html(AJS.I18n.getText("jirachart.panel.createdvsresolvedchart.daysprevious.required.error"));
                return false;
            }

            switch (periodName) {

                case "hourly":
                    isValid  = dayprevious <= 10;
                    if (!isValid) {
                        container.find(".days-previous-error").html(AJS.I18n.getText("jirachart.panel.createdvsresolvedchart.daysprevious.error", 10, periodName));
                    }
                    break;

                case "daily":
                    isValid  = dayprevious <= 300;
                    if (!isValid) {
                        container.find(".days-previous-error").html(AJS.I18n.getText("jirachart.panel.createdvsresolvedchart.daysprevious.error", 300, periodName));
                    }
                    break;

                case "weekly":
                    isValid  = dayprevious <= 1750;
                    if (!isValid) {
                        container.find(".days-previous-error").html(AJS.I18n.getText("jirachart.panel.createdvsresolvedchart.daysprevious.error", 1750, periodName));
                    }
                    break;

                case "monthly":
                    isValid  = dayprevious <= 7500;
                    if (!isValid) {
                        container.find(".days-previous-error").html(AJS.I18n.getText("jirachart.panel.createdvsresolvedchart.daysprevious.error", 7500, periodName));
                    }
                    break;

                case "quarterly":
                    isValid  = dayprevious <= 22500;
                    if (!isValid) {
                        container.find(".days-previous-error").html(AJS.I18n.getText("jirachart.panel.createdvsresolvedchart.daysprevious.error", 22500, periodName));
                    }
                    break;

                case "yearly":
                    isValid  = dayprevious <= 36500;
                    if (!isValid) {
                        container.find(".days-previous-error").html(AJS.I18n.getText("jirachart.panel.createdvsresolvedchart.daysprevious.error", 36500, periodName));
                    }
                    break;
            }
            return isValid;
        }
    return {
        title : CREATED_VS_RESOLVED_CHART_TITLE,
        id: CREATED_VS_RESOLVED_CHART_ID,

        init : function(panel, id) {
            // get content from soy template
            var contentJiraChart = Confluence.Templates.ConfluenceJiraPlugin.contentJiraChart({
                'isMultiServer' : AJS.Editor.JiraConnector.servers.length > 1,
                'chartType' : id
            });
            panel.html(contentJiraChart);
        },

        renderChart : function() {
            var params = this.getMacroParamsFromDialog();
            var dataToSend = {
                "contentId" : AJS.Meta.get("page-id"),
                "macro" : {
                    "name" : "jirachart",
                    "params" : {
                        "jql" : params.jql,
                        "serverId" : params.serverId,
                        "width" : params.width,
                        "periodName": params.periodName,
                        "daysprevious": params.daysprevious,
                        "isCumulative": params.isCumulative,
                        "showUnresolvedTrend": params.showUnresolvedTrend,
                        "border" : params.border,
                        "showinfor" : params.showinfor,
                        "chartType": "createdvsresolved"
                    }
                }
            };
            validateDayPrevious();

            AJS.Editor.JiraChart.previewChart(dataToSend);
        },



        getMacroParamsFromDialog: function() {
            container = $("#jira-chart-content-createdvsresolvedchart");
            var selectedServer = AJS.Editor.JiraChart.getSelectedServer(container);
            return {
                jql: encodeURIComponent(container.find('#jira-chart-search-input').val()),
                periodName: container.find('#periodName').val(),
                width: AJS.Editor.JiraChart.convertFormatWidth(container.find('#jira-chart-width').val()),
                daysprevious: container.find('#daysprevious').val(),
                isCumulative: container.find('#cumulative').prop('checked') ? 30 : false,
                showUnresolvedTrend: container.find('#showunresolvedtrend').prop('checked'),
                versionLabel: container.find('#versionLabel').val(),
                border: container.find('#jira-chart-border').prop('checked'),
                showinfor: container.find('#jira-chart-show-infor').prop('checked'),
                serverId:  selectedServer.id,
                server: selectedServer.name,
                isAuthenticated: !selectedServer.authUrl,
                chartType: "createdvsresolved"
            };

        },

        isExistImageChart: function() {
            container = $("#jira-chart-content-createdvsresolvedchart");
            return $("#jira-chart-content-createdvsresolvedchart").find("#chart-preview-iframe").contents().find(".jira-chart-macro-img").length > 0;
        },

        focusForm: function() {
            container.find("#jira-chart-search-input").focus();
        }

    };

};

AJS.Editor.JiraChart.Panels.push(new AJS.Editor.JiraChart.Panels.CreatedVsResolvedChart(AJS.$));