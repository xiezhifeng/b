package it.webdriver.com.atlassian.confluence.jiraissues.searchedpanel;

import com.atlassian.test.categories.OnDemandSuiteTest;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.junit.Assert.assertTrue;

@Category(OnDemandSuiteTest.class)
public class JiraIssuesSearchWebDriverTest extends AbstractJiraIssuesSearchPanelWebDriverTest
{

    @Test
    public void testSearchWithButton()
    {
        search("test");
        assertTrue(jiraIssuesDialog.isIssueExistInSearchResult("TSTT-1"));
        assertTrue(jiraIssuesDialog.isIssueExistInSearchResult("TST-1"));
    }

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
    public void testSearchWithFilterHaveJQL()
    {
        search(JIRA_DISPLAY_URL + "/issues/?filter=10000");
        assertTrue(jiraIssuesDialog.isIssueExistInSearchResult("TSTT-5"));
        assertTrue(jiraIssuesDialog.isIssueExistInSearchResult("TSTT-4"));
    }

    @Test
    public void testSearchWithFilterEmptyJQL()
    {
        search(JIRA_DISPLAY_URL + "/issues/?filter=10001");
        assertTrue(jiraIssuesDialog.isIssueExistInSearchResult("TSTT-5"));
        assertTrue(jiraIssuesDialog.isIssueExistInSearchResult("TSTT-4"));
    }

    @Test
    public void testSearchWithFilterNotExist()
    {
        search(JIRA_DISPLAY_URL + "/issues/?filter=10002");
        assertTrue(jiraIssuesDialog.getWarningMessage().contains("The JIRA server didn't understand your search query."));
    }
}
