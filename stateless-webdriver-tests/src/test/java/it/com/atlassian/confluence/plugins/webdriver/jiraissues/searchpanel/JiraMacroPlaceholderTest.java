package it.com.atlassian.confluence.plugins.webdriver.jiraissues.searchpanel;

import com.atlassian.confluence.plugins.pageobjects.DisplayOptionPanel;
import com.atlassian.pageobjects.elements.query.Poller;

import org.hamcrest.Matchers;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class JiraMacroPlaceholderTest extends AbstractJiraIssuesSearchPanelTest
{
    @Test
    public void testPlaceHolderWhenMacroContainsOneIssue() throws Exception
    {
        openJiraIssueSearchPanelAndStartSearch("TST-1").clickInsertDialog();
        editPage.getEditor().getContent().waitForInlineMacro(JIRA_ISSUE_MACRO_NAME);
        Poller.waitUntilTrue(editPage.getEditor().getContent().htmlContains("/plugins/servlet/confluence/placeholder/macro"));
    }

    @Test
    public void testPlaceHolderWhenMacroContainsMultiIssues() throws Exception
    {
        openJiraIssueSearchPanelAndStartSearch("TSTT-1, TST-1").clickInsertDialog();
        editPage.getEditor().getContent().waitForInlineMacro(JIRA_ISSUE_MACRO_NAME);
        Poller.waitUntilTrue(editPage.getEditor().getContent().htmlContains("/confluence/download/resources/confluence.extra.jira/jira-table.png"));
    }

    @Test
    public void testPlaceHolderWhenMacroContainsJQL() throws Exception
    {
        openJiraIssueSearchPanelAndStartSearch("project = 'Alphanumeric Key Test'").clickInsertDialog();
        editPage.getEditor().getContent().waitForInlineMacro(JIRA_ISSUE_MACRO_NAME);
        Poller.waitUntilTrue(editPage.getEditor().getContent().htmlContains("/confluence/download/resources/confluence.extra.jira/jira-table.png"));
    }

    @Test
    public void testPlaceHolderCountWhenMacroContainsMultiIssues() throws Exception
    {
        openJiraIssueSearchPanelAndStartSearch("project = 'Alphanumeric Key Test'");
        jiraMacroSearchPanelDialog.openDisplayOption();
        DisplayOptionPanel displayOptionPanel = jiraMacroSearchPanelDialog.getDisplayOptionPanel();
        displayOptionPanel.clickDisplayTotalCount();
        jiraMacroSearchPanelDialog.clickInsertDialog();
        editPage.getEditor().getContent().waitForInlineMacro(JIRA_ISSUE_MACRO_NAME);

        Poller.waitUntilTrue(editPage.getEditor().getContent().htmlContains("/confluence/plugins/servlet/image-generator?totalIssues"));
    }
}
