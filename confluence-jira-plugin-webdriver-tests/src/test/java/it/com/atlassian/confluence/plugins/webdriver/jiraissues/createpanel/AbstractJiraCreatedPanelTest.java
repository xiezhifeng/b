package it.com.atlassian.confluence.plugins.webdriver.jiraissues.createpanel;

import com.atlassian.util.concurrent.NotNull;
import it.com.atlassian.confluence.plugins.webdriver.AbstractJiraTest;
import it.com.atlassian.confluence.plugins.webdriver.pageobjects.jiraissuefillter.JiraMacroCreatePanelDialog;

import com.atlassian.confluence.webdriver.pageobjects.component.editor.MacroPlaceholder;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

public class AbstractJiraCreatedPanelTest extends AbstractJiraTest
{
    protected JiraMacroCreatePanelDialog jiraMacroCreatePanelDialog;

    @BeforeClass
    public static void init() throws Exception
    {
        editPage = gotoEditTestPage(user.get());
    }

    @Before
    public void setup() throws Exception
    {
        getReadyOnEditTestPage();

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
        jiraMacroCreatePanelDialog.selectTabItem("Create New Issue");
        jiraMacroCreatePanelDialog.selectProject(project);

        waitForAjaxRequest();

        jiraMacroCreatePanelDialog.selectIssueType(issueType);
        jiraMacroCreatePanelDialog.getSummaryElement().type(summary);

        jiraMacroCreatePanelDialog.setEpicName(epicName);

        jiraMacroCreatePanelDialog.clickInsertDialog();
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
