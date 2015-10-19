AJS.Editor.JiraChart.Panel = function() {
    this.COLLAPSED_OPTION_TOP_POSITION = 300;
};

AJS.Editor.JiraChart.Panel.prototype = {

    /**
     * Init chart in the first time show dialog
     */
    init: function(panel) {
        var chartType = this.chartType;
        var contentJiraChart = Confluence.Templates.ConfluenceJiraPlugin.contentJiraChart({
            'isMultiServer' : AJS.Editor.JiraConnector.servers.length > 1,
            'chartType' : chartType
        });
        panel.html(contentJiraChart);
        this.container = AJS.$(this.containerId);
        this.clearChartContent(this.container);
        this.loadServers();
        this.bindingChartElements();
        this.bindingActions();

        if (this.preBinding) {
            this.preBinding();
        }
    },

    doSearch: function() {
        if (!AJS.Editor.JiraChart.Helper.convertSearchTextToJQL(this.container)) {
            return;
        }

        this.renderChart();
    },

    loadServers: function() {
        var _this = this;

        if (AJS.Editor.JiraConnector.servers.length > 0) {
            AJS.Editor.JiraConnector.Panel.prototype.applinkServerSelect(this.container.find('#jira-chart-servers'),
                function(server) {
                    _this.clearChartContent();
                    _this.validateSelectedServer(server);
                }
            );
        }
    },

    clearChartContent: function() {
        this.container.find('.jira-oauth-message-marker').remove();
        this.container.find('.jira-chart-img').empty();
        this.container.find('#jira-chart-search-input').empty();
    },

    /**
     * Binding event to all element
     */
    bindingActions: function() {
        var thiz = this;

        var eventHandler = function() {
            if (thiz.isFormValid()) {
                thiz.doSearch();
            } else {
                AJS.Editor.JiraChart.disableInsert();
            }
        };

        var clickableElements = thiz.container.find(thiz.clickableElements);
        clickableElements.click(eventHandler);

        var onChangeElements = thiz.container.find(thiz.onChangeElements);
        onChangeElements.change(eventHandler);

        //for auto convert when paste url
        thiz.chartElements.jql.change(function() {
            if (this.value !== thiz.jqlWhenEnterKeyPress) {
                thiz.container.find(".jira-chart-img").empty();
                AJS.Editor.JiraChart.disableInsert();
            }
            thiz.jqlWhenEnterKeyPress = "";
        }).bind("paste", function() {
            setTimeout(function () {
                if (thiz.isFormValid()) {
                    thiz.jqlWhenEnterKeyPress = thiz.chartElements.jql.val();
                    thiz.doSearch();
                }
            }, 100);
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
                    if (thiz.isFormValid()) {
                        thiz.doSearch();
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
            if (thiz.isFormValid() && AJS.Editor.JiraChart.validateServerSupportedChart(thiz.container)) {
                thiz.doSearch();
            } else {
                AJS.Editor.JiraChart.disableInsert();
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
            AJS.Editor.JiraChart.disableInsert();
        });
    },

    resetDialogValue: function() {
        var $inputElements = AJS.$('input', this.container);
        $inputElements.filter(':text').val('');
        $inputElements.filter(':checked').removeAttr('checked');
        this.container.find('#jira-chart-search-input').val();
        this.container.find(".jira-chart-img").empty();
        this.resetDisplayOption();
    },

    resetDisplayOption: function() {
        var thiz = this;
        var displayOption = this.chartElements.displayOption;
        displayOption.addClass('jirachart-display-opts-open');
        displayOption.removeClass('jirachart-display-opts-close');
        setTimeout(function() {
            var jiraChartOption = thiz.container.find('.jira-chart-option');
            jiraChartOption.scrollTop(0);
            jiraChartOption.css({
                overflow: 'hidden',
                top: thiz.COLLAPSED_OPTION_TOP_POSITION + 'px'
            });
            displayOption.attr('aria-disabled', true);
        }, 0);
    },

    bindSelectOption: function() {
        var thiz = this;
        var jiraChartOption = thiz.container.find('.jira-chart-option');
        var displayOptPanel = function(open) {
            var topMargin = 40;
            var top = jiraChartOption.position().top + "px";
            var bottom =  "";
            var animateConfig = {top: thiz.COLLAPSED_OPTION_TOP_POSITION};

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
            jiraChartOption.animate(animateConfig, 500, function() {
                jiraChartOption.attr('aria-disabled', !open);
            });
        };

        jiraChartOption.css('top', this.COLLAPSED_OPTION_TOP_POSITION + 'px');
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

    isImageChartExisted: function() {
        return this.container.find("#chart-preview-iframe").contents().find(".jira-chart-macro-img").length > 0;
    },

    focusForm: function() {
        this.container.find("#jira-chart-search-input").focus();
    },

    handleInsertButton : function() {
        if (this.isFormValid() && this.isResultValid()) {
            AJS.Editor.JiraChart.enableInsert();
        } else {
            AJS.Editor.JiraChart.disableInsert();
        }
    },

    insertLink: function() {
        var currentChart = this;

        var isChartExists = this.container.find('#chart-preview-iframe').length > 0;

        if (isChartExists) {
            var macroInputParams = currentChart.getMacroParamsFromDialog();

            if (macroInputParams && AJS.Editor.inRichTextMode()) {
                tinymce.confluence.macrobrowser.macroBrowserComplete({
                    name: 'jirachart',
                    params: macroInputParams
                });
            }

            AJS.trigger('jira.links.macro.dialog.close');
        } else {
            this.doSearch();
        }
    },

    validateSelectedServer: function(server) {
        var selectedServer;

        if (server) {
            selectedServer = server;
        } else {
            selectedServer = AJS.Editor.JiraChart.Helper.getSelectedServer(this.container);
        }

        AJS.Editor.JiraChart.checkOau(this.container, selectedServer);
        AJS.Editor.JiraChart.validateServerSupportedChart(this.container);
    }
};

