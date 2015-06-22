package it.webdriver.com.atlassian.confluence.jiraissues.searchedpanel;

import com.atlassian.confluence.pageobjects.page.content.EditContentPage;
import it.webdriver.com.atlassian.confluence.pageobjects.DisplayOptionPanel;
import jdk.nashorn.internal.ir.annotations.Ignore;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

@Ignore
public class JiraMacroPlaceholderWebDriverTest extends AbstractJiraIssuesSearchPanelWebDriverTest
{
    @Test
    public void testPlaceHolderWhenMacroContainsOneIssue()
    {
        EditContentPage editContentPage = search("TST-1").clickInsertDialog();
        waitUntilInlineMacroAppearsInEditor(editContentPage, JIRA_ISSUE_MACRO_NAME);
        String htmlContent = editContentPage.getEditor().getContent().getTimedHtml().now();
        assertTrue(htmlContent.contains("/plugins/servlet/confluence/placeholder/macro"));
    }

    @Test
    public void testPlaceHolderWhenMacroContainsMultiIssues()
    {
        EditContentPage editContentPage = search("TSTT-1, TST-1").clickInsertDialog();
        waitUntilInlineMacroAppearsInEditor(editContentPage, JIRA_ISSUE_MACRO_NAME);
        String htmlContent = editContentPage.getEditor().getContent().getTimedHtml().now();
        assertTrue(htmlContent.contains("/confluence/download/resources/confluence.extra.jira/jira-table.png"));
    }

    @Test
    public void testPlaceHolderWhenMacroContainsJQL()
    {
        EditContentPage editContentPage = search("project = 'Alphanumeric Key Test'").clickInsertDialog();
        waitUntilInlineMacroAppearsInEditor(editContentPage, JIRA_ISSUE_MACRO_NAME);
        String htmlContent = editContentPage.getEditor().getContent().getTimedHtml().now();
        assertTrue(htmlContent.contains("/confluence/download/resources/confluence.extra.jira/jira-table.png"));
    }

    @Test
    public void testPlaceHolderCountWhenMacroContainsMultiIssues()
    {
        search("project = 'Alphanumeric Key Test'");
        DisplayOptionPanel displayOptionPanel = jiraIssuesDialog.getDisplayOptionPanel();
        displayOptionPanel.clickDisplayTotalCount();
        jiraIssuesDialog.clickInsertDialog();
        waitUntilInlineMacroAppearsInEditor(editContentPage, JIRA_ISSUE_MACRO_NAME);
        String htmlContent = editContentPage.getEditor().getContent().getTimedHtml().now();
        assertTrue(htmlContent.contains("/confluence/plugins/servlet/image-generator?totalIssues"));
    }
}
