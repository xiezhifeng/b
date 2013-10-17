package it.webdriver.com.atlassian.confluence;

import it.webdriver.com.atlassian.confluence.helper.JiraRestHelper;
import it.webdriver.com.atlassian.confluence.pageobjects.JiraCreatedMacroDialog;
import it.webdriver.com.atlassian.confluence.pageobjects.JiraIssuesDialog;
import it.webdriver.com.atlassian.confluence.pageobjects.JiraMacroPropertyPanel;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;

import com.atlassian.confluence.it.Page;
import com.atlassian.confluence.it.User;
import com.atlassian.confluence.json.parser.JSONException;
import com.atlassian.confluence.pageobjects.component.editor.EditorContent;
import com.atlassian.confluence.pageobjects.component.editor.MacroPlaceholder;
import com.atlassian.confluence.pageobjects.page.content.EditContentPage;
import com.atlassian.confluence.pageobjects.page.content.ViewPage;
import com.atlassian.confluence.plugins.jira.beans.JiraIssueBean;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.Poller;

public class JiraIssuesWebDriverTest extends AbstractJiraWebDriverTest
{
    private static final String TITLE_DIALOG_JIRA_ISSUE = "Insert JIRA Issue";
    
    private static final String[] LIST_VALUE_COLUMN = {"Issue Type", "Resolved", "Summary", "Key"};
    
    private JiraIssuesDialog openSelectMacroDialog()
    {
        EditContentPage editPage = product.loginAndEdit(User.ADMIN, Page.TEST);
        editPage.openMacroBrowser();
        JiraIssuesDialog jiraIssuesDialog = product.getPageBinder().bind(JiraIssuesDialog.class);
        jiraIssuesDialog.open();

        Assert.assertTrue(TITLE_DIALOG_JIRA_ISSUE.equals(jiraIssuesDialog.getTitleDialog()));

        return jiraIssuesDialog;
    }

    /**
     * check JQL search field when input filter URL convert to JQL
     */
    @Test
    public void checkPasteFilterUrlInJQLSearchField()
    {
        JiraIssuesDialog jiraIssueDialog = openSelectMacroDialog();
        String filterQuery = "filter=10001";
        String filterURL = JIRA_BASE_URL + "/issues/?" + filterQuery;
        jiraIssueDialog.pasteJqlSearch(filterURL);

        Poller.waitUntilTrue(jiraIssueDialog.getJQLSearchElement().timed().isEnabled());
        Poller.waitUntilTrue(jiraIssueDialog.getSearchButton().timed().isEnabled());
        jiraIssueDialog.clickJqlSearch();

        Assert.assertEquals(filterQuery, jiraIssueDialog.getJqlSearch());
    }

    /**
     * check JQL search field when input filter JQL convert to JQL
     */
    @Test
    public void checkPasteFilterJqlInJQLSearchField()
    {
        JiraIssuesDialog jiraIssueDialog = openSelectMacroDialog();
        String filterQuery = "filter=10001";
        jiraIssueDialog.pasteJqlSearch(filterQuery);

        Poller.waitUntilTrue(jiraIssueDialog.getJQLSearchElement().timed().isEnabled());
        Poller.waitUntilTrue(jiraIssueDialog.getSearchButton().timed().isEnabled());
        jiraIssueDialog.clickJqlSearch();

        Assert.assertEquals(filterQuery, jiraIssueDialog.getJqlSearch());
    }
    
    @Test
    public void checkColumnInDialog() 
    {
        JiraIssuesDialog jiraIssueDialog = openSelectMacroDialog();
        jiraIssueDialog.inputJqlSearch("status = open");
        
        jiraIssueDialog.clickSearchButton();
        jiraIssueDialog.openDisplayOption();
        
        //clean all column default and add new list column
        jiraIssueDialog.cleanAllOptionColumn();
        for(int i = 0; i< LIST_VALUE_COLUMN.length; i++)
        {
            jiraIssueDialog.clickSelected2Element();
            jiraIssueDialog.selectOption(LIST_VALUE_COLUMN[i]);
        }
        
        EditContentPage editPage = jiraIssueDialog.clickInsertDialog();
        EditorContent editorContent = editPage.getContent();
        List<MacroPlaceholder> listMacroChart = editorContent.macroPlaceholderFor("jira");
        Assert.assertEquals(1, listMacroChart.size());
        String htmlMacro = editorContent.getHtml();
        Assert.assertTrue(htmlMacro.contains("data-macro-parameters=\"columns=type,resolutiondate,summary,key"));
    }

    @Test
    public void checkMaxIssueValidNumber()
    {
        // Invalid number
        JiraIssuesDialog jiraIssueDialog = openSelectMacroDialog();
        jiraIssueDialog.fillMaxIssues("100kdkdkd");
        Assert.assertTrue(jiraIssueDialog.hasMaxIssuesErrorMsg());
    }

    @Test
    public void checkMaxIssueAboveRange()
    {
        // Out of range
        JiraIssuesDialog jiraIssueDialog = openSelectMacroDialog();
        jiraIssueDialog.fillMaxIssues("1000000");
        Assert.assertTrue(jiraIssueDialog.hasMaxIssuesErrorMsg());
    }

    @Test
    public void checkMaxIssueBelowRange()
    {
        // Out of range
        JiraIssuesDialog jiraIssueDialog = openSelectMacroDialog();
        jiraIssueDialog.fillMaxIssues("-10");
        Assert.assertTrue(jiraIssueDialog.hasMaxIssuesErrorMsg());
    }

