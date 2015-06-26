package it.com.atlassian.confluence.plugins.webdriver.jiraissues.createpanel;

import com.atlassian.confluence.it.TestProperties;
import com.atlassian.confluence.webdriver.pageobjects.page.content.EditContentPage;
import com.atlassian.test.categories.OnDemandSuiteTest;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

@Category(OnDemandSuiteTest.class)
public class JiraCreatedSLATTest extends AbstractJiraCreatedPanelTest
{
    @Test
    public void testCreateIssue() throws Exception
    {
        jiraMacroCreatePanelDialog = openJiraMacroCreateNewIssuePanelFromMenu();
        jiraMacroCreatePanelDialog.waitUntilProjectLoaded(getProjectId(PROJECT_TSTT));

        jiraMacroCreatePanelDialog.selectProject(PROJECT_TSTT);
        jiraMacroCreatePanelDialog.setSummary("summary");

        EditContentPage editContentPage = jiraMacroCreatePanelDialog.insertIssue();
        editPage.getEditor().getContent().waitForInlineMacro(JIRA_ISSUE_MACRO_NAME);
        assertEquals(editContentPage.getEditor().getContent().macroPlaceholderFor(JIRA_ISSUE_MACRO_NAME).size(), 1);
    }

    @Test
    public void testProjectsAndIssueTypesLoaded() throws Exception
    {
        jiraMacroCreatePanelDialog = openJiraMacroCreateNewIssuePanelFromMenu();
        jiraMacroCreatePanelDialog.waitUntilProjectLoaded(getProjectId(PROJECT_TSTT));

        assertEquals(jiraMacroCreatePanelDialog.getAllProjects().size(), TestProperties.isOnDemandMode() ? 4 : 8);

        int numOfIssueType = TestProperties.isOnDemandMode() ? 6 : 7;

        jiraMacroCreatePanelDialog.selectProject(PROJECT_TP);
        assertEquals(jiraMacroCreatePanelDialog.getAllIssueTypes().size(), numOfIssueType);

        jiraMacroCreatePanelDialog.selectProject(PROJECT_TST);
        assertEquals(jiraMacroCreatePanelDialog.getAllIssueTypes().size(), numOfIssueType);
    }

    @Test
    public void testIssueTypeIsSubTaskNotExist() throws Exception
    {
        jiraMacroCreatePanelDialog = openJiraMacroCreateNewIssuePanelFromMenu();
        jiraMacroCreatePanelDialog.waitUntilProjectLoaded(getProjectId(PROJECT_TSTT));
        jiraMacroCreatePanelDialog.selectProject(PROJECT_TSTT);
        assertFalse(jiraMacroCreatePanelDialog.getAllIssueTypes().contains("Technical task"));
    }
}