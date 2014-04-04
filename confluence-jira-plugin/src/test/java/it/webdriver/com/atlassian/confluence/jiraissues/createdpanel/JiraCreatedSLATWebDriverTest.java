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

        jiraCreatedMacroDialog.selectProject(getProjectId(PROJECT_TSTT));
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

        jiraCreatedMacroDialog.waitUntilProjectLoaded(getProjectId(PROJECT_TP));
        assertEquals(project.getAllOptions().size(), TestProperties.isOnDemandMode() ? 3 : 8);

        int numOfIssueType = TestProperties.isOnDemandMode() ? 4 : 7;
        jiraCreatedMacroDialog.selectProject(getProjectId(PROJECT_TP));
        assertEquals(jiraCreatedMacroDialog.getIssuesType().getAllOptions().size(), numOfIssueType);

        jiraCreatedMacroDialog.selectProject(getProjectId(PROJECT_TST));
        assertEquals(jiraCreatedMacroDialog.getIssuesType().getAllOptions().size(), numOfIssueType);
    }

    @Test
    public void testIssueTypeIsSubTaskNotExist()
    {
        openJiraCreatedMacroDialog(true);

        SelectElement project = jiraCreatedMacroDialog.getProject();
        Poller.waitUntilTrue(project.timed().isEnabled());
        jiraCreatedMacroDialog.selectProject(getProjectId(PROJECT_TP));
        assertFalse(jiraCreatedMacroDialog.getIssuesType().getText().contains("Technical task"));
    }

}
