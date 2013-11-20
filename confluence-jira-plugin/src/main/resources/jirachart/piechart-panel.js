AJS.Editor.JiraChart.Panels.PieChart = function() {
    
    var checkWidthField = function(val){
        return AJS.Editor.JiraChart.validateWidth(val);
    };
    
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
		    var contentJiraChart = Confluence.Templates.ConfluenceJiraPlugin.contentJiraChart({
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
	                    innerImageContainer.html('').hide(); // this will be re-show right after iframe is loaded
                        var $iframe = $('<iframe frameborder="0" name="macro-browser-preview-frame" id="chart-preview-iframe"></iframe>');
                        $iframe.appendTo(innerImageContainer);

                        // window and document belong to iframe
                        var win = $iframe[0].contentWindow,
                            doc = win.document;

                        // write data into iframe
                        doc.open();
                        doc.write(data);
                        doc.close();

                        // make sure everyting has loaded completely
                        $iframe[0].onload = function(iframe) {
                            win.AJS.$('#main').addClass('chart-preview-main');
                            innerImageContainer.show();
                        }

                        AJS.$('.insert-jira-chart-macro-button', window.parent.document).enable();
                    })
            .error(
                    function(jqXHR, textStatus, errorThrown) {
	                    AJS.log("Jira Chart Macro - Fail to get data from macro preview");
	                    imageContainer.html(Confluence.Templates.ConfluenceJiraPlugin
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
	    },
	    
	    validate: function(element){
	     // remove error message if have 
            AJS.$(element).next('#jira-chart-macro-dialog-validation-error').remove();
            
	        var $element = AJS.$(element);
	        var width = AJS.Editor.JiraChart.convertFormatWidth($element.val());
	        // do the validation logic
	        
	        if(!checkWidthField(width) && width !== "") {
                var inforErrorWidth;
                if(AJS.Editor.JiraChart.isNumber(width)) {
                    inforErrorWidth = "wrongNumber";
                }else {
                    inforErrorWidth = "wrongFormat";
                }
                
                $element.after(Confluence.Templates.ConfluenceJiraPlugin.warningValWidthColumn({'error': inforErrorWidth}));
                return false;
             }
	        return true;
	    } 
	};
};

AJS.Editor.JiraChart.Panels.push(new AJS.Editor.JiraChart.Panels.PieChart());