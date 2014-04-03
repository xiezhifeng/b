package it.webdriver.com.atlassian.confluence.jiraissues.createdpanel;

import com.atlassian.confluence.it.TestProperties;
import com.atlassian.confluence.pageobjects.page.content.EditContentPage;
import com.atlassian.pageobjects.elements.SelectElement;
import com.atlassian.pageobjects.elements.query.Poller;
import com.atlassian.test.categories.OnDemandSuiteTest;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

@Category(OnDemandSuiteTest.class)
public class JiraCreatedSLATWebDriverTest extends AbstractJiraCreatedPanelWebDriverTest
{

    @Test
    public void testCreateIssue()
    {
        openJiraCreatedMacroDialog(true);

        SelectElement project = jiraCreatedMacroDialog.getProject();
        Poller.waitUntilTrue(project.timed().isEnabled());

        String projectId = TestProperties.isOnDemandMode() ? jiraProjects.get(PROJECT_TST).getProjectId() : "10011";

        jiraCreatedMacroDialog.selectProject(projectId);
        jiraCreatedMacroDialog.setSummary("summary");

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
