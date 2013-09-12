AJS.Editor.JiraChart.Panels.PieChart = function() {
	return {
	    title : function() {
		    return AJS.I18n.getText('jirachart.panel.piechart.title');
	    },
	    init : function(panel) {
		    // add body content
		    var thiz = this;
		    var servers = AJS.Editor.JiraConnector.servers;
		    var isMultiServer = (servers.length > 1);
		    // get content from soy template
		    var contentJiraChart = Confluence.Templates.ConfluenceJiraPlugin
		            .contentJiraChart({
			            'isMultiServer' : isMultiServer
		            });
		    panel.html(contentJiraChart);

		    if (isMultiServer) {
			    AJS.Editor.JiraConnector.Panel.prototype
			            .applinkServerSelect(AJS.$('#jira-chart-servers'),
			                    function(server) {
				                    thiz.checkOau(AJS.$('#jira-chart-content'),
				                            server);
			                    });
		    }
	    },
	    renderChart : function(imageContainer, params) {
		    var innerImageContainer = imageContainer;
		    var previewUrl = Confluence.getContextPath()
		            + "/rest/tinymce/1/macro/preview";
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
			                    innerImageContainer.html('');
			                    var $iframe = $('<iframe frameborder="0" name="macro-browser-preview-frame" id="chart-preview-iframe"><html/></iframe>');
			                    $iframe.appendTo(innerImageContainer);
			                    var doc = $iframe[0].contentWindow.document;
			                    doc.open();
			                    doc.write(data);
			                    doc.close();
			                    AJS.$('.insert-jira-chart-macro-button',
			                            window.parent.document).enable();
		                    })
		            .error(
		                    function(jqXHR, textStatus, errorThrown) {
			                    console
			                            .log("Fail to get data from macro preview");
			                    imageContainer
			                            .html(Confluence.Templates.ConfluenceJiraPlugin
			                                    .showMessageRenderJiraChart());
		                    });
		    return;
	    },

	    checkOau : function(container, server) {
		    AJS.$('.jira-oauth-message-marker', container).remove();
		    var oauObject = {
		        selectedServer : server,
		        msg : AJS.Editor.JiraConnector.Panel.prototype.msg
		    };

		    if (server && server.authUrl) {
			    var oauForm = AJS.Editor.JiraConnector.Panel.prototype.createOauthForm
			            .call(oauObject, function() {
				            AJS.$('.jira-oauth-message-marker', container)
				                    .remove();
				            AJS.Editor.JiraChart.search(container);
			            });
			    container.find('div.jira-chart-search').append(oauForm);
		    }
	    }
	};
};

AJS.Editor.JiraChart.Panels.push(new AJS.Editor.JiraChart.Panels.PieChart());