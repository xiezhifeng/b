package it.com.atlassian.confluence.plugins.webdriver.jiraissues.createpanel;

import com.atlassian.util.concurrent.NotNull;
import it.com.atlassian.confluence.plugins.webdriver.AbstractJiraTest;

import com.atlassian.confluence.webdriver.pageobjects.component.editor.MacroPlaceholder;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

public abstract class AbstractJiraCreatedPanelTest extends AbstractJiraTest
{
    @BeforeClass
    public static void init() throws Exception
    {
        editPage = gotoEditTestPage(user.get());
    }

    @Before
    public void setup() throws Exception
    {
       if (editPage == null)
        {
            editPage = gotoEditTestPage(user.get());
        }
        else
        {
            if (editPage.getEditor().isCancelVisibleNow())
            {
                // in editor page.
                editPage.getEditor().getContent().clear();
            }
            else
            {
                // in view page, and then need to go to edit page.
                editPage = gotoEditTestPage(user.get());
            }
        }
        jiraMacroCreatePanelDialog = openJiraMacroCreateNewIssuePanelFromMenu();
        jiraMacroCreatePanelDialog.waitUntilProjectLoaded(getProjectId(PROJECT_TSTT));
    }

    @After
    public void tearDown() throws Exception
    {
        closeDialog(jiraMacroCreatePanelDialog);
    }

    @AfterClass
    public static void clean() throws Exception
    {
        cancelEditPage(editPage);
    }

    protected String createJiraIssue(String project, String issueType, String summary,
                                     @NotNull String epicName)
    {
        jiraMacroCreatePanelDialog.selectMenuItem("Create New Issue");
        jiraMacroCreatePanelDialog.selectProject(project);

        waitForAjaxRequest();

        jiraMacroCreatePanelDialog.selectIssueType(issueType);
        jiraMacroCreatePanelDialog.getSummaryElement().type(summary);

        jiraMacroCreatePanelDialog.setEpicName(epicName);

        jiraMacroCreatePanelDialog.insertIssue();
        editPage.getEditor().getContent().waitForInlineMacro(JIRA_ISSUE_MACRO_NAME);
        MacroPlaceholder jim  = editPage.getEditor().getContent().macroPlaceholderFor(JIRA_ISSUE_MACRO_NAME).get(0);
        return getIssueKey(jim.getAttribute("data-macro-parameters"));
    }




    protected String getIssueKey(String macroParam)
    {
        String jql = (macroParam.split("\\|"))[0];
        return (jql.split("="))[1];
    }
}
