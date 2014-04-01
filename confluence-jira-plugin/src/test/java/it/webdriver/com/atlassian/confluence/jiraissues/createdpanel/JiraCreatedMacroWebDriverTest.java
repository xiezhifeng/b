package it.webdriver.com.atlassian.confluence.jiraissues.createdpanel;

import com.atlassian.confluence.pageobjects.component.editor.MacroPlaceholder;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.Poller;
import com.google.common.collect.Iterables;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertTrue;

public class JiraCreatedMacroWebDriverTest extends AbstractJiraCreatedPanelWebDriverTest
{

    @Test
    public void testComponentsVisible()
    {
        openJiraCreatedMacroDialog(true);
        jiraCreatedMacroDialog.selectProject("10120");
        assertTrue(jiraCreatedMacroDialog.getComponents().isVisible());
    }

    @Test
    public void testCreateEpicIssue() throws InterruptedException
    {
        jiraCreatedMacroDialog = openJiraCreatedMacroDialog(true);
        
        editContentPage = createJiraIssue("10000", "6", "SUMMARY", "EPIC NAME", "admin");
        
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
    }
}
