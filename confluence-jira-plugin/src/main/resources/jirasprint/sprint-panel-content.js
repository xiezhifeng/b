AJS.Editor.JiraSprint.Panel.PieChart = function($) {

    AJS.Editor.JiraSprint.Panel.call(this);

    var DIALOG_SPRINT_TITLE = AJS.I18n.getText('jirasprint.panel.title');
    var SPRINT_TYPE = "pie";
    var thiz = this;


    this.title = DIALOG_SPRINT_TITLE;
    this.sprintType = SPRINT_TYPE;
    this.containerId = "#jira-sprint-content";
    this.clickableElements = ".jira-sprint-search button, .jira-sprint-show-border, .jira-sprint-show-infor";
    this.onChangeElements = "#jira-sprint-statType, #jira-sprint-width";


    this.isFormValid = function() {
        return AJS.Editor.JiraSprint.Helper.isChartWidthValid(thiz.chartElements.width) && AJS.Editor.JiraSprint.Helper.isJqlNotEmpty(thiz.chartElements.jql);
    };

    this.isResultValid = function() {
        return this.container.find("#sprint-preview-iframe").contents().find(".jira-sprint-macro-wrapper").length;
    };

    this.bindingActions = function() {
        var thiz = this;
        AJS.Editor.JiraSprint.Panel.prototype.bindingActions.call(thiz);

        thiz.container.find(".widthInfo").tooltip({gravity: 'w'});
    };

    this.bindingServerChange = function() {
        thiz.chartElements.server.change(function() {
            AJS.Editor.JiraSprint.Helper.populateStatType(thiz.container, thiz.chartElements.statType);
            if (thiz.isFormValid()) {
                AJS.Editor.JiraSprint.search(thiz.container);
            } else {
                AJS.Editor.JiraSprint.disableInsert();
            }
        });
    };

    this.bindingChartElements = function() {
        this.chartElements = AJS.Editor.JiraSprint.Helper.bindingCommonChartElements(this.container);
        this.chartElements.statType = this.container.find('#jira-chart-statType');
    };

    this.getChartParamsRequest = function() {
        var params = this.getMacroParamsFromDialog();
        var data = AJS.Editor.JiraSprint.Helper.getCommonChartParamsRequest(params, this.sprintType);
        data.macro.params.statType = params.statType;
        return data;
    };

    this.getMacroParamsFromDialog = function() {
        var macroParams = AJS.Editor.JiraSprint.Helper.getCommonMacroParamsFromDialog(this.chartElements, this.container);
        macroParams.boardId = this.container.find('#jira-sprint-board').val();
        macroParams.key = this.container.find('#jira-sprint-sprintid').val();
        return macroParams;
    };

    this.bindingDataFromMacroToForm = function(params) {
        if (params) {
            AJS.Editor.JiraSprint.Helper.bindingCommonDataFromMacroToForm(this.chartElements, params);
            this.chartElements.statType.val(params['statType']);
        }
    };

    this.preBinding = function() {
        AJS.Editor.JiraSprint.Helper.populateStatType(this.container, this.container.find('#jira-sprint-statType'));
    };
};

AJS.Editor.JiraSprint.Panel.PieChart.prototype = AJS.Editor.JiraSprint.Panel.prototype;
AJS.Editor.JiraSprint.Panel.PieChart.prototype.constructor = AJS.Editor.JiraSprint.Panel.PieChart;

AJS.Editor.JiraSprint.Panels.push(new AJS.Editor.JiraSprint.Panel.PieChart(AJS.$));
