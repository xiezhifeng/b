package it.com.atlassian.confluence.plugins.webdriver.jiraissues.searchpanel;

import it.com.atlassian.confluence.plugins.webdriver.pageobjects.DisplayOptionPanel;

import com.atlassian.pageobjects.elements.query.Poller;

import org.junit.Test;

public class JiraMacroPlaceholderTest extends AbstractJiraIssuesSearchPanelWithoutSavingTest
{
    @Test
    public void testPlaceHolderWhenMacroContainsOneIssue() throws Exception
    {
        dialogSearchPanel = openJiraIssueSearchPanelAndStartSearch("TST-1");
        dialogSearchPanel.clickInsertDialog();
        dialogSearchPanel.waitUntilHidden();

        editPage.getEditor().getContent().waitForInlineMacro(JIRA_ISSUE_MACRO_NAME);
        Poller.waitUntilTrue(editPage.getEditor().getContent().htmlContains("/plugins/servlet/confluence/placeholder/macro"));
    }

    @Test
    public void testPlaceHolderWhenMacroContainsMultiIssues() throws Exception
    {
        dialogSearchPanel = openJiraIssueSearchPanelAndStartSearch("TSTT-1, TST-1");
        dialogSearchPanel.clickInsertDialog();
        dialogSearchPanel.waitUntilHidden();

        editPage.getEditor().getContent().waitForInlineMacro(JIRA_ISSUE_MACRO_NAME);
        Poller.waitUntilTrue(editPage.getEditor().getContent().htmlContains("/confluence/download/resources/confluence.extra.jira/jira-table.png"));
    }

    @Test
    public void testPlaceHolderWhenMacroContainsJQL() throws Exception
    {
        dialogSearchPanel = openJiraIssueSearchPanelAndStartSearch("project = 'Alphanumeric Key Test'");
        dialogSearchPanel.clickInsertDialog();
        dialogSearchPanel.waitUntilHidden();

        editPage.getEditor().getContent().waitForInlineMacro(JIRA_ISSUE_MACRO_NAME);
        Poller.waitUntilTrue(editPage.getEditor().getContent().htmlContains("/confluence/download/resources/confluence.extra.jira/jira-table.png"));
    }

    @Test
    public void testPlaceHolderCountWhenMacroContainsMultiIssues() throws Exception
    {
        openJiraIssueSearchPanelAndStartSearch("project = 'Alphanumeric Key Test'");
        dialogSearchPanel.openDisplayOption();
        DisplayOptionPanel displayOptionPanel = dialogSearchPanel.getDisplayOptionPanel();
        displayOptionPanel.clickDisplayTotalCount();
        dialogSearchPanel.clickInsertDialog();
        dialogSearchPanel.waitUntilHidden();
        editPage.getEditor().getContent().waitForInlineMacro(JIRA_ISSUE_MACRO_NAME);

        Poller.waitUntilTrue(editPage.getEditor().getContent().htmlContains("/confluence/plugins/servlet/image-generator?totalIssues"));
    }
}
