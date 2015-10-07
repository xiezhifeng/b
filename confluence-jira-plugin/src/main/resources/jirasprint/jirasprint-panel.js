AJS.Editor.JiraSprint.Panel = function() {};

AJS.Editor.JiraSprint.Panel.prototype = {

    /**
     * Init sprint in the first time show dialog
     */
    init: function(panel) {
        var sprintType = this.sprintType;
        var contentJiraSprint = Confluence.Templates.ConfluenceJiraPlugin.contentJiraSprint({});
        panel.html(contentJiraSprint);
        this.container = AJS.$(this.containerId);
        AJS.Editor.JiraSprint.clearSprintContent(this.container);
        AJS.Editor.JiraSprint.loadServers(this.container);
        this.bindingChartElements();
        this.bindingActions();
    },

    /**
     * Binding event to all element
     */
    bindingActions: function() {
        var thiz = this;

        var eventHandler = function() {
            if (thiz.isFormValid()) {
                AJS.Editor.JiraSprint.search(thiz.container);
            } else {
                AJS.Editor.JiraSprint.disableInsert();
            }
        };

        var clickableElements = thiz.container.find(thiz.clickableElements);
        clickableElements.click(eventHandler);

        var onChangeElements = thiz.container.find(thiz.onChangeElements);
        onChangeElements.change(eventHandler);

        //for auto convert when paste url
        thiz.chartElements.jql.change(function() {
            if (this.value !== thiz.jqlWhenEnterKeyPress) {
                thiz.container.find(".jira-sprint-img").empty();
                AJS.Editor.JiraSprint.disableInsert();
            }
            thiz.jqlWhenEnterKeyPress = "";
        }).bind("paste", function() {
            setTimeout(function () {
                if (thiz.isFormValid()) {
                    thiz.jqlWhenEnterKeyPress = thiz.chartElements.jql.val();
                    AJS.Editor.JiraSprint.search(thiz.container);
                }
            }, 100);
        });

        //binding enter event
        var input = thiz.container.find("input[type='text']");
        var setJQLWhenEnterPress = function($input) {
            if ($input.attr('id') === 'jira-sprint-search-input') {
                thiz.jqlWhenEnterKeyPress = $input.val();
            }
        };

        input.unbind('keydown').keydown(function(e) {
            if (e.which == 13){
                var keyup = function(e) {
                    input.unbind('keyup', keyup);
                    if (thiz.isFormValid()) {
                        AJS.Editor.JiraSprint.search(thiz.container);
                    }
                    setJQLWhenEnterPress(input);
                    return AJS.stopEvent(e);
                };
                input.keyup(keyup);
                return AJS.stopEvent(e);
            }
        });

        //bind select option
        thiz.bindSelectOption();
        thiz.bindingServerChange();
    },

    bindingServerChange: function() {
        var thiz = this;
        thiz.chartElements.server.change(function() {
            if (thiz.isFormValid() && AJS.Editor.JiraSprint.validateServerSupportedChart(thiz.container)) {
                AJS.Editor.JiraSprint.search(thiz.container);
            } else {
                AJS.Editor.JiraSprint.disableInsert();
            }
        });
    },

    //render chart
    renderChart: function() {
        var thiz = this;
        var chartParams = this.getChartParamsRequest();
        var previewUrl = Confluence.getContextPath() + "/rest/tinymce/1/macro/preview";
        var imageContainer = this.container.find(".jira-chart-img");
        //load image loading
        imageContainer.html('<div class="loading-data"></div>');
        var imageLoading = imageContainer.find(".loading-data")[0];
        AJS.$.data(imageLoading, "spinner", Raphael.spinner(imageLoading, 50, "#666"));

        if (thiz.request) {
            thiz.request.abort();
        }

        thiz.request = AJS.$.ajax({
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

            // make sure everyting has loaded completely
            $iframe.on('load', function() {
                win.AJS.$('#main').addClass('chart-preview-main');
                imageContainer.show();
                thiz.handleInsertButton();
            });

            //prevent call AJS.MacroBrowser.previewOnload when onload.
            //business of this function is not any effect to my function
            data = data.replace("window.onload", "var chartTest");

            // write data into iframe
            doc.open();
            doc.write(data);
            doc.close();
        })
        .error(function(status) {
            if(status.statusText != 'abort') {
                AJS.log("Jira Chart Macro - Fail to get data from macro preview");
                imageContainer.html(Confluence.Templates.ConfluenceJiraPlugin.jiraChartErrorMessage({message: AJS.I18n.getText('jirachart.error.execution')}));
            }
            AJS.Editor.JiraSprint.disableInsert();
        });
    },

    resetDialogValue: function() {
        var $inputElements = AJS.$('input', this.container);
        $inputElements.filter(':text').val('');
        $inputElements.filter(':checked').removeAttr('checked');
        this.container.find('#jira-sprint-search-input').val();
        this.container.find(".jira-sprint-img").empty();
        this.resetDisplayOption();
    },

    resetDisplayOption: function() {
        var thiz = this;
        var displayOption = this.chartElements.displayOption;
        displayOption.addClass('jirasprint-display-opts-open');
        displayOption.removeClass('jirasprint-display-opts-close');
        setTimeout(function() {
            var jiraChartOption = thiz.container.find('.jira-sprint-option');
            jiraChartOption.scrollTop(0);
            jiraChartOption.css({
                overflow: 'hidden',
                top: '430px'
            });
        }, 0);
    },

    bindSelectOption: function() {
        var thiz = this;
        var jiraChartOption = thiz.container.find('.jira-chart-option');
        var displayOptPanel = function(open) {
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

        jiraChartOption.css("top", "430px");
        var displayOptsBtn = this.chartElements.displayOption;
        displayOptsBtn.click(function(e) {
            var thiz = AJS.$(this);
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

    focusForm: function() {
        this.container.find("#jira-sprint-search-input").focus();
    },

    handleInsertButton : function() {
        if (this.isFormValid() && this.isResultValid()) {
            AJS.Editor.JiraSprint.enableInsert();
        } else {
            AJS.Editor.JiraSprint.disableInsert();
        }
    }

};

