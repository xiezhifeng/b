package it.webdriver.com.atlassian.confluence.jiraissues.searchedpanel;

import com.atlassian.confluence.it.TestProperties;

import org.apache.commons.httpclient.HttpStatus;
import org.junit.Test;

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
        assertTrue(jiraIssuesDialog.getWarningMessage().contains("The JIRA server didn't understand your search query."));
    }
}
