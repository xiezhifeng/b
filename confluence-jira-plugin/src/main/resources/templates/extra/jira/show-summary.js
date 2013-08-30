
AJS.toInit(function() {
    var SUMMARY_PARAM = 'showSummary';
    var MACRO_NAME = 'jira';
    var MACRO_NAME_FULL = 'jiraissues';
    var SUMMARY_BUTTON = 'show-summary';
    
    AJS.Confluence.PropertyPanel.Macro.registerInitHandler(displaySummaryMenuContextHandler, MACRO_NAME);
    AJS.Confluence.PropertyPanel.Macro.registerInitHandler(displaySummaryMenuContextHandler, MACRO_NAME_FULL);
    
    function displaySummaryMenuContextHandler(macroNode, buttons, options) {
        var summaryButton = AJS.$.grep(buttons, function(e) {
            return e.parameterName ==SUMMARY_BUTTON;
        })[0];
        //there is a issue the api (registerInitHandler) which return only two buttons (Edit & Remove), -> cannot find SummaryButton
        if (!summaryButton) {
            AJS.logError("JIRA Issues Macro - Show-summary : The system cannot find SummaryButton, all buttons are: ");
            for ( var i in buttons) {
                AJS.logError(buttons[i].text);
            }
            return;
        }

        if (isSingleIssueMacro(macroNode)) {
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
    

    
    /**
     * try to detect Jira placeHolder is SINGLE or TABLE
     */
    function isSingleIssueMacro(macroNode) {
        var macroDiv = AJS.$(macroNode);
        var src = macroDiv.attr("src");
        if(!src) {
            return true;
        }
        var countParam = AJS.SummaryHelper.getParam(macroNode, 'count');
        
        if(src.indexOf("confluence.extra.jira/jira-table")==-1 && countParam!='true') {
            return true;
        }
        return false;
    }
    
});

AJS.bind("add-handler.property-panel", function(event, panel) {
    panel.registerButtonHandler('show-summary', function(event, macroNode) {
        var currentShowSummaryParam = AJS.SummaryHelper.getParam(macroNode, 'showSummary');
        AJS.SummaryHelper.updateMacro('jira', macroNode, 'showSummary', currentShowSummaryParam == 'false' ? 'true' : 'false');
    });
});


AJS.SummaryHelper = (function() {
    return {
        /**
         * get current parameters and split them into a nice object
         */
        getCurrentParams : function(macroDiv) {
            return Confluence.MacroParameterSerializer.deserialize(macroDiv.attr("data-macro-parameters"));
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
            
            AJS.Rte.getEditor().selection.select(macroDiv[0]);
            AJS.Rte.BookmarkManager.storeBookmark();

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



