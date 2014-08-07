package it.webdriver.com.atlassian.confluence.jiraissues.createdpanel;

import com.atlassian.confluence.it.TestProperties;
import com.atlassian.confluence.pageobjects.page.content.EditContentPage;
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
        jiraCreatedMacroDialog.waitUntilProjectLoaded(getProjectId(PROJECT_TSTT));

        jiraCreatedMacroDialog.selectProject("Test Project");
        jiraCreatedMacroDialog.setSummary("summary");

        EditContentPage editContentPage = jiraCreatedMacroDialog.insertIssue();
        waitUntilInlineMacroAppearsInEditor(editContentPage, JIRA_ISSUE_MACRO_NAME);
        assertEquals(editContentPage.getEditor().getContent().macroPlaceholderFor(JIRA_ISSUE_MACRO_NAME).size(), 1);
    }

    @Test
    public void testProjectsAndIssueTypesLoaded()
    {
        openJiraCreatedMacroDialog(true);
        jiraCreatedMacroDialog.waitUntilProjectLoaded(getProjectId(PROJECT_TSTT));

        assertEquals(jiraCreatedMacroDialog.getAllProjects().size(), TestProperties.isOnDemandMode() ? 4 : 8);

        int numOfIssueType = TestProperties.isOnDemandMode() ? 6 : 7;
        jiraCreatedMacroDialog.selectProject(PROJECT_TP);
        assertEquals(jiraCreatedMacroDialog.getAllIssueTypes().size(), numOfIssueType);

        jiraCreatedMacroDialog.selectProject(PROJECT_TST);
        assertEquals(jiraCreatedMacroDialog.getAllIssueTypes().size(), numOfIssueType);
    }

    @Test
    public void testIssueTypeIsSubTaskNotExist()
    {
        openJiraCreatedMacroDialog(true);
        jiraCreatedMacroDialog.waitUntilProjectLoaded(getProjectId(PROJECT_TSTT));
        jiraCreatedMacroDialog.selectProject(PROJECT_TSTT);
        assertFalse(jiraCreatedMacroDialog.getAllIssueTypes().contains("Technical task"));
    }
}
