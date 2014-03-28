package it.webdriver.com.atlassian.confluence.jiraissues.searchedpanel;

import com.atlassian.confluence.it.TestProperties;
import com.atlassian.confluence.it.User;
import com.atlassian.confluence.plugins.jira.beans.JiraIssueBean;
import it.webdriver.com.atlassian.confluence.helper.JiraRestHelper;
import org.apache.commons.httpclient.HttpStatus;
import org.junit.Before;
import org.junit.Test;

import static com.google.common.base.Preconditions.checkNotNull;
import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class JiraIssuesSearchWebDriverTest extends AbstractJiraIssuesSearchPanelWebDriverTest
{
    private final String PROJECT_TSTT = "Project TSTT Name";
    private final String PROJECT_TST = "Project TST Name";
    private final String PROJECT_TP = "Project TP Name";

    private final int PROJECT_TSTT_ISSUE_COUNT = 5;
    private final int PROJECT_TST_ISSUE_COUNT = 1;
    private final int PROJECT_TP_ISSUE_COUNT = 2;

    @Before
    public void setUpJiraTestData() throws Exception
    {
        if (TestProperties.isOnDemandMode())
        {

            jiraProjects.put(PROJECT_TSTT, JiraRestHelper.createJiraProject("TSTT", PROJECT_TSTT, "", "", User.ADMIN, client));
            jiraProjects.put(PROJECT_TST, JiraRestHelper.createJiraProject("TST", PROJECT_TST, "", "", User.ADMIN, client));
            jiraProjects.put(PROJECT_TP, JiraRestHelper.createJiraProject("TP", PROJECT_TP, "", "", User.ADMIN, client));

            for (int i = 0; i < PROJECT_TSTT_ISSUE_COUNT; i++)
            {
                checkNotNull(JiraRestHelper.createIssue(new JiraIssueBean(
                        jiraProjects.get(PROJECT_TSTT).getProjectId(),
                        jiraProjects.get(PROJECT_TSTT).getProjectIssueTypes().get(JiraRestHelper.IssueType.BUG.toString()),
                        "test", "")));
            }

            for (int i = 0; i < PROJECT_TST_ISSUE_COUNT; i++)
            {
                checkNotNull(JiraRestHelper.createIssue(new JiraIssueBean(
                        jiraProjects.get(PROJECT_TST).getProjectId(),
                        jiraProjects.get(PROJECT_TST).getProjectIssueTypes().get(JiraRestHelper.IssueType.TASK.toString()),
                        "test", "")));
            }

            for (int i = 0; i < PROJECT_TP_ISSUE_COUNT; i++)
            {
                checkNotNull(JiraRestHelper.createIssue(new JiraIssueBean(
                        jiraProjects.get(PROJECT_TP).getProjectId(),
                        jiraProjects.get(PROJECT_TP).getProjectIssueTypes().get(JiraRestHelper.IssueType.NEW_FEATURE.toString()),
                        "test", "")));
            }
        }
    }

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
        String filterId = "10000";

        if (TestProperties.isOnDemandMode())
        {
            filterId = JiraRestHelper.createJiraFilter("All Open Bugs", "status=open", "", client);
            checkNotNull(filterId);
        }

        search(jiraDisplayUrl + "/issues/?filter=" + filterId);
        assertTrue(jiraIssuesDialog.isIssueExistInSearchResult("TSTT-5"));
        assertTrue(jiraIssuesDialog.isIssueExistInSearchResult("TSTT-4"));

        assertEquals(JiraRestHelper.deleteJiraFilter(filterId, client), HttpStatus.SC_NO_CONTENT);
    }

    @Test
    public void testSearchWithFilterNotExist()
    {
        search(jiraDisplayUrl + "/issues/?filter=10002");
        assertTrue(jiraIssuesDialog.getWarningMessage().contains("The JIRA server didn't understand your search query."));
    }

    @Test
    public void testSearchWithFilterEmptyJQL()
    {
        String filterId = "10001";

        if (TestProperties.isOnDemandMode())
        {
            filterId = JiraRestHelper.createJiraFilter("All Open Bugs", "", "", client);
            checkNotNull(filterId);
        }

        search(jiraDisplayUrl + "/issues/?filter=" + filterId);
        assertTrue(jiraIssuesDialog.isIssueExistInSearchResult("TSTT-5"));
        assertTrue(jiraIssuesDialog.isIssueExistInSearchResult("TSTT-4"));

        assertEquals(JiraRestHelper.deleteJiraFilter(filterId, client), HttpStatus.SC_NO_CONTENT);
    }
}
