package it.webdriver.com.atlassian.confluence.jiraissues.searchedpanel;

import com.atlassian.confluence.it.Page;
import com.atlassian.confluence.it.User;
import com.atlassian.confluence.pageobjects.component.editor.EditorContent;
import com.atlassian.confluence.pageobjects.component.editor.MacroPlaceholder;
import com.atlassian.confluence.pageobjects.page.content.EditContentPage;
import com.atlassian.confluence.pageobjects.page.content.ViewPage;
import com.atlassian.confluence.plugins.jira.beans.JiraIssueBean;
import com.atlassian.pageobjects.elements.query.Poller;
import it.webdriver.com.atlassian.confluence.helper.ApplinkHelper;
import it.webdriver.com.atlassian.confluence.helper.JiraRestHelper;
import it.webdriver.com.atlassian.confluence.pageobjects.DisplayOptionPanel;
import it.webdriver.com.atlassian.confluence.pageobjects.JiraIssuesDialog;
import it.webdriver.com.atlassian.confluence.pageobjects.JiraIssuesPage;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.junit.Assert.*;

public class JiraIssuesWebDriverTest extends AbstractJiraIssuesSearchPanelWebDriverTest
{
    private static final List<String> LIST_TEST_COLUMN = Arrays.asList("Issue Type", "Resolved", "Summary", "Key");

    private static List<String> LIST_DEFAULT_COLUMN = Arrays.asList("Key", "Summary", "Issue Type", "Created", "Updated", "Due Date", "Assignee", "Reporter", "Priority", "Status", "Resolution");

    private static final String NO_ISSUES_COUNT_TEXT = "No issues found";
    private static final String ONE_ISSUE_COUNT_TEXT = "1 issue";
    private static final String MORE_ISSUES_COUNT_TEXT = "issues";

    @Test
    public void testDialogValidation()
    {
        jiraIssuesDialog = openJiraIssuesDialog();
        jiraIssuesDialog.pasteJqlSearch("status = open");
        jiraIssuesDialog.fillMaxIssues("20a");
        jiraIssuesDialog.uncheckKey("TSTT-5");
        assertTrue("Insert button is disabled", !jiraIssuesDialog.isInsertable());
    }

    @Test
    public void testColumnsAreDisableInCountMode()
    {
        jiraIssuesDialog = openJiraIssuesDialog()
                .pasteJqlSearch("status = open")
                .clickSearchButton();
        jiraIssuesDialog.getDisplayOptionPanel().clickDisplayTotalCount();
        jiraIssuesDialog.clickInsertDialog();
        waitUntilInlineMacroAppearsInEditor(editContentPage, JIRA_ISSUE_MACRO_NAME);
        MacroPlaceholder macroPlaceholder = editContentPage.getContent().macroPlaceholderFor(JIRA_ISSUE_MACRO_NAME).iterator().next();
        jiraIssuesDialog = openJiraIssuesDialogFromMacroPlaceholder(macroPlaceholder);

        assertTrue(jiraIssuesDialog.getDisplayOptionPanel().isColumnsDisabled());
    }

    @Test
    public void testSortIssueTable()
    {
        JiraIssuesPage page = createPageWithTableJiraIssueMacroAndJQL("project = TSTT");
        assertEquals(page.getIssuesTableColumns().size(), LIST_DEFAULT_COLUMN.size());

        String KeyValueAtFirstTimeLoad = page.getFirstRowValueOfSummay();
        page.clickColumnHeaderIssueTable("Summary");
        String keyAfterSort = page.getFirstRowValueOfSummay();
        assertNotSame(KeyValueAtFirstTimeLoad, keyAfterSort);
    }

    @Test
    public void testColumnNotSupportSortableInIssueTable()
    {
        jiraIssuesDialog = openJiraIssuesDialog();
        jiraIssuesDialog.inputJqlSearch("status = open");
        jiraIssuesDialog.clickSearchButton();
        jiraIssuesDialog.openDisplayOption();
        jiraIssuesDialog.getDisplayOptionPanel().addColumn("Linked Issues");
        jiraIssuesDialog.clickInsertDialog();
        waitUntilInlineMacroAppearsInEditor(editContentPage, JIRA_ISSUE_MACRO_NAME);
        editContentPage.save();
        JiraIssuesPage page = product.getPageBinder().bind(JiraIssuesPage.class);
        String keyValueAtFirstTime = page.getFirstRowValueOfSummay();
        page.clickColumnHeaderIssueTable("Linked Issues");
        String keyAfterSort = page.getFirstRowValueOfSummay();
        assertEquals(keyValueAtFirstTime, keyAfterSort);
    }

