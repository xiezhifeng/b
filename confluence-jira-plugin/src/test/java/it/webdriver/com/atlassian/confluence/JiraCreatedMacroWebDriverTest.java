package it.webdriver.com.atlassian.confluence;

import com.atlassian.confluence.it.Page;
import com.atlassian.confluence.it.User;
import com.atlassian.confluence.pageobjects.component.editor.MacroPlaceholder;
import com.atlassian.confluence.pageobjects.page.content.EditContentPage;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.Poller;
import com.google.common.collect.Iterables;
import it.webdriver.com.atlassian.confluence.pageobjects.JiraCreatedMacroDialog;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import java.util.List;

import static org.junit.Assert.assertFalse;

public class JiraCreatedMacroWebDriverTest extends AbstractJiraWebDriverTest
{

    private JiraCreatedMacroDialog openJiraCreatedMacroDialog(boolean isFromMenu)
    {
        EditContentPage editPage = product.loginAndEdit(User.ADMIN, Page.TEST);
        JiraCreatedMacroDialog jiraMacroDialog;
        if(isFromMenu)
        {
            editPage.openInsertMenu();
            jiraMacroDialog = product.getPageBinder().bind(JiraCreatedMacroDialog.class);
            jiraMacroDialog.open();
            jiraMacroDialog.selectMenuItem("Create New Issue");
        }
        else
        {
            WebDriver driver  = product.getTester().getDriver();
            driver.switchTo().frame("wysiwygTextarea_ifr");
            driver.findElement(By.id("tinymce")).sendKeys("{ji");
            driver.switchTo().defaultContent();
            driver.findElement(By.cssSelector(".autocomplete-macro-jira")).click();
            jiraMacroDialog = product.getPageBinder().bind(JiraCreatedMacroDialog.class);
        }
        return jiraMacroDialog;
    }

    @Test
    public void testCreateEpicIssue() throws InterruptedException
    {
        JiraCreatedMacroDialog jiraMacroDialog = openJiraCreatedMacroDialog(true);
        
        EditContentPage editContentPage = createJiraIssue(jiraMacroDialog, "10000", "6", "SUMMARY", "EPIC NAME", "admin");
        
        List<MacroPlaceholder> listMacroChart = editContentPage.getContent().macroPlaceholderFor("jira");
        Assert.assertEquals(1, listMacroChart.size());
        editContentPage.save();
    }

    @Test
    public void testOpenRightDialog() throws InterruptedException
    {
        JiraCreatedMacroDialog jiraMacroDialog = openJiraCreatedMacroDialog(false);
        Assert.assertEquals(jiraMacroDialog.getSelectedMenu().getText(), "Search");
    }

    @Test
    public void testIssueTypeDisableFirstLoad()
    {
        JiraCreatedMacroDialog jiraIssueDialog = openJiraCreatedMacroDialog(true);
        Poller.waitUntilTrue(jiraIssueDialog.getProject().timed().isVisible());

        PageElement issueTypeSelect = jiraIssueDialog.getIssuesType();
        Poller.waitUntilTrue(issueTypeSelect.timed().isVisible());
        assertFalse(issueTypeSelect.isEnabled());
    }

    @Test
    public void testDisplayUnsupportedFieldsMessage()
    {
        JiraCreatedMacroDialog jiraMacroDialog = openJiraCreatedMacroDialog(true);

        jiraMacroDialog.selectMenuItem("Create New Issue");
        jiraMacroDialog.selectProject("10220");

        waitForAjaxRequest(product.getTester().getDriver());

        jiraMacroDialog.selectIssueType("3");

        // Check display unsupported fields message
        String unsupportedMessage = "The required field Flagged is not available in this form.";
        Poller.waitUntil(jiraMacroDialog.getJiraErrorMessages(), Matchers.containsString(unsupportedMessage), Poller.by(10 * 1000));
        Poller.waitUntilTrue("Insert button is disabled when there are unsupported fields", jiraMacroDialog.isInsertButtonDisabled());

        jiraMacroDialog.setSummary("Test input summary");
        Poller.waitUntilTrue("Insert button is still disabled when input summary", jiraMacroDialog.isInsertButtonDisabled());
    }

    @Test
    public void testErrorMessageForRequiredFields()
    {
        JiraCreatedMacroDialog jiraMacroDialog = openJiraCreatedMacroDialog(true);

        jiraMacroDialog.selectMenuItem("Create New Issue");
        jiraMacroDialog.selectProject("10320");

        waitForAjaxRequest(product.getTester().getDriver());
        jiraMacroDialog.selectIssueType("1");

        jiraMacroDialog.submit();

        Iterable<PageElement> clientErrors = jiraMacroDialog.getFieldErrorMessages();

        Assert.assertEquals("Summary is required", Iterables.get(clientErrors, 0).getText());
        Assert.assertEquals("Reporter is required", Iterables.get(clientErrors, 1).getText());
        Assert.assertEquals("Due Date is required", Iterables.get(clientErrors, 2).getText());

        jiraMacroDialog.setSummary("    ");
        jiraMacroDialog.setReporter("admin");
        jiraMacroDialog.setDuedate("zzz");

        jiraMacroDialog.submit();
        clientErrors = jiraMacroDialog.getFieldErrorMessages();
        Assert.assertEquals("Summary is required", Iterables.get(clientErrors, 0).getText());

        jiraMacroDialog.setSummary("blah");
        jiraMacroDialog.submit();

        waitForAjaxRequest(product.getTester().getDriver());

        Iterable<PageElement> serverErrors = jiraMacroDialog.getFieldErrorMessages();
        Assert.assertEquals("Error parsing date string: zzz", Iterables.get(serverErrors, 0).getText());
    }

    protected EditContentPage createJiraIssue(JiraCreatedMacroDialog jiraMacroDialog, String project,
                                              String issueType, String summary, String epicName, String reporter)
    {
        jiraMacroDialog.selectMenuItem("Create New Issue");
        jiraMacroDialog.selectProject(project);

        waitForAjaxRequest(product.getTester().getDriver());

        jiraMacroDialog.selectIssueType(issueType);
        jiraMacroDialog.setSummary(summary);
        if(epicName != null)
        {
            jiraMacroDialog.setEpicName(epicName);
        }
        if (reporter != null)
        {
            jiraMacroDialog.setReporter(reporter);
        }
        EditContentPage editContentPage = jiraMacroDialog.insertIssue();
        waitForMacroOnEditor(editContentPage, "jira");
        return editContentPage;
    }

}
