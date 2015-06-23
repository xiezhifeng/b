package it.com.atlassian.confluence.plugins.webdriver.jiraissues.createpanel;

import com.atlassian.confluence.it.TestProperties;
import com.atlassian.confluence.webdriver.pageobjects.page.content.EditContentPage;
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
        jiraMacroCreatePanelDialog = openJiraCreatedMacroDialog(true);
        jiraMacroCreatePanelDialog.waitUntilProjectLoaded(getProjectId(PROJECT_TSTT));

        jiraMacroCreatePanelDialog.selectProject("Test Project");
        jiraMacroCreatePanelDialog.setSummary("summary");

        EditContentPage editContentPage = jiraMacroCreatePanelDialog.insertIssue();
        waitUntilInlineMacroAppearsInEditor(editContentPage, JIRA_ISSUE_MACRO_NAME);
        assertEquals(editContentPage.getEditor().getContent().macroPlaceholderFor(JIRA_ISSUE_MACRO_NAME).size(), 1);
    }

    @Test
    public void testProjectsAndIssueTypesLoaded()
    {
        jiraMacroCreatePanelDialog = openJiraCreatedMacroDialog(true);
        jiraMacroCreatePanelDialog.waitUntilProjectLoaded(getProjectId(PROJECT_TSTT));

        assertEquals(jiraMacroCreatePanelDialog.getAllProjects().size(), TestProperties.isOnDemandMode() ? 4 : 8);

        int numOfIssueType = TestProperties.isOnDemandMode() ? 6 : 7;
        jiraMacroCreatePanelDialog.selectProject(PROJECT_TP);
        assertEquals(jiraMacroCreatePanelDialog.getAllIssueTypes().size(), numOfIssueType);

        jiraMacroCreatePanelDialog.selectProject(PROJECT_TST);
        assertEquals(jiraMacroCreatePanelDialog.getAllIssueTypes().size(), numOfIssueType);
    }

    @Test
    public void testIssueTypeIsSubTaskNotExist()
    {
        jiraMacroCreatePanelDialog = openJiraCreatedMacroDialog(true);
        jiraMacroCreatePanelDialog.waitUntilProjectLoaded(getProjectId(PROJECT_TSTT));
        jiraMacroCreatePanelDialog.selectProject(PROJECT_TSTT);
        assertFalse(jiraMacroCreatePanelDialog.getAllIssueTypes().contains("Technical task"));
    }
}
