package it.com.atlassian.confluence.plugins.webdriver.jiraissues.searchpanel;

import com.atlassian.confluence.plugins.helper.ApplinkHelper;
import com.atlassian.confluence.plugins.helper.JiraRestHelper;
import com.atlassian.confluence.plugins.jira.beans.JiraIssueBean;
import com.atlassian.confluence.plugins.pageobjects.DisplayOptionPanel;
import com.atlassian.confluence.plugins.pageobjects.JiraIssuesPage;
import com.atlassian.confluence.plugins.pageobjects.jirachart.PieChartDialog;
import com.atlassian.confluence.plugins.pageobjects.jiraissuefillter.JiraMacroSearchPanelDialog;
import com.atlassian.confluence.webdriver.pageobjects.component.dialog.MacroBrowserDialog;
import com.atlassian.confluence.webdriver.pageobjects.component.editor.MacroPlaceholder;
import com.atlassian.confluence.webdriver.pageobjects.page.content.EditContentPage;
import com.atlassian.confluence.webdriver.pageobjects.page.content.ViewPage;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.Poller;
import com.atlassian.test.categories.OnDemandAcceptanceTest;
import org.json.JSONException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.IOException;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.junit.Assert.*;

public class JiraIssuesTest extends AbstractJiraIssuesSearchPanelTest
{
    private static final String NO_ISSUES_COUNT_TEXT = "No issues found";
    private static final String ONE_ISSUE_COUNT_TEXT = "1 issue";
    private static final String MORE_ISSUES_COUNT_TEXT = "issues";

    private PieChartDialog pieChartDialog;
    private String globalAppLinkId;

    private PieChartDialog openJiraChartMacroDialog()
    {
        MacroBrowserDialog macroBrowserDialog = openMacroBrowser(editPage);
        macroBrowserDialog.searchForFirst("jira chart").select();
        return this.product.getPageBinder().bind(PieChartDialog.class);
    }

    @Test
    @Category(OnDemandAcceptanceTest.class)
    public void testJiraChartMacroLink()
    {
        jiraMacroSearchPanelDialog = openJiraIssuesDialog();
        checkNotNull(jiraMacroSearchPanelDialog.getJiraChartMacroAnchor());
        assertEquals(jiraMacroSearchPanelDialog.getJiraChartMacroAnchor().getAttribute("class"), "item-button jira-left-panel-link");
        pieChartDialog = jiraMacroSearchPanelDialog.clickJiraChartMacroAnchor();
        assertEquals(pieChartDialog.getJiraIssuesMacroAnchor().getAttribute("class"), "item-button jira-left-panel-link");
    }

    @Test
    public void testDialogValidation()
    {
        jiraMacroSearchPanelDialog = openJiraIssuesDialog();
        jiraMacroSearchPanelDialog.pasteJqlSearch("status = open");
        jiraMacroSearchPanelDialog.fillMaxIssues("20a");
        jiraMacroSearchPanelDialog.uncheckKey("TSTT-5");
        assertTrue("Insert button is disabled", !jiraMacroSearchPanelDialog.isInsertable());
    }

    @Test
    public void testColumnsAreDisableInCountMode()
    {
        jiraMacroSearchPanelDialog = ((JiraMacroSearchPanelDialog)
                openJiraIssuesDialog().pasteJqlSearch("status = open")).clickSearchButton();

        jiraMacroSearchPanelDialog.openDisplayOption();
        jiraMacroSearchPanelDialog.getDisplayOptionPanel().clickDisplayTotalCount();
        jiraMacroSearchPanelDialog.clickInsertDialog();
        waitUntilInlineMacroAppearsInEditor(editPage, JIRA_ISSUE_MACRO_NAME);
        MacroPlaceholder macroPlaceholder = editPage.getEditor().getContent().macroPlaceholderFor(JIRA_ISSUE_MACRO_NAME).iterator().next();
        jiraMacroSearchPanelDialog = openJiraIssuesDialogFromMacroPlaceholder(editPage, macroPlaceholder);

        assertTrue(jiraMacroSearchPanelDialog.getDisplayOptionPanel().isColumnsDisabled());
    }

