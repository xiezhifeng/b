AJS.Editor.JiraChart.Panel.CreatedVsResolvedChart = function($) {

    AJS.Editor.JiraChart.Panel.call(this);

    var CREATED_VS_RESOLVED_CHART_TITLE = AJS.I18n.getText('jirachart.panel.createdvsresolvedchart.title');
    var CREATED_VS_RESOLVED_CHART_ID = "createdvsresolved";
    var thiz = this;
    var setupDefaultValues = function() {
        thiz.container.find('#created-vs-resolved-chart-periodName').val("daily");
        thiz.container.find('#created-vs-resolved-chart-daysprevious').val("30");
    };

    var isFormValid = function() {
        return validateDayPrevious() && thiz.container.find(".days-previous-error").is(':empty') && thiz.container.find("#jira-chart-macro-dialog-validation-error").length == 0;
    };

    var validateDayPrevious = function() {
        var periodName  = thiz.chartElements.periodName.val();
        var dayprevious = $.trim(thiz.chartElements.daysprevious.val());
        var error = thiz.container.find(".days-previous-error");
        if (dayprevious === "") {
            thiz.container.find(".days-previous-error").html(AJS.I18n.getText("jirachart.panel.createdvsresolvedchart.daysprevious.required.error"));
            return false;
        }

        if(!AJS.Editor.JiraChart.Helper.isNumber(dayprevious) || dayprevious < 0) {
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


    this.title = CREATED_VS_RESOLVED_CHART_TITLE;
    this.id = CREATED_VS_RESOLVED_CHART_ID;
    this.containerId = "#jira-chart-content-createdvsresolved";
    this.clickableElements = ".jira-chart-search button, .jira-chart-show-border, .jira-chart-show-infor, #created-vs-resolved-chart-cumulative, #created-vs-resolved-chart-showunresolvedtrend";
    this.validateClickableElements = function() {
        return validateDayPrevious() && isFormValid();
    };

    this.init = function(panel) {
        AJS.Editor.JiraChart.Panel.prototype.init.call(this, panel);
        setupDefaultValues();
    };

    this.bindingChartElements = function() {
        this.chartElements = AJS.Editor.JiraChart.Helper.bindingCommonChartElements(this.container);
        this.chartElements.periodName = this.container.find('#created-vs-resolved-chart-periodName');
        this.chartElements.daysprevious = this.container.find('#created-vs-resolved-chart-daysprevious');
        this.chartElements.isCumulative = this.container.find('#created-vs-resolved-chart-cumulative');
        this.chartElements.showUnresolvedTrend = this.container.find('#created-vs-resolved-chart-showunresolvedtrend');
        this.chartElements.versionLabel = this.container.find('#created-vs-resolved-chart-versionLabel');
    };

    this.bindingActions = function() {
        AJS.Editor.JiraChart.Panel.prototype.bindingActions.call(this);

        // bind change event on periodName
        this.container.find("#created-vs-resolved-chart-periodName, #created-vs-resolved-chart-daysprevious, #created-vs-resolved-chart-versionLabel").change(function() {
            if (validateDayPrevious() && isFormValid()) {
                AJS.Editor.JiraChart.search(thiz.container);
            } else {
                AJS.Editor.JiraChart.disableInsert();
            }
        });

        //added tooltip
        this.container.find(".widthInfo").tooltip({gravity: 'w'});
        this.container.find(".showunresolvedtrendInfo").tooltip({gravity: 'w'});
        this.container.find(".cumulativeInfo").tooltip({gravity: 'w'});
        this.container.find(".versionLabelInfo").tooltip({gravity: 'w'});
        this.container.find(".daysPreviousInfo").tooltip({gravity: 'w'});
        this.container.find(".periodNameInfo").tooltip({gravity: 'w'});
    };

    this.getChartParamsRequest = function() {
        var params = this.getMacroParamsFromDialog();
        var data = AJS.Editor.JiraChart.Helper.getCommonChartParamsRequest(params, this.id);
        data.macro.params.periodName = params.periodName;
        data.macro.params.daysprevious = params.daysprevious;
        data.macro.params.isCumulative = params.isCumulative;
        data.macro.params.showUnresolvedTrend = params.showUnresolvedTrend;
        return data;
    };

    this.getMacroParamsFromDialog = function() {
        var macroParams = AJS.Editor.JiraChart.Helper.getCommonMacroParamsFromDialog(this.chartElements, this.container);
        macroParams.chartType = CREATED_VS_RESOLVED_CHART_ID;
        macroParams.periodName = this.chartElements.periodName.val();
        macroParams.daysprevious = $.trim(this.chartElements.daysprevious.val());
        macroParams.isCumulative = this.chartElements.isCumulative.prop('checked');
        macroParams.showUnresolvedTrend = this.chartElements.showUnresolvedTrend.prop('checked');
        macroParams.versionLabel = this.chartElements.versionLabel.prop('checked');

        return macroParams;
    };

    this.resetDialogValue = function() {
        AJS.Editor.JiraChart.Panel.prototype.resetDialogValue.call(this);
        setupDefaultValues();
    };

    this.bindingDataFromMacroToForm = function(params) {
        if (params) {
            AJS.Editor.JiraChart.Helper.bindingCommonDataFromMacroToForm(thiz.chartElements, params);
            thiz.chartElements.isCumulative.attr('checked', (params['isCumulative'] !== 'false'));
            thiz.chartElements.showUnresolvedTrend.attr('checked', (params['showUnresolvedTrend'] === 'true'));
            thiz.chartElements.periodName.val(params['periodName'] === "" ? "daily" : params['periodName']);
            thiz.chartElements.versionLabel.val(params['versionLabel']);
            thiz.chartElements.daysprevious.val(params['daysprevious'] === "" ? "30" : params['daysprevious']);
        }
    };
};

AJS.Editor.JiraChart.Panel.CreatedVsResolvedChart.prototype = AJS.Editor.JiraChart.Panel.prototype;
AJS.Editor.JiraChart.Panel.CreatedVsResolvedChart.prototype.constructor = AJS.Editor.JiraChart.Panels.CreatedVsResolvedChart;

if (AJS.DarkFeatures.isEnabled('jirachart.createdvsresolved')) {
    AJS.Editor.JiraChart.Panels.push(new AJS.Editor.JiraChart.Panel.CreatedVsResolvedChart(AJS.$));
}
