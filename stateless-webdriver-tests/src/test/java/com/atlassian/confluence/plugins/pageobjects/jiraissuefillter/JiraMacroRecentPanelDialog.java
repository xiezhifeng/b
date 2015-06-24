package com.atlassian.confluence.plugins.pageobjects.jiraissuefillter;

import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.Poller;

public class JiraMacroRecentPanelDialog extends JiraIssueFilterDialog
{
    @ElementBy(cssSelector = ".jiraSearchResults")
    protected PageElement issuesTable;

    public boolean isResultContainIssueKey(String issueKey)
    {
        Poller.waitUntilTrue(issuesTable.timed().isVisible());
        return issuesTable.getText().contains(issueKey);
    }
}
