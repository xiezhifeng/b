AJS.toInit(function(){
	
	// default image /download/resources/confluence.extra.jira/images/jira-chart-macro-icon.png
	// register chart image load event
	AJS.$("#jira-chart-macro-img")
	.load(function(event){
		console.log('Jira chart image loaded');
		//Enale the insert button
		AJS.$('.insert-jira-chart-macro-button', window.parent.document).enable();
	}).error(function(event){
		console.log('Cannot load jira chart image');
		console.log(event);
		
		console.log('Disable insert button');
		AJS.$('.insert-jira-chart-macro-button', window.parent.document).disable();
		var image = AJS.$(event.target);
		var imageWrapper = image.parent();
		var imageContainer = imageWrapper.parent();
		
		console.log('Removing bad jira chart image')
		// remove image and show error message
		imageWrapper.remove();
		
		var erroMsg = AJS.I18n.getText("jirachart.error.execution");
		AJS.messages.error(imageContainer, {
			   body: erroMsg
			});
	});
});


