AJS.toInit(function() {

	// default image
	// /download/resources/confluence.extra.jira/images/jira-chart-macro-icon.png
	// register chart image load event
	AJS.$("#jira-chart-macro-img").load(
	        function(event) {
		        // Enale the insert button
		        AJS
		                .$('.insert-jira-chart-macro-button',
		                        window.parent.document).enable();
	        }).error(
	        function(event) {
		        AJS
		                .$('.insert-jira-chart-macro-button',
		                        window.parent.document).disable();
		        var image = AJS.$(event.target);
		        var imageContainer = image.parent();

		        // remove image and show error message
		        image.remove();

		        var erroMsg = AJS.I18n.getText("jirachart.error.execution");
		        AJS.messages.error(imageContainer, {
			        body : erroMsg
		        });
	        });
});
