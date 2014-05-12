AJS.Editor.JiraChart.Panel = function() {};

AJS.Editor.JiraChart.Panel.prototype = {

    /**
     * Init chart in the first time show dialog
     */
    init: function(panel) {
        var chartType = this.id;
        var contentJiraChart = Confluence.Templates.ConfluenceJiraPlugin.contentJiraChart({
            'isMultiServer' : AJS.Editor.JiraConnector.servers.length > 1,
            'chartType' : chartType
        });
        panel.html(contentJiraChart);
        this.container = AJS.$(this.containerId);
        AJS.Editor.JiraChart.clearChartContent(this.container);
        AJS.Editor.JiraChart.loadServers(this.container);

        this.bindingChartElements();
        this.bindingActions();
    },



    /**
     * Binding event to all element
     */
    bindingActions: function() {
        var thiz = this;
        var clickableElements = thiz.container.find(thiz.clickableElements);
        clickableElements.click(function() {
            if(thiz.validateClickableElements) {
                AJS.Editor.JiraChart.search(thiz.container);
            } else {
                AJS.Editor.JiraChart.disableInsert();
            }

        });

        //bind out focus in width field
        var $width = thiz.chartElements.width;
        $width.change(function() {
            if (AJS.Editor.JiraChart.Helper.isChartWidthValid($width)) {
                AJS.Editor.JiraChart.search(thiz.container);
            }
        });

        //for auto convert when paste url
        thiz.container.find("#jira-chart-search-input").change(function() {
            if (this.value !== thiz.jqlWhenEnterKeyPress) {
                AJS.Editor.JiraChart.enableInsert();
            }
            thiz.jqlWhenEnterKeyPress = "";
        }).bind("paste", function() {
            if(thiz.isFormValid()) {
                setTimeout(function () {
                    AJS.Editor.JiraChart.search(thiz.container);
                }, 100);
            }
        });

        //binding enter event
        var input = thiz.container.find("input[type='text']");
        var setJQLWhenEnterPress = function($input) {
            if ($input.attr('id') === 'jira-chart-search-input') {
                thiz.jqlWhenEnterKeyPress = $input.val();
            }
        };

        input.unbind('keydown').keydown(function(e) {
            if (e.which == 13){
                var keyup = function(e) {
                    input.unbind('keyup', keyup);
                    AJS.Editor.JiraChart.search(thiz.container);
                    setJQLWhenEnterPress(input);
                    return AJS.stopEvent(e);
                };
                input.keyup(keyup);
                return AJS.stopEvent(e);
            }
        });

        //bind sever select
        thiz.chartElements.server.change(function() {
            thiz.resetDialogValue();
        });

        //bind select option
        thiz.bindSelectOption();
    },

    //render chart
    renderChart: function() {
        var chartParams = this.getChartParamsRequest();
        var previewUrl = Confluence.getContextPath() + "/rest/tinymce/1/macro/preview";
        var imageContainer = this.container.find(".jira-chart-img");
        //load image loading
        imageContainer.html('<div class="loading-data"></div>');
        var imageLoading = imageContainer.find(".loading-data")[0];
        AJS.$.data(imageLoading, "spinner", Raphael.spinner(imageLoading, 50, "#666"));

        AJS.$.ajax({
            url : previewUrl,
            type : "POST",
            contentType : "application/json",
            data : JSON.stringify(chartParams)
        })
            .done(function(data) {
                imageContainer.html('').hide(); // this will be re-show right after iframe is loaded
                var $iframe = AJS.$('<iframe frameborder="0" id="chart-preview-iframe"></iframe>');
                $iframe.appendTo(imageContainer);

                // window and document belong to iframe
                var win = $iframe[0].contentWindow,
                    doc = win.document;

                //prevent call AJS.MacroBrowser.previewOnload when onload.
                //business of this function is not any effect to my function
                data = data.replace("window.onload", "var chartTest");

                // write data into iframe
                doc.open();
                doc.write(data);
                doc.close();

                var setupInsertButton = function($iframe) {
                    if ($iframe.contents().find(".jira-chart-macro-img").length > 0) {
                        AJS.Editor.JiraChart.enableInsert();
                    } else {
                        AJS.Editor.JiraChart.disableInsert();
                    }
                };
                // make sure everyting has loaded completely
                $iframe.on('load', function() {
                    win.AJS.$('#main').addClass('chart-preview-main');
                    imageContainer.show();
                    setupInsertButton(AJS.$(this));
                });
            })
            .error(function() {
                AJS.log("Jira Chart Macro - Fail to get data from macro preview");
                imageContainer.html(Confluence.Templates.ConfluenceJiraPlugin.showMessageRenderJiraChart());
                AJS.Editor.JiraChart.disableInsert();
            });
    },

    resetDialogValue: function() {
        var $inputElements = $('input', this.container);
        $inputElements.filter(':text').val('');
        $inputElements.filter(':checked').removeAttr('checked');
        this.container.find('#jira-chart-search-input').val();
        this.container.find(".jira-chart-img").empty();
        this.resetDisplayOption();
    },

    resetDisplayOption: function() {
        var thiz = this;
        var displayOption = thiz.container.find('.jirachart-display-opts-close, .jirachart-display-opts-open');
        displayOption.addClass('jirachart-display-opts-open');
        displayOption.removeClass('jirachart-display-opts-close');
        setTimeout(function() {
            var jiraChartOption = thiz.container.find('.jira-chart-option');
            jiraChartOption.scrollTop(0);
            jiraChartOption.css({
                overflow: 'hidden',
                top: '430px'
            });
        }, 0);
    },

    bindSelectOption: function() {
        var thiz = this;
        var displayOptPanel = function(open) {
            var jiraChartOption = thiz.container.find('.jira-chart-option');
            var topMargin = 40;
            var top = jiraChartOption.position().top + "px";
            var bottom =  "";
            var animateConfig = {top: 430};

            if (open) {
                top = "";
                bottom =  topMargin - jiraChartOption.find("#jiraChartMacroOption").height() + "px";
                animateConfig = {bottom: 0};
                jiraChartOption.css("overflow", "auto");
            } else {
                jiraChartOption.css("overflow", "hidden");
            }
            jiraChartOption.css("top", top);
            jiraChartOption.css("bottom", bottom);
            jiraChartOption.animate(animateConfig, 500);
        };

        var displayOptsOverlay = this.container.find('.jira-chart-option');
        displayOptsOverlay.css("top", "430px");
        var displayOptsBtn = this.container.find('.jirachart-display-opts-close, .jirachart-display-opts-open');
        displayOptsBtn.click(function(e) {
            var thiz = $(this);
            e.preventDefault();
            if (thiz.hasClass("disabled")) {
                return;
            }
            var isOpenButton = thiz.hasClass('jirachart-display-opts-open');

            if (isOpenButton) {
                displayOptPanel(true);
                thiz.addClass('jirachart-display-opts-close');
                thiz.removeClass('jirachart-display-opts-open');
            } else {
                displayOptPanel();
                thiz.removeClass('jirachart-display-opts-close');
                thiz.addClass('jirachart-display-opts-open');

            }
        });

    },

    isExistImageChart: function() {
        return this.container.find("#chart-preview-iframe").contents().find(".jira-chart-macro-img").length > 0;
    },

    focusForm: function() {
        this.container.find("#jira-chart-search-input").focus();
    }
};

