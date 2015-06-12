package it.webdriver.com.atlassian.confluence.jiraissues.createdpanel;

import com.atlassian.confluence.pageobjects.component.editor.MacroPlaceholder;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.Poller;
import com.google.common.collect.Iterables;
import it.webdriver.com.atlassian.confluence.helper.JiraRestHelper;
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
        waitForAjaxRequest(product.getTester().getDriver());
        jiraCreatedMacroDialog.selectProject("Jira integration plugin");
        waitForAjaxRequest(product.getTester().getDriver());
        assertTrue(jiraCreatedMacroDialog.getComponents().isVisible());
    }

    @Test
    public void testCreateEpicIssue() throws InterruptedException
    {
        jiraCreatedMacroDialog = openJiraCreatedMacroDialog(true);
        jiraCreatedMacroDialog.waitUntilProjectLoaded(getProjectId(PROJECT_TSTT));
        String issueKey = createJiraIssue("Test Project 1", "Epic", "SUMMARY", "EPIC NAME");

        List<MacroPlaceholder> listMacroChart = editContentPage.getContent().macroPlaceholderFor(JIRA_ISSUE_MACRO_NAME);
        Assert.assertEquals(1, listMacroChart.size());

        JiraRestHelper.deleteIssue(issueKey);
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
        jiraCreatedMacroDialog.waitUntilProjectLoaded(getProjectId(PROJECT_TSTT));
        waitForAjaxRequest(product.getTester().getDriver());
        jiraCreatedMacroDialog.selectProject("Special Project 1");
        waitForAjaxRequest(product.getTester().getDriver());
        jiraCreatedMacroDialog.selectIssueType("Bug");

        // Check display unsupported fields message
        String unsupportedMessage = "The required field Flagged is not available in this form.";
        Poller.waitUntil(jiraCreatedMacroDialog.getJiraErrorMessages(), Matchers.containsString(unsupportedMessage),
                Poller.by(10 * 1000));

        Poller.waitUntilFalse("Insert button is disabled when there are unsupported fields",
                jiraCreatedMacroDialog.isInsertButtonEnabled());

        jiraCreatedMacroDialog.setSummary("Test input summary");
        Poller.waitUntilFalse("Insert button is still disabled when input summary",
                jiraCreatedMacroDialog.isInsertButtonEnabled());

        // Select a project which has not un supported field then Insert Button must be enabled.
        waitForAjaxRequest(product.getTester().getDriver());
        jiraCreatedMacroDialog.selectProject("Test Project");
        waitForAjaxRequest(product.getTester().getDriver());
        Poller.waitUntilTrue("Insert button is enable when switch back to a project which hasn't unsupported fields",
                jiraCreatedMacroDialog.isInsertButtonEnabled());
    }

    @Test
    public void testErrorMessageForRequiredFields()
    {
        jiraCreatedMacroDialog = openJiraCreatedMacroDialog(true);
        jiraCreatedMacroDialog.waitUntilProjectLoaded(getProjectId(PROJECT_TSTT));
        waitForAjaxRequest(product.getTester().getDriver());
        jiraCreatedMacroDialog.selectProject("Test Project 3");
        waitForAjaxRequest(product.getTester().getDriver());
        jiraCreatedMacroDialog.selectIssueType("Bug");
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
}
