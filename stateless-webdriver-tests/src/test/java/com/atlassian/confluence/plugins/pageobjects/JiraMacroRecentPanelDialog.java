package com.atlassian.confluence.plugins.pageobjects;

import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.Poller;

public class JiraMacroRecentPanelDialog extends JiraIssueMacroDialog
{
    @ElementBy(cssSelector = ".jiraSearchResults")
    protected PageElement issuesTable;

    public JiraMacroRecentPanelDialog()
    {
        super("jira-connector");
    }

    public boolean isResultContainIssueKey(String issueKey)
    {
        Poller.waitUntilTrue(issuesTable.timed().isVisible());
        return issuesTable.getText().contains(issueKey);
    }
}
