package it.com.atlassian.confluence.plugins.webdriver.jiraissues.createpanel;

import com.atlassian.util.concurrent.NotNull;
import it.com.atlassian.confluence.plugins.webdriver.AbstractJiraTest;

import com.atlassian.confluence.webdriver.pageobjects.component.editor.MacroPlaceholder;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

public class AbstractJiraCreatedPanelTest extends AbstractJiraTest
{
    @Before
    public void setup() throws Exception
    {
        editPage = gotoEditTestPage(user.get());

        jiraMacroCreatePanelDialog = openJiraMacroCreateNewIssuePanelFromMenu();
        jiraMacroCreatePanelDialog.waitUntilProjectLoaded(getProjectId(PROJECT_TSTT));
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
