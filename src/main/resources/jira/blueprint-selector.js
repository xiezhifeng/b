(function() {
    if (!Confluence.Blueprint.Selector)
        return;

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

        // Creating a page for a sprint that has started - time for a retrospective!
        if (params.sprintId && params.agileMode === "work")
            return "com.atlassian.confluence.plugins.confluence-software-blueprints:retrospectives-item";
    });
})();