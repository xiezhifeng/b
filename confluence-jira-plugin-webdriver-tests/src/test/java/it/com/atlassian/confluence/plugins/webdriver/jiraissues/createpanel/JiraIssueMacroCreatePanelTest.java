package it.com.atlassian.confluence.plugins.webdriver.jiraissues.createpanel;

import com.atlassian.confluence.webdriver.pageobjects.component.dialog.MacroBrowserDialog;
import com.atlassian.confluence.webdriver.pageobjects.component.dialog.MacroForm;
import com.atlassian.confluence.webdriver.pageobjects.component.dialog.MacroItem;
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
import org.junit.Test;
import org.openqa.selenium.By;

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

    private void openJiraMacroCreateNewIssuePanelFromMenu() throws Exception {
        JiraMacroSearchPanelDialog dialog = openJiraIssueSearchPanelDialogFromMacroBrowser();
        dialog.selectMenuItem("Create New Issue");
        jiraMacroCreatePanelDialog = pageBinder.bind(JiraMacroCreatePanelDialog.class);
    }

    private JiraMacroSearchPanelDialog openJiraIssueSearchPanelDialogFromMacroBrowser() throws Exception {
        MacroBrowserDialog macroBrowserDialog = openMacroBrowser(editContentPage);
        // Although, `MacroBrowserDialog` has `searchFor` method to do search. But it's flaky test.
        // Here we tried to clear field search first then try to search the searching term.
        PageElement searchFiled = macroBrowserDialog.getDialog().find(By.id("macro-browser-search"));
        searchFiled.clear();

        Iterable<MacroItem> macroItems = macroBrowserDialog.searchFor("embed jira issues");
        Poller.waitUntil(searchFiled.timed().getValue(), Matchers.equalToIgnoringCase("embed jira issues"));

        MacroForm macroForm = macroItems.iterator().next().select();
        macroForm.waitUntilHidden();

        return pageBinder.bind(JiraMacroSearchPanelDialog.class);
    }

    private String createJiraIssue(String project, String issueType, String summary, String epicName) {
        jiraMacroCreatePanelDialog.selectMenuItem("Create New Issue");
        jiraMacroCreatePanelDialog.selectProject(project);
        waitForAjaxRequest();
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