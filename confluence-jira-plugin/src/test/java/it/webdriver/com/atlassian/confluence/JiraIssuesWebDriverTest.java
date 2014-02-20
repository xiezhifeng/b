package it.webdriver.com.atlassian.confluence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import it.webdriver.com.atlassian.confluence.helper.JiraRestHelper;
import it.webdriver.com.atlassian.confluence.pageobjects.DisplayOptionPanel;
import it.webdriver.com.atlassian.confluence.pageobjects.JiraIssuesDialog;
import it.webdriver.com.atlassian.confluence.pageobjects.JiraIssuesPage;
import it.webdriver.com.atlassian.confluence.pageobjects.JiraMacroPropertyPanel;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

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

    private static final List<String> LIST_TEST_COLUMN = Arrays.asList("Issue Type", "Resolved", "Summary", "Key");

    private static List<String> LIST_DEFAULT_COLUMN = Arrays.asList("Key, Summary, Issue Type, Created, Updated, Due Date, Assignee, Reporter, Priority, Status, Resolution");

    private static final String NO_ISSUES_COUNT_TEXT = "No issues found";

    private static final String ONE_ISSUE_COUNT_TEXT = "1 issue";

    private static final String MORE_ISSUES_COUNT_TEXT = "issues";

    private JiraIssuesDialog openJiraIssuesDialog()
    {
        super.openMacroBrowser();
        return selectAndOpenJiraIssueDialog();
    }

    private JiraIssuesDialog openJiraIssueDialogFromBlogPost()
    {
        super.opeMacroBrowserInBlogPost();
        return selectAndOpenJiraIssueDialog();
    }
    private JiraIssuesDialog openJiraIssuesDialogFromEditPage(EditContentPage editPage)
    {
        super.openMacroBrowser(editPage);
        return selectAndOpenJiraIssueDialog();
    }

    private JiraIssuesDialog selectAndOpenJiraIssueDialog()
    {
        JiraIssuesDialog jiraIssuesDialog = product.getPageBinder().bind(JiraIssuesDialog.class);
        jiraIssuesDialog.open();
        Poller.waitUntilTrue(jiraIssuesDialog.getJQLSearchElement().timed().isPresent());
        assertTrue(TITLE_DIALOG_JIRA_ISSUE.equals(jiraIssuesDialog.getTitleDialog()));
        assertTrue(jiraIssuesDialog.isJqlSearchTextFocus());
        return jiraIssuesDialog;
    }

    private JiraIssuesDialog openJiraIssuesDialogFromMacroPlaceholder(MacroPlaceholder macroPlaceholder)
    {
        macroPlaceholder.click();
        product.getPageBinder().bind(JiraMacroPropertyPanel.class).edit();
        return product.getPageBinder().bind(JiraIssuesDialog.class);
    }

    @Test
    public void testDialogValidation()
    {
        JiraIssuesDialog jiraIssueDialog = openJiraIssuesDialog();
        jiraIssueDialog.pasteJqlSearch("status = open");
        jiraIssueDialog.fillMaxIssues("20a");
        jiraIssueDialog.uncheckKey("TSTT-5");
        assertTrue("Insert button is disabled", !jiraIssueDialog.isInsertable());
    }

    @Test
    public void testColumnsAreDisableInCountMode()
    {
        JiraIssuesDialog jiraIssuesDialog = openJiraIssuesDialog()
                .pasteJqlSearch("status = open")
                .clickSearchButton();
        jiraIssuesDialog.getDisplayOptionPanel().clickDisplayTotalCount();
        EditContentPage editPage = jiraIssuesDialog.clickInsertDialog();
        MacroPlaceholder macroPlaceholder = editPage.getContent().macroPlaceholderFor("jira").iterator().next();
        jiraIssuesDialog = openJiraIssuesDialogFromMacroPlaceholder(macroPlaceholder);

        assertTrue(jiraIssuesDialog.getDisplayOptionPanel().isColumnsDisabled());
    }

    @Test
    public void testSortIssueTable()
    {
        JiraIssuesPage page = createPageWithTableJiraIssueMacroAndJQL("project = TSTT");
        String KeyValueAtFirstTimeLoad = page.getFirstRowValueOfSummay();
        page.clickHeaderIssueTable("Summary");
        String keyAfterSort = page.getFirstRowValueOfSummay();
        assertNotSame(KeyValueAtFirstTimeLoad, keyAfterSort);
    }

    @Test
    public void testColumnNotSupportSortableInIssueTable()
    {
        JiraIssuesDialog jiraIssueDialog = openJiraIssuesDialog();
        jiraIssueDialog.inputJqlSearch("status = open");
        jiraIssueDialog.clickSearchButton();
        jiraIssueDialog.openDisplayOption();
        jiraIssueDialog.getDisplayOptionPanel().addColumn("Linked Issues");
        EditContentPage editContentPage = jiraIssueDialog.clickInsertDialog();
        waitForMacroOnEditor(editContentPage, "jira");
        editContentPage.save();
        JiraIssuesPage page = product.getPageBinder().bind(JiraIssuesPage.class);
        String keyValueAtFirstTime = page.getFirstRowValueOfSummay();
        page.clickHeaderIssueTable("Linked Issues");
        String keyAfterSort = page.getFirstRowValueOfSummay();
        assertEquals(keyValueAtFirstTime, keyAfterSort);
    }
    /**
     * check JQL search field when input filter URL convert to JQL
     */
    @Test
    public void checkPasteFilterUrlInJQLSearchField()
    {
        JiraIssuesDialog jiraIssueDialog = openJiraIssuesDialog();
        String filterQuery = "filter=10001";
        String filterURL = "http://127.0.0.1:11990/jira/issues/?" + filterQuery;
        jiraIssueDialog.pasteJqlSearch(filterURL);

        Poller.waitUntilTrue(jiraIssueDialog.getJQLSearchElement().timed().isEnabled());
        Poller.waitUntilTrue(jiraIssueDialog.getSearchButton().timed().isEnabled());
        jiraIssueDialog.clickJqlSearch();

        assertEquals(filterQuery, jiraIssueDialog.getJqlSearch());
    }

    /**
     * check JQL search field when input filter JQL convert to JQL
     */
    @Test
    public void checkPasteFilterJqlInJQLSearchField()
    {
        JiraIssuesDialog jiraIssueDialog = openJiraIssuesDialog();
        String filterQuery = "filter=10001";
        jiraIssueDialog.pasteJqlSearch(filterQuery);

        Poller.waitUntilTrue(jiraIssueDialog.getJQLSearchElement().timed().isEnabled());
        Poller.waitUntilTrue(jiraIssueDialog.getSearchButton().timed().isEnabled());
        jiraIssueDialog.clickJqlSearch();

        assertEquals(filterQuery, jiraIssueDialog.getJqlSearch());
    }

    @Test
    public void checkColumnInDialog()
    {
        EditContentPage editPage = insertJiraIssueMacroWithEditColumn(LIST_TEST_COLUMN, "status=open");
        String htmlMacro = editPage.getContent().getHtml();
        assertTrue(htmlMacro.contains("data-macro-parameters=\"columns=type,resolutiondate,summary,key"));
    }

    @Test
    public void checkMaxIssueValidNumber()
    {
        // Invalid number
        JiraIssuesDialog jiraIssueDialog = openJiraIssuesDialog();
        jiraIssueDialog.fillMaxIssues("100kdkdkd");
        assertTrue(jiraIssueDialog.hasMaxIssuesErrorMsg());
    }

    @Test
    public void checkMaxIssueAboveRange()
    {
        // Out of range
        JiraIssuesDialog jiraIssueDialog = openJiraIssuesDialog();
        jiraIssueDialog.fillMaxIssues("1000000");
        assertTrue(jiraIssueDialog.hasMaxIssuesErrorMsg());
    }

    @Test
    public void checkMaxIssueBelowRange()
    {
        // Out of range
        JiraIssuesDialog jiraIssueDialog = openJiraIssuesDialog();
        jiraIssueDialog.fillMaxIssues("-10");
        assertTrue(jiraIssueDialog.hasMaxIssuesErrorMsg());
    }

    @Test
    public void checkMaxIssueDisplayOption()
    {
        // behaviour when click difference display option
        JiraIssuesDialog jiraIssueDialog = openJiraIssuesDialog();
        jiraIssueDialog.fillMaxIssues("-10");
        assertTrue(jiraIssueDialog.hasMaxIssuesErrorMsg());
        DisplayOptionPanel displayOptionPanel = jiraIssueDialog.getDisplayOptionPanel();
        displayOptionPanel.clickDisplaySingle();
        displayOptionPanel.clickDisplayTotalCount();
        displayOptionPanel.clickDisplayTable();
        assertTrue(jiraIssueDialog.hasMaxIssuesErrorMsg());
    }

    @Test
    public void checkMaxIssueNumberKeeping()
    {
        JiraIssuesDialog jiraIssueDialog = openJiraIssuesDialog();
        jiraIssueDialog.fillMaxIssues("5");
        EditContentPage editPage = jiraIssueDialog.clickInsertDialog();
        waitForMacroOnEditor(editPage, "jira");

        MacroPlaceholder macroPlaceholder  = editPage.getContent().macroPlaceholderFor("jira").iterator().next();
        JiraIssuesDialog jiraMacroDialog = openJiraIssuesDialogFromMacroPlaceholder(macroPlaceholder);
        assertEquals(jiraMacroDialog.getMaxIssuesTxt().getValue(), "5");
    }

    @Test
    public void checkDefaultValue()
    {
        JiraIssuesDialog jiraIssueDialog = openJiraIssuesDialog();
        jiraIssueDialog.showDisplayOption();
        String value = jiraIssueDialog.getMaxIssuesTxt().getValue();
        assertEquals("20", value);
    }

    @Test
    public void checkEmptyDefaultValue()
    {
        JiraIssuesDialog jiraIssueDialog = openJiraIssuesDialog();
        jiraIssueDialog.showDisplayOption();
        jiraIssueDialog.getMaxIssuesTxt().clear();
        jiraIssueDialog.getMaxIssuesTxt().javascript().execute("jQuery(arguments[0]).trigger('blur')");
        String value = jiraIssueDialog.getMaxIssuesTxt().getValue();
        assertEquals("1000", value);
    }

    @Test
    public void checkMaxIssueHappyCase()

    {
        JiraIssuesDialog jiraIssueDialog = openJiraIssuesDialog();
        jiraIssueDialog.showDisplayOption();
        jiraIssueDialog.fillMaxIssues("1");
        List<PageElement> issuses = jiraIssueDialog.insertAndSave();
        assertNotNull(issuses);
        assertEquals(1, issuses.size());
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
        }
        catch (IOException e)
        {
            fail("Fail to create New JiraIssue using Rest API");
        }
        catch (JSONException e)
        {
            fail("Fail to create New JiraIssue using Rest API");
        }

        viewPage.clickRefreshedIcon();
        int newIssuesCount = viewPage.getNumberOfIssuesInTable();

        assertEquals(currentIssuesCount + 1, newIssuesCount);

        JiraRestHelper.deleteIssue(id);
    }

    @Test
    public void testRefreshCacheHaveSameData()
    {
        JiraIssuesPage viewPage = createPageWithTableJiraIssueMacro();
        int currentIssuesCount = viewPage.getNumberOfIssuesInTable();

        viewPage.clickRefreshedIcon();
        int newIssuesCount = viewPage.getNumberOfIssuesInTable();

        assertEquals(currentIssuesCount, newIssuesCount);
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
        }
        catch (JSONException e)
        {
            assertTrue("Fail to create New JiraIssue using Rest API", false);
        }
        catch (IOException e)
        {
            assertTrue("Fail to create New JiraIssue using Rest API", false);
        }

        EditContentPage editPage = viewPage.edit();
        // Make property panel visible
        MacroPlaceholder macroPlaceholder = editPage.getContent().macroPlaceholderFor("jira").iterator().next();
        JiraIssuesDialog jiraMacroDialog = openJiraIssuesDialogFromMacroPlaceholder(macroPlaceholder);
        jiraMacroDialog.clickSearchButton().clickInsertDialog();
        waitForMacroOnEditor(editPage, "jira");
        viewPage = editPage.save();
        assertTrue(viewPage.getMainContent().getText().contains(issueSummary));

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
            fail("Fail to create New JiraIssue using Rest API");
        }
        catch (JSONException e)
        {
            fail("Fail to create New JiraIssue using Rest API");
        }

        viewPage = gotoPage(viewPage.getPageId());
        int newIssuesCount = viewPage.getIssueCount();
        assertEquals(oldIssuesCount + 1, newIssuesCount);

        JiraRestHelper.deleteIssue(id);
    }

    @Test
    public void checkColumnKeepingAfterSearch()
    {
        JiraIssuesDialog jiraIssueDialog = openJiraIssuesDialog();
        jiraIssueDialog.inputJqlSearch("status = open");
        jiraIssueDialog.clickSearchButton();
        jiraIssueDialog.openDisplayOption();

        DisplayOptionPanel displayOptionPanel = jiraIssueDialog.getDisplayOptionPanel();
        List<String>  firstSelectedColumns = displayOptionPanel.getSelectedColumns();
        displayOptionPanel.removeSelectedColumn("Resolution");
        displayOptionPanel.removeSelectedColumn("Status");

        //Search again and check list columns after removed "Resolution" and "Status" columns
        jiraIssueDialog.clickSearchButton();
        jiraIssueDialog.openDisplayOption();
        List<String>  removedSelectedColumns = displayOptionPanel.getSelectedColumns();
        assertEquals(firstSelectedColumns.size() - 2, removedSelectedColumns.size());
        assertFalse(removedSelectedColumns.contains("Resolution"));
        assertFalse(removedSelectedColumns.contains("Status"));

        //Search again and check list columns after add "Status" column
        displayOptionPanel.addColumn("Status");
        jiraIssueDialog.clickSearchButton();
        jiraIssueDialog.openDisplayOption();
        List<String>  addedSelectedColumns = displayOptionPanel.getSelectedColumns();
        assertTrue(addedSelectedColumns.contains("Status"));
    }

    @Test
    public void testNoIssuesCountText()
    {
        JiraIssuesPage jiraIssuesPage = createPageWithTableJiraIssueMacroAndJQL("status=Reopened");
        assertEquals(NO_ISSUES_COUNT_TEXT, jiraIssuesPage.getNumberOfIssuesText());
    }

    @Test
    public void testOneIssueResultText()
    {
        JiraIssuesPage jiraIssuesPage = createPageWithTableJiraIssueMacroAndJQL("project = TST");
        assertEquals(ONE_ISSUE_COUNT_TEXT, jiraIssuesPage.getNumberOfIssuesText());
    }

    @Test
    public void testMoreIssueResultText()
    {
        JiraIssuesPage jiraIssuesPage = createPageWithTableJiraIssueMacroAndJQL("status=Open");
        assertTrue(jiraIssuesPage.getNumberOfIssuesText().contains(MORE_ISSUES_COUNT_TEXT));
    }

    @Test
    public void testChangeApplinkName()
    {
        String applinkId = getPrimaryApplinkId();
        String jimMarkup = "{jira:jqlQuery=status\\=open||serverId="+applinkId+"||server=oldInvalidName}";
        EditContentPage editPage = product.loginAndEdit(User.ADMIN, Page.TEST);
        editPage.getContent().setContent(jimMarkup);
        editPage.save();
        assertTrue(bindCurrentPageToJiraIssues().getNumberOfIssuesInTable() > 0);
    }

    @Test
    public void testCanInsertMacroWhenChangeTab()
    {
        JiraIssuesDialog jiraIssueDialog = openJiraIssuesDialog();
        jiraIssueDialog.inputJqlSearch("status = open");
        jiraIssueDialog.clickSearchButton();

        //change to create issue panel to make disable insert button
        jiraIssueDialog.selectMenuItem(2);

        //back again search panel
        jiraIssueDialog.selectMenuItem(1);
        Poller.waitUntilTrue(jiraIssueDialog.getInsertButton().timed().isEnabled());
        EditContentPage editPage = jiraIssueDialog.clickInsertDialog();
        waitForMacroOnEditor(editPage, "jira");
        assertEquals(editPage.getContent().macroPlaceholderFor("jira").size(), 1);
    }

    @Test
    public void checkColumnLoadDefaultWhenInsert()
    {
        EditContentPage editPage = insertJiraIssueMacroWithEditColumn(LIST_TEST_COLUMN, "status=open");

        JiraIssuesDialog jiraIssueDialog = openJiraIssuesDialogFromEditPage(editPage);

        assertTrue(jiraIssueDialog.getJqlSearch().equals(""));
        assertFalse(jiraIssueDialog.getIssuesTable().isPresent());

        jiraIssueDialog.inputJqlSearch("status = open");
        jiraIssueDialog.clickSearchButton();
        jiraIssueDialog.openDisplayOption();

        List<String> columns = jiraIssueDialog.getDisplayOptionPanel().getSelectedColumns();
        assertEquals(columns.toString(), LIST_DEFAULT_COLUMN.toString());
    }

    @Test
    public void checkTableOptionEnableWhenChooseOneIssue()
    {
        JiraIssuesDialog jiraIssueDialog = openJiraIssuesDialog();
        jiraIssueDialog.inputJqlSearch("status=open");
        jiraIssueDialog.clickSearchButton();

        jiraIssueDialog.clickSelectAllIssueOption();
        jiraIssueDialog.clickSelectIssueOption("TP-1");

        jiraIssueDialog.openDisplayOption();
        DisplayOptionPanel displayOptionPanel = jiraIssueDialog.getDisplayOptionPanel();
        assertTrue(displayOptionPanel.isInsertSingleIssueEnable());
        assertTrue(displayOptionPanel.isInsertTableIssueEnable());
        assertFalse(displayOptionPanel.isInsertCountIssueEnable());

        jiraIssueDialog.clickSelectIssueOption("TP-2");
        assertTrue(displayOptionPanel.isInsertCountIssueEnable());
        assertTrue(displayOptionPanel.isInsertTableIssueEnable());
        assertFalse(displayOptionPanel.isInsertSingleIssueEnable());

    }

    @Test
    public void testInsertTableByKeyQuery()
    {
        JiraIssuesDialog jiraIssueDialog = openJiraIssuesDialog();
        jiraIssueDialog.inputJqlSearch("key = TP-1");
        jiraIssueDialog.clickSearchButton();

        jiraIssueDialog.openDisplayOption();
        jiraIssueDialog.getDisplayOptionPanel().clickDisplayTable();

        EditContentPage editPage = jiraIssueDialog.clickInsertDialog();
        waitForMacroOnEditor(editPage, "jira");
        editPage.save();
        JiraIssuesPage jiraIssuesPage = bindCurrentPageToJiraIssues();

        assertTrue(jiraIssuesPage.getIssuesTableElement().isPresent());
        assertFalse(jiraIssuesPage.getIssuesCountElement().isPresent());
        assertFalse(jiraIssuesPage.getRefreshedIconElement().isPresent());
    }

    @Test
    public void testInsertSingleIssueIntoBlogPostContainsJiraMetaData()
    {
        JiraIssuesPage page = createPageWithSingleJiraIssue(openJiraIssueDialogFromBlogPost());
        assertEquals("1 JIRA links", page.getTextOfJiraMetaData());
    }

    @Test
    public void testInsertSingleIssueIntoPageContainsJiraMetaData()
    {
        JiraIssuesPage page = createPageWithSingleJiraIssue(openJiraIssuesDialog());
        assertEquals("1 JIRA links", page.getTextOfJiraMetaData());
    }

    private JiraIssuesPage createPageWithSingleJiraIssue(JiraIssuesDialog jiraIssuesDialog)
    {
        jiraIssuesDialog.inputJqlSearch("key = TP-1");
        jiraIssuesDialog.clickSearchButton().clickInsertDialog();
        EditContentPage editContentPage = jiraIssuesDialog.clickInsertDialog();
        waitForMacroOnEditor(editContentPage, "jira");
        editContentPage.save();
       return bindCurrentPageToJiraIssues();
    }

    private JiraIssuesPage createPageWithTableJiraIssueMacro()
    {
        return createPageWithTableJiraIssueMacroAndJQL("status=open");
    }

    private JiraIssuesPage createPageWithTableJiraIssueMacroAndJQL(String jql)
    {
        JiraIssuesDialog jiraIssuesDialog = openJiraIssuesDialog();
        jiraIssuesDialog.inputJqlSearch(jql);
        jiraIssuesDialog.clickSearchButton();

        EditContentPage editContentPage = jiraIssuesDialog.clickInsertDialog();
        waitForMacroOnEditor(editContentPage, "jira");
        editContentPage.save();
        return bindCurrentPageToJiraIssues();
    }

    private JiraIssuesPage createPageWithCountJiraIssueMacro(String jql)
    {
        JiraIssuesDialog jiraIssuesDialog = openJiraIssuesDialog();
        jiraIssuesDialog.inputJqlSearch(jql);
        jiraIssuesDialog.clickSearchButton();
        jiraIssuesDialog.getDisplayOptionPanel().clickDisplayTotalCount();
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

    private EditContentPage insertJiraIssueMacroWithEditColumn(List<String> columnNames, String jql)
    {
        JiraIssuesDialog jiraIssueDialog = openJiraIssuesDialog();
        jiraIssueDialog.inputJqlSearch(jql);
        jiraIssueDialog.clickSearchButton();
        jiraIssueDialog.openDisplayOption();

        //clean all column default and add new list column
        jiraIssueDialog.cleanAllOptionColumn();
        DisplayOptionPanel displayOptionPanel = jiraIssueDialog.getDisplayOptionPanel();
        for(String columnName : columnNames)
        {
            displayOptionPanel.addColumn(columnName);
        }

        EditContentPage editPage = jiraIssueDialog.clickInsertDialog();
        waitForMacroOnEditor(editPage, "jira");
        EditorContent editorContent = editPage.getContent();
        List<MacroPlaceholder> listMacroChart = editorContent.macroPlaceholderFor("jira");
        assertEquals(1, listMacroChart.size());

        return editPage;
    }
}
