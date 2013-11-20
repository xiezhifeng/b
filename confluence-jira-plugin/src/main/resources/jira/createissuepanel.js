AJS.Editor.JiraConnector.Panel.Create = function(){};

AJS.Editor.JiraConnector.Panel.Create.prototype = AJS.$.extend(AJS.Editor.JiraConnector.Panel.Create.prototype, AJS.Editor.JiraConnector.Panel.prototype);
AJS.Editor.JiraConnector.Panel.Create.prototype = AJS.$.extend(AJS.Editor.JiraConnector.Panel.Create.prototype, {
    setSummary: function(summary){
        AJS.$('.issue-summary', this.container).val(summary);
    },
    title: function(){
        return AJS.I18n.getText("insert.jira.issue.create");
    },
    init: function(panel) {
        this.createIssueForm = new jiraIntegration.CreateJiraIssueForm({
            el: panel,
            insertCallback: AJS.$.proxy(AJS.Editor.JiraConnector.Panel.prototype.insertIssueLinkWithParams, this),
            disableInsert: AJS.Editor.JiraConnector.Panel.prototype.disableInsert,
            enableInsert: AJS.Editor.JiraConnector.Panel.prototype.enableInsert
        });
        this.createIssueForm.init();
    },
    insertLink: function() {
        this.createIssueForm && this.createIssueForm.insertLink();
    }
});
AJS.Editor.JiraConnector.Panels.push(new AJS.Editor.JiraConnector.Panel.Create());
