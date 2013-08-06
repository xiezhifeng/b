
AJS.toInit(function() {
    var SUMMARY_PARAM = 'showSummary';
    var MACRO_NAME = 'jira';
    var MACRO_NAME_FULL = 'jiraissues';
    var SUMMARY_BUTTON = 'show-summary';
    
    AJS.Confluence.PropertyPanel.Macro.registerInitHandler(displaySummaryMenuContextHandler, MACRO_NAME);
    AJS.Confluence.PropertyPanel.Macro.registerInitHandler(displaySummaryMenuContextHandler, MACRO_NAME_FULL);
    
    function displaySummaryMenuContextHandler(macroNode, buttons, options) {
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
            
            var currentShowSummaryParam = AJS.SummaryHelper.getParam(macroNode, SUMMARY_PARAM);
            if (currentShowSummaryParam == 'false') {
                summaryButton.text = AJS.I18n.getText("confluence.extra.jira.button.summary.show");
            } else {
                summaryButton.text = AJS.I18n.getText("confluence.extra.jira.button.summary.hide");
            }
            
        } else {
            summaryButton.className += ' hidden';
        }
    }
    
    AJS.Confluence.PropertyPanel.Macro.registerButtonHandler(SUMMARY_BUTTON, function(e, macroNode) {
        var currentShowSummaryParam = AJS.SummaryHelper.getParam(macroNode, SUMMARY_PARAM);
        AJS.SummaryHelper.updateMacro(MACRO_NAME, macroNode, SUMMARY_PARAM, AJS.SummaryHelper.switchBoolean(currentShowSummaryParam));
    });
    
    /**
     * try to detect Jira placeHolder is SINGLE or TABLE
     */
    function isSingleIssueMacro(macroNode) {
        var macroDiv = AJS.$(macroNode);
        var src = macroDiv.attr("src");
        if(src==undefined) {
            return true;
        }
        var countParam = AJS.SummaryHelper.getParam(macroNode, 'count');
        
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

AJS.SummaryHelper = (function() {
    return {
        switchBoolean : function(currentState) {
            if (currentState == 'false') {
                return 'true';
            } else {
                return 'false';
            }
        },
        /**
         * get current parameters and split them into a nice object
         */
        getCurrentParams : function(macroDiv) {
            var macroParameters = Confluence.MacroParameterSerializer.deserialize(macroDiv.attr("data-macro-parameters"));
            return macroParameters;
        },
        getParam : function(macroNode, paramName) {
            var macroDiv = AJS.$(macroNode);
            var currentParams = AJS.SummaryHelper.getCurrentParams(macroDiv);
            return currentParams[paramName];

        },
        /**
         * Update macro parameter/body
         */
        updateMacro : function(macroId, macroNode, macroParam, param) {
            var macroDiv = AJS.$(macroNode);

            // get/set parameters and body of macro
            var currentParams = AJS.SummaryHelper.getCurrentParams(macroDiv);
            currentParams[macroParam] = param;

            // create macro request object
            var macroRenderRequest = {
                contentId : Confluence.Editor.getContentId(),
                macro : {
                    name : macroId,
                    params : currentParams,
                    defaultParameterValue : macroDiv.attr("data-macro-default-parameter")
                }
            };

            // insert new macro content
            tinymce.confluence.MacroUtils.insertMacro(macroRenderRequest);
        }
        
    };
    
})();



