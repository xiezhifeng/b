(function() {
    AJS.bind("init.rte", function() {

        AJS.bind('editor.text-placeholder.activated', function(e, data) {
            if(data && data.placeholderType === 'jira') {
                AJS.Editor.JiraConnector.open(AJS.Editor.JiraConnector.source.instructionalText);
            }
        });

        if(AJS.Rte.Placeholder && AJS.Rte.Placeholder.addPlaceholderType) {

            AJS.Rte.Placeholder.addPlaceholderType({
                type: 'jira',
                label: AJS.I18n.getText("property.panel.textplaceholder.display.jira"),
                tooltip: AJS.I18n.getText("property.panel.textplaceholder.display.jira.tooltip"),
                activation: {
                    click: true,
                    keypress: false
                }
            });
        }
    });
})(AJS.$);