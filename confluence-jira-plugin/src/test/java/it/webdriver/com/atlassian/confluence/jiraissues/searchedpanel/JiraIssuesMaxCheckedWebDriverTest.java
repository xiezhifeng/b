package it.webdriver.com.atlassian.confluence.jiraissues.searchedpanel;

import com.atlassian.confluence.pageobjects.component.editor.MacroPlaceholder;
import com.atlassian.pageobjects.elements.PageElement;
import it.webdriver.com.atlassian.confluence.pageobjects.DisplayOptionPanel;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class JiraIssuesMaxCheckedWebDriverTest extends AbstractJiraIssuesSearchPanelWebDriverTest
{
    @Test
    public void checkMaxIssueValidNumber()
    {
        // Invalid number
        jiraIssuesDialog = openJiraIssuesDialog();
        jiraIssuesDialog.showDisplayOption();
        jiraIssuesDialog.getDisplayOptionPanel().fillMaxIssues("100kdkdkd");
        assertTrue(jiraIssuesDialog.hasMaxIssuesErrorMsg());
    }

    @Test
    public void checkMaxIssueAboveRange()
    {
        // Out of range
        jiraIssuesDialog = openJiraIssuesDialog();
        jiraIssuesDialog.showDisplayOption();
        jiraIssuesDialog.getDisplayOptionPanel().fillMaxIssues("1000000");
        assertTrue(jiraIssuesDialog.hasMaxIssuesErrorMsg());
    }

    @Test
    public void checkMaxIssueBelowRange()
    {
        // Out of range
        jiraIssuesDialog = openJiraIssuesDialog();
        jiraIssuesDialog.showDisplayOption();
        jiraIssuesDialog.getDisplayOptionPanel().fillMaxIssues("-10");
        assertTrue(jiraIssuesDialog.hasMaxIssuesErrorMsg());
    }

    @Test
    public void checkMaxIssueDisplayOption()
    {
        // behaviour when click difference display option
        jiraIssuesDialog = openJiraIssuesDialog();
        jiraIssuesDialog.showDisplayOption();
        DisplayOptionPanel displayOptionPanel = jiraIssuesDialog.getDisplayOptionPanel();
        displayOptionPanel.fillMaxIssues("-10");
        assertTrue(jiraIssuesDialog.hasMaxIssuesErrorMsg());
        displayOptionPanel.clickDisplaySingle();
        displayOptionPanel.clickDisplayTotalCount();
        displayOptionPanel.clickDisplayTable();
        assertTrue(jiraIssuesDialog.hasMaxIssuesErrorMsg());
    }

    @Test
    public void checkMaxIssueNumberKeeping()
    {
        jiraIssuesDialog = openJiraIssuesDialog();
        jiraIssuesDialog.showDisplayOption();
        jiraIssuesDialog.getDisplayOptionPanel().fillMaxIssues("5");
        jiraIssuesDialog.clickInsertDialog();
        waitUntilInlineMacroAppearsInEditor(editContentPage, JIRA_ISSUE_MACRO_NAME);

        MacroPlaceholder macroPlaceholder  = editContentPage.getContent().macroPlaceholderFor(JIRA_ISSUE_MACRO_NAME).iterator().next();
        jiraIssuesDialog = openJiraIssuesDialogFromMacroPlaceholder(macroPlaceholder);
        assertEquals(jiraIssuesDialog.getDisplayOptionPanel().getMaxIssuesTxt().getValue(), "5");
    }

    @Test
    public void checkDefaultValue()
    {
        jiraIssuesDialog = openJiraIssuesDialog();
        jiraIssuesDialog.showDisplayOption();
        String value = jiraIssuesDialog.getDisplayOptionPanel().getMaxIssuesTxt().getValue();
        assertEquals("20", value);
    }

    @Test
    public void checkEmptyDefaultValue()
    {
        jiraIssuesDialog = openJiraIssuesDialog();
        jiraIssuesDialog.showDisplayOption();
        DisplayOptionPanel displayOptionPanel = jiraIssuesDialog.getDisplayOptionPanel();
        displayOptionPanel.getMaxIssuesTxt().clear();
        displayOptionPanel.getMaxIssuesTxt().javascript().execute("jQuery(arguments[0]).trigger('blur')");
        String value = displayOptionPanel.getMaxIssuesTxt().getValue();
        assertEquals("1000", value);
    }

    @Test
    public void checkMaxIssueHappyCase()

    {
        jiraIssuesDialog = openJiraIssuesDialog();
        jiraIssuesDialog.showDisplayOption();
        jiraIssuesDialog.getDisplayOptionPanel().fillMaxIssues("1");
        List<PageElement> issuses = jiraIssuesDialog.insertAndSave();
        assertNotNull(issuses);
        assertEquals(1, issuses.size());
    }
}