    @Test
    public void testJIMTableIsCachedOnPageReload() throws Exception
    {
        ViewPage viewPage = createPageWithJiraIssueMacro("project = TSTT");
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
        jiraMacroSearchPanelDialog = openJiraIssuesDialog();
        String filterQuery = "filter=10001";
        String filterURL = "http://127.0.0.1:11990/jira/issues/?" + filterQuery;
        jiraMacroSearchPanelDialog.pasteJqlSearch(filterURL);

        Poller.waitUntilTrue(jiraMacroSearchPanelDialog.getJQLSearchElement().timed().isEnabled());
        Poller.waitUntilTrue(jiraMacroSearchPanelDialog.getSearchButton().timed().isEnabled());
        jiraMacroSearchPanelDialog.clickJqlSearch();

        assertEquals(filterQuery, jiraMacroSearchPanelDialog.getJqlSearch());
    }

    /**
     * check JQL search field when input filter JQL convert to JQL
     */
    @Test
    public void checkPasteFilterJqlInJQLSearchField()
    {
        jiraMacroSearchPanelDialog = openJiraIssuesDialog();
        String filterQuery = "filter=10001";
        jiraMacroSearchPanelDialog.pasteJqlSearch(filterQuery);

        Poller.waitUntilTrue(jiraMacroSearchPanelDialog.getJQLSearchElement().timed().isEnabled());
        Poller.waitUntilTrue(jiraMacroSearchPanelDialog.getSearchButton().timed().isEnabled());
        jiraMacroSearchPanelDialog.clickJqlSearch();

        assertEquals(filterQuery, jiraMacroSearchPanelDialog.getJqlSearch());
    }

    @Test
    public void checkColumnInDialog()
    {
        insertJiraIssueMacroWithEditColumn(LIST_TEST_COLUMN, "status=open");
        String htmlMacro = editPage.getEditor().getContent().getTimedHtml().now();
        assertTrue(htmlMacro.contains("data-macro-parameters=\"columns=type,resolutiondate,summary,key"));
    }

    @Test
    public void testRefreshCacheHaveDataChange() throws Exception
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
    public void testReturnsFreshDataAfterUserEditsMacro() throws Exception
    {
        ViewPage viewPage = createPageWithJiraIssueMacro("project = TSTT");
        String issueSummary = "issue created using rest";
        JiraIssueBean newIssue = new JiraIssueBean("10011", "1", issueSummary, "test desc");
        String id = JiraRestHelper.createIssue(newIssue);
        checkNotNull(id);

        EditContentPage editPage = viewPage.edit();
        waitUntilInlineMacroAppearsInEditor(editPage, JIRA_ISSUE_MACRO_NAME);
        MacroPlaceholder macroPlaceholder = editPage.getEditor().getContent().macroPlaceholderFor(JIRA_ISSUE_MACRO_NAME).iterator().next();
        JiraMacroSearchPanelDialog jiraIssuesDialog = openJiraIssuesDialogFromMacroPlaceholder(editPage, macroPlaceholder);
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
    public void testIssueCountHaveDataChange() throws Exception
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
        jiraMacroSearchPanelDialog = openJiraIssuesDialog();
        jiraMacroSearchPanelDialog.inputJqlSearch("status = open");
        jiraMacroSearchPanelDialog.clickSearchButton();
        jiraMacroSearchPanelDialog.openDisplayOption();

        DisplayOptionPanel displayOptionPanel = jiraMacroSearchPanelDialog.getDisplayOptionPanel();
        List<String>  firstSelectedColumns = displayOptionPanel.getSelectedColumns();
        displayOptionPanel.removeSelectedColumn("Resolution");
        displayOptionPanel.removeSelectedColumn("Status");

        //Search again and check list columns after removed "Resolution" and "Status" columns
        jiraMacroSearchPanelDialog.clickSearchButton();
        jiraMacroSearchPanelDialog.openDisplayOption();
        List<String>  removedSelectedColumns = displayOptionPanel.getSelectedColumns();
        assertEquals(firstSelectedColumns.size() - 2, removedSelectedColumns.size());
        assertFalse(removedSelectedColumns.contains("Resolution"));
        assertFalse(removedSelectedColumns.contains("Status"));

        //Search again and check list columns after add "Status" column
        displayOptionPanel.addColumn("Status");
        jiraMacroSearchPanelDialog.clickSearchButton();
        jiraMacroSearchPanelDialog.openDisplayOption();
        List<String>  addedSelectedColumns = displayOptionPanel.getSelectedColumns();
        assertTrue(addedSelectedColumns.contains("Status"));
    }

