package it.com.atlassian.confluence.plugins.webdriver.jiraissues.searchpanel.pageview;

import com.atlassian.confluence.plugins.jira.beans.JiraIssueBean;
import com.atlassian.confluence.webdriver.pageobjects.component.editor.MacroPlaceholder;
import com.atlassian.confluence.webdriver.pageobjects.page.content.EditContentPage;
import com.atlassian.confluence.webdriver.pageobjects.page.content.ViewPage;
import com.atlassian.gzipfilter.org.apache.commons.lang.StringUtils;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.Poller;
import it.com.atlassian.confluence.plugins.webdriver.helper.ApplinkHelper;
import it.com.atlassian.confluence.plugins.webdriver.helper.JiraRestHelper;
import it.com.atlassian.confluence.plugins.webdriver.jiraissues.searchpanel.AbstractJiraIssuesSearchPanelTest;
import it.com.atlassian.confluence.plugins.webdriver.pageobjects.JiraIssuesPage;
import it.com.atlassian.confluence.plugins.webdriver.pageobjects.jiraissuefillter.JiraMacroSearchPanelDialog;
import org.json.JSONException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class JiraIssues extends AbstractJiraIssuesSearchPanelTest
{
    protected String globalTestAppLinkId;


    @After
    public void tearDown() throws Exception
    {
        if (StringUtils.isNotEmpty(globalTestAppLinkId))
        {
            ApplinkHelper.deleteApplink(client, globalTestAppLinkId, getAuthQueryString());
        }
        globalTestAppLinkId = "";
        super.tearDown();
    }

    protected JiraIssuesPage createPageWithTableJiraIssueMacro() throws Exception
    {
        return createPageWithJiraIssueMacro("status=open");
    }

    protected JiraIssuesPage createPageWithCountJiraIssueMacro(String jql) throws Exception
    {
        jiraMacroSearchPanelDialog = openJiraIssueSearchPanelDialogFromMacroBrowser(editPage);
        jiraMacroSearchPanelDialog.inputJqlSearch(jql);
        jiraMacroSearchPanelDialog.clickSearchButton();
        jiraMacroSearchPanelDialog.openDisplayOption();
        jiraMacroSearchPanelDialog.getDisplayOptionPanel().clickDisplayTotalCount();
        EditContentPage editContentPage = jiraMacroSearchPanelDialog.clickInsertDialog();
        editPage.getEditor().getContent().waitForInlineMacro(JIRA_ISSUE_MACRO_NAME);
        editContentPage.save();
        return bindCurrentPageToJiraIssues();
    }

    protected JiraIssuesPage gotoPage(Long pageId)
    {
        product.viewPage(String.valueOf(pageId));
        return bindCurrentPageToJiraIssues();
    }

    protected JiraIssuesPage setupErrorEnv(String jql) throws IOException, JSONException
    {
        String authArgs = getAuthQueryString();
        String applinkId = ApplinkHelper.createAppLink(client, "jira_applink", authArgs, "http://test.jira.com", "http://test.jira.com", false);
        globalTestAppLinkId = applinkId;
        createMacroPlaceholderFromQueryString(editPage, "{jiraissues:" + jql + "|serverId=" + applinkId + "}", OLD_JIRA_ISSUE_MACRO_NAME);
        editPage.save();

        return bindCurrentPageToJiraIssues();
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
        product.refresh();
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
        viewPage.clickRefreshedIcon();
        product.refresh();
    }

    @Test
    public void testRefreshCacheHaveSameData() throws Exception
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
        editPage.getEditor().getContent().waitForInlineMacro(JIRA_ISSUE_MACRO_NAME);

        MacroPlaceholder macroPlaceholder = editPage.getEditor().getContent().macroPlaceholderFor(JIRA_ISSUE_MACRO_NAME).iterator().next();
        JiraMacroSearchPanelDialog jiraIssuesDialog = openJiraIssuesDialogFromMacroPlaceholder(editPage, macroPlaceholder);
        jiraIssuesDialog.clickSearchButton();
        Poller.waitUntilTrue(jiraIssuesDialog.resultsTableIsVisible());
        jiraIssuesDialog.clickInsertDialog();
        editPage.getEditor().getContent().waitForInlineMacro(JIRA_ISSUE_MACRO_NAME);
        viewPage = editPage.save();

        Poller.waitUntilTrue(
                "Could not find issue summary. Content was: " + viewPage.getMainContent().getText() + ". Expected to find: " + issueSummary,
                viewPage.getMainContent().timed().hasText(issueSummary)
        );

        JiraRestHelper.deleteIssue(id);
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
    public void testInsertTableByKeyQuery() throws Exception
    {
        jiraMacroSearchPanelDialog = openJiraIssueSearchPanelDialogFromMacroBrowser(editPage);
        jiraMacroSearchPanelDialog.inputJqlSearch("key = TP-1");
        jiraMacroSearchPanelDialog.clickSearchButton();

        jiraMacroSearchPanelDialog.openDisplayOption();
        jiraMacroSearchPanelDialog.getDisplayOptionPanel().clickDisplayTable();

        jiraMacroSearchPanelDialog.clickInsertDialog();
        editPage.getEditor().getContent().waitForInlineMacro(JIRA_ISSUE_MACRO_NAME);
        editPage.save();
        JiraIssuesPage jiraIssuesPage = bindCurrentPageToJiraIssues();

        assertTrue(jiraIssuesPage.getIssuesTableElement().isPresent());
        assertFalse(jiraIssuesPage.getIssuesCountElement().isPresent());
        assertFalse(jiraIssuesPage.getRefreshedIconElement().isPresent());
    }

    @Test
    public void testNumOfColumnInViewMode() throws Exception
    {
        EditContentPage editContentPage = insertJiraIssueMacroWithEditColumn(LIST_TEST_COLUMN, "status=open");
        editContentPage.save();
        JiraIssuesPage jiraIssuesPage = bindCurrentPageToJiraIssues();
        assertEquals(jiraIssuesPage.getIssuesTableColumns().size(), LIST_TEST_COLUMN.size());
    }

    @Test
    public void testXSSInViewMode() throws Exception
    {
        EditContentPage editContentPage = insertJiraIssueMacroWithEditColumn(LIST_DEFAULT_COLUMN, "status=open");
        editContentPage.save();
        JiraIssuesPage jiraIssuesPage = bindCurrentPageToJiraIssues();
        Assert.assertTrue(jiraIssuesPage.getFirstRowValueOfAssignee().contains("<script>alert('Administrator')</script>admin"));
    }

    @Test
    public void testSingleErrorJiraLink() throws IOException, JSONException
    {
        JiraIssuesPage jiraIssuesPage = setupErrorEnv("key=TEST");
        PageElement jiraErrorLink = jiraIssuesPage.getJiraErrorLink();

        Assert.assertEquals("TEST", jiraErrorLink.getText());
        Assert.assertEquals("http://test.jira.com/browse/TEST?src=confmacro", jiraErrorLink.getAttribute("href"));

        ApplinkHelper.deleteApplink(client, globalTestAppLinkId, getAuthQueryString());
        globalTestAppLinkId = null;
    }

    @Test
    public void testTableErrorJiraLink() throws IOException, JSONException
    {
        JiraIssuesPage jiraIssuesPage = setupErrorEnv("status=open");
        PageElement jiraErrorLink = jiraIssuesPage.getJiraErrorLink();

        Assert.assertTrue(jiraIssuesPage.getErrorMessage().hasClass("jim-error-message-table"));
        Assert.assertEquals("View these issues in JIRA", jiraErrorLink.getText());
        Assert.assertEquals("http://test.jira.com/secure/IssueNavigator.jspa?reset=true&jqlQuery=status%3Dopen&src=confmacro", jiraErrorLink.getAttribute("href"));

        ApplinkHelper.deleteApplink(client, globalTestAppLinkId, getAuthQueryString());
        globalTestAppLinkId = null;
    }

    @Test
    public void testJIMTableErrorWithWrongJQL() throws Exception
    {
        EditContentPage editContentPage = gotoEditTestPage(user.get());
        editContentPage.getEditor().getContent().setContent("{jira:project = TSTTTTT}");
        editContentPage.getEditor().getContent().waitForInlineMacro(JIRA_ISSUE_MACRO_NAME);
        editContentPage.save();

        JiraIssuesPage jiraIssuesPage = bindCurrentPageToJiraIssues();
        Poller.waitUntilTrue("JIM table contains an error", jiraIssuesPage.getErrorMessage().timed().hasClass("jim-error-message-table"));
    }

    @Test
    public void testCountErrorJiraLink() throws IOException, JSONException
    {
        JiraIssuesPage jiraIssuesPage = setupErrorEnv("status=open|count=true");
        PageElement jiraErrorLink = jiraIssuesPage.getJiraErrorLink();

        Assert.assertTrue(jiraIssuesPage.getErrorMessage().hasClass("jim-error-message-table"));
        Assert.assertEquals("View these issues in JIRA", jiraErrorLink.getText());
        Assert.assertEquals("http://test.jira.com/secure/IssueNavigator.jspa?reset=true&jqlQuery=status%3Dopen&src=confmacro", jiraErrorLink.getAttribute("href"));

        ApplinkHelper.deleteApplink(client, globalTestAppLinkId, getAuthQueryString());
        globalTestAppLinkId = null;
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

    @Test
    public void testMultiValueFieldsForTableMode() throws Exception
    {
        EditContentPage editContentPage = insertJiraIssueMacroWithEditColumn(LIST_MULTIVALUE_COLUMN, "key IN (TP-1, TP-2)");
        editContentPage.save();
        JiraIssuesPage jiraIssuesPage = bindCurrentPageToJiraIssues();

        // Multiple values case
        String component = jiraIssuesPage.getValueInTable(4, 1);
        assertEquals("Components are not correct", "Component 1, Component 2, Component 3", component);
        String fixVersion = jiraIssuesPage.getValueInTable(5, 1);
        assertEquals("Fix Versions are not correct", "1.0, 1.1", fixVersion);

        // Single values case
        component = jiraIssuesPage.getValueInTable(4, 2);
        assertEquals("Component is not correct", "Component 1", component);
        fixVersion = jiraIssuesPage.getValueInTable(5, 2);
        assertEquals("Fix Version is not correct", "1.1", fixVersion);
    }
}
