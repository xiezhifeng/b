/* This is a pretty hacky way to get the JIRA issue dialog keyboard shortcut into the help panel */
if(Confluence.KeyboardShortcuts) {
    Confluence.KeyboardShortcuts.Editor.push(
    {
        context: "editor.actions",
        descKey: AJS.I18n.getText("insert.jira.issue.keyboardshortcut.dialog.label") + ":",
        keys: [
            [AJS.I18n.getText("insert.jira.issue.keyboardshortcut.dialog.keys")]
        ]
    });
}
