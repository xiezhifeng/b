AJS.Editor.JiraChart.Panels.CreatedVsResolvedChart = function($) {
    var CREATED_VS_RESOLVED_CHART_TITLE = AJS.I18n.getText('jirachart.panel.createdvsresolvedchart.title');
    var CREATED_VS_RESOLVED_CHART_ID = "createdvsresolvedchart";
    var container;
    var showErrorMessage = function(days, periodName) {
        container.find(".days-previous-error").html(AJS.I18n.getText("jirachart.panel.createdvsresolvedchart.daysprevious.error", days, periodName));
    };

    var clearErrorMessage = function() {
        container.find(".days-previous-error").empty();
    };

    var initializeDefaultValues = function() {
        container.find('#periodName').val("daily");
        container.find('#daysprevious').val("30");
    };

    var bindingActions = function() {

        // bind change event on periodName
        container.find("#periodName, #daysprevious, #cumulative, #showunresolvedtrend, #versionLabel").change(function(event) {
            if (validateDayPrevious())
            {
                AJS.Editor.JiraChart.search(container);
            }
        });
        container.find("#daysprevious").focusout(function() {
            validateDayPrevious();
        });

    };

    var validateDayPrevious = function() {
        var periodName  = container.find("#periodName").val();
        var dayprevious = container.find("#daysprevious").val();
        if (dayprevious.trim() === "") {
            container.find(".days-previous-error").html(AJS.I18n.getText("jirachart.panel.createdvsresolvedchart.daysprevious.required.error"));
            return false;
        }

        if(!AJS.Editor.JiraChart.isNumber(dayprevious) || dayprevious < 0) {
            container.find(".days-previous-error").html(AJS.I18n.getText("jirachart.panel.createdvsresolvedchart.daysprevious.wrongnumber"));
            return false;
        }

        switch (periodName) {

            case "hourly":
                isValid  = dayprevious <= 10;
                if (!isValid) {
                    showErrorMessage(10, periodName);
                }
                break;

            case "daily":
                isValid  = dayprevious <= 300;
                if (!isValid) {
                    showErrorMessage(300, periodName);
                }
                break;

            case "weekly":
                isValid  = dayprevious <= 1750;
                if (!isValid) {
                    showErrorMessage(1750, periodName);
                }
                break;

            case "monthly":
                isValid  = dayprevious <= 7500;
                if (!isValid) {
                    showErrorMessage(7500, periodName);
                }
                break;

            case "quarterly":
                isValid  = dayprevious <= 22500;
                if (!isValid) {
                    showErrorMessage(22500, periodName);
                }
                break;

            case "yearly":
                isValid  = dayprevious <= 36500;
                if (!isValid) {
                    showErrorMessage(36500, periodName);
                }
                break;
            default:
                isValid = false;
        }
        if (isValid) {
            clearErrorMessage();
        }

        return isValid;
    };

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
            container = $("#jira-chart-content-createdvsresolvedchart");
            initializeDefaultValues();
            bindingActions();
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
                isCumulative: container.find('#cumulative').prop('checked'),
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
            return container.find("#chart-preview-iframe").contents().find(".jira-chart-macro-img").length > 0;
        },

        focusForm: function() {
            container.find("#jira-chart-search-input").focus();
        },

        resetDialogValue: function() {

            container.filter(":checked").removeAttr("checked");
            container.find("#periodName").val("daily");
            container.find("#daysprevious").val("30");
            container.find("#showunresolvedtrend").removeAttr("checked");
            container.find("#cumulative").removeAttr("checked")
            container.find("#jira-chart-border").removeAttr("checked");
            container.find("#jira-chart-show-infor").removeAttr("checked");
            container.find("#jira-chart-search-input").val("");
            container.find("#jira-chart-width").val("");
            container.find(".jira-chart-img").empty();
            container.find("#versionLabel").val("");

        },

        bindingDataFromMacroToForm : function(params) {
            if (params) {
                container.find('#jira-chart-search-input').val(decodeURIComponent(params['jql']));
                container.find('#jira-chart-width').val(params['width']);
                container.find('#jira-chart-border').attr('checked', (params['border'] === 'true'));
                container.find('#jira-chart-show-infor').attr('checked', (params['showinfor'] === 'true'));
                container.find('#cumulative').attr('checked', (params['isCumulative'] !== 'false'));
                container.find('#showunresolvedtrend').attr('checked', (params['showUnresolvedTrend'] === 'true'));
                container.find('#periodName').val(params['periodName'] === "" ? "daily" : params['periodName']);
                container.find('#versionLabel').val(params['versionLabel']);
                container.find('#daysprevious').val(params['daysprevious'] === "" ? "30" : params['daysprevious']);
                if (AJS.Editor.JiraConnector.servers.length > 1) {
                    container.find('#jira-chart-servers').val(params['serverId']);
                }
            }
        }

    };

};

AJS.Editor.JiraChart.Panels.push(new AJS.Editor.JiraChart.Panels.CreatedVsResolvedChart(AJS.$));