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
    public void tearDown()
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
    public void testOpenRightDialog() throws InterruptedException
    {
        jiraCreatedMacroDialog = openJiraCreatedMacroDialog(false);
        Assert.assertEquals(jiraCreatedMacroDialog.getSelectedMenu().getText(), "Search");
    }

    @Test
    public void testProjectsLoaded()
    {
        openJiraCreatedMacroDialog(true);
        SelectElement project = jiraCreatedMacroDialog.getProject();
        Poller.waitUntilTrue(project.timed().isEnabled());
        assertEquals(project.getAllOptions().size(), 8);

        jiraCreatedMacroDialog.selectProject("10011");
        assertEquals(jiraCreatedMacroDialog.getIssuesType().getAllOptions().size(), 4);

        jiraCreatedMacroDialog.selectProject("10000");
        assertEquals(jiraCreatedMacroDialog.getIssuesType().getAllOptions().size(), 7);
    }

    @Test
    public void testComponentsVisible()
    {
        openJiraCreatedMacroDialog(true);
        jiraCreatedMacroDialog.selectProject("10120");
        assertTrue(jiraCreatedMacroDialog.getComponents().isVisible());
    }

    @Test
    public void testIssueTypeIsSubTaskNotExist()
    {
        openJiraCreatedMacroDialog(true);

        SelectElement project = jiraCreatedMacroDialog.getProject();
        Poller.waitUntilTrue(project.timed().isEnabled());
        jiraCreatedMacroDialog.selectProject("10120");
        assertFalse(jiraCreatedMacroDialog.getIssuesType().getText().contains("Technical task"));
    }

    @Test
    public void testIssueTypeDisableFirstLoad()
    {
        jiraCreatedMacroDialog = openJiraCreatedMacroDialog(true);
        Poller.waitUntilTrue(jiraCreatedMacroDialog.getProject().timed().isVisible());

        PageElement issueTypeSelect = jiraCreatedMacroDialog.getIssuesType();
        Poller.waitUntilTrue(issueTypeSelect.timed().isVisible());
        assertFalse(issueTypeSelect.isEnabled());
    }

    @Test
    public void testDisplayUnsupportedFieldsMessage()
    {
        jiraCreatedMacroDialog = openJiraCreatedMacroDialog(true);

        jiraCreatedMacroDialog.selectMenuItem("Create New Issue");
        jiraCreatedMacroDialog.selectProject("10220");

        waitForAjaxRequest(product.getTester().getDriver());

        jiraCreatedMacroDialog.selectIssueType("3");

        // Check display unsupported fields message
        String unsupportedMessage = "The required field Flagged is not available in this form.";
        Poller.waitUntil(jiraCreatedMacroDialog.getJiraErrorMessages(), Matchers.containsString(unsupportedMessage), Poller.by(10 * 1000));
        Poller.waitUntilTrue("Insert button is disabled when there are unsupported fields", jiraCreatedMacroDialog.isInsertButtonDisabled());

        jiraCreatedMacroDialog.setSummary("Test input summary");
        Poller.waitUntilTrue("Insert button is still disabled when input summary", jiraCreatedMacroDialog.isInsertButtonDisabled());
    }

    @Test
    public void testErrorMessageForRequiredFields()
    {
        jiraCreatedMacroDialog = openJiraCreatedMacroDialog(true);

        jiraCreatedMacroDialog.selectMenuItem("Create New Issue");
        jiraCreatedMacroDialog.selectProject("10320");

        waitForAjaxRequest(product.getTester().getDriver());
        jiraCreatedMacroDialog.selectIssueType("1");

        jiraCreatedMacroDialog.submit();

        Iterable<PageElement> clientErrors = jiraCreatedMacroDialog.getFieldErrorMessages();

        Assert.assertEquals("Summary is required", Iterables.get(clientErrors, 0).getText());
        Assert.assertEquals("Reporter is required", Iterables.get(clientErrors, 1).getText());
        Assert.assertEquals("Due Date is required", Iterables.get(clientErrors, 2).getText());

        jiraCreatedMacroDialog.setSummary("    ");
        jiraCreatedMacroDialog.setReporter("admin");
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

    @Test
    public void testDisplayUsernameInReporterSelectBox()
    {
        jiraCreatedMacroDialog = openJiraCreatedMacroDialog(true);

        jiraCreatedMacroDialog.selectMenuItem("Create New Issue");
        jiraCreatedMacroDialog.selectProject("10010");

        waitForAjaxRequest(product.getTester().getDriver());

        jiraCreatedMacroDialog.selectIssueType("3");
        jiraCreatedMacroDialog.searchReporter("admin");

        assertTrue("Dropdown list display fullname - (username)", jiraCreatedMacroDialog.getReporterList().contains("admin - (admin)"));
        jiraCreatedMacroDialog.chooseReporter("admin - (admin)");

        assertTrue("Display Reporter's fullname", jiraCreatedMacroDialog.getReporterText().equals("admin"));
    }

    @Test
    public void testCreateIssue()
    {
        openJiraCreatedMacroDialog(true);

        SelectElement project = jiraCreatedMacroDialog.getProject();
        Poller.waitUntilTrue(project.timed().isEnabled());
        jiraCreatedMacroDialog.selectProject("10011");
        jiraCreatedMacroDialog.setSummary("summary");
        jiraCreatedMacroDialog.setReporter("admin");

        EditContentPage editContentPage = jiraCreatedMacroDialog.insertIssue();
        waitUntilInlineMacroAppearsInEditor(editContentPage, JIRA_ISSUE_MACRO_NAME);
        assertEquals(editContentPage.getEditor().getContent().macroPlaceholderFor(JIRA_ISSUE_MACRO_NAME).size(), 1);
    }

    @Test
    public void testCreateEpicIssue() throws InterruptedException
    {
        jiraCreatedMacroDialog = openJiraCreatedMacroDialog(true);

        editContentPage = createJiraIssue("10000", "6", "SUMMARY", "EPIC NAME", "admin");

        List<MacroPlaceholder> listMacroChart = editContentPage.getContent().macroPlaceholderFor(JIRA_ISSUE_MACRO_NAME);
        Assert.assertEquals(1, listMacroChart.size());
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

}
