AJS.Editor.JiraChart.Panel.TwoDimensionalChart = function($) {

    AJS.Editor.JiraChart.Panel.call(this);

    var CHART_TYPE = "twodimensional";
    var thiz = this;

    var setupDefaultValues = function() {
    };

    this.title = AJS.I18n.getText('jirachart.panel.twodimensionalchart.title');
    this.chartType = CHART_TYPE;
    this.containerId = "#jira-chart-content-twodimensional";
    this.clickableElements = ".jira-chart-search button, .jira-chart-show-border, .jira-chart-show-infor, #created-vs-resolved-chart-cumulative, #created-vs-resolved-chart-showunresolvedtrend";
    this.onChangeElements = "#created-vs-resolved-chart-periodName, #created-vs-resolved-chart-daysprevious, #created-vs-resolved-chart-versionLabel, #jira-chart-servers, #jira-chart-width";

    this.isFormValid = function() {
        return AJS.Editor.JiraChart.Helper.isChartWidthValid(thiz.chartElements.width);
    };

    this.init = function(panel) {
        AJS.Editor.JiraChart.Panel.prototype.init.call(this, panel);
        setupDefaultValues();
    };

    this.bindingChartElements = function() {
        this.chartElements = AJS.Editor.JiraChart.Helper.bindingCommonChartElements(this.container);
    };

    this.bindingActions = function() {
        AJS.Editor.JiraChart.Panel.prototype.bindingActions.call(this);
    };

    this.getChartParamsRequest = function() {
        var params = this.getMacroParamsFromDialog();
        var data = AJS.Editor.JiraChart.Helper.getCommonChartParamsRequest(params, this.chartType);
        return data;
    };

    this.getMacroParamsFromDialog = function() {
        var macroParams = AJS.Editor.JiraChart.Helper.getCommonMacroParamsFromDialog(this.chartElements, this.container);
        macroParams.chartType = CHART_TYPE;
        return macroParams;
    };

    this.resetDialogValue = function() {
        AJS.Editor.JiraChart.Panel.prototype.resetDialogValue.call(this);
        setupDefaultValues();
    };

    this.bindingDataFromMacroToForm = function(params) {
        if (params) {
            AJS.Editor.JiraChart.Helper.bindingCommonDataFromMacroToForm(thiz.chartElements, params);
        }
    };
};

AJS.Editor.JiraChart.Panel.TwoDimensionalChart.prototype = AJS.Editor.JiraChart.Panel.prototype;
AJS.Editor.JiraChart.Panel.TwoDimensionalChart.prototype.constructor = AJS.Editor.JiraChart.Panel.TwoDimensionalChart;
AJS.Editor.JiraChart.Panels.push(new AJS.Editor.JiraChart.Panel.TwoDimensionalChart(AJS.$));
