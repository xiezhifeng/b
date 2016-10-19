package it.com.atlassian.confluence.plugins.webdriver.jiraissues.searchpanel.macrobrowser;

import com.atlassian.pageobjects.elements.timeout.TimeoutType;
import it.com.atlassian.confluence.plugins.webdriver.helper.ApplinkHelper;
import it.com.atlassian.confluence.plugins.webdriver.jiraissues.searchpanel.AbstractJiraIssueMacroSearchPanelTest;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Test;

import static com.atlassian.pageobjects.elements.query.Poller.waitUntil;
import static com.atlassian.pageobjects.elements.query.Poller.waitUntilTrue;
import static it.com.atlassian.confluence.plugins.webdriver.helper.JiraRestHelper.deleteJiraFilter;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;

public class JiraIssuesSearchMacroBrowserTest extends AbstractJiraIssueMacroSearchPanelTest
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
        waitUntilTrue(jiraMacroSearchPanelDialog.isIssueExistInSearchResult("TSTT-1"));
        waitUntilTrue(jiraMacroSearchPanelDialog.isIssueExistInSearchResult("TST-1"));
    }

    @Test
    public void testSearchWithJQL() throws Exception
    {
        openJiraIssueSearchPanelAndStartSearch("project=TP");
        waitUntilTrue(jiraMacroSearchPanelDialog.isIssueExistInSearchResult("TP-2"));
        waitUntilTrue(jiraMacroSearchPanelDialog.isIssueExistInSearchResult("TP-1"));
    }

    @Test
    public void testSearchForAlphanumericIssueKey() throws Exception
    {
        openJiraIssueSearchPanelAndStartSearch("TST-1");
        waitUntilTrue(jiraMacroSearchPanelDialog.isIssueExistInSearchResult("TST-1"));
    }

    @Test
    public void testSearchWithFilterEmptyJQL() throws Exception
    {
        String filterId = "10001";
        openJiraIssueSearchPanelAndStartSearch(JIRA_DISPLAY_URL + "/issues/?filter=" + filterId);
        waitUntilTrue(jiraMacroSearchPanelDialog.isIssueExistInSearchResult("TSTT-5"));
        waitUntilTrue(jiraMacroSearchPanelDialog.isIssueExistInSearchResult("TSTT-4"));
        assertEquals(deleteJiraFilter(filterId, client), HttpStatus.SC_NO_CONTENT);
    }

    @Test
    public void testSearchWithFilterNotExist() throws Exception
    {
        openJiraIssueSearchPanelAndStartSearch(JIRA_DISPLAY_URL + "/issues/?filter=10002");
        waitUntil(
                jiraMacroSearchPanelDialog.getWarningMessageElement().withTimeout(TimeoutType.SLOW_PAGE_LOAD).timed().getText(),
                containsString("The JIRA server didn't understand your search query.")
        );
    }

    @Test
    public void testPasteUrlWithNoJiraServer() throws Exception
    {
        jiraMacroSearchPanelDialog = openJiraIssueSearchPanelDialogFromMacroBrowser();
        jiraMacroSearchPanelDialog.pasteJqlSearch("http://anotherserver.com/jira/browse/TST-1");

        waitUntilTrue(jiraMacroSearchPanelDialog.hasInfoMessage());
        assertThat(jiraMacroSearchPanelDialog.getInfoMessage(), containsString("No server found match with your URL.Click here to set this up"));
        jiraMacroSearchPanelDialog.clickSearchButton();
        waitUntilTrue(jiraMacroSearchPanelDialog.hasInfoMessage());
        assertThat(jiraMacroSearchPanelDialog.getInfoMessage(), containsString("No server found match with your URL.Click here to set this up"));

        waitUntilTrue(jiraMacroSearchPanelDialog.hasInsertButton());
        assertFalse(jiraMacroSearchPanelDialog.isInsertable());
    }
}
