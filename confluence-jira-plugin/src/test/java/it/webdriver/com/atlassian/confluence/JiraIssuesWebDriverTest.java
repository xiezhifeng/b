package it.webdriver.com.atlassian.confluence;

import it.webdriver.com.atlassian.confluence.helper.JiraRestHelper;
import it.webdriver.com.atlassian.confluence.pageobjects.JiraIssuesDialog;
import it.webdriver.com.atlassian.confluence.pageobjects.JiraIssuesPage;
import it.webdriver.com.atlassian.confluence.pageobjects.JiraMacroPropertyPanel;

import java.io.IOException;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

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

    private static final String NO_ISSUES_COUNT_TEXT = "No issues found";

    private static final String ONE_ISSUE_COUNT_TEXT = "1 issue";

    private static final String MORE_ISSUES_COUNT_TEXT = "issues";
    
    private JiraIssuesDialog openSelectMacroDialog()
    {
        super.openMacroBrowser();
        JiraIssuesDialog jiraIssuesDialog = product.getPageBinder().bind(JiraIssuesDialog.class);
        jiraIssuesDialog.open();
        Poller.waitUntilTrue(jiraIssuesDialog.getJQLSearchElement().timed().isPresent());
        Assert.assertTrue(TITLE_DIALOG_JIRA_ISSUE.equals(jiraIssuesDialog.getTitleDialog()));

        return jiraIssuesDialog;
    }

    @Test
    public void testDialogValidation() 
    {
        JiraIssuesDialog jiraIssueDialog = openSelectMacroDialog();
        jiraIssueDialog.pasteJqlSearch("status = open");
        jiraIssueDialog.fillMaxIssues("20a");
        jiraIssueDialog.uncheckKey("TSTT-5");
        Assert.assertTrue("Insert button is disabled",!jiraIssueDialog.isInsertable());
    }
    
    @Test
    public void testColumnsAreDisableInCountMode() 
    {
        EditContentPage editPage = openSelectMacroDialog()
                                        .pasteJqlSearch("status = open")
                                        .clickSearchButton()
                                        .clickDisplayTotalCount()
                                        .clickInsertDialog();
        editPage.getContent().macroPlaceholderFor("jira").iterator().next().click();
        // edit macro
        product.getPageBinder().bind(JiraMacroPropertyPanel.class).edit();
        JiraIssuesDialog jiraIssuesDialog = product.getPageBinder().bind(JiraIssuesDialog.class);
        Assert.assertTrue(jiraIssuesDialog.isColumnsDisabled());
    }
    
    /**
     * check JQL search field when input filter URL convert to JQL
     */
    @Test
    public void checkPasteFilterUrlInJQLSearchField()
    {
        JiraIssuesDialog jiraIssueDialog = openSelectMacroDialog();
        String filterQuery = "filter=10001";
        String filterURL = "http://127.0.0.1:11990/jira/issues/?" + filterQuery;
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
            jiraIssueDialog.addColumn(LIST_VALUE_COLUMN[i]);
        }
        
        EditContentPage editPage = jiraIssueDialog.clickInsertDialog();
        waitForMacroOnEditor(editPage, "jira");
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
    public void checkMaxIssueNumberKeeping()
    {
        JiraIssuesDialog jiraIssueDialog = openSelectMacroDialog();
        jiraIssueDialog.fillMaxIssues("5");
        EditContentPage editPage = jiraIssueDialog.clickInsertDialog();
        waitForMacroOnEditor(editPage, "jira");

        editPage.getContent().macroPlaceholderFor("jira").iterator().next().click();
        product.getPageBinder().bind(JiraMacroPropertyPanel.class).edit();
        JiraIssuesDialog jiraMacroDialog = product.getPageBinder().bind(JiraIssuesDialog.class);
        Assert.assertEquals(jiraMacroDialog.getMaxIssuesTxt().getValue(), "5");
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
        jiraIssueDialog.getMaxIssuesTxt().javascript().execute("jQuery(arguments[0]).trigger('blur')");
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
        JiraIssuesPage viewPage = createPageWithTableJiraIssueMacro();
        int currentIssuesCount = viewPage.getNumberOfIssuesInTable();

        JiraIssueBean newIssue = new JiraIssueBean("10000", "2", "New feature", "");
        String id = "";
        try
        {
            id = JiraRestHelper.createIssue(newIssue);
        } catch (IOException e)
        {
            Assert.fail("Fail to create New JiraIssue using Rest API");
        }
        catch (JSONException e)
        {
            Assert.fail("Fail to create New JiraIssue using Rest API");
        }

        viewPage.clickRefreshedIcon();
        int newIssuesCount = viewPage.getNumberOfIssuesInTable();

        Assert.assertEquals(currentIssuesCount + 1, newIssuesCount);

        JiraRestHelper.deleteIssue(id);
    }

    @Test
    public void testRefreshCacheHaveSameData()
    {
        JiraIssuesPage viewPage = createPageWithTableJiraIssueMacro();
        int currentIssuesCount = viewPage.getNumberOfIssuesInTable();

        viewPage.clickRefreshedIcon();
        int newIssuesCount = viewPage.getNumberOfIssuesInTable();

        Assert.assertEquals(currentIssuesCount, newIssuesCount);
    }
    
    @Test
    public void testReturnFreshDataAfterUserEditsMacro()
    {
        ViewPage viewPage = createPageWithTableJiraIssueMacroAndJQL("project = TSTT");
        String issueSummary = "issue created using rest";
        JiraIssueBean newIssue = new JiraIssueBean("10011", "1", issueSummary, "test desc");
        String id = "";

        try
        {
            id = JiraRestHelper.createIssue(newIssue);
        } catch (JSONException e)
        {
            Assert.assertTrue("Fail to create New JiraIssue using Rest API", false);
        }
        catch (IOException e)
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
        waitForMacroOnEditor(editPage, "jira");
        viewPage = editPage.save();
        Assert.assertTrue(viewPage.getMainContent().getText().contains(issueSummary));

        JiraRestHelper.deleteIssue(id);
    }
    
    @Test
    public void testIssueCountHaveDataChange()
    {
        String jql = "status=open";
        JiraIssuesPage viewPage = createPageWithCountJiraIssueMacro(jql);
        int oldIssuesCount = viewPage.getIssueCount();

        JiraIssueBean newIssue = new JiraIssueBean("10000", "2", "New feature", "");
        String id = "";
        try
        {
            id = JiraRestHelper.createIssue(newIssue);
        } catch (IOException e)
        {
            Assert.fail("Fail to create New JiraIssue using Rest API");
        }
        catch (JSONException e)
        {
            Assert.fail("Fail to create New JiraIssue using Rest API");
        }

        viewPage = gotoPage(viewPage.getPageId());
        int newIssuesCount = viewPage.getIssueCount();
        Assert.assertEquals(oldIssuesCount + 1, newIssuesCount);

        JiraRestHelper.deleteIssue(id);
    }

    @Test
    public void checkColumnKeepingAfterSearch()
    {
        JiraIssuesDialog jiraIssueDialog = openSelectMacroDialog();
        jiraIssueDialog.inputJqlSearch("status = open");
        jiraIssueDialog.clickSearchButton();
        jiraIssueDialog.openDisplayOption();

        List<String>  firstSelectedColumns = jiraIssueDialog.getSelectedColumns();
        jiraIssueDialog.removeSelectedColumn("Resolution");
        jiraIssueDialog.removeSelectedColumn("Status");

        //Search again and check list columns after removed "Resolution" and "Status" columns
        jiraIssueDialog.clickSearchButton();
        jiraIssueDialog.openDisplayOption();
        List<String>  removedSelectedColumns = jiraIssueDialog.getSelectedColumns();
        Assert.assertEquals(firstSelectedColumns.size() - 2, removedSelectedColumns.size());
        Assert.assertFalse(removedSelectedColumns.contains("Resolution"));
        Assert.assertFalse(removedSelectedColumns.contains("Status"));

        //Search again and check list columns after add "Status" column
        jiraIssueDialog.addColumn("Status");
        jiraIssueDialog.clickSearchButton();
        jiraIssueDialog.openDisplayOption();
        List<String>  addedSelectedColumns = jiraIssueDialog.getSelectedColumns();
        Assert.assertTrue(addedSelectedColumns.contains("Status"));
    }

    @Test
    public void testNoIssuesCountText()
    {
        JiraIssuesPage jiraIssuesPage = createPageWithTableJiraIssueMacroAndJQL("status=Reopened");
        Assert.assertEquals(NO_ISSUES_COUNT_TEXT, jiraIssuesPage.getNumberOfIssuesText());
    }

    @Test
    public void testOneIssueResultText()
    {
        JiraIssuesPage jiraIssuesPage = createPageWithTableJiraIssueMacroAndJQL("project = TST");
        Assert.assertEquals(ONE_ISSUE_COUNT_TEXT, jiraIssuesPage.getNumberOfIssuesText());
    }

    @Test
    public void testMoreIssueResultText()
    {
        JiraIssuesPage jiraIssuesPage = createPageWithTableJiraIssueMacroAndJQL("status=Open");
        Assert.assertTrue(jiraIssuesPage.getNumberOfIssuesText().contains(MORE_ISSUES_COUNT_TEXT));
    }

    @Test
    public void testChangeApplinkName()
    {
        String applinkId = getPrimaryApplinkId();
        String jimMarkup = "{jira:jqlQuery=status\\=open||serverId="+applinkId+"||server=oldInvalidName}";
        EditContentPage editPage = product.loginAndEdit(User.ADMIN, Page.TEST);
        editPage.getContent().setContent(jimMarkup);
        editPage.save();
        Assert.assertTrue(bindCurrentPageToJiraIssues().getNumberOfIssuesInTable() > 0);
    }

    private JiraIssuesPage createPageWithTableJiraIssueMacro()
    {
        return createPageWithTableJiraIssueMacroAndJQL("status=open");
    }

    private JiraIssuesPage createPageWithTableJiraIssueMacroAndJQL(String jql)
    {
        JiraIssuesDialog jiraIssuesDialog = openSelectMacroDialog();
        jiraIssuesDialog.inputJqlSearch(jql);
        jiraIssuesDialog.clickSearchButton();
        EditContentPage editContentPage = jiraIssuesDialog.clickInsertDialog();
        waitForMacroOnEditor(editContentPage, "jira");
        editContentPage.save();
        return bindCurrentPageToJiraIssues();
    }

    private JiraIssuesPage createPageWithCountJiraIssueMacro(String jql)
    {
        JiraIssuesDialog jiraIssuesDialog = openSelectMacroDialog();
        jiraIssuesDialog.inputJqlSearch(jql);
        jiraIssuesDialog.clickSearchButton();
        jiraIssuesDialog.clickDisplayTotalCount();
        EditContentPage editContentPage = jiraIssuesDialog.clickInsertDialog();
        waitForMacroOnEditor(editContentPage, "jira");
        editContentPage.save();
        return bindCurrentPageToJiraIssues();
    }

    private JiraIssuesPage gotoPage(Long pageId)
    {
        product.viewPage(String.valueOf(pageId));
        return bindCurrentPageToJiraIssues();
    }

    private JiraIssuesPage bindCurrentPageToJiraIssues()
    {
        return product.getPageBinder().bind(JiraIssuesPage.class);
    }

}
