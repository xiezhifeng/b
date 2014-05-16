AJS.Editor.JiraChart.Panel.TwoDimensionalChart = function($) {

    AJS.Editor.JiraChart.Panel.call(this);

    var CHART_TYPE = "twodimensional";
    var thiz = this;

    this.title = AJS.I18n.getText('jirachart.panel.twodimensionalchart.title');
    this.chartType = CHART_TYPE;
    this.containerId = "#jira-chart-content-twodimensional";
    this.clickableElements = ".jira-chart-search button, .jira-chart-show-border, .jira-chart-show-infor, #twodimensional-show-total";
    this.onChangeElements = "#twodimensional-xaxis, #twodimensional-yaxis, #twodimensional-sortby, #twodimensional-sort-direction, #jira-chart-servers, #jira-chart-width";

    this.isFormValid = function() {
        return AJS.Editor.JiraChart.Helper.isChartWidthValid(thiz.chartElements.width);
    };

    this.init = function(panel) {
        AJS.Editor.JiraChart.Panel.prototype.init.call(this, panel);
    };

    this.bindingChartElements = function() {
        this.chartElements = AJS.Editor.JiraChart.Helper.bindingCommonChartElements(this.container);
        this.chartElements.xstattype = this.container.find('#twodimensional-xaxis');
        this.chartElements.ystattype = this.container.find('#twodimensional-yaxis');
        this.chartElements.sortBy = this.container.find('#twodimensional-sortby');
        this.chartElements.sortDirection = this.container.find('#twodimensional-sort-direction');
        this.chartElements.showTotals = this.container.find('#twodimensional-show-total');
    };

    this.bindingActions = function() {
        AJS.Editor.JiraChart.Panel.prototype.bindingActions.call(this);
    };

    this.getChartParamsRequest = function() {
        var params = this.getMacroParamsFromDialog();
        var data = AJS.Editor.JiraChart.Helper.getCommonChartParamsRequest(params, this.chartType);
        data.macro.params.xstattype = params.xstattype;
        data.macro.params.ystattype = params.ystattype;
        data.macro.params.sortBy = params.sortBy;
        data.macro.params.sortDirection = params.sortDirection;
        data.macro.params.showTotals = params.showTotals;
        return data;
    };

    this.getMacroParamsFromDialog = function() {
        var macroParams = AJS.Editor.JiraChart.Helper.getCommonMacroParamsFromDialog(this.chartElements, this.container);
        macroParams.chartType = CHART_TYPE;
        macroParams.xstattype = this.chartElements.xstattype.val();
        macroParams.ystattype = $.trim(this.chartElements.ystattype.val());
        macroParams.sortBy = $.trim(this.chartElements.sortBy.val());
        macroParams.sortDirection = $.trim(this.chartElements.sortDirection.val());
        macroParams.showTotals = this.chartElements.showTotals.prop('checked');
        return macroParams;
    };

    this.bindingDataFromMacroToForm = function(params) {
        if (params) {
            AJS.Editor.JiraChart.Helper.bindingCommonDataFromMacroToForm(thiz.chartElements, params);
        }
    };

    this.isImageChartExisted = function() {
        return this.container.find("#chart-preview-iframe").contents().find(".two-dimensional-chart-table").length > 0;
    };
};

AJS.Editor.JiraChart.Panel.TwoDimensionalChart.prototype = AJS.Editor.JiraChart.Panel.prototype;
AJS.Editor.JiraChart.Panel.TwoDimensionalChart.prototype.constructor = AJS.Editor.JiraChart.Panel.TwoDimensionalChart;
AJS.Editor.JiraChart.Panels.push(new AJS.Editor.JiraChart.Panel.TwoDimensionalChart(AJS.$));