    @Test
    public void testJIMTableIsCachedOnPageReload()
    {
        ViewPage viewPage = createPageWithTableJiraIssueMacroAndJQL("project = TSTT");
        String issueSummary = "JIM cache test : issue created using rest";
        JiraIssueBean newIssue = new JiraIssueBean("10011", "1", issueSummary, "test desc");
        String id = JiraRestHelper.createIssue(newIssue);
        checkNotNull(id);

        product.refresh();
        Poller.waitUntilTrue(viewPage.contentVisibleCondition());
        assertFalse("JIM table was not cached. Content was: " + viewPage.getMainContent().getText(), viewPage.getMainContent().getText().contains(issueSummary));
        JiraRestHelper.deleteIssue(id);

    }

    /**
     * check JQL search field when input filter URL convert to JQL
     */
    @Test
    public void checkPasteFilterUrlInJQLSearchField()
    {
        jiraIssuesDialog = openJiraIssuesDialog();
        String filterQuery = "filter=10001";
        String filterURL = "http://127.0.0.1:11990/jira/issues/?" + filterQuery;
        jiraIssuesDialog.pasteJqlSearch(filterURL);

        Poller.waitUntilTrue(jiraIssuesDialog.getJQLSearchElement().timed().isEnabled());
        Poller.waitUntilTrue(jiraIssuesDialog.getSearchButton().timed().isEnabled());
        jiraIssuesDialog.clickJqlSearch();

        assertEquals(filterQuery, jiraIssuesDialog.getJqlSearch());
    }

    /**
     * check JQL search field when input filter JQL convert to JQL
     */
    @Test
    public void checkPasteFilterJqlInJQLSearchField()
    {
        jiraIssuesDialog = openJiraIssuesDialog();
        String filterQuery = "filter=10001";
        jiraIssuesDialog.pasteJqlSearch(filterQuery);

        Poller.waitUntilTrue(jiraIssuesDialog.getJQLSearchElement().timed().isEnabled());
        Poller.waitUntilTrue(jiraIssuesDialog.getSearchButton().timed().isEnabled());
        jiraIssuesDialog.clickJqlSearch();

        assertEquals(filterQuery, jiraIssuesDialog.getJqlSearch());
    }

    @Test
    public void checkColumnInDialog()
    {
        insertJiraIssueMacroWithEditColumn(LIST_TEST_COLUMN, "status=open");
        String htmlMacro = editContentPage.getContent().getHtml();
        assertTrue(htmlMacro.contains("data-macro-parameters=\"columns=type,resolutiondate,summary,key"));
    }

