package it.com.atlassian.confluence.plugins.webdriver.jiraissues.createpanel;

import com.atlassian.confluence.plugins.pageobjects.jiraissuefillter.JiraMacroCreatePanelDialog;
import com.atlassian.confluence.plugins.pageobjects.jiraissuefillter.JiraMacroSearchPanelDialog;
import com.atlassian.confluence.webdriver.pageobjects.component.editor.MacroPlaceholder;
import com.atlassian.confluence.webdriver.pageobjects.page.content.EditContentPage;

import it.com.atlassian.confluence.plugins.webdriver.AbstractJiraODTest;
import org.junit.After;
import org.junit.Before;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class AbstractJiraCreatedPanelTest extends AbstractJiraODTest
{
    protected JiraMacroCreatePanelDialog jiraMacroCreatePanelDialog;
    protected static EditContentPage editPage;

    @Before
    public void setup() throws Exception
    {
        editPage = gotoEditTestPage(user.get());
    }

    @After
    public void tearDown() throws Exception
    {
        closeDialog(jiraMacroCreatePanelDialog);

        if (editPage != null && editPage.getEditor().isCancelVisibleNow())
        {
            editPage.getEditor().clickCancel();
        }
        super.tearDown();
    }

    protected JiraMacroCreatePanelDialog openJiraMacroCreateNewIssuePanelFromMenu() throws Exception
    {
        JiraMacroSearchPanelDialog dialog = openJiraIssueSearchPanelDialogFromMacroBrowser(editPage);
        dialog.selectMenuItem("Create New Issue");

        jiraMacroCreatePanelDialog = pageBinder.bind(JiraMacroCreatePanelDialog.class);
        return jiraMacroCreatePanelDialog;
    }

    protected String createJiraIssue(String project, String issueType, String summary,
                                     String epicName)
    {
        jiraMacroCreatePanelDialog.selectMenuItem("Create New Issue");
        jiraMacroCreatePanelDialog.selectProject(project);

        waitForAjaxRequest();

        jiraMacroCreatePanelDialog.selectIssueType(issueType);
        jiraMacroCreatePanelDialog.setSummary(summary);
        if(epicName != null)
        {
            jiraMacroCreatePanelDialog.setEpicName(epicName);
        }

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
