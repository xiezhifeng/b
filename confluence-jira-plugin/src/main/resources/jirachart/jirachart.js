AJS.toInit(function() {

	// default image
	// /download/resources/confluence.extra.jira/images/jira-chart-macro-icon.png
	// register chart image load event
	AJS.$(".jira-chart-macro-img").load(
	        function(event) {
		        // Enale the insert button
	        	AJS.log('Jira Chart Macro - chart image loaded');
		        AJS.$('.insert-jira-chart-macro-button',
		                        window.parent.document).enable();
	        }).error(
	        function(event) {
	        	AJS.log('Jira Chart Macro - chart image loaded error');
		        AJS.$('.insert-jira-chart-macro-button',
		                        window.parent.document).disable();
		        var image = AJS.$(event.target);
				var imageWrapper = image.parent();
				var imageContainer = imageWrapper.parent();
		        // remove image and show error message
                imageWrapper.remove();
		        var erroMsg = AJS.I18n.getText("jirachart.error.execution");
		        AJS.messages.error(imageContainer, {
			        body : erroMsg
		        });
	        });

    if (AJS.MacroBrowser)
    {
        if (AJS.MacroBrowser.previewOnload)
        {
            var originalPreviewOnload = AJS.MacroBrowser.previewOnload;
            AJS.MacroBrowser.previewOnload = function(body){
                var macroBrower = AJS.MacroBrowser;
                if (macroBrower.dialog && macroBrower.dialog.activeMetadata && macroBrower.dialog.activeMetadata.macroName){
                    originalPreviewOnload(body);
                }
            };
        }
    }
});


