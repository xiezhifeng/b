package it.com.atlassian.confluence.plugins.webdriver.jiraissues.searchpanel;

import com.atlassian.confluence.it.TestProperties;
import com.atlassian.confluence.plugins.helper.ApplinkHelper;
import com.atlassian.confluence.plugins.pageobjects.JiraIssuesPage;
import org.apache.commons.httpclient.HttpStatus;
import org.hamcrest.core.StringContains;
import org.json.JSONException;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;

import static com.atlassian.confluence.plugins.helper.JiraRestHelper.createJiraFilter;
import static com.atlassian.confluence.plugins.helper.JiraRestHelper.deleteJiraFilter;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class JiraIssuesSearchTest extends AbstractJiraIssuesSearchPanelTest
{
    @Test
    public void testSearchWithEnter()
    {
        openJiraIssuesDialog();
        jiraMacroSearchPanelDialog.inputJqlSearch("test");
        jiraMacroSearchPanelDialog.sendReturnKeyToJqlSearch();
        assertTrue(jiraMacroSearchPanelDialog.isIssueExistInSearchResult("TSTT-1"));
        assertTrue(jiraMacroSearchPanelDialog.isIssueExistInSearchResult("TST-1"));
    }

    @Test
    public void testSearchWithJQL()
    {
        search("project=TP");
        assertTrue(jiraMacroSearchPanelDialog.isIssueExistInSearchResult("TP-2"));
        assertTrue(jiraMacroSearchPanelDialog.isIssueExistInSearchResult("TP-1"));
    }

    @Test
    public void testSearchForAlphanumericIssueKey()
    {
        search("TST-1");
        assertTrue(jiraMacroSearchPanelDialog.isIssueExistInSearchResult("TST-1"));
    }

    /* This test failed due to failed functionality */
    @Ignore
    @Test
    public void testSearchWithFilterEmptyJQL()
    {
        String filterId = "10001";

        if (TestProperties.isOnDemandMode())
        {
            filterId = createJiraFilter("All Open Bugs", "", "", client);
            checkNotNull(filterId);
        }

        search(JIRA_DISPLAY_URL + "/issues/?filter=" + filterId);
        assertTrue(jiraMacroSearchPanelDialog.isIssueExistInSearchResult("TSTT-5"));
        assertTrue(jiraMacroSearchPanelDialog.isIssueExistInSearchResult("TSTT-4"));

        assertEquals(deleteJiraFilter(filterId, client), HttpStatus.SC_NO_CONTENT);
    }

    @Test
    public void testSearchWithFilterNotExist()
    {
        search(JIRA_DISPLAY_URL + "/issues/?filter=10002");
        Assert.assertThat(jiraMacroSearchPanelDialog.getWarningMessage(), StringContains.containsString("The JIRA server didn't understand your search query."));
    }

    @Test
    public void testColumnNotSupportSortableInIssueTable()
    {
        jiraMacroSearchPanelDialog = openJiraIssuesDialog();
        jiraMacroSearchPanelDialog.inputJqlSearch("status = open");
        jiraMacroSearchPanelDialog.clickSearchButton();
        jiraMacroSearchPanelDialog.openDisplayOption();
        jiraMacroSearchPanelDialog.getDisplayOptionPanel().addColumn("Linked Issues");
        jiraMacroSearchPanelDialog.clickInsertDialog();
        waitUntilInlineMacroAppearsInEditor(editPage, JIRA_ISSUE_MACRO_NAME);
        editPage.getEditor().clickSaveAndWaitForPageChange();
        JiraIssuesPage page = product.getPageBinder().bind(JiraIssuesPage.class);
        String keyValueAtFirstTime = page.getFirstRowValueOfSummay();
        page.clickColumnHeaderIssueTable("Linked Issues");
        String keyAfterSort = page.getFirstRowValueOfSummay();
        assertEquals(keyValueAtFirstTime, keyAfterSort);
    }

    @Test
    public void testPasteUrlWithNoJiraServer()
    {
        openJiraIssuesDialog();
        jiraMacroSearchPanelDialog.pasteJqlSearch("http://anotherserver.com/jira/browse/TST-1");
        Assert.assertThat(jiraMacroSearchPanelDialog.getInfoMessage(), StringContains.containsString("No server found match with your URL.Click here to set this up"));

        jiraMacroSearchPanelDialog.clickSearchButton();
        Assert.assertThat(jiraMacroSearchPanelDialog.getInfoMessage(), StringContains.containsString("No server found match with your URL.Click here to set this up"));
        Assert.assertFalse(jiraMacroSearchPanelDialog.isInsertable());
    }

    @Test
    public void testPasteUrlWithJiraServer() throws IOException, JSONException
    {
        //create another primary applink
        String jiraURL = "http://jira.test.com";
        String authArgs = getAuthQueryString();
        String appLinkId = ApplinkHelper.createAppLink(client, "TEST", authArgs, jiraURL, jiraURL, true);

        JiraIssuesPage jiraIssuesPage = createPageWithJiraIssueMacro(JIRA_DISPLAY_URL + "/browse/TST-1", true);
        Assert.assertTrue(jiraIssuesPage.isSingleContainText("Test bug"));

        ApplinkHelper.deleteApplink(client, appLinkId, authArgs);
    }

    @Test
    public void testPasteUrlWithJiraServerNoPermission() throws IOException, JSONException
    {
        //create oath applink
        String jiraURL = "http://jira.test.com";
        String authArgs = getAuthQueryString();
        String appLinkId = ApplinkHelper.createAppLink(client, "TEST", authArgs, jiraURL, jiraURL, false);
        ApplinkHelper.enableApplinkOauthMode(client, appLinkId, authArgs);

        product.refresh();
        openJiraIssuesDialog();
        jiraMacroSearchPanelDialog.pasteJqlSearch(jiraURL + "/browse/TST-1");
        Assert.assertThat(jiraMacroSearchPanelDialog.getInfoMessage(), StringContains.containsString("Login & Approve to retrieve data from TEST"));
        Assert.assertFalse(jiraMacroSearchPanelDialog.getSearchButton().isEnabled());

        ApplinkHelper.deleteApplink(client, appLinkId, authArgs);
    }

    @Test
    public void testPasteXmlUrl()
    {
        JiraIssuesPage jiraIssuesPage = createPageWithJiraIssueMacro(JIRA_DISPLAY_URL + "/si/jira.issueviews:issue-xml/TST-1/TST-1.xml", true);
        Assert.assertTrue(jiraIssuesPage.isSingleContainText("Test bug"));
    }

}
