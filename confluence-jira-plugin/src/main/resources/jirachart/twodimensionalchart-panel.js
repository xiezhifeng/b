AJS.Editor.JiraChart.Panel.TwoDimensionalChart = function($) {

    AJS.Editor.JiraChart.Panel.call(this);

    var CHART_TYPE = "twodimensional";
    var DEFAULT_NUMBER_OF_ROWS = 5;
    var thiz = this;

    var validateNumberToShow = function() {
        var $numberToShowError = $('.twodimensional-number-of-result-error');
        var numberToShow = thiz.chartElements.numberToShow.val();
        if (AJS.Editor.JiraChart.Helper.isNumber(numberToShow) && numberToShow > 0) {
            $numberToShowError.empty();
            return true;
        }

        $numberToShowError.html(AJS.I18n.getText("jirachart.panel.twodimensionalchart.numberToShow.error.label"));
        return false;
    };

    var setDefaultValues = function() {
        thiz.chartElements.numberToShow.val(DEFAULT_NUMBER_OF_ROWS);
        thiz.chartElements.xstattype.val('statuses');
        thiz.chartElements.ystattype.val('assignees');
    };

    this.title = AJS.I18n.getText('jirachart.panel.twodimensionalchart.title');
    this.chartType = CHART_TYPE;
    this.containerId = "#jira-chart-content-twodimensional";
    this.clickableElements = ".jira-chart-search button, .jira-chart-show-border, .jira-chart-show-infor, #twodimensional-show-total";
    this.onChangeElements = "#twodimensional-xaxis, #twodimensional-yaxis, #twodimensional-number-of-result";

    this.isFormValid = function() {
        var isNumberToShowValid = validateNumberToShow();
        return isNumberToShowValid && AJS.Editor.JiraChart.Helper.isJqlNotEmpty(thiz.chartElements.jql);
    };

    this.isResultValid = function() {
        return this.container.find("#chart-preview-iframe").contents().find(".two-dimensional-chart-table").length;
    };

    this.init = function(panel) {
        AJS.Editor.JiraChart.Panel.prototype.init.call(this, panel);
        setDefaultValues();
    };

    this.bindingChartElements = function() {
        this.chartElements = AJS.Editor.JiraChart.Helper.bindingCommonChartElements(this.container);
        this.chartElements.xstattype = this.container.find('#twodimensional-xaxis');
        this.chartElements.ystattype = this.container.find('#twodimensional-yaxis');
        this.chartElements.sortBy = this.container.find('#twodimensional-sortby');
        this.chartElements.sortDirection = this.container.find('#twodimensional-sort-direction');
        this.chartElements.showTotals = this.container.find('#twodimensional-show-total');
        this.chartElements.numberToShow = this.container.find('#twodimensional-number-of-result');
    };

    this.getChartParamsRequest = function() {
        var params = this.getMacroParamsFromDialog();
        var data = AJS.Editor.JiraChart.Helper.getCommonChartParamsRequest(params, this.chartType);
        data.macro.params.xstattype = params.xstattype;
        data.macro.params.ystattype = params.ystattype;
        data.macro.params.sortBy = params.sortBy;
        data.macro.params.sortDirection = params.sortDirection;
        data.macro.params.showTotals = params.showTotals;
        data.macro.params.numberToShow = params.numberToShow;
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
        macroParams.numberToShow = this.chartElements.numberToShow.val();
        return macroParams;
    };

    this.bindingDataFromMacroToForm = function(params) {
        if (params) {
            AJS.Editor.JiraChart.Helper.bindingCommonDataFromMacroToForm(thiz.chartElements, params);
            thiz.chartElements.xstattype.val(params.xstattype);
            thiz.chartElements.ystattype.val(params.ystattype);
            thiz.chartElements.sortBy.val(params.sortBy);
            thiz.chartElements.sortDirection.val(params.sortDirection);
            thiz.chartElements.showTotals.attr('checked', params.showTotals === 'true');
            thiz.chartElements.numberToShow.val(params.numberToShow);
        }
    };

    this.resetDialogValue = function() {
        AJS.Editor.JiraChart.Panel.prototype.resetDialogValue.call(this);
        setDefaultValues();
    };

    this.isImageChartExisted = function() {
        return this.container.find("#chart-preview-iframe").contents().find(".two-dimensional-chart-table").length > 0;
    };

    this.preBinding = function() {
        AJS.Editor.JiraChart.Helper.populateStatType(this.container, this.container.find('#twodimensional-xaxis'));
        AJS.Editor.JiraChart.Helper.populateStatType(this.container, this.container.find('#twodimensional-yaxis'));
        thiz.chartElements.xstattype.val('statuses');
        thiz.chartElements.ystattype.val('assignees');
    };

    this.bindingServerChange = function() {
        thiz.chartElements.server.change(function() {
            AJS.Editor.JiraChart.Helper.populateStatType(thiz.container, thiz.chartElements.xstattype);
            AJS.Editor.JiraChart.Helper.populateStatType(thiz.container, thiz.chartElements.ystattype);
            thiz.chartElements.xstattype.val('statuses');
            thiz.chartElements.ystattype.val('assignees');
            if (thiz.isFormValid()) {
                AJS.Editor.JiraChart.search(thiz.container);
            } else {
                AJS.Editor.JiraChart.disableInsert();
            }
        });
    };
};

AJS.Editor.JiraChart.Panel.TwoDimensionalChart.prototype = AJS.Editor.JiraChart.Panel.prototype;
AJS.Editor.JiraChart.Panel.TwoDimensionalChart.prototype.constructor = AJS.Editor.JiraChart.Panel.TwoDimensionalChart;
AJS.Editor.JiraChart.Panels.push(new AJS.Editor.JiraChart.Panel.TwoDimensionalChart(AJS.$));
