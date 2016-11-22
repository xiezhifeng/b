package it.com.atlassian.confluence.plugins.webdriver.jiraissues.createpanel;

import com.atlassian.confluence.webdriver.pageobjects.component.editor.MacroPlaceholder;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.Poller;
import com.google.common.collect.Iterables;
import it.com.atlassian.confluence.plugins.webdriver.AbstractJiraIssueMacroTest;
import it.com.atlassian.confluence.plugins.webdriver.helper.JiraRestHelper;
import it.com.atlassian.confluence.plugins.webdriver.pageobjects.jiraissuefillter.JiraMacroCreatePanelDialog;
import it.com.atlassian.confluence.plugins.webdriver.pageobjects.jiraissuefillter.JiraMacroSearchPanelDialog;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.openqa.selenium.TimeoutException;

import java.util.List;

import static org.junit.Assert.assertTrue;

public class JiraIssueMacroCreatePanelTest extends AbstractJiraIssueMacroTest {

    private JiraMacroCreatePanelDialog jiraMacroCreatePanelDialog;

    @Before
    public void setup() throws Exception {
        openJiraMacroCreateNewIssuePanelFromMenu();
        jiraMacroCreatePanelDialog.waitUntilProjectLoaded(getProjectId(PROJECT_TSTT));
    }

    @After
    public void clear() throws Exception {
        closeDialog(jiraMacroCreatePanelDialog);
    }

    @Test
    public void testComponentsVisible() throws Exception {
        jiraMacroCreatePanelDialog.selectProject("Jira integration plugin");
        assertTrue(jiraMacroCreatePanelDialog.getComponents().isVisible());
    }

    @Test
    public void testCreateEpicIssue() throws Exception {
        String issueKey = null;
        try {
            issueKey = createJiraIssue(PROJECT_TP, "Epic", "SUMMARY", "EPIC NAME");
            List<MacroPlaceholder> listMacroChart = editContentPage.getEditor()
                    .getContent()
                    .macroPlaceholderFor(JIRA_ISSUE_MACRO_NAME);
            Assert.assertEquals(1, listMacroChart.size());
        } finally {
            JiraRestHelper.deleteIssue(issueKey);
        }
    }

    @Test
    public void testErrorMessageForRequiredFields() throws Exception {
        jiraMacroCreatePanelDialog.selectProject("Test Project 3");
        jiraMacroCreatePanelDialog.selectIssueType("Bug");
        jiraMacroCreatePanelDialog.getSummaryElement().clear();
        jiraMacroCreatePanelDialog.submit();

        Poller.waitUntilTrue(
                "Create panel errors are not visible",
                jiraMacroCreatePanelDialog.areFieldErrorMessagesVisible()
        );

        Iterable<PageElement> clientErrors = jiraMacroCreatePanelDialog.getFieldErrorMessages();

        Assert.assertEquals("Summary is required", Iterables.get(clientErrors, 0).getText());
        Assert.assertEquals("Due Date is required", Iterables.get(clientErrors, 1).getText());

        jiraMacroCreatePanelDialog.getSummaryElement().type("    ");
        jiraMacroCreatePanelDialog.setDuedate("zzz");

        jiraMacroCreatePanelDialog.submit();
        clientErrors = jiraMacroCreatePanelDialog.getFieldErrorMessages();
        Assert.assertEquals("Summary is required", Iterables.get(clientErrors, 0).getText());

        jiraMacroCreatePanelDialog.getSummaryElement().type("blah");
        jiraMacroCreatePanelDialog.submit();

        waitForAjaxRequest();

        Iterable<PageElement> serverErrors = jiraMacroCreatePanelDialog.getFieldErrorMessages();
        Assert.assertEquals("Error parsing date string: zzz", Iterables.get(serverErrors, 0).getText());
    }

    @Ignore("CONFDEV-38148 fails intermittently in JiraMacroCreatePanelDialog.selectIssueType when the expected project and issue type are already selected")
    @Test
    public void testDisplayUnsupportedFieldsMessage() throws Exception
    {
        jiraMacroCreatePanelDialog.selectProject("Special Project 1");
        jiraMacroCreatePanelDialog.selectIssueType("Bug");

        // Check display unsupported fields message
        String unsupportedMessage = "The required field Flagged is not available in this form.";
        Poller.waitUntil(jiraMacroCreatePanelDialog.getJiraErrorMessages(), Matchers.containsString(unsupportedMessage));

        Poller.waitUntilFalse("Insert button is disabled when there are unsupported fields",
                jiraMacroCreatePanelDialog.isInsertButtonEnabled());

        jiraMacroCreatePanelDialog.getSummaryElement().type("Test input summary");
        Poller.waitUntilFalse("Insert button is still disabled when input summary",
                jiraMacroCreatePanelDialog.isInsertButtonEnabled());

        // Select a project which has not un supported field then Insert Button must be enabled.
        jiraMacroCreatePanelDialog.selectProject(PROJECT_TSTT);
        Poller.waitUntilTrue("Insert button is enable when switch back to a project which hasn't unsupported fields",
                jiraMacroCreatePanelDialog.isInsertButtonEnabled());
    }

    private void openJiraMacroCreateNewIssuePanelFromMenu() throws Exception {
        JiraMacroSearchPanelDialog dialog = openJiraIssueSearchPanelDialogFromMacroBrowser();
        dialog.selectMenuItem("Create New Issue");
        jiraMacroCreatePanelDialog = pageBinder.bind(JiraMacroCreatePanelDialog.class);
    }

    private String createJiraIssue(String project, String issueType, String summary, String epicName) {
        jiraMacroCreatePanelDialog.selectMenuItem("Create New Issue");
        jiraMacroCreatePanelDialog.selectProject(project);
        try {
            waitForAjaxRequest();
        } catch(TimeoutException e) {
            // Failed to wait for AJAX request.
        }
        jiraMacroCreatePanelDialog.selectIssueType(issueType);
        jiraMacroCreatePanelDialog.getSummaryElement().type(summary);
        jiraMacroCreatePanelDialog.setEpicName(epicName);
        jiraMacroCreatePanelDialog.insertIssue();

        editContentPage.getEditor().getContent().waitForInlineMacro(JIRA_ISSUE_MACRO_NAME);
        MacroPlaceholder jim = editContentPage.getEditor()
                .getContent()
                .macroPlaceholderFor(JIRA_ISSUE_MACRO_NAME)
                .get(0);
        return getIssueKey(jim.getAttribute("data-macro-parameters"));
    }

    private String getIssueKey(String macroParam) {
        String jql = (macroParam.split("\\|"))[0];
        return (jql.split("="))[1];
    }

}