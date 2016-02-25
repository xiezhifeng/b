(function() {
    if (!Confluence.Blueprint.Selector)
        return;

    var blueprints = new Object();
    blueprints['decisions']      = 'com.atlassian.confluence.plugins.confluence-business-blueprints:decisions-blueprint-item';
    blueprints['meeting-notes']  = 'com.atlassian.confluence.plugins.confluence-business-blueprints:meeting-notes-item';
    blueprints['requirements']   = 'com.atlassian.confluence.plugins.confluence-software-blueprints:requirements-item';
    blueprints['retrospectives'] = 'com.atlassian.confluence.plugins.confluence-software-blueprints:retrospectives-item';
    blueprints['blank-pages']    = 'com.atlassian.confluence.plugins.confluence-create-content-plugin:create-blank-page';


    /**
     * The create dialog accepts arbitrary parameters via the init-dialog action which are passed through to
     * the context property of a BlueprintPageCreateEvent. We handle some of these parameters for content creation
     * that is initiated from JIRA (see com.atlassian.confluence.plugins.jira.ConfluenceEventListener).
     *
     * This function registers a handler function that specifies the blueprint to select based on the presence
     * of parameters that indicate that the content creation was initiated from JIRA.
     */
    Confluence.Blueprint.Selector.registerSelector(function(params) {

        // Creating a page for an epic while planning - select product requirements by default
        if (params.issueKey && params.agileMode === "plan")
            return "com.atlassian.confluence.plugins.confluence-software-blueprints:requirements-item";

        // Creating a page for a sprint while planning - select meeting notes by default
        if (params.sprintId && params.agileMode === "plan")
            return "com.atlassian.confluence.plugins.confluence-business-blueprints:meeting-notes-item";

        // Creating a page after a sprint has been completed - time for a retrospective!
        if (params.sprintId && params.agileMode === "report")
            return "com.atlassian.confluence.plugins.confluence-software-blueprints:retrospectives-item";

        // blueprintShortKey will be passed from JIRA side
        params.blueprintShortKey = params.blueprintShortKey || "";
        // be able to select blueprint as requested
        return blueprints[params.blueprintShortKey];
    });
})();