AJS.Editor.JiraChart.Panels.PieChart = function($) {

    var PIE_CHART_TITLE = AJS.I18n.getText('jirachart.panel.piechart.title');
    var PIE_CHART_ID = "pie";
    var container;
    var jqlWhenEnterKeyPress;
    var previousJiraChartWidth = "";

    var bindingActions = function() {
        var clickableElements = container.find(".jira-chart-search button, #jira-pie-chart-show-border, #jira-pie-chart-show-infor");
        //bind search button, click in border
        clickableElements.click(function() {
            AJS.Editor.JiraChart.search(container);
        });


        //bind out focus in width field
        container.find("#jira-pie-chart-width").focusout(function(event) {
            if (AJS.Editor.JiraChart.validate(container.find('#jira-pie-chart-width'))) {
                var jiraChartWidth = AJS.Editor.JiraChart.convertFormatWidth(this.value);
                if (jiraChartWidth != previousJiraChartWidth) {
                    previousJiraChartWidth = jiraChartWidth;
                    AJS.Editor.JiraChart.search(container);
                }
                AJS.Editor.JiraChart.enableInsert();
            }
        });

        //for auto convert when paste url
        container.find("#jira-chart-search-input").change(function() {
            if (this.value !== jqlWhenEnterKeyPress) {
                AJS.Editor.JiraChart.enableInsert();
            }
            jqlWhenEnterKeyPress = "";
        }).bind("paste", function() {
            AJS.Editor.JiraChart.autoConvert(container);
        });

        container.find("#jira-chart-statType").change(function(event) {
            AJS.Editor.JiraChart.search(container);
        });

        AJS.Editor.JiraChart.setActionOnEnter(container.find("input[type='text']"), AJS.Editor.JiraChart.search, container);
        AJS.Editor.JiraChart.bindSelectOption(container);

    };

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
            container = $("#jira-chart-content-pie");
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
                        "border" : params.border,
                        "showinfor" : params.showinfor,
                        "statType" : params.statType,
                        "chartType": "pie"
                    }
                }
            };

            AJS.Editor.JiraChart.previewChart(dataToSend);
        },

        getMacroParamsFromDialog: function() {
            var selectedServer = AJS.Editor.JiraChart.getSelectedServer(container);
            return {
                jql: encodeURIComponent(container.find('#jira-chart-search-input').val()),
                statType: container.find('#jira-chart-statType').val(),
                width: AJS.Editor.JiraChart.convertFormatWidth(container.find('#jira-pie-chart-width').val()),
                border: container.find('#jira-pie-chart-show-border').prop('checked'),
                showinfor: container.find('#jira-pie-chart-show-infor').prop('checked'),
                serverId:  selectedServer.id,
                server: selectedServer.name,
                isAuthenticated: !selectedServer.authUrl,
                chartType: "pie"
            };

        } ,

        isExistImageChart: function() {
            return container.find("#chart-preview-iframe").contents().find(".jira-chart-macro-img").length > 0;
        },

        focusForm: function() {
            container.find("#jira-chart-search-input").focus();
        },

        resetDialogValue: function() {
            var $inputElements = $('input', container);
            $inputElements.filter(':text').val('');
            $inputElements.filter(':checked').removeAttr('checked');
            container.find('#jira-chart-search-input').val();
            container.find(".jira-chart-img").empty();

            AJS.Editor.JiraChart.resetDisplayOption(container);
        },

        bindingDataFromMacroToForm: function(params) {
            if (params) {
                container.find('#jira-chart-search-input').val(decodeURIComponent(params['jql']));
                container.find('#jira-chart-statType').val(params['statType']);
                container.find('#jira-pie-chart-width').val(params['width']);
                container.find('#jira-pie-chart-show-border').attr('checked', (params['border'] === 'true'));
                container.find('#jira-pie-chart-show-infor').attr('checked', (params['showinfor'] === 'true'));
                if (AJS.Editor.JiraConnector.servers.length > 1) {
                    container.find('#jira-chart-servers').val(params['serverId']);
                }
            }
        }

    };

};

AJS.Editor.JiraChart.Panels.push(new AJS.Editor.JiraChart.Panels.PieChart(AJS.$));