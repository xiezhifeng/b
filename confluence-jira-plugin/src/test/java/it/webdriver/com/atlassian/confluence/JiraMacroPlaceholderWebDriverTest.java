package it.webdriver.com.atlassian.confluence;

import com.atlassian.confluence.pageobjects.component.dialog.MacroBrowserDialog;
import com.atlassian.confluence.pageobjects.page.content.EditContentPage;
import it.webdriver.com.atlassian.confluence.pageobjects.DisplayOptionPanel;
import it.webdriver.com.atlassian.confluence.pageobjects.JiraIssuesDialog;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class JiraMacroPlaceholderWebDriverTest extends AbstractJiraWebDriverTest
{

    private JiraIssuesDialog jiraIssuesDialog = null;

    private JiraIssuesDialog openJiraIssuesDialog()
    {
        MacroBrowserDialog macroBrowserDialog = openMacroBrowser();
        macroBrowserDialog.searchForFirst("embed jira issues").select();
        jiraIssuesDialog =  product.getPageBinder().bind(JiraIssuesDialog.class);
        return jiraIssuesDialog;
    }

    @Test
    public void testPlaceHolderWhenMacroContainsOneIssue()
    {
        openJiraIssuesDialog();
        EditContentPage editContentPage = search("TST-1").clickInsertDialog();

        waitUntilInlineMacroAppearsInEditor(editContentPage, "jira");
        String htmlContent = editContentPage.getEditor().getContent().getTimedHtml().now();
        assertTrue(htmlContent.contains("/plugins/servlet/confluence/placeholder/macro"));
    }

    @Test
    public void testPlaceHolderWhenMacroContainsMultiIssues()
    {
        openJiraIssuesDialog();
        EditContentPage editContentPage = search("TSTT-1, TST-1").clickInsertDialog();
        waitUntilInlineMacroAppearsInEditor(editContentPage, "jira");
        String htmlContent = editContentPage.getEditor().getContent().getTimedHtml().now();
        assertTrue(htmlContent.contains("/confluence/download/resources/confluence.extra.jira/jira-table.png"));
    }

    @Test
    public void testPlaceHolderWhenMacroContainsJQL()
    {
        openJiraIssuesDialog();
        EditContentPage editContentPage = search("project = 'Alphanumeric Key Test'").clickInsertDialog();
        waitUntilInlineMacroAppearsInEditor(editContentPage, "jira");
        String htmlContent = editContentPage.getEditor().getContent().getTimedHtml().now();
        assertTrue(htmlContent.contains("/confluence/download/resources/confluence.extra.jira/jira-table.png"));
    }

    @Test
    public void testPlaceHolderCountWhenMacroContainsMultiIssues()
    {
        openJiraIssuesDialog();
        search("project = 'Alphanumeric Key Test'");
        DisplayOptionPanel displayOptionPanel = jiraIssuesDialog.getDisplayOptionPanel();
        displayOptionPanel.clickDisplayTotalCount();
        jiraIssuesDialog.clickInsertDialog();
        waitUntilInlineMacroAppearsInEditor(editContentPage, "jira");
        String htmlContent = editContentPage.getEditor().getContent().getTimedHtml().now();
        assertTrue(htmlContent.contains("/confluence/plugins/servlet/image-generator?totalIssues=2"));
    }

    private JiraIssuesDialog search(String searchValue)
    {
        openJiraIssuesDialog();
        jiraIssuesDialog.inputJqlSearch(searchValue);
        return jiraIssuesDialog.clickSearchButton();
    }

}