    @Test
    public void testRefreshCacheHaveDataChange()
    {
        JiraIssuesPage viewPage = createPageWithTableJiraIssueMacro();
        int currentIssuesCount = viewPage.getNumberOfIssuesInTable();

        JiraIssueBean newIssue = new JiraIssueBean("10000", "2", "New feature", "");
        String id = JiraRestHelper.createIssue(newIssue);
        checkNotNull(id);

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
    public void testReturnsFreshDataAfterUserEditsMacro()
    {
        ViewPage viewPage = createPageWithTableJiraIssueMacroAndJQL("project = TSTT");
        String issueSummary = "issue created using rest";
        JiraIssueBean newIssue = new JiraIssueBean("10011", "1", issueSummary, "test desc");
        String id = JiraRestHelper.createIssue(newIssue);
        checkNotNull(id);

        EditContentPage editPage = viewPage.edit();
        waitUntilInlineMacroAppearsInEditor(editPage, JIRA_ISSUE_MACRO_NAME);
        MacroPlaceholder macroPlaceholder = editPage.getContent().macroPlaceholderFor(JIRA_ISSUE_MACRO_NAME).iterator().next();
        JiraIssuesDialog jiraIssuesDialog = openJiraIssuesDialogFromMacroPlaceholder(macroPlaceholder);
        jiraIssuesDialog.clickSearchButton();
        Poller.waitUntilTrue(jiraIssuesDialog.resultsTableIsVisible());
        jiraIssuesDialog.clickInsertDialog();
        waitUntilInlineMacroAppearsInEditor(editPage, JIRA_ISSUE_MACRO_NAME);
        viewPage = editPage.save();

        Poller.waitUntilTrue(
                "Could not find issue summary. Content was: " + viewPage.getMainContent().getText() + ". Expected to find: " + issueSummary,
                viewPage.getMainContent().timed().hasText(issueSummary)
        );

        JiraRestHelper.deleteIssue(id);
    }

    @Test
    public void testIssueCountHaveDataChange()
    {
        String jql = "status=open";
        JiraIssuesPage viewPage = createPageWithCountJiraIssueMacro(jql);
        int oldIssuesCount = viewPage.getIssueCount();

        JiraIssueBean newIssue = new JiraIssueBean("10000", "2", "New feature", "");
        String id = JiraRestHelper.createIssue(newIssue);
        checkNotNull(id);

        viewPage = gotoPage(viewPage.getPageId());
        int newIssuesCount = viewPage.getIssueCount();
        assertEquals(oldIssuesCount + 1, newIssuesCount);

        JiraRestHelper.deleteIssue(id);
    }

    @Test
    public void checkColumnKeepingAfterSearch()
    {
        jiraIssuesDialog = openJiraIssuesDialog();
        jiraIssuesDialog.inputJqlSearch("status = open");
        jiraIssuesDialog.clickSearchButton();
        jiraIssuesDialog.openDisplayOption();

        DisplayOptionPanel displayOptionPanel = jiraIssuesDialog.getDisplayOptionPanel();
        List<String>  firstSelectedColumns = displayOptionPanel.getSelectedColumns();
        displayOptionPanel.removeSelectedColumn("Resolution");
        displayOptionPanel.removeSelectedColumn("Status");

        //Search again and check list columns after removed "Resolution" and "Status" columns
        jiraIssuesDialog.clickSearchButton();
        jiraIssuesDialog.openDisplayOption();
        List<String>  removedSelectedColumns = displayOptionPanel.getSelectedColumns();
        assertEquals(firstSelectedColumns.size() - 2, removedSelectedColumns.size());
        assertFalse(removedSelectedColumns.contains("Resolution"));
        assertFalse(removedSelectedColumns.contains("Status"));

        //Search again and check list columns after add "Status" column
        displayOptionPanel.addColumn("Status");
        jiraIssuesDialog.clickSearchButton();
        jiraIssuesDialog.openDisplayOption();
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
        String applinkId = ApplinkHelper.getPrimaryApplinkId(client, authArgs);
        String jimMarkup = "{jira:jqlQuery=status\\=open||serverId="+applinkId+"||server=oldInvalidName}";
        EditContentPage editPage = product.loginAndEdit(User.ADMIN, Page.TEST);
        editPage.getContent().setContent(jimMarkup);
        editPage.save();
        assertTrue(bindCurrentPageToJiraIssues().getNumberOfIssuesInTable() > 0);
    }

    @Test
    public void testCanInsertMacroWhenChangeTab()
    {
        jiraIssuesDialog = openJiraIssuesDialog();
        jiraIssuesDialog.inputJqlSearch("status = open");
        jiraIssuesDialog.clickSearchButton();

        //change to create issue panel to make disable insert button
        jiraIssuesDialog.selectMenuItem(2);

        //back again search panel
        jiraIssuesDialog.selectMenuItem(1);
        Poller.waitUntilTrue(jiraIssuesDialog.getInsertButton().timed().isEnabled());
        jiraIssuesDialog.clickInsertDialog();
        waitUntilInlineMacroAppearsInEditor(editContentPage, JIRA_ISSUE_MACRO_NAME);
        assertEquals(editContentPage.getContent().macroPlaceholderFor(JIRA_ISSUE_MACRO_NAME).size(), 1);
    }

    @Test
    public void checkColumnLoadDefaultWhenInsert()
    {
        insertJiraIssueMacroWithEditColumn(LIST_TEST_COLUMN, "status=open");
        jiraIssuesDialog = openJiraIssuesDialog();

        assertTrue(jiraIssuesDialog.getJqlSearch().equals(""));
        assertFalse(jiraIssuesDialog.getIssuesTable().isPresent());

        jiraIssuesDialog.inputJqlSearch("status = open");
        jiraIssuesDialog.clickSearchButton();
        jiraIssuesDialog.openDisplayOption();

        List<String> columns = jiraIssuesDialog.getDisplayOptionPanel().getSelectedColumns();
        assertEquals(columns.toString(), LIST_DEFAULT_COLUMN.toString());
    }

    @Test
    public void checkTableOptionEnableWhenChooseOneIssue()
    {
        jiraIssuesDialog = openJiraIssuesDialog();
        jiraIssuesDialog.inputJqlSearch("status=open");
        jiraIssuesDialog.clickSearchButton();

        jiraIssuesDialog.clickSelectAllIssueOption();
        jiraIssuesDialog.clickSelectIssueOption("TP-1");

        jiraIssuesDialog.openDisplayOption();
        DisplayOptionPanel displayOptionPanel = jiraIssuesDialog.getDisplayOptionPanel();
        assertTrue(displayOptionPanel.isInsertSingleIssueEnable());
        assertTrue(displayOptionPanel.isInsertTableIssueEnable());
        assertFalse(displayOptionPanel.isInsertCountIssueEnable());

        jiraIssuesDialog.clickSelectIssueOption("TP-2");
        assertTrue(displayOptionPanel.isInsertCountIssueEnable());
        assertTrue(displayOptionPanel.isInsertTableIssueEnable());
        assertFalse(displayOptionPanel.isInsertSingleIssueEnable());
    }

    @Test
    public void testInsertTableByKeyQuery()
    {
        jiraIssuesDialog = openJiraIssuesDialog();
        jiraIssuesDialog.inputJqlSearch("key = TP-1");
        jiraIssuesDialog.clickSearchButton();

        jiraIssuesDialog.openDisplayOption();
        jiraIssuesDialog.getDisplayOptionPanel().clickDisplayTable();

        jiraIssuesDialog.clickInsertDialog();
        waitUntilInlineMacroAppearsInEditor(editContentPage, JIRA_ISSUE_MACRO_NAME);
        editContentPage.save();
        JiraIssuesPage jiraIssuesPage = bindCurrentPageToJiraIssues();

        assertTrue(jiraIssuesPage.getIssuesTableElement().isPresent());
        assertFalse(jiraIssuesPage.getIssuesCountElement().isPresent());
        assertFalse(jiraIssuesPage.getRefreshedIconElement().isPresent());
    }

    @Test
    public void testNumOfColumnInViewMode()
    {
        EditContentPage editContentPage = insertJiraIssueMacroWithEditColumn(LIST_TEST_COLUMN, "status=open");
        editContentPage.save();
        JiraIssuesPage jiraIssuesPage = bindCurrentPageToJiraIssues();
        assertEquals(jiraIssuesPage.getIssuesTableColumns().size(), LIST_TEST_COLUMN.size());
    }


    private JiraIssuesPage createPageWithTableJiraIssueMacro()
    {
        return createPageWithTableJiraIssueMacroAndJQL("status=open");
    }

    private JiraIssuesPage createPageWithTableJiraIssueMacroAndJQL(String jql)
    {
        jiraIssuesDialog = openJiraIssuesDialog();
        jiraIssuesDialog.inputJqlSearch(jql);
        jiraIssuesDialog.clickSearchButton();

        EditContentPage editContentPage = jiraIssuesDialog.clickInsertDialog();
        waitUntilInlineMacroAppearsInEditor(editContentPage, JIRA_ISSUE_MACRO_NAME);
        editContentPage.save();
        return bindCurrentPageToJiraIssues();
    }

    private JiraIssuesPage createPageWithCountJiraIssueMacro(String jql)
    {
        jiraIssuesDialog = openJiraIssuesDialog();
        jiraIssuesDialog.inputJqlSearch(jql);
        jiraIssuesDialog.clickSearchButton();
        jiraIssuesDialog.getDisplayOptionPanel().clickDisplayTotalCount();
        EditContentPage editContentPage = jiraIssuesDialog.clickInsertDialog();
        waitUntilInlineMacroAppearsInEditor(editContentPage, JIRA_ISSUE_MACRO_NAME);
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
        jiraIssuesDialog = openJiraIssuesDialog();
        jiraIssuesDialog.inputJqlSearch(jql);
        jiraIssuesDialog.clickSearchButton();
        jiraIssuesDialog.openDisplayOption();

        //clean all column default and add new list column
        jiraIssuesDialog.cleanAllOptionColumn();
        DisplayOptionPanel displayOptionPanel = jiraIssuesDialog.getDisplayOptionPanel();
        for(String columnName : columnNames)
        {
            displayOptionPanel.addColumn(columnName);
        }

        EditContentPage editPage = jiraIssuesDialog.clickInsertDialog();
        waitUntilInlineMacroAppearsInEditor(editPage, JIRA_ISSUE_MACRO_NAME);
        EditorContent editorContent = editPage.getContent();
        List<MacroPlaceholder> listMacroChart = editorContent.macroPlaceholderFor(JIRA_ISSUE_MACRO_NAME);
        assertEquals(1, listMacroChart.size());

        return editPage;
    }
}
