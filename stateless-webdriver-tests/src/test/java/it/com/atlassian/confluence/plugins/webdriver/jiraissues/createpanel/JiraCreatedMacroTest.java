package it.com.atlassian.confluence.plugins.webdriver.jiraissues.createpanel;

import com.atlassian.confluence.plugins.helper.JiraRestHelper;
import com.atlassian.confluence.webdriver.pageobjects.component.editor.MacroPlaceholder;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.Poller;
import com.google.common.collect.Iterables;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertTrue;

public class JiraCreatedMacroTest extends AbstractJiraCreatedPanelTest
{
    @Test
    public void testComponentsVisible()
    {
        jiraMacroCreatePanelDialog = openJiraCreatedMacroDialog(true);
        jiraMacroCreatePanelDialog.selectProject("Jira integration plugin");
        assertTrue(jiraMacroCreatePanelDialog.getComponents().isVisible());
    }

    @Test
    public void testCreateEpicIssue() throws InterruptedException
    {
        jiraMacroCreatePanelDialog = openJiraCreatedMacroDialog(true);
        jiraMacroCreatePanelDialog.waitUntilProjectLoaded(getProjectId(PROJECT_TSTT));
        String issueKey = createJiraIssue("Test Project 1", "Epic", "SUMMARY", "EPIC NAME");

        List<MacroPlaceholder> listMacroChart = editPage.getEditor().getContent().macroPlaceholderFor(JIRA_ISSUE_MACRO_NAME);
        Assert.assertEquals(1, listMacroChart.size());

        JiraRestHelper.deleteIssue(issueKey);
    }

    @Test
    public void testOpenRightDialog() throws InterruptedException
    {
        jiraMacroCreatePanelDialog = openJiraCreatedMacroDialog(false);
        Assert.assertEquals(jiraMacroCreatePanelDialog.getSelectedMenu().getText(), "Search");
    }

    @Test
    public void testErrorMessageForRequiredFields()
    {
        jiraMacroCreatePanelDialog = openJiraCreatedMacroDialog(true);
        jiraMacroCreatePanelDialog.waitUntilProjectLoaded(getProjectId(PROJECT_TSTT));

        jiraMacroCreatePanelDialog.selectProject("Test Project 3");
        jiraMacroCreatePanelDialog.selectIssueType("Bug");
        // the summary is cached from the previous section, TODO: clean dialog before closing it
        jiraMacroCreatePanelDialog.clearSummary();
        jiraMacroCreatePanelDialog.submit();

        Iterable<PageElement> clientErrors = jiraMacroCreatePanelDialog.getFieldErrorMessages();

        Assert.assertEquals("Summary is required", Iterables.get(clientErrors, 0).getText());
        Assert.assertEquals("Due Date is required", Iterables.get(clientErrors, 1).getText());

        jiraMacroCreatePanelDialog.setSummary("    ");
        jiraMacroCreatePanelDialog.setDuedate("zzz");

        jiraMacroCreatePanelDialog.submit();
        clientErrors = jiraMacroCreatePanelDialog.getFieldErrorMessages();
        Assert.assertEquals("Summary is required", Iterables.get(clientErrors, 0).getText());

        jiraMacroCreatePanelDialog.setSummary("blah");
        jiraMacroCreatePanelDialog.submit();

        waitForAjaxRequest(product.getTester().getDriver());

        Iterable<PageElement> serverErrors = jiraMacroCreatePanelDialog.getFieldErrorMessages();
        Assert.assertEquals("Error parsing date string: zzz", Iterables.get(serverErrors, 0).getText());
    }
    
    public void testDisplayUnsupportedFieldsMessage()
    {
        jiraMacroCreatePanelDialog = openJiraCreatedMacroDialog(true);
        jiraMacroCreatePanelDialog.waitUntilProjectLoaded(getProjectId(PROJECT_TSTT));

        jiraMacroCreatePanelDialog.selectProject("Special Project 1");
        jiraMacroCreatePanelDialog.selectIssueType("Bug");

        // Check display unsupported fields message
        String unsupportedMessage = "The required field Flagged is not available in this form.";
        Poller.waitUntil(jiraMacroCreatePanelDialog.getJiraErrorMessages(), Matchers.containsString(unsupportedMessage),
                Poller.by(10 * 1000));

        Poller.waitUntilFalse("Insert button is disabled when there are unsupported fields",
                jiraMacroCreatePanelDialog.isInsertButtonEnabled());

        jiraMacroCreatePanelDialog.setSummary("Test input summary");
        Poller.waitUntilFalse("Insert button is still disabled when input summary",
                jiraMacroCreatePanelDialog.isInsertButtonEnabled());

        // Select a project which has not un supported field then Insert Button must be enabled.
        jiraMacroCreatePanelDialog.selectProject("Test Project");
        Poller.waitUntilTrue("Insert button is enable when switch back to a project which hasn't unsupported fields",
                jiraMacroCreatePanelDialog.isInsertButtonEnabled());
    }
}
