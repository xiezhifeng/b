package it.webdriver.com.atlassian.confluence.jiraissues.searchedpanel;

import com.atlassian.confluence.it.User;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class JiraIssuesSearchWebDriverTest extends AbstractJiraIssuesSearchPanelWebDriverTest
{
    private final String PROJECT_ONE_NAME = "Test Project One";
    private final String PROJECT_TWO_NAME = "Test Project Two";

    @Before
    public void setupJiraTestData() throws Exception
    {
        createJiraProject("TSTT", PROJECT_ONE_NAME, "", "", User.ADMIN);
        createJiraProject("TST", PROJECT_TWO_NAME, "", "", User.ADMIN);
    }

    @Test
    public void testSearchWithButton()
    {
        createJiraIssue("test", IssueType.BUG, PROJECT_ONE_NAME);
        createJiraIssue("test", IssueType.TASK, PROJECT_TWO_NAME);

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
        search(jiraDisplayUrl + "/issues/?filter=10000");
        assertTrue(jiraIssuesDialog.isIssueExistInSearchResult("TSTT-5"));
        assertTrue(jiraIssuesDialog.isIssueExistInSearchResult("TSTT-4"));
    }

    @Test
    public void testSearchWithFilterEmptyJQL()
    {
        search(jiraDisplayUrl + "/issues/?filter=10001");
        assertTrue(jiraIssuesDialog.isIssueExistInSearchResult("TSTT-5"));
        assertTrue(jiraIssuesDialog.isIssueExistInSearchResult("TSTT-4"));
    }

    @Test
    public void testSearchWithFilterNotExist()
    {
        search(jiraDisplayUrl + "/issues/?filter=10002");
        assertTrue(jiraIssuesDialog.getWarningMessage().contains("The JIRA server didn't understand your search query."));
    }
}
