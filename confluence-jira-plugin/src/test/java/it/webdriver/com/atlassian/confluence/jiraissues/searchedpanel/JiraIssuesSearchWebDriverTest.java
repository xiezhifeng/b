package it.webdriver.com.atlassian.confluence.jiraissues.searchedpanel;

import com.atlassian.confluence.it.TestProperties;
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
        // Create test data for OD instances. This can be removed once we update all the tests to not longer rely on
        // the backup data and we remove the .zip file
        if (TestProperties.isOnDemandMode())
        {
            createJiraProject("TSTT", PROJECT_ONE_NAME, "", "", User.ADMIN);
            createJiraProject("TST", PROJECT_TWO_NAME, "", "", User.ADMIN);

            createJiraIssue("test", IssueType.BUG, PROJECT_ONE_NAME);
            createJiraIssue("test", IssueType.TASK, PROJECT_TWO_NAME);
        }

        search("test");
        assertTrue(jiraIssuesDialog.isIssueExistInSearchResult("TSTT-1"));
        assertTrue(jiraIssuesDialog.isIssueExistInSearchResult("TST-1"));
    }

    @Test
    public void testSearchWithEnter()
    {
        if (TestProperties.isOnDemandMode())
        {
            createJiraProject("TSTT", PROJECT_ONE_NAME, "", "", User.ADMIN);
            createJiraProject("TST", PROJECT_TWO_NAME, "", "", User.ADMIN);

            createJiraIssue("test", IssueType.BUG, PROJECT_ONE_NAME);
            createJiraIssue("test", IssueType.TASK, PROJECT_TWO_NAME);
        }

        openJiraIssuesDialog();
        jiraIssuesDialog.inputJqlSearch("test");
        jiraIssuesDialog.sendReturnKeyToJqlSearch();
        assertTrue(jiraIssuesDialog.isIssueExistInSearchResult("TSTT-1"));
        assertTrue(jiraIssuesDialog.isIssueExistInSearchResult("TST-1"));
    }

    @Test
    public void testSearchWithJQL()
    {
        if (TestProperties.isOnDemandMode())
        {
            createJiraProject("TP", PROJECT_ONE_NAME, "", "", User.ADMIN);
            createJiraIssue("test", IssueType.NEW_FEATURE, PROJECT_ONE_NAME);
        }

        search("project=TP");
        assertTrue(jiraIssuesDialog.isIssueExistInSearchResult("TP-2"));
        assertTrue(jiraIssuesDialog.isIssueExistInSearchResult("TP-1"));
    }

    @Test
    public void testSearchForAlphanumericIssueKey()
    {
        if (TestProperties.isOnDemandMode())
        {
            createJiraProject("TST", PROJECT_ONE_NAME, "", "", User.ADMIN);
            createJiraIssue("test", IssueType.NEW_FEATURE, PROJECT_ONE_NAME);
        }

        search("TST-1");
        assertTrue(jiraIssuesDialog.isIssueExistInSearchResult("TST-1"));
    }

    @Test
    public void testSearchWithFilterHaveJQL()
    {
        String filterId = "10000";

        if (TestProperties.isOnDemandMode())
        {
            createJiraProject("TSTT", PROJECT_ONE_NAME, "", "", User.ADMIN);

            for (int i = 0; i < 5; i++)
            {
                createJiraIssue("summary" + i, IssueType.BUG, PROJECT_ONE_NAME);
            }

            filterId = createJiraFilter("All Open Bugs", "status=open", "");
        }

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