    @Test
    public void testNoIssuesCountText()
    {
        JiraIssuesPage jiraIssuesPage = createPageWithJiraIssueMacro("status=Reopened");
        assertEquals(NO_ISSUES_COUNT_TEXT, jiraIssuesPage.getNumberOfIssuesText());
    }

    @Test
    public void testOneIssueResultText()
    {
        JiraIssuesPage jiraIssuesPage = createPageWithJiraIssueMacro("project = TST");
        assertEquals(ONE_ISSUE_COUNT_TEXT, jiraIssuesPage.getNumberOfIssuesText());
    }

    @Test
    public void testMoreIssueResultText()
    {
        JiraIssuesPage jiraIssuesPage = createPageWithJiraIssueMacro("status=Open");
        assertTrue(jiraIssuesPage.getNumberOfIssuesText().contains(MORE_ISSUES_COUNT_TEXT));
    }

    @Test
    public void testChangeApplinkName()
    {
        String authArgs = getAuthQueryString();
        String applinkId = ApplinkHelper.getPrimaryApplinkId(client, authArgs);
        String jimMarkup = "{jira:jqlQuery=status\\=open||serverId="+applinkId+"||server=oldInvalidName}";

        editPage.getEditor().getContent().setContent(jimMarkup);
        editPage.save();
        assertTrue(bindCurrentPageToJiraIssues().getNumberOfIssuesInTable() > 0);
    }

    @Test
    public void testCanInsertMacroWhenChangeTab()
    {
        jiraMacroSearchPanelDialog = openJiraIssuesDialog();
        jiraMacroSearchPanelDialog.inputJqlSearch("status = open");
        jiraMacroSearchPanelDialog.clickSearchButton();

        //change to create issue panel to make disable insert button
        jiraMacroSearchPanelDialog.selectMenuItem(2);

        //back again search panel
        jiraMacroSearchPanelDialog.selectMenuItem(1);
        Poller.waitUntilTrue(jiraMacroSearchPanelDialog.getInsertButton().timed().isEnabled());
        jiraMacroSearchPanelDialog.clickInsertDialog();
        waitUntilInlineMacroAppearsInEditor(editPage, JIRA_ISSUE_MACRO_NAME);
        assertEquals(editPage.getEditor().getContent().macroPlaceholderFor(JIRA_ISSUE_MACRO_NAME).size(), 1);
    }



    @Test
    public void checkTableOptionEnableWhenChooseOneIssue()
    {
        jiraMacroSearchPanelDialog = openJiraIssuesDialog();
        jiraMacroSearchPanelDialog.inputJqlSearch("status=open");
        jiraMacroSearchPanelDialog.clickSearchButton();

        jiraMacroSearchPanelDialog.clickSelectAllIssueOption();
        jiraMacroSearchPanelDialog.clickSelectIssueOption("TP-1");

        jiraMacroSearchPanelDialog.openDisplayOption();
        DisplayOptionPanel displayOptionPanel = jiraMacroSearchPanelDialog.getDisplayOptionPanel();
        assertTrue(displayOptionPanel.isInsertSingleIssueEnable());
        assertTrue(displayOptionPanel.isInsertTableIssueEnable());
        assertFalse(displayOptionPanel.isInsertCountIssueEnable());

        jiraMacroSearchPanelDialog.clickSelectIssueOption("TP-2");
        assertTrue(displayOptionPanel.isInsertCountIssueEnable());
        assertTrue(displayOptionPanel.isInsertTableIssueEnable());
        assertFalse(displayOptionPanel.isInsertSingleIssueEnable());
    }

