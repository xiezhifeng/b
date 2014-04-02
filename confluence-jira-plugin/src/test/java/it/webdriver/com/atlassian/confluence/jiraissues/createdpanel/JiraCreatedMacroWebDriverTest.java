package it.webdriver.com.atlassian.confluence.jiraissues.createdpanel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.atlassian.pageobjects.elements.SelectElement;
import it.webdriver.com.atlassian.confluence.AbstractJiraWebDriverTest;
import it.webdriver.com.atlassian.confluence.pageobjects.JiraCreatedMacroDialog;

import java.util.List;

import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import com.atlassian.confluence.pageobjects.component.editor.MacroPlaceholder;
import com.atlassian.confluence.pageobjects.page.content.EditContentPage;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.Poller;
import com.google.common.collect.Iterables;

public class JiraCreatedMacroWebDriverTest extends AbstractJiraWebDriverTest
{
    private JiraCreatedMacroDialog jiraCreatedMacroDialog = null;

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

    private JiraCreatedMacroDialog openJiraCreatedMacroDialog(boolean isFromMenu)
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

    @Test
    public void testProjectsLoaded()
    {
        openJiraCreatedMacroDialog(true);
        jiraCreatedMacroDialog.waitUntilProjectLoaded();

        SelectElement project = jiraCreatedMacroDialog.getProject();
        assertEquals(jiraCreatedMacroDialog.getTotalOptions(project), 8);
    }

    @Test
    public void testIssuetypeLoaded() {
        openJiraCreatedMacroDialog(true);
        SelectElement issueType = jiraCreatedMacroDialog.getIssuesType();

        jiraCreatedMacroDialog.selectProject("Test Project");
        assertEquals(jiraCreatedMacroDialog.getTotalOptions(issueType), 4);

        jiraCreatedMacroDialog.selectProject("Test Project 1");
        assertEquals(jiraCreatedMacroDialog.getTotalOptions(issueType), 7);
    }

    @Test
    public void testComponentsVisible()
    {
        openJiraCreatedMacroDialog(true);
        jiraCreatedMacroDialog.selectProject("Jira integration plugin");
        assertTrue(jiraCreatedMacroDialog.getComponents().isVisible());
    }

    @Test
    public void testCreateIssue()
    {
        openJiraCreatedMacroDialog(true);

        SelectElement project = jiraCreatedMacroDialog.getProject();
        Poller.waitUntilTrue(project.timed().isEnabled());
        jiraCreatedMacroDialog.selectProject("Test Project");
        jiraCreatedMacroDialog.setSummary("summary");

        EditContentPage editContentPage = jiraCreatedMacroDialog.insertIssue();
        waitUntilInlineMacroAppearsInEditor(editContentPage, JIRA_ISSUE_MACRO_NAME);
        assertEquals(editContentPage.getEditor().getContent().macroPlaceholderFor(JIRA_ISSUE_MACRO_NAME).size(), 1);
    }

    @Test
    public void testIssueTypeIsSubTaskNotExist()
    {
        openJiraCreatedMacroDialog(true);

        SelectElement project = jiraCreatedMacroDialog.getProject();
        Poller.waitUntilTrue(project.timed().isEnabled());
        jiraCreatedMacroDialog.selectProject("Jira integration plugin");
        assertFalse(jiraCreatedMacroDialog.getIssuesType().getText().contains("Technical task"));
    }

    @Test
    public void testCreateEpicIssue() throws InterruptedException
    {
        jiraCreatedMacroDialog = openJiraCreatedMacroDialog(true);

        editContentPage = createJiraIssue("Test Project 1", "6", "SUMMARY", "EPIC NAME");
        
        List<MacroPlaceholder> listMacroChart = editContentPage.getContent().macroPlaceholderFor(JIRA_ISSUE_MACRO_NAME);
        Assert.assertEquals(1, listMacroChart.size());
    }

    @Test
    public void testOpenRightDialog() throws InterruptedException
    {
        jiraCreatedMacroDialog = openJiraCreatedMacroDialog(false);
        Assert.assertEquals(jiraCreatedMacroDialog.getSelectedMenu().getText(), "Search");
    }

    @Test
    public void testDisplayUnsupportedFieldsMessage()
    {
        jiraCreatedMacroDialog = openJiraCreatedMacroDialog(true);

        jiraCreatedMacroDialog.selectMenuItem("Create New Issue");
        jiraCreatedMacroDialog.selectProject("Special Project 1");

        waitForAjaxRequest(product.getTester().getDriver());

        jiraCreatedMacroDialog.selectIssueType("3");

        // Check display unsupported fields message
        String unsupportedMessage = "The required field Flagged is not available in this form.";
        Poller.waitUntil(jiraCreatedMacroDialog.getJiraErrorMessages(), Matchers.containsString(unsupportedMessage), Poller.by(10 * 1000));

        Poller.waitUntilFalse("Insert button is disabled when there are unsupported fields", jiraCreatedMacroDialog.isInsertButtonEnabled());

        jiraCreatedMacroDialog.setSummary("Test input summary");
        Poller.waitUntilFalse("Insert button is still disabled when input summary", jiraCreatedMacroDialog.isInsertButtonEnabled());

        // Select a project which has not un supported field then Insert Button must be enabled.
        jiraCreatedMacroDialog.selectProject("Test Project");
        waitForAjaxRequest(product.getTester().getDriver());
        Poller.waitUntilTrue("Insert button is enable when switch back to a project which hasn't unsupported fields", jiraCreatedMacroDialog.isInsertButtonEnabled());
    }

    @Test
    public void testErrorMessageForRequiredFields()
    {
        jiraCreatedMacroDialog = openJiraCreatedMacroDialog(true);

        jiraCreatedMacroDialog.selectMenuItem("Create New Issue");
        jiraCreatedMacroDialog.selectProject("Test Project 3");

        waitForAjaxRequest(product.getTester().getDriver());
        jiraCreatedMacroDialog.selectIssueType("1");

        jiraCreatedMacroDialog.submit();

        Iterable<PageElement> clientErrors = jiraCreatedMacroDialog.getFieldErrorMessages();

        Assert.assertEquals("Summary is required", Iterables.get(clientErrors, 0).getText());
        Assert.assertEquals("Due Date is required", Iterables.get(clientErrors, 1).getText());

        jiraCreatedMacroDialog.setSummary("    ");
        jiraCreatedMacroDialog.setDuedate("zzz");

        jiraCreatedMacroDialog.submit();
        clientErrors = jiraCreatedMacroDialog.getFieldErrorMessages();
        Assert.assertEquals("Summary is required", Iterables.get(clientErrors, 0).getText());

        jiraCreatedMacroDialog.setSummary("blah");
        jiraCreatedMacroDialog.submit();

        waitForAjaxRequest(product.getTester().getDriver());

        Iterable<PageElement> serverErrors = jiraCreatedMacroDialog.getFieldErrorMessages();
        Assert.assertEquals("Error parsing date string: zzz", Iterables.get(serverErrors, 0).getText());
    }

    protected EditContentPage createJiraIssue(String project, String issueType, String summary,
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

        EditContentPage editContentPage = jiraCreatedMacroDialog.insertIssue();
        waitUntilInlineMacroAppearsInEditor(editContentPage, JIRA_ISSUE_MACRO_NAME);
        return editContentPage;
    }

}
