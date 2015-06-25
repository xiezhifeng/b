package it.com.atlassian.confluence.plugins.webdriver.jiraissues.searchpanel;

import com.atlassian.confluence.plugins.pageobjects.DisplayOptionPanel;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class JiraMacroPlaceholderTest extends AbstractJiraIssuesSearchPanelTest
{
    @Test
    public void testPlaceHolderWhenMacroContainsOneIssue() throws Exception
    {
        openJiraIssueSearchPanelAndStartSearch("TST-1").clickInsertDialog();
        waitUntilInlineMacroAppearsInEditor(editPage, JIRA_ISSUE_MACRO_NAME);
        String htmlContent = editPage.getEditor().getContent().getTimedHtml().now();
        assertTrue(htmlContent.contains("/plugins/servlet/confluence/placeholder/macro"));
    }

    @Test
    public void testPlaceHolderWhenMacroContainsMultiIssues() throws Exception
    {
        openJiraIssueSearchPanelAndStartSearch("TSTT-1, TST-1").clickInsertDialog();
        waitUntilInlineMacroAppearsInEditor(editPage, JIRA_ISSUE_MACRO_NAME);
        String htmlContent = editPage.getEditor().getContent().getTimedHtml().now();
        assertTrue(htmlContent.contains("/confluence/download/resources/confluence.extra.jira/jira-table.png"));
    }

    @Test
    public void testPlaceHolderWhenMacroContainsJQL() throws Exception
    {
        openJiraIssueSearchPanelAndStartSearch("project = 'Alphanumeric Key Test'").clickInsertDialog();
        waitUntilInlineMacroAppearsInEditor(editPage, JIRA_ISSUE_MACRO_NAME);
        String htmlContent = editPage.getEditor().getContent().getTimedHtml().now();
        assertTrue(htmlContent.contains("/confluence/download/resources/confluence.extra.jira/jira-table.png"));
    }

    @Test
    public void testPlaceHolderCountWhenMacroContainsMultiIssues() throws Exception
    {
        openJiraIssueSearchPanelAndStartSearch("project = 'Alphanumeric Key Test'");
        jiraMacroSearchPanelDialog.openDisplayOption();
        DisplayOptionPanel displayOptionPanel = jiraMacroSearchPanelDialog.getDisplayOptionPanel();
        displayOptionPanel.clickDisplayTotalCount();
        jiraMacroSearchPanelDialog.clickInsertDialog();
        waitUntilInlineMacroAppearsInEditor(editPage, JIRA_ISSUE_MACRO_NAME);
        String htmlContent = editPage.getEditor().getContent().getTimedHtml().now();
        assertTrue(htmlContent.contains("/confluence/plugins/servlet/image-generator?totalIssues"));
    }
}
