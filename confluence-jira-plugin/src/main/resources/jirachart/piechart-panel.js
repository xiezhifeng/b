AJS.Editor.JiraChart.Panels.JiraChart = function(type) {

    var CHART_TITLE = "";
    if (type === 'piechart')
    {
        CHART_TITLE = AJS.I18n.getText("jirachart.panel.piechart.title");
    } else if (type === 'createdvsresolvedchart')
    {
        CHART_TITLE = AJS.I18n.getText("jirachart.panel.createdvsresolvedchart.title");
    }

    var setupInsertButton = function($iframe) {
        if ($iframe.contents().find("." + type + "jira-chart-macro-img").length > 0) {
            AJS.Editor.JiraChart.enableInsert();
        } else {
            AJS.Editor.JiraChart.disableInsert();
        }
    };

    return {
        title : CHART_TITLE,

        init : function(panel) {
            // get content from soy template
            var contentJiraChart = Confluence.Templates.ConfluenceJiraPlugin.contentJiraChart({
                'isMultiServer' : AJS.Editor.JiraConnector.servers.length > 1,
                'chartType' : type
            });
            panel.html(contentJiraChart);
        },

        renderChart : function(imageContainer, params) {
            var innerImageContainer = imageContainer;
            var previewUrl = Confluence.getContextPath() + "/rest/tinymce/1/macro/preview";
            var dataToSend = {
                "contentId" : AJS.Meta.get("page-id"),
                "macro" : {
                    "name" : "jirachart",
                    "params" : {
                        "jql" : params.jql,
                        "serverId" : params.serverId,
                        "width" : params.width,
                        "border" : params.border,
                        "showinfor" : params.showinfor,
                        "statType" : params.statType
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
        }
    };
};

AJS.Editor.JiraChart.Panels.push(new AJS.Editor.JiraChart.Panels.JiraChart('piechart'));