    @Test
    public void testInsertTableByKeyQuery()
    {
        jiraMacroSearchPanelDialog = openJiraIssuesDialog();
        jiraMacroSearchPanelDialog.inputJqlSearch("key = TP-1");
        jiraMacroSearchPanelDialog.clickSearchButton();

        jiraMacroSearchPanelDialog.openDisplayOption();
        jiraMacroSearchPanelDialog.getDisplayOptionPanel().clickDisplayTable();

        jiraMacroSearchPanelDialog.clickInsertDialog();
        waitUntilInlineMacroAppearsInEditor(editPage, JIRA_ISSUE_MACRO_NAME);
        editPage.save();
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

    @Test
    public void testSingleErrorJiraLink() throws IOException, JSONException
    {
        JiraIssuesPage jiraIssuesPage = setupErrorEnv("key=TEST");
        PageElement jiraErrorLink = jiraIssuesPage.getJiraErrorLink();

        Assert.assertEquals("TEST", jiraErrorLink.getText());
        Assert.assertEquals("http://test.jira.com/browse/TEST?src=confmacro", jiraErrorLink.getAttribute("href"));

        ApplinkHelper.deleteApplink(client, globalAppLinkId, getAuthQueryString());
    }

    @Test
    public void testTableErrorJiraLink() throws IOException, JSONException
    {
        JiraIssuesPage jiraIssuesPage = setupErrorEnv("status=open");
        PageElement jiraErrorLink = jiraIssuesPage.getJiraErrorLink();

        Assert.assertTrue(jiraIssuesPage.getErrorMessage().hasClass("jim-error-message-table"));
        Assert.assertEquals("View these issues in JIRA", jiraErrorLink.getText());
        Assert.assertEquals("http://test.jira.com/secure/IssueNavigator.jspa?reset=true&jqlQuery=status%3Dopen&src=confmacro", jiraErrorLink.getAttribute("href"));

        ApplinkHelper.deleteApplink(client, globalAppLinkId, getAuthQueryString());
    }

    @Test
    public void testCountErrorJiraLink() throws IOException, JSONException
    {
        JiraIssuesPage jiraIssuesPage = setupErrorEnv("status=open|count=true");
        PageElement jiraErrorLink = jiraIssuesPage.getJiraErrorLink();

        Assert.assertTrue(jiraIssuesPage.getErrorMessage().hasClass("jim-error-message-table"));
        Assert.assertEquals("View these issues in JIRA", jiraErrorLink.getText());
        Assert.assertEquals("http://test.jira.com/secure/IssueNavigator.jspa?reset=true&jqlQuery=status%3Dopen&src=confmacro", jiraErrorLink.getAttribute("href"));

        ApplinkHelper.deleteApplink(client, globalAppLinkId, getAuthQueryString());
    }

    private JiraIssuesPage setupErrorEnv(String jql) throws IOException, JSONException
    {
        String authArgs = getAuthQueryString();
        String applinkId = ApplinkHelper.createAppLink(client, "jira_applink", authArgs, "http://test.jira.com", "http://test.jira.com", false);
        globalAppLinkId = applinkId;
        createMacroPlaceholderFromQueryString(editPage, "{jiraissues:" + jql + "|serverId=" + applinkId + "}");
        waitUntilInlineMacroAppearsInEditor(editPage, OLD_JIRA_ISSUE_MACRO_NAME);
        editPage.save();

        return bindCurrentPageToJiraIssues();
    }

    @Test
    public void testXSSInViewMode()
    {
        EditContentPage editContentPage = insertJiraIssueMacroWithEditColumn(LIST_DEFAULT_COLUMN, "status=open");
        editContentPage.save();
        JiraIssuesPage jiraIssuesPage = bindCurrentPageToJiraIssues();
        Assert.assertTrue(jiraIssuesPage.getFirstRowValueOfAssignee().contains("<script>alert('Administrator')</script>admin"));
    }

    private JiraIssuesPage createPageWithTableJiraIssueMacro()
    {
        return createPageWithJiraIssueMacro("status=open");
    }

    private JiraIssuesPage createPageWithCountJiraIssueMacro(String jql)
    {
        jiraMacroSearchPanelDialog = openJiraIssuesDialog();
        jiraMacroSearchPanelDialog.inputJqlSearch(jql);
        jiraMacroSearchPanelDialog.clickSearchButton();
        jiraMacroSearchPanelDialog.openDisplayOption();
        jiraMacroSearchPanelDialog.getDisplayOptionPanel().clickDisplayTotalCount();
        EditContentPage editContentPage = jiraMacroSearchPanelDialog.clickInsertDialog();
        waitUntilInlineMacroAppearsInEditor(editContentPage, JIRA_ISSUE_MACRO_NAME);
        editContentPage.save();
        return bindCurrentPageToJiraIssues();
    }

    private JiraIssuesPage gotoPage(Long pageId)
    {
        product.viewPage(String.valueOf(pageId));
        return bindCurrentPageToJiraIssues();
    }
}
