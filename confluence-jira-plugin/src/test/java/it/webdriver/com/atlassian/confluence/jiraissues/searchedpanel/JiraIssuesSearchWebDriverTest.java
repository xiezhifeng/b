package it.webdriver.com.atlassian.confluence.jiraissues.searchedpanel;

import com.atlassian.confluence.it.User;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class JiraIssuesSearchWebDriverTest extends AbstractJiraIssuesSearchPanelWebDriverTest
{
    private final String PROJECT_ONE_NAME = "Test Project One";
    private final String PROJECT_TWO_NAME = "Test Project Two";

    @Test
    public void testSearchWithButton()
    {
        createJiraProject("TSTT", PROJECT_ONE_NAME, "", "", User.ADMIN);
        createJiraProject("TST", PROJECT_TWO_NAME, "", "", User.ADMIN);

        createJiraIssue("test", IssueType.BUG, PROJECT_ONE_NAME);
        createJiraIssue("test", IssueType.TASK, PROJECT_TWO_NAME);

        search("test");
        assertTrue(jiraIssuesDialog.isIssueExistInSearchResult("TSTT-1"));
        assertTrue(jiraIssuesDialog.isIssueExistInSearchResult("TST-1"));
    }

    @Test
    public void testSearchWithEnter()
    {
        createJiraProject("TSTT", PROJECT_ONE_NAME, "", "", User.ADMIN);
        createJiraProject("TST", PROJECT_TWO_NAME, "", "", User.ADMIN);

        createJiraIssue("test", IssueType.BUG, PROJECT_ONE_NAME);
        createJiraIssue("test", IssueType.TASK, PROJECT_TWO_NAME);

        openJiraIssuesDialog();
        jiraIssuesDialog.inputJqlSearch("test");
        jiraIssuesDialog.sendReturnKeyToJqlSearch();
        assertTrue(jiraIssuesDialog.isIssueExistInSearchResult("TSTT-1"));
        assertTrue(jiraIssuesDialog.isIssueExistInSearchResult("TST-1"));
    }

    @Test
    public void testSearchWithJQL()
    {
        createJiraProject("TP", PROJECT_ONE_NAME, "", "", User.ADMIN);
        createJiraIssue("test", IssueType.NEW_FEATURE, PROJECT_ONE_NAME);

        search("project=TP");
        assertTrue(jiraIssuesDialog.isIssueExistInSearchResult("TP-2"));
        assertTrue(jiraIssuesDialog.isIssueExistInSearchResult("TP-1"));
    }

    @Test
    public void testSearchForAlphanumericIssueKey()
    {
        createJiraProject("TST", PROJECT_ONE_NAME, "", "", User.ADMIN);
        createJiraIssue("test", IssueType.NEW_FEATURE, PROJECT_ONE_NAME);

        search("TST-1");
        assertTrue(jiraIssuesDialog.isIssueExistInSearchResult("TST-1"));
    }

    @Test
    public void testSearchWithFilterHaveJQL()
    {
        createJiraProject("TSTT", PROJECT_ONE_NAME, "", "", User.ADMIN);

        for (int i = 0; i < 5; i++)
        {
            createJiraIssue("summary" + i, IssueType.BUG, PROJECT_ONE_NAME);
        }

        String filterId = createJiraFilter("All Open Bugs", "status=open", "");

        search(jiraDisplayUrl + "/issues/?filter=" + filterId);
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
