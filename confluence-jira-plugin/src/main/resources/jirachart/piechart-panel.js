AJS.Editor.JiraChart.Panel.PieChart = function($) {

    AJS.Editor.JiraChart.Panel.call(this);

    var PIE_CHART_TITLE = AJS.I18n.getText('jirachart.panel.piechart.title');
    var CHART_TYPE = "pie";
    var thiz = this;


    this.title = PIE_CHART_TITLE;
    this.chartType = CHART_TYPE;
    this.containerId = "#jira-chart-content-pie";
    this.clickableElements = ".jira-chart-search button, .jira-chart-show-border, .jira-chart-show-infor";
    this.onChangeElements = "#jira-chart-statType, #jira-chart-width";


    this.isFormValid = function() {
        return AJS.Editor.JiraChart.Helper.isChartWidthValid(thiz.chartElements.width) && AJS.Editor.JiraChart.Helper.isJqlNotEmpty(thiz.chartElements.jql);
    };

    this.isResultValid = function() {
        return this.container.find("#chart-preview-iframe").contents().find(".jira-chart-macro-wrapper").length;
    };

    this.bindingActions = function() {
        var thiz = this;
        AJS.Editor.JiraChart.Panel.prototype.bindingActions.call(thiz);

        thiz.container.find(".widthInfo").tooltip({gravity: 'w'});
    };

    this.bindingServerChange = function() {
        thiz.chartElements.server.change(function() {
            AJS.Editor.JiraChart.Helper.populateStatType(thiz.container, thiz.chartElements.statType);
            if (thiz.isFormValid()) {
                AJS.Editor.JiraChart.search(thiz.container);
            } else {
                AJS.Editor.JiraChart.disableInsert();
            }
        });
    };

    this.bindingChartElements = function() {
        this.chartElements = AJS.Editor.JiraChart.Helper.bindingCommonChartElements(this.container);
        this.chartElements.statType = this.container.find('#jira-chart-statType');
    };

    this.getChartParamsRequest = function() {
        var params = this.getMacroParamsFromDialog();
        var data = AJS.Editor.JiraChart.Helper.getCommonChartParamsRequest(params, this.chartType);
        data.macro.params.statType = params.statType;
        return data;
    };

    this.getMacroParamsFromDialog = function() {
        var macroParams = AJS.Editor.JiraChart.Helper.getCommonMacroParamsFromDialog(this.chartElements, this.container);
        macroParams.chartType = CHART_TYPE;
        macroParams.statType = this.chartElements.statType.val();
        return macroParams;
    };

    this.bindingDataFromMacroToForm = function(params) {
        if (params) {
            AJS.Editor.JiraChart.Helper.bindingCommonDataFromMacroToForm(this.chartElements, params);
            this.chartElements.statType.val(params['statType']);
        }
    };

    this.preBinding = function() {
        AJS.Editor.JiraChart.Helper.populateStatType(this.container, this.container.find('#jira-chart-statType'));
    };
};

AJS.Editor.JiraChart.Panel.PieChart.prototype = AJS.Editor.JiraChart.Panel.prototype;
AJS.Editor.JiraChart.Panel.PieChart.prototype.constructor = AJS.Editor.JiraChart.Panel.PieChart;

AJS.Editor.JiraChart.Panels.push(new AJS.Editor.JiraChart.Panel.PieChart(AJS.$));
