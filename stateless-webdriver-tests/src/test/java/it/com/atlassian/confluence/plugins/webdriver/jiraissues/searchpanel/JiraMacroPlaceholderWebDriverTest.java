package it.com.atlassian.confluence.plugins.webdriver.jiraissues.searchpanel;

import com.atlassian.confluence.plugins.pageobjects.DisplayOptionPanel;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class JiraMacroPlaceholderWebDriverTest extends AbstractJiraIssuesSearchPanelWebDriverTest
{
    @Test
    public void testPlaceHolderWhenMacroContainsOneIssue()
    {
        search("TST-1").clickInsertDialog();
        waitUntilInlineMacroAppearsInEditor(editPage, JIRA_ISSUE_MACRO_NAME);
        String htmlContent = editPage.getEditor().getContent().getTimedHtml().now();
        assertTrue(htmlContent.contains("/plugins/servlet/confluence/placeholder/macro"));
    }

    @Test
    public void testPlaceHolderWhenMacroContainsMultiIssues()
    {
        search("TSTT-1, TST-1").clickInsertDialog();
        waitUntilInlineMacroAppearsInEditor(editPage, JIRA_ISSUE_MACRO_NAME);
        String htmlContent = editPage.getEditor().getContent().getTimedHtml().now();
        assertTrue(htmlContent.contains("/confluence/download/resources/confluence.extra.jira/jira-table.png"));
    }

    @Test
    public void testPlaceHolderWhenMacroContainsJQL()
    {
        search("project = 'Alphanumeric Key Test'").clickInsertDialog();
        waitUntilInlineMacroAppearsInEditor(editPage, JIRA_ISSUE_MACRO_NAME);
        String htmlContent = editPage.getEditor().getContent().getTimedHtml().now();
        assertTrue(htmlContent.contains("/confluence/download/resources/confluence.extra.jira/jira-table.png"));
    }

    @Test
    public void testPlaceHolderCountWhenMacroContainsMultiIssues()
    {
        search("project = 'Alphanumeric Key Test'");
        jiraMacroSearchPanelDialog.openDisplayOption();
        DisplayOptionPanel displayOptionPanel = jiraMacroSearchPanelDialog.getDisplayOptionPanel();
        displayOptionPanel.clickDisplayTotalCount();
        jiraMacroSearchPanelDialog.clickInsertDialog();
        waitUntilInlineMacroAppearsInEditor(editPage, JIRA_ISSUE_MACRO_NAME);
        String htmlContent = editPage.getEditor().getContent().getTimedHtml().now();
        assertTrue(htmlContent.contains("/confluence/plugins/servlet/image-generator?totalIssues"));
    }
}
