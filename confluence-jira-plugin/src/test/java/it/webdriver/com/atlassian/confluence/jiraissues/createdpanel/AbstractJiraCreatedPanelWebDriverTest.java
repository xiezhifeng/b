package it.webdriver.com.atlassian.confluence.jiraissues.createdpanel;

import com.atlassian.confluence.pageobjects.page.content.EditContentPage;
import it.webdriver.com.atlassian.confluence.AbstractJiraWebDriverTest;
import it.webdriver.com.atlassian.confluence.pageobjects.JiraCreatedMacroDialog;
import org.junit.After;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class AbstractJiraCreatedPanelWebDriverTest extends AbstractJiraWebDriverTest
{
    protected JiraCreatedMacroDialog jiraCreatedMacroDialog = null;

    @After
    public void tearDown() throws Exception
    {
        if (jiraCreatedMacroDialog != null && jiraCreatedMacroDialog.isVisible())
        {
            // for some reason Dialog.clickCancelAndWaitUntilClosed() throws compilation issue against 5.5-SNAPSHOT as of Feb 27 2014
            jiraCreatedMacroDialog.clickCancel();
            jiraCreatedMacroDialog.waitUntilHidden();
        }
        super.tearDown();
    }

    protected EditContentPage createJiraIssue(String project, String issueType, String summary,
                                              String epicName, String reporter)
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
        if (reporter != null)
        {
            jiraCreatedMacroDialog.setReporter(reporter);
        }

        EditContentPage editContentPage = jiraCreatedMacroDialog.insertIssue();
        waitUntilInlineMacroAppearsInEditor(editContentPage, JIRA_ISSUE_MACRO_NAME);
        return editContentPage;
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
