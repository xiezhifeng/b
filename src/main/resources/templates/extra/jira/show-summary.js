
var SUMMARY_PARAM = 'showSummary';
var MACRO_NAME='jira';
var SUMMARY_BUTTON='show-summary';

AJS.toInit(function () {
    AJS.Confluence.PropertyPanel.Macro.registerInitHandler(function (macroNode, buttons, options) {
    	var summaryButton = AJS.$.grep(buttons, function(e){ return e.parameterName == SUMMARY_BUTTON; })[0];
    	var currentShowSummaryParam = getParam(macroNode, SUMMARY_PARAM);
    	if(currentShowSummaryParam=='true') {
    		summaryButton.text=AJS.I18n.getText("button.summary.hide");
    	} else {
    		summaryButton.text=AJS.I18n.getText("button.summary.show");
    	}
    }, MACRO_NAME);

 });

AJS.Confluence.PropertyPanel.Macro.registerButtonHandler(SUMMARY_BUTTON, function(e, macroNode) {
	var currentShowSummaryParam = getParam(macroNode, SUMMARY_PARAM);
	updateMacro(MACRO_NAME, macroNode, SUMMARY_PARAM, switchBoolean(currentShowSummaryParam));
});

/**
 * Update macro parameter/body
 */
var updateMacro = function(macroId, macroNode, macroParam, param) {
	var $macroDiv = AJS.$(macroNode);

	AJS.Rte.getEditor().selection.select($macroDiv[0]);
	AJS.Rte.BookmarkManager.storeBookmark();

	// get/set parameters and body of macro
	var currentParams = getCurrentParams($macroDiv);
	currentParams[macroParam] = param;

	// create macro request object
	var macroRenderRequest = {
		contentId : Confluence.Editor.getContentId(),
		macro : {
			name : macroId,
			params : currentParams,
			defaultParameterValue : ""
		}
	};

	// insert new macro content
	tinymce.confluence.MacroUtils.insertMacro(macroRenderRequest);
}


/**
 * get current parameters and split them into a nice object
 */
var getCurrentParams = function(macroDiv) {
	var currentParams = {};
	if (macroDiv.attr("data-macro-parameters")) {
		AJS.$.each(macroDiv.attr("data-macro-parameters").split("|"), function(idx, item) {
			var param = item.split("=");
			currentParams[param[0]] = param[1];
		});
	}
	return currentParams;
}

function getParam(macroNode, paramName) {
	var $macroDiv = AJS.$(macroNode);
	var currentParams = getCurrentParams($macroDiv);
	return currentParams[paramName];
	
}
var switchBoolean =function(currentState) {
	if(currentState=='true') {
		return 'false';
	} else {
		return 'true';
	}
}

