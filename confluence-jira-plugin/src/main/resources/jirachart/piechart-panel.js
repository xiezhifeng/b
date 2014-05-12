AJS.Editor.JiraChart.Panel.PieChart = function($) {

    AJS.Editor.JiraChart.Panel.call(this);

    var PIE_CHART_TITLE = AJS.I18n.getText('jirachart.panel.piechart.title');
    var PIE_CHART_ID = "pie";
    var thiz = this;

    this.id = PIE_CHART_ID;
    this.title = PIE_CHART_TITLE;
    this.containerId = "#jira-chart-content-pie";
    this.clickableElements = ".jira-chart-search button, .jira-chart-show-border, .jira-chart-show-infor";

    this.isFormValid = function() {
        return this.container.find("#jira-chart-macro-dialog-validation-error").length == 0;
    };
    this.validateClickableElements = this.isFormValid;

    this.bindingActions = function() {
        AJS.Editor.JiraChart.Panel.prototype.bindingActions.call(this);

        thiz.container.find("#jira-chart-statType").change(function() {
            AJS.Editor.JiraChart.search(thiz.container);
        });

        this.container.find(".widthInfo").tooltip({gravity: 'w'});
    };

    this.bindingChartElements = function() {
        this.chartElements = AJS.Editor.JiraChart.Helper.bindingCommonChartElements(this.container);
        this.chartElements.statType = this.container.find('#jira-chart-statType');
    };

    this.getChartParamsRequest = function() {
        var params = this.getMacroParamsFromDialog();
        var data = AJS.Editor.JiraChart.Helper.getCommonChartParamsRequest(params, this.id);
        data.macro.params.statType = params.statType;
        return data;
    };

    this.getMacroParamsFromDialog = function() {
        var macroParams = AJS.Editor.JiraChart.Helper.getCommonMacroParamsFromDialog(this.chartElements, this.container);
        macroParams.chartType = PIE_CHART_ID;
        macroParams.statType = this.chartElements.statType.val();
        return macroParams;
    };

    this.bindingDataFromMacroToForm = function(params) {
        if (params) {
            AJS.Editor.JiraChart.Helper.bindingCommonDataFromMacroToForm(this.chartElements, params);
            this.chartElements.statType.val(params['statType']);
        }
    };
};

AJS.Editor.JiraChart.Panel.PieChart.prototype = Object.create(AJS.Editor.JiraChart.Panel.prototype);
AJS.Editor.JiraChart.Panel.PieChart.prototype.constructor = AJS.Editor.JiraChart.Panel.PieChart;

AJS.Editor.JiraChart.Panels.push(new AJS.Editor.JiraChart.Panel.PieChart(AJS.$));