    @Test
    public void checkMaxIssueDisplayOption()
    {
        // behaviour when click difference display option
        JiraIssuesDialog jiraIssueDialog = openSelectMacroDialog();
        jiraIssueDialog.fillMaxIssues("-10");
        Assert.assertTrue(jiraIssueDialog.hasMaxIssuesErrorMsg());
        jiraIssueDialog.clickDisplaySingle();
        jiraIssueDialog.clickDisplayTotalCount();
        jiraIssueDialog.clickDisplayTable();
        Assert.assertTrue(jiraIssueDialog.hasMaxIssuesErrorMsg());
    }

    @Test
    public void checkDefaultValue()
    {
        JiraIssuesDialog jiraIssueDialog = openSelectMacroDialog();
        jiraIssueDialog.showDisplayOption();
        String value = jiraIssueDialog.getMaxIssuesTxt().getValue();
        Assert.assertEquals("20", value);
    }

    @Test
    public void checkEmptyDefaultValue()
    {
        JiraIssuesDialog jiraIssueDialog = openSelectMacroDialog();
        jiraIssueDialog.showDisplayOption();
        jiraIssueDialog.getMaxIssuesTxt().clear();
        String value = jiraIssueDialog.getMaxIssuesTxt().getValue();
        Assert.assertEquals("1000", value);
    }

    @Test
    public void checkMaxIssueHappyCase()
    {
        JiraIssuesDialog jiraIssueDialog = openSelectMacroDialog();
        jiraIssueDialog.showDisplayOption();
        jiraIssueDialog.fillMaxIssues("1");
        List<PageElement> issuses = jiraIssueDialog.insertAndSave();
        Assert.assertNotNull(issuses);
        Assert.assertEquals(1, issuses.size());
    }

    @Test
    public void testRefreshCacheHaveDataChange()
    {
        ViewPage viewPage = createPageWithTableJiraIssueMacro();
        EditContentPage editContentPage = insertNewIssue(viewPage);
        viewPage = editContentPage.save();
        validateCacheResult(viewPage, 1);
    }

    @Test
    public void testRefreshCacheHaveSameData()
    {
        ViewPage viewPage = createPageWithTableJiraIssueMacro();
        validateCacheResult(viewPage, 0);
    }
    
    @Test
    public void testReturnFreshDataAfterUserEditsMacro()
    {
        ViewPage viewPage = createPageWithTableJiraIssueMacroAndJQL("project = TSTT");
        String issueSummary = "issue created using rest";
        JiraIssueBean newIssue = new JiraIssueBean("10011", "1", issueSummary, "test desc");
        try
        {
            JiraRestHelper.createIssue(newIssue);
        } catch (JSONException e)
        {
            Assert.assertTrue("Fail to create New JiraIssue using Rest API", false);
        }
        EditContentPage editPage = viewPage.edit();
        // Make property panel visible
        editPage.getContent().macroPlaceholderFor("jira").iterator().next().click();
        // edit macro
        product.getPageBinder().bind(JiraMacroPropertyPanel.class).edit();
        JiraIssuesDialog jiraMacroDialog = product.getPageBinder().bind(JiraIssuesDialog.class);
        jiraMacroDialog.clickSearchButton().clickInsertDialog();
        viewPage = editPage.save();
        Assert.assertTrue(viewPage.getMainContent().getText().contains(issueSummary));
    }
    
    private void validateCacheResult(ViewPage viewPage, int numOfNewIssues)
    {
        PageElement mainContent = viewPage.getMainContent();
        int numberOfIssues = getNumberIssue(mainContent);
        clickRefreshedIcon(mainContent);
        Poller.waitUntilTrue(mainContent.find(By.cssSelector("table.aui")).timed().isVisible());
        Assert.assertTrue(numberOfIssues + numOfNewIssues == getNumberIssue(mainContent));
    }

    private ViewPage createPageWithTableJiraIssueMacro()
    {
        return createPageWithTableJiraIssueMacroAndJQL("status=open");
    }

    private ViewPage createPageWithTableJiraIssueMacroAndJQL(String jql)
    {
        JiraIssuesDialog jiraIssuesDialog = openSelectMacroDialog();
        jiraIssuesDialog.inputJqlSearch(jql);
        jiraIssuesDialog.clickSearchButton();
        EditContentPage editContentPage = jiraIssuesDialog.clickInsertDialog();
        waitForMacroOnEditor(editContentPage, "jira");
        return editContentPage.save();
    }

    private int getNumberIssue(PageElement mainContent)
    {
        return mainContent.findAll(By.cssSelector("table.aui .rowNormal")).size()
                + mainContent.findAll(By.cssSelector("table.aui .rowAlternate")).size();
    }

    private void clickRefreshedIcon(PageElement mainContent)
    {
        PageElement refreshedIcon = mainContent.find(By.cssSelector(".icon-refresh"));
        refreshedIcon.click();
    }

    private EditContentPage insertNewIssue(ViewPage viewPage)
    {
        EditContentPage editContentPage = viewPage.edit();
        editContentPage.openInsertMenu();
        JiraCreatedMacroDialog jiraMacroDialog = product.getPageBinder().bind(JiraCreatedMacroDialog.class);
        jiraMacroDialog.open();
        return createJiraIssue(jiraMacroDialog, "10000", "1", "TEST CACHE", null);
    }

}
