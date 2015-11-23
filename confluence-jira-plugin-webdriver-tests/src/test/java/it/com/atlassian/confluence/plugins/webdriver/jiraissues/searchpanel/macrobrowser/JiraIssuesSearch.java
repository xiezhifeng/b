package it.com.atlassian.confluence.plugins.webdriver.jiraissues.searchpanel.macrobrowser;

import it.com.atlassian.confluence.plugins.webdriver.helper.ApplinkHelper;
import com.atlassian.pageobjects.elements.query.Poller;
import com.atlassian.pageobjects.elements.timeout.TimeoutType;

import it.com.atlassian.confluence.plugins.webdriver.jiraissues.searchpanel.AbstractJiraIssuesSearchPanelWithoutSavingTest;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.lang.math.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Test;

import static it.com.atlassian.confluence.plugins.webdriver.helper.JiraRestHelper.createJiraFilter;
import static it.com.atlassian.confluence.plugins.webdriver.helper.JiraRestHelper.deleteJiraFilter;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class JiraIssuesSearch extends AbstractJiraIssuesSearchPanelWithoutSavingTest
{
    private String globalTestAppLinkId;

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

    @Test
    public void testSearchWithEnter() throws Exception
    {
        dialogSearchPanel = openJiraIssueSearchPanelDialogFromMacroBrowser(editPage);
        dialogSearchPanel.inputJqlSearch("test");
        dialogSearchPanel.sendReturnKeyToJqlSearch();
        Poller.waitUntilTrue(dialogSearchPanel.isIssueExistInSearchResult("TSTT-1"));
        Poller.waitUntilTrue(dialogSearchPanel.isIssueExistInSearchResult("TST-1"));
    }

    @Test
    public void testSearchWithJQL() throws Exception
    {
        openJiraIssueSearchPanelAndStartSearch("project=TP");
        Poller.waitUntilTrue(dialogSearchPanel.isIssueExistInSearchResult("TP-2"));
        Poller.waitUntilTrue(dialogSearchPanel.isIssueExistInSearchResult("TP-1"));
    }

    @Test
    public void testSearchForAlphanumericIssueKey() throws Exception
    {
        openJiraIssueSearchPanelAndStartSearch("TST-1");
        Poller.waitUntilTrue(dialogSearchPanel.isIssueExistInSearchResult("TST-1"));
    }

    @Test
    public void testSearchWithFilterEmptyJQL() throws Exception
    {
        String filterId = createJiraFilter("All Open Bugs-" + RandomUtils.nextLong(), "", "", client);
        checkNotNull(filterId);

        openJiraIssueSearchPanelAndStartSearch(JIRA_DISPLAY_URL + "/issues/?filter=" + filterId);
        Poller.waitUntilTrue(dialogSearchPanel.isIssueExistInSearchResult("TSTT-5"));
        Poller.waitUntilTrue(dialogSearchPanel.isIssueExistInSearchResult("TSTT-4"));

        assertEquals(deleteJiraFilter(filterId, client), HttpStatus.SC_NO_CONTENT);
    }

    @Test
    public void testSearchWithFilterNotExist() throws Exception
    {
        openJiraIssueSearchPanelAndStartSearch(JIRA_DISPLAY_URL + "/issues/?filter=10002");
        Poller.waitUntil(dialogSearchPanel.getWarningMessageElement().withTimeout(TimeoutType.SLOW_PAGE_LOAD).timed().getText(), Matchers.containsString("The JIRA server didn't understand your search query."));
    }

    @Test
    public void testPasteUrlWithNoJiraServer() throws Exception
    {
        dialogSearchPanel = openJiraIssueSearchPanelDialogFromMacroBrowser(editPage);
        dialogSearchPanel.pasteJqlSearch("http://anotherserver.com/jira/browse/TST-1");

        Poller.waitUntilTrue(dialogSearchPanel.hasInfoMessage());
        assertTrue(dialogSearchPanel.getInfoMessage().contains("No server found match with your URL.Click here to set this up"));
        dialogSearchPanel.clickSearchButton();
        Poller.waitUntilTrue(dialogSearchPanel.hasInfoMessage());
        assertTrue(dialogSearchPanel.getInfoMessage().contains("No server found match with your URL.Click here to set this up"));

        Poller.waitUntilFalse("Insert button is disabled", dialogSearchPanel.isInsertButtonEnabledTimed());
    }
}
