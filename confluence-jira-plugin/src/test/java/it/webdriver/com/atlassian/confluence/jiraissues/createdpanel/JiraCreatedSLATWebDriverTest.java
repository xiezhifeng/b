package it.webdriver.com.atlassian.confluence.jiraissues.createdpanel;

import com.atlassian.confluence.it.TestProperties;
import com.atlassian.confluence.it.User;
import com.atlassian.confluence.pageobjects.page.content.EditContentPage;
import com.atlassian.confluence.plugins.jira.beans.JiraIssueBean;
import com.atlassian.pageobjects.elements.SelectElement;
import com.atlassian.pageobjects.elements.query.Poller;
import com.atlassian.test.categories.OnDemandSuiteTest;
import it.webdriver.com.atlassian.confluence.helper.JiraRestHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static com.google.common.base.Preconditions.checkNotNull;
import static it.webdriver.com.atlassian.confluence.helper.JiraRestHelper.createIssue;
import static it.webdriver.com.atlassian.confluence.helper.JiraRestHelper.createJiraProject;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

@Category(OnDemandSuiteTest.class)
public class JiraCreatedSLATWebDriverTest extends AbstractJiraCreatedPanelWebDriverTest
{

    private static final String PROJECT_TSTT = "Project TSTT Name";
    private static final String PROJECT_TST = "Project TST Name";
    private static final String PROJECT_TP = "Project TP Name";

    private static final int PROJECT_TSTT_ISSUE_COUNT = 5;
    private static final int PROJECT_TST_ISSUE_COUNT = 1;
    private static final int PROJECT_TP_ISSUE_COUNT = 2;

    @Before
    public void setUpJiraTestData() throws Exception
    {
        if (TestProperties.isOnDemandMode())
        {

            jiraProjects.put(PROJECT_TSTT, createJiraProject("TSTT", PROJECT_TSTT, "", "", User.ADMIN, client));
            jiraProjects.put(PROJECT_TST, createJiraProject("TST", PROJECT_TST, "", "", User.ADMIN, client));
            jiraProjects.put(PROJECT_TP, createJiraProject("TP", PROJECT_TP, "", "", User.ADMIN, client));

            for (int i = 0; i < PROJECT_TSTT_ISSUE_COUNT; i++)
            {
                checkNotNull(createIssue(new JiraIssueBean(
                        jiraProjects.get(PROJECT_TSTT).getProjectId(),
                        jiraProjects.get(PROJECT_TSTT).getProjectIssueTypes().get(JiraRestHelper.IssueType.BUG.toString()),
                        "test", "")));
            }

            for (int i = 0; i < PROJECT_TST_ISSUE_COUNT; i++)
            {
                checkNotNull(createIssue(new JiraIssueBean(
                        jiraProjects.get(PROJECT_TST).getProjectId(),
                        jiraProjects.get(PROJECT_TST).getProjectIssueTypes().get(JiraRestHelper.IssueType.TASK.toString()),
                        "test", "")));
            }

            for (int i = 0; i < PROJECT_TP_ISSUE_COUNT; i++)
            {
                checkNotNull(createIssue(new JiraIssueBean(
                        jiraProjects.get(PROJECT_TP).getProjectId(),
                        jiraProjects.get(PROJECT_TP).getProjectIssueTypes().get(JiraRestHelper.IssueType.NEW_FEATURE.toString()),
                        "test", "")));
            }
        }
    }

    @Test
    public void testCreateIssue()
    {
        openJiraCreatedMacroDialog(true);

        SelectElement project = jiraCreatedMacroDialog.getProject();
        Poller.waitUntilTrue(project.timed().isEnabled());
        jiraCreatedMacroDialog.selectProject(jiraProjects.get(PROJECT_TP).getProjectId());
        jiraCreatedMacroDialog.setSummary("summary");
        if(!TestProperties.isOnDemandMode())
        {
            jiraCreatedMacroDialog.setReporter("admin");
        }
        EditContentPage editContentPage = jiraCreatedMacroDialog.insertIssue();
        waitUntilInlineMacroAppearsInEditor(editContentPage, JIRA_ISSUE_MACRO_NAME);
        assertEquals(editContentPage.getEditor().getContent().macroPlaceholderFor(JIRA_ISSUE_MACRO_NAME).size(), 1);
    }

    @Test
    public void testProjectsLoaded()
    {
        openJiraCreatedMacroDialog(true);
        SelectElement project = jiraCreatedMacroDialog.getProject();


        if(TestProperties.isOnDemandMode())
        {
            jiraCreatedMacroDialog.waitUntilProjectLoaded(jiraProjects.get(PROJECT_TP).getProjectId());
            assertEquals(project.getAllOptions().size(), 3);

            jiraCreatedMacroDialog.selectProject(jiraProjects.get(PROJECT_TP).getProjectId());
            assertEquals(jiraCreatedMacroDialog.getIssuesType().getAllOptions().size(), 4);

            jiraCreatedMacroDialog.selectProject(jiraProjects.get(PROJECT_TST).getProjectId());
            assertEquals(jiraCreatedMacroDialog.getIssuesType().getAllOptions().size(), 4);
        }
        else
        {
            jiraCreatedMacroDialog.waitUntilProjectLoaded("10011");
            assertEquals(project.getAllOptions().size(), 8);

            jiraCreatedMacroDialog.selectProject("10011");
            assertEquals(jiraCreatedMacroDialog.getIssuesType().getAllOptions().size(), 4);

            jiraCreatedMacroDialog.selectProject("10000");
            assertEquals(jiraCreatedMacroDialog.getIssuesType().getAllOptions().size(), 7);
        }
    }

    @Test
    public void testIssueTypeIsSubTaskNotExist()
    {
        openJiraCreatedMacroDialog(true);

        SelectElement project = jiraCreatedMacroDialog.getProject();
        Poller.waitUntilTrue(project.timed().isEnabled());
        if(TestProperties.isOnDemandMode())
        {
            jiraCreatedMacroDialog.selectProject(jiraProjects.get(PROJECT_TP).getProjectId());
        }
        else
        {
            jiraCreatedMacroDialog.selectProject("10120");
        }

        assertFalse(jiraCreatedMacroDialog.getIssuesType().getText().contains("Technical task"));
    }

}
