AJS.Editor.JiraChart.Panels.CreatedVsResolvedChart = function($) {
    var CREATED_VS_RESOLVED_CHART_TITLE = AJS.I18n.getText('jirachart.panel.createdvsresolvedchart.title');
    var CREATED_VS_RESOLVED_CHART_ID = "createdvsresolved";
    var container;
    var jqlWhenEnterKeyPress;

    var initializeDefaultValues = function() {
        container.find('#created-vs-resolved-chart-periodName').val("daily");
        container.find('#created-vs-resolved-chart-daysprevious').val("30");
    };

    var bindingActions = function() {
        var clickableElements = container.find(".jira-chart-search button, #jira-createdvsresolved-chart-show-border, #jira-createdvsresolved-chart-show-infor, #created-vs-resolved-chart-cumulative, #created-vs-resolved-chart-showunresolvedtrend");
        //bind search button, click in border
        clickableElements.click(function() {
            if (validateDayPrevious() && isFormValid()) {
                AJS.Editor.JiraChart.search(container);
            } else {
                AJS.Editor.JiraChart.disableInsert();
            }

        });

        // bind change events
        container.find("#created-vs-resolved-chart-periodName, #created-vs-resolved-chart-daysprevious, #created-vs-resolved-chart-versionLabel, #jira-chart-servers").change(function(event) {

            if (validateDayPrevious() && isFormValid()) {
                AJS.Editor.JiraChart.search(container);
            } else {
                AJS.Editor.JiraChart.disableInsert();
            }
        });

        container.find("#jira-createdvsresolved-chart-width").change(function(event) {

            if (AJS.Editor.JiraChart.validate(container.find('#jira-createdvsresolved-chart-width')) && validateDayPrevious()) {
                AJS.Editor.JiraChart.search(container);
            } else {
                AJS.Editor.JiraChart.disableInsert();
            }
        });

        //for auto convert when paste url
        container.find("#jira-chart-search-input").change(function() {
            if (this.value !== jqlWhenEnterKeyPress) {
                AJS.Editor.JiraChart.enableInsert();
            }
            jqlWhenEnterKeyPress = "";
        }).bind("paste", function() {
            if (validateDayPrevious() && isFormValid()) {
                AJS.Editor.JiraChart.autoConvert(container);
            }
        });

        AJS.Editor.JiraChart.bindSelectedServer(container);
        AJS.Editor.JiraChart.setActionOnEnter(container.find("input[type='text']"), AJS.Editor.JiraChart.search, container);
        AJS.Editor.JiraChart.bindSelectOption(container);

        // added tooltips
        container.find(".widthInfo").tooltip({gravity: 'w'});
        container.find(".showunresolvedtrendInfo").tooltip({gravity: 'w'});
        container.find(".cumulativeInfo").tooltip({gravity: 'w'});
        container.find(".versionLabelInfo").tooltip({gravity: 'w'});
        container.find(".daysPreviousInfo").tooltip({gravity: 'w'});
        container.find(".periodNameInfo").tooltip({gravity: 'w'});

    };

    var isFormValid = function() {
        return container.find(".days-previous-error").is(':empty') && container.find("#jira-chart-macro-dialog-validation-error").length == 0;
    };

    var validateDayPrevious = function() {
        var periodName  = container.find("#created-vs-resolved-chart-periodName").val();
        var dayprevious = $.trim(container.find("#created-vs-resolved-chart-daysprevious").val());
        var error = container.find(".days-previous-error");
        if (dayprevious === "") {
            container.find(".days-previous-error").html(AJS.I18n.getText("jirachart.panel.createdvsresolvedchart.daysprevious.required.error"));
            return false;
        }

        if(!AJS.Editor.JiraChart.isNumber(dayprevious) || dayprevious < 0) {
            error.html(AJS.I18n.getText("jirachart.panel.createdvsresolvedchart.daysprevious.wrongnumber"));
            return false;
        }

        switch (periodName) {

            case "hourly":
                isValid  = dayprevious <= 10;
                if (!isValid) {
                    error.html(AJS.I18n.getText("jirachart.panel.createdvsresolvedchart.daysprevious.error", 10, periodName));
                }
                break;

            case "daily":
                isValid  = dayprevious <= 300;
                if (!isValid) {
                    error.html(AJS.I18n.getText("jirachart.panel.createdvsresolvedchart.daysprevious.error", 300, periodName));
                }
                break;

            case "weekly":
                isValid  = dayprevious <= 1750;
                if (!isValid) {
                    error.html(AJS.I18n.getText("jirachart.panel.createdvsresolvedchart.daysprevious.error", 1750, periodName));
                }
                break;

            case "monthly":
                isValid  = dayprevious <= 7500;
                if (!isValid) {
                    error.html(AJS.I18n.getText("jirachart.panel.createdvsresolvedchart.daysprevious.error", 7500, periodName));
                }
                break;

            case "quarterly":
                isValid  = dayprevious <= 22500;
                if (!isValid) {
                    error.html(AJS.I18n.getText("jirachart.panel.createdvsresolvedchart.daysprevious.error", 22500, periodName));
                }
                break;

            case "yearly":
                isValid  = dayprevious <= 36500;
                if (!isValid) {
                    error.html(AJS.I18n.getText("jirachart.panel.createdvsresolvedchart.daysprevious.error", 36500, periodName));
                }
                break;
            default:
                isValid = false;
        }
        if (isValid) {
            error.empty();
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
            container = $("#jira-chart-content-createdvsresolved");
            initializeDefaultValues();
            bindingActions();
            AJS.Editor.JiraChart.clearChartContent(container);
            AJS.Editor.JiraChart.loadServers(container);
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
                        "versionLabel": params.versionLabel,
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

            var selectedServer = AJS.Editor.JiraChart.getSelectedServer(container);
            return {
                jql: encodeURIComponent(container.find('#jira-chart-search-input').val()),
                periodName: container.find('#created-vs-resolved-chart-periodName').val(),
                width: AJS.Editor.JiraChart.convertFormatWidth(container.find('#jira-createdvsresolved-chart-width').val()),
                daysprevious: $.trim(container.find('#created-vs-resolved-chart-daysprevious').val()),
                isCumulative: container.find('#created-vs-resolved-chart-cumulative').prop('checked'),
                showUnresolvedTrend: container.find('#created-vs-resolved-chart-showunresolvedtrend').prop('checked'),
                versionLabel: container.find('#created-vs-resolved-chart-versionLabel').val(),
                border: container.find('#jira-createdvsresolved-chart-show-border').prop('checked'),
                showinfor: container.find('#jira-createdvsresolved-chart-show-infor').prop('checked'),
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
            container.find("#created-vs-resolved-chart-periodName").val("daily");
            container.find("#created-vs-resolved-chart-daysprevious").val("30");
            container.find("#created-vs-resolved-chart-showunresolvedtrend").removeAttr("checked");
            container.find("#created-vs-resolved-chart-cumulative").removeAttr("checked")
            container.find("#jira-createdvsresolved-chart-show-border").removeAttr("checked");
            container.find("#jira-createdvsresolved-chart-show-infor").removeAttr("checked");
            container.find("#jira-chart-search-input").val("");
            container.find("#jira-createdvsresolved-chart-width").val("");
            container.find(".jira-chart-img").empty();
            container.find("#created-vs-resolved-chart-versionLabel").val("");
            container.find("#jira-chart-macro-dialog-validation-error").remove();
            container.find(".days-previous-error").empty();

            AJS.Editor.JiraChart.resetDisplayOption(container);
        },

        bindingDataFromMacroToForm : function(params) {
            if (params) {
                container.find('#jira-chart-search-input').val(decodeURIComponent(params['jql']));
                container.find('#jira-createdvsresolved-chart-width').val(params['width']);
                container.find('#jira-createdvsresolved-chart-show-border').attr('checked', (params['border'] === 'true'));
                container.find('#jira-createdvsresolved-chart-show-infor').attr('checked', (params['showinfor'] === 'true'));
                container.find('#created-vs-resolved-chart-cumulative').attr('checked', (params['isCumulative'] !== 'false'));
                container.find('#created-vs-resolved-chart-showunresolvedtrend').attr('checked', (params['showUnresolvedTrend'] === 'true'));
                container.find('#created-vs-resolved-chart-periodName').val(params['periodName'] === "" ? "daily" : params['periodName']);
                container.find('#created-vs-resolved-chart-versionLabel').val(params['versionLabel']);
                container.find('#created-vs-resolved-chart-daysprevious').val(params['daysprevious'] === "" ? "30" : params['daysprevious']);
                if (AJS.Editor.JiraConnector.servers.length > 1) {
                    container.find('#jira-chart-servers').val(params['serverId']);
                }
            }
        }

    };

};

if (AJS.DarkFeatures.isEnabled('jirachart.createdvsresolved')) {
    AJS.Editor.JiraChart.Panels.push(new AJS.Editor.JiraChart.Panels.CreatedVsResolvedChart(AJS.$));
}
