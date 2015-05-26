package it.webdriver.com.atlassian.confluence.jiraissues.searchedpanel;

import com.atlassian.confluence.it.Page;
import com.atlassian.confluence.it.TestProperties;

import com.atlassian.confluence.it.User;
import com.atlassian.confluence.pageobjects.page.content.ViewPage;
import com.atlassian.pageobjects.elements.query.Poller;
import it.webdriver.com.atlassian.confluence.helper.ApplinkHelper;
import org.apache.commons.httpclient.HttpStatus;
import org.hamcrest.core.StringContains;
import org.json.JSONException;
import org.junit.Assert;
import org.junit.Test;

import it.webdriver.com.atlassian.confluence.pageobjects.JiraIssuesPage;

import java.io.IOException;

import static com.google.common.base.Preconditions.checkNotNull;
import static it.webdriver.com.atlassian.confluence.helper.JiraRestHelper.createJiraFilter;
import static it.webdriver.com.atlassian.confluence.helper.JiraRestHelper.deleteJiraFilter;
import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class JiraIssuesSearchWebDriverTest extends AbstractJiraIssuesSearchPanelWebDriverTest
{
    @Test
    public void testSearchWithEnter()
    {
        openJiraIssuesDialog();
        jiraIssuesDialog.inputJqlSearch("test");
        jiraIssuesDialog.sendReturnKeyToJqlSearch();
        assertTrue(jiraIssuesDialog.isIssueExistInSearchResult("TSTT-1"));
        assertTrue(jiraIssuesDialog.isIssueExistInSearchResult("TST-1"));
    }

    @Test
    public void testSearchWithJQL()
    {
        search("project=TP");
        assertTrue(jiraIssuesDialog.isIssueExistInSearchResult("TP-2"));
        assertTrue(jiraIssuesDialog.isIssueExistInSearchResult("TP-1"));
    }

    @Test
    public void testSearchForAlphanumericIssueKey()
    {
        search("TST-1");
        assertTrue(jiraIssuesDialog.isIssueExistInSearchResult("TST-1"));
    }

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
        assertTrue(jiraIssuesDialog.isIssueExistInSearchResult("TSTT-5"));
        assertTrue(jiraIssuesDialog.isIssueExistInSearchResult("TSTT-4"));

        assertEquals(deleteJiraFilter(filterId, client), HttpStatus.SC_NO_CONTENT);
    }


    @Test
    public void testSearchWithFilterNotExist()
    {
        search(JIRA_DISPLAY_URL + "/issues/?filter=10002");
        Assert.assertThat(jiraIssuesDialog.getWarningMessage(), StringContains.containsString("The JIRA server didn't understand your search query."));
    }

    @Test
    public void testColumnNotSupportSortableInIssueTable()
    {
        jiraIssuesDialog = openJiraIssuesDialog();
        jiraIssuesDialog.inputJqlSearch("status = open");
        jiraIssuesDialog.clickSearchButton();
        jiraIssuesDialog.showDisplayOption();
        jiraIssuesDialog.getDisplayOptionPanel().addColumn("Linked Issues");
        jiraIssuesDialog.clickInsertDialog();
        waitUntilInlineMacroAppearsInEditor(editContentPage, JIRA_ISSUE_MACRO_NAME);
        editContentPage.getEditor().clickSaveAndWaitForPageChange();
        JiraIssuesPage page = product.getPageBinder().bind(JiraIssuesPage.class);
        String keyValueAtFirstTime = page.getFirstRowValueOfSummay();
        page.clickColumnHeaderIssueTable("Linked Issues");
        String keyAfterSort = page.getFirstRowValueOfSummay();
        Assert.assertEquals(keyValueAtFirstTime, keyAfterSort);
    }

    @Test
    public void testPasteUrlWithNoJiraServer()
    {
        openJiraIssuesDialog();
        jiraIssuesDialog.pasteJqlSearch("http://anotherserver.com/jira/browse/TST-1");
        Assert.assertThat(jiraIssuesDialog.getInfoMessage(), StringContains.containsString("No server found match with your URL.Click here to set this up"));

        jiraIssuesDialog.clickSearchButton();
        Assert.assertThat(jiraIssuesDialog.getInfoMessage(), StringContains.containsString("No server found match with your URL.Click here to set this up"));
        Assert.assertFalse(jiraIssuesDialog.isInsertable());
    }

    @Test
    public void testPasteUrlWithJiraServer() throws IOException, JSONException
    {
        //create another primary applink
        String jiraURL = "http://jira.test.com";
        String applinkId = ApplinkHelper.createAppLink(client, "TEST", authArgs, jiraURL, jiraURL, true);

        JiraIssuesPage jiraIssuesPage = createPageWithJiraIssueMacro(JIRA_DISPLAY_URL + "/browse/TST-1", true);
        Assert.assertTrue(jiraIssuesPage.isSingleContainText("Test bug"));
        ApplinkHelper.deleteApplink(client, applinkId, authArgs);
    }

    @Test
    public void testPasteUrlWithJiraServerNoPermission() throws IOException, JSONException
    {
        ApplinkHelper.removeAllAppLink(client, authArgs);
        //create oath applink
        String jiraURL = "http://jira.test.com";
        String applinkId = ApplinkHelper.createAppLink(client, "TEST", authArgs, jiraURL, jiraURL, false);
        ApplinkHelper.enableApplinkOauthMode(client, applinkId, authArgs);
        ApplinkHelper.setupAppLink(ApplinkHelper.ApplinkMode.TRUSTED, client, authArgs);

        Assert.assertTrue("Applink must be still existed", ApplinkHelper.isExistAppLink(client, authArgs));


        ViewPage viewPage = editContentPage.save();
        viewPage.edit();
        openJiraIssuesDialog();
        jiraIssuesDialog.pasteJqlSearch(jiraURL + "/browse/TST-1");
        Poller.waitUntil(jiraIssuesDialog.getInfoMessageElement().timed().getText(), StringContains.containsString("Login & Approve to retrieve data from TEST"), Poller.by(20000));
        Assert.assertFalse(jiraIssuesDialog.getSearchButton().isEnabled());
        ApplinkHelper.deleteApplink(client, applinkId, authArgs);
    }

    @Test
    public void testPasteXmlUrl()
    {
        JiraIssuesPage jiraIssuesPage = createPageWithJiraIssueMacro(JIRA_DISPLAY_URL + "/si/jira.issueviews:issue-xml/TST-1/TST-1.xml", true);
        Assert.assertTrue(jiraIssuesPage.isSingleContainText("Test bug"));
    }

}
