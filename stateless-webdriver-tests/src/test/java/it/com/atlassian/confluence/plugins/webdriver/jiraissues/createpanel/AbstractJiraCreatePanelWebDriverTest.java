package it.com.atlassian.confluence.plugins.webdriver.jiraissues.createpanel;

import com.atlassian.confluence.plugins.webdriver.page.JiraCreatedMacroDialog;
import com.atlassian.confluence.webdriver.pageobjects.component.editor.MacroPlaceholder;
import it.com.atlassian.confluence.plugins.webdriver.AbstractJiraODWebDriverTest;
import org.junit.After;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class AbstractJiraCreatePanelWebDriverTest extends AbstractJiraODWebDriverTest
{
    protected JiraCreatedMacroDialog jiraCreatedMacroDialog;

    @After
    public void closeDialog() throws Exception
    {
        closeDialog(jiraCreatedMacroDialog);
        editPage.getEditor().getContent().clear();
    }

    protected JiraCreatedMacroDialog openJiraCreatedMacroDialog(boolean isFromMenu)
    {
        JiraCreatedMacroDialog jiraCreatedMacroDialog;

        if (isFromMenu)
        {
            editPage.getEditor().openInsertMenu();
            jiraCreatedMacroDialog = product.getPageBinder().bind(JiraCreatedMacroDialog.class);
            jiraCreatedMacroDialog.open();
            jiraCreatedMacroDialog.selectMenuItem("Create New Issue");
        }
        else
        {
            WebDriver driver  = product.getTester().getDriver();
            driver.switchTo().frame("wysiwygTextarea_ifr");
            driver.findElement(By.id("tinymce")).sendKeys("{ji");
            driver.switchTo().defaultContent();
            driver.findElement(By.cssSelector(".autocomplete-macro-jira")).click();
            jiraCreatedMacroDialog = product.getPageBinder().bind(JiraCreatedMacroDialog.class);
        }

        return jiraCreatedMacroDialog;
    }

    protected String createJiraIssue(String project, String issueType, String summary,
                                     String epicName)
    {
        jiraCreatedMacroDialog.selectMenuItem("Create New Issue");
        jiraCreatedMacroDialog.selectProject(project);

        waitForAjaxRequest(product.getTester().getDriver());

        jiraCreatedMacroDialog.selectIssueType(issueType);
        jiraCreatedMacroDialog.setSummary(summary);
        if(epicName != null)
        {
            jiraCreatedMacroDialog.setEpicName(epicName);
        }

        jiraCreatedMacroDialog.insertIssue();
        waitUntilInlineMacroAppearsInEditor(editPage, JIRA_ISSUE_MACRO_NAME);
        MacroPlaceholder jim  = editPage.getEditor().getContent().macroPlaceholderFor(JIRA_ISSUE_MACRO_NAME).get(0);
        return getIssueKey(jim.getAttribute("data-macro-parameters"));
    }

    private String getIssueKey(String macroParam)
    {
        String jql = (macroParam.split("\\|"))[0];
        return (jql.split("="))[1];
    }
}
