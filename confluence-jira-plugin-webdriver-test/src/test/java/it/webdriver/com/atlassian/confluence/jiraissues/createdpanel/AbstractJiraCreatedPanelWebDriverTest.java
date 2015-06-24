package it.webdriver.com.atlassian.confluence.jiraissues.createdpanel;

import com.atlassian.confluence.pageobjects.component.editor.MacroPlaceholder;
import it.webdriver.com.atlassian.confluence.AbstractJiraODWebDriverTest;
import it.webdriver.com.atlassian.confluence.pageobjects.JiraCreatedMacroDialog;
import org.junit.After;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public abstract class AbstractJiraCreatedPanelWebDriverTest extends AbstractJiraODWebDriverTest
{
    protected JiraCreatedMacroDialog jiraCreatedMacroDialog = null;

    @After
    public void closeDialog() throws Exception
    {
        closeDialog(jiraCreatedMacroDialog);
        super.tearDown();
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
        waitUntilInlineMacroAppearsInEditor(editContentPage, JIRA_ISSUE_MACRO_NAME);
        MacroPlaceholder jim  = editContentPage.getEditor().getContent().macroPlaceholderFor(JIRA_ISSUE_MACRO_NAME).get(0);
        return getIssueKey(jim.getAttribute("data-macro-parameters"));
    }

    private String getIssueKey(String macroParam)
    {
        String jql = (macroParam.split("\\|"))[0];
        return (jql.split("="))[1];
    }

    protected JiraCreatedMacroDialog openJiraCreatedMacroDialog(boolean isFromMenu)
    {
        if(isFromMenu)
        {
            editContentPage.openInsertMenu();
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
}
