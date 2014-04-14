AJS.Editor.JiraChart.Panels.CreatedVsResolvedChart = function() {
    var CREATED_VS_RESOLVED_CHART_TITLE = AJS.I18n.getText('jirachart.panel.createdvsresolvedchart.title');
    var CREATED_VS_RESOLVED_CHART_ID = "createdvsresolvedchart";

    var setupInsertButton = function($iframe) {
        if ($iframe.contents().find(".jira-chart-img").length > 0) {
            AJS.Editor.JiraChart.enableInsert();
        } else {
            AJS.Editor.JiraChart.disableInsert();
        }
    };

    return {
        title : CREATED_VS_RESOLVED_CHART_TITLE,
        id: CREATED_VS_RESOLVED_CHART_ID,

        init : function(panel, id) {
            // get content from soy template
            var contentJiraChart = Confluence.Templates.ConfluenceJiraPlugin.contentJiraChart({
                'isMultiServer' : AJS.Editor.JiraConnector.servers.length > 1,
                'chartType' : id
            });
            panel.html(contentJiraChart);
        },

        renderChart : function(imageContainer, params) {
            var innerImageContainer = imageContainer;
            var previewUrl = Confluence.getContextPath() + "/rest/tinymce/1/macro/preview";
            var params = this.getMacroParamsFromDialog();
            var dataToSend = {
                "contentId" : AJS.Meta.get("page-id"),
                "macro" : {
                    "name" : "jirachart",
                    "params" : {
                        "jql" : params.jql,
                        "serverId" : params.serverId,
                        "width" : params.width,
                        "periodName": params.periodName,
                        "daysprevious": params.daysprevious,
                        "isCumulative": params.isCumulative,
                        "showUnresolvedTrend": params.showUnresolvedTrend,
                        "border" : params.border,
                        "showinfor" : params.showinfor,
                        "chartType": "createdvsresolved"
                    }
                }
            };

            AJS.$.ajax({
                url : previewUrl,
                type : "POST",
                contentType : "application/json",
                data : JSON.stringify(dataToSend)
            })
                .done(
                function(data) {
                    innerImageContainer.html('').hide(); // this will be re-show right after iframe is loaded
                    var $iframe = AJS.$('<iframe frameborder="0" name="macro-browser-preview-frame" id="chart-preview-iframe"></iframe>');
                    $iframe.appendTo(innerImageContainer);

                    // window and document belong to iframe
                    var win = $iframe[0].contentWindow,
                        doc = win.document;

                    // write data into iframe
                    doc.open();
                    doc.write(data);
                    doc.close();

                    // make sure everyting has loaded completely
                    $iframe.on('load', function() {
                        win.AJS.$('#main').addClass('chart-preview-main');
                        innerImageContainer.show();
                        setupInsertButton(AJS.$(this));
                    });
                })
                .error(
                function(jqXHR, textStatus, errorThrown) {
                    AJS.log("Jira Chart Macro - Fail to get data from macro preview");
                    imageContainer.html(Confluence.Templates.ConfluenceJiraPlugin.showMessageRenderJiraChart());
                    AJS.Editor.JiraChart.disableInsert();
                });
        },

        validate: function(element){
            // remove error message if have
            AJS.$(element).next('#jira-chart-macro-dialog-validation-error').remove();

            var $element = AJS.$(element);
            var width = AJS.Editor.JiraChart.convertFormatWidth($element.val());
            // do the validation logic

            if (!AJS.Editor.JiraChart.validateWidth(width) && width !== "") {

                var inforErrorWidth = "wrongFormat";

                if (AJS.Editor.JiraChart.isNumber(width)) {
                    inforErrorWidth = "wrongNumber";
                }

                $element.after(Confluence.Templates.ConfluenceJiraPlugin.warningValWidthColumn({'error': inforErrorWidth}));
                return false;
            }
            return true;
        },

        getMacroParamsFromDialog: function() {
            var container = $("#jira-chart-content-createdvsresolvedchart");
            var selectedServer = AJS.Editor.JiraChart.getSelectedServer(container);
            return {
                jql: encodeURIComponent(container.find('#jira-chart-inputsearch').val()),
                periodName: container.find('#periodName').val(),
                width: AJS.Editor.JiraChart.convertFormatWidth(container.find('#jira-chart-width').val()),
                daysprevious: container.find('#daysprevious').val(),
                isCumulative: container.find('#yes-cumulative').prop('checked') ? 30 : false,
                showUnresolvedTrend: container.find('#yes-showunresolvedtrend').prop('checked'),
                versionLabel: container.find('#versionLabel').val(),
                border: container.find('#jira-chart-border').prop('checked'),
                showinfor: container.find('#jira-chart-show-infor').prop('checked'),
                serverId:  selectedServer.id,
                server: selectedServer.name,
                isAuthenticated: !selectedServer.authUrl
            };

        },

        chartImageIsExist: function() {
            return $("#jira-chart-content-createdvsresolvedchart").find("#chart-preview-iframe").contents().find(".jira-chart-macro-img").length > 0 ? true : false;
        }

    };

};

AJS.Editor.JiraChart.Panels.push(new AJS.Editor.JiraChart.Panels.CreatedVsResolvedChart());