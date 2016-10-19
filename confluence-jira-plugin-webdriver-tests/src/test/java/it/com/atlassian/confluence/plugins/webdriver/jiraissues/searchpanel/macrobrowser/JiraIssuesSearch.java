package it.com.atlassian.confluence.plugins.webdriver.jiraissues.searchpanel.macrobrowser;

import com.atlassian.pageobjects.elements.query.Poller;
import com.atlassian.pageobjects.elements.timeout.TimeoutType;
import it.com.atlassian.confluence.plugins.webdriver.helper.ApplinkHelper;
import it.com.atlassian.confluence.plugins.webdriver.jiraissues.searchpanel.AbstractJiraIssueMacroSearchPanelTest;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.lang3.StringUtils;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import static it.com.atlassian.confluence.plugins.webdriver.helper.JiraRestHelper.deleteJiraFilter;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class JiraIssuesSearch extends AbstractJiraIssueMacroSearchPanelTest
{
    private String globalTestAppLinkId;

    @After
    public void clearAppLink() throws Exception
    {
        if (StringUtils.isNotEmpty(globalTestAppLinkId))
        {
            ApplinkHelper.deleteApplink(client, globalTestAppLinkId, getAuthQueryString());
        }
        globalTestAppLinkId = "";
    }

    @Test
    public void testSearchWithEnter() throws Exception
    {
        jiraMacroSearchPanelDialog = openJiraIssueSearchPanelDialogFromMacroBrowser();
        jiraMacroSearchPanelDialog.inputJqlSearch("test");
        jiraMacroSearchPanelDialog.sendReturnKeyToJqlSearch();
        Poller.waitUntilTrue(jiraMacroSearchPanelDialog.isIssueExistInSearchResult("TSTT-1"));
        Poller.waitUntilTrue(jiraMacroSearchPanelDialog.isIssueExistInSearchResult("TST-1"));
    }

    @Test
    public void testSearchWithJQL() throws Exception
    {
        openJiraIssueSearchPanelAndStartSearch("project=TP");
        Poller.waitUntilTrue(jiraMacroSearchPanelDialog.isIssueExistInSearchResult("TP-2"));
        Poller.waitUntilTrue(jiraMacroSearchPanelDialog.isIssueExistInSearchResult("TP-1"));
    }

    @Test
    public void testSearchForAlphanumericIssueKey() throws Exception
    {
        openJiraIssueSearchPanelAndStartSearch("TST-1");
        Poller.waitUntilTrue(jiraMacroSearchPanelDialog.isIssueExistInSearchResult("TST-1"));
    }

    @Test
    public void testSearchWithFilterEmptyJQL() throws Exception
    {
        String filterId = "10001";
        openJiraIssueSearchPanelAndStartSearch(JIRA_DISPLAY_URL + "/issues/?filter=" + filterId);
        Poller.waitUntilTrue(jiraMacroSearchPanelDialog.isIssueExistInSearchResult("TSTT-5"));
        Poller.waitUntilTrue(jiraMacroSearchPanelDialog.isIssueExistInSearchResult("TSTT-4"));
        assertEquals(deleteJiraFilter(filterId, client), HttpStatus.SC_NO_CONTENT);
    }

    @Test
    public void testSearchWithFilterNotExist() throws Exception
    {
        openJiraIssueSearchPanelAndStartSearch(JIRA_DISPLAY_URL + "/issues/?filter=10002");
        Poller.waitUntil(jiraMacroSearchPanelDialog.getWarningMessageElement().withTimeout(TimeoutType.SLOW_PAGE_LOAD).timed().getText(), Matchers.containsString("The JIRA server didn't understand your search query."));
    }

    @Test
    public void testPasteUrlWithNoJiraServer() throws Exception
    {
        jiraMacroSearchPanelDialog = openJiraIssueSearchPanelDialogFromMacroBrowser();
        jiraMacroSearchPanelDialog.pasteJqlSearch("http://anotherserver.com/jira/browse/TST-1");

        Poller.waitUntilTrue(jiraMacroSearchPanelDialog.hasInfoMessage());
        assertTrue(jiraMacroSearchPanelDialog.getInfoMessage().contains("No server found match with your URL.Click here to set this up"));
        jiraMacroSearchPanelDialog.clickSearchButton();
        Poller.waitUntilTrue(jiraMacroSearchPanelDialog.hasInfoMessage());
        assertTrue(jiraMacroSearchPanelDialog.getInfoMessage().contains("No server found match with your URL.Click here to set this up"));

        Poller.waitUntilTrue(jiraMacroSearchPanelDialog.hasInsertButton());
        Assert.assertFalse(jiraMacroSearchPanelDialog.isInsertable());
    }
}
