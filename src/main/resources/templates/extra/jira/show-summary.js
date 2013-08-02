
var SUMMARY_PARAM = 'showSummary';
var MACRO_NAME = 'jira';
var SUMMARY_BUTTON = 'show-summary';

AJS.toInit(function() {
    AJS.Confluence.PropertyPanel.Macro.registerInitHandler(function(macroNode, buttons, options) {
        var summaryButton = findButton(buttons, SUMMARY_BUTTON);
        /**
         * SINGLE ISSUE
         *  separate the 'view-in-jira &show-summary' button with 'Edit & Remove'
         *  handle text in 'show-summary'
         * TABLE ISSUE:
         *  hide 'show-summary' button.
         */
        if (isSingleIssueMacro(macroNode)) {
            findButton(buttons, 'Edit').className  +=' last';
            findButton(buttons, 'view-in-jira').className  +=' first';
            findButton(buttons, SUMMARY_BUTTON).className  +=' last';
            findButton(buttons, 'Remove').className  +=' first';
            
            var currentShowSummaryParam = getParam(macroNode, SUMMARY_PARAM);
            if (currentShowSummaryParam == 'false') {
                summaryButton.text = AJS.I18n.getText("confluence.extra.jira.button.summary.show");
            } else {
                summaryButton.text = AJS.I18n.getText("confluence.extra.jira.button.summary.hide");
            }
            
        } else {
            summaryButton.className += ' hidden';
        }
        
    }, MACRO_NAME);
    
    /**
     * try to detect Jira placeHolder is SINGLE or TABLE
     */
    function isSingleIssueMacro(macroNode) {
        var macroDiv = AJS.$(macroNode);
        var src = macroDiv.attr("src");
        if(src==undefined) {
            return true;
        }
        var countParam = getParam(macroNode, 'count');
        
        if(src.indexOf("confluence.extra.jira/jira-table")==-1 && countParam!='true') {
            return true;
        }
        return false;
    }
    function findButton(buttons, buttonName) {
        var button = AJS.$.grep(buttons, function(e) {
            return e.parameterName == buttonName || e.text==buttonName;
        })[0];
        return button;
    }

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
var switchBoolean = function(currentState) {
    if (currentState == 'false') {
        return 'true';
    } else {
        return 'false';
    }
}
