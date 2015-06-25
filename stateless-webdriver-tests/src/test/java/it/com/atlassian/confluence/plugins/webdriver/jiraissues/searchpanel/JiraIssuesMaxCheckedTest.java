package it.com.atlassian.confluence.plugins.webdriver.jiraissues.searchpanel;

import com.atlassian.confluence.plugins.pageobjects.DisplayOptionPanel;
import com.atlassian.confluence.webdriver.pageobjects.component.editor.MacroPlaceholder;
import com.atlassian.pageobjects.elements.PageElement;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class JiraIssuesMaxCheckedTest extends AbstractJiraIssuesSearchPanelWebDriverTest
{
    @Test
    public void checkMaxIssueValidNumber()
    {
        // Invalid number
        jiraMacroSearchPanelDialog = openJiraIssuesDialog();
        jiraMacroSearchPanelDialog.fillMaxIssues("100kdkdkd");
        assertTrue(jiraMacroSearchPanelDialog.hasMaxIssuesErrorMsg());
    }

    @Test
    public void checkMaxIssueAboveRange()
    {
        // Out of range
        jiraMacroSearchPanelDialog = openJiraIssuesDialog();
        jiraMacroSearchPanelDialog.fillMaxIssues("1000000");
        assertTrue(jiraMacroSearchPanelDialog.hasMaxIssuesErrorMsg());
    }

    @Test
    public void checkMaxIssueBelowRange()
    {
        // Out of range
        jiraMacroSearchPanelDialog = openJiraIssuesDialog();
        jiraMacroSearchPanelDialog.fillMaxIssues("-10");
        assertTrue(jiraMacroSearchPanelDialog.hasMaxIssuesErrorMsg());
    }

    @Test
    public void checkMaxIssueDisplayOption()
    {
        // behaviour when click difference display option
        jiraMacroSearchPanelDialog = openJiraIssuesDialog();
        jiraMacroSearchPanelDialog.fillMaxIssues("-10");
        assertTrue(jiraMacroSearchPanelDialog.hasMaxIssuesErrorMsg());
        jiraMacroSearchPanelDialog.openDisplayOption();
        DisplayOptionPanel displayOptionPanel = jiraMacroSearchPanelDialog.getDisplayOptionPanel();
        displayOptionPanel.clickDisplaySingle();
        displayOptionPanel.clickDisplayTotalCount();
        displayOptionPanel.clickDisplayTable();
        assertTrue(jiraMacroSearchPanelDialog.hasMaxIssuesErrorMsg());
    }

    @Test
    public void checkMaxIssueNumberKeeping()
    {
        jiraMacroSearchPanelDialog = openJiraIssuesDialog();
        jiraMacroSearchPanelDialog.fillMaxIssues("5");
        jiraMacroSearchPanelDialog.clickInsertDialog();
        waitUntilInlineMacroAppearsInEditor(editPage, JIRA_ISSUE_MACRO_NAME);

        MacroPlaceholder macroPlaceholder  = editPage.getEditor().getContent().macroPlaceholderFor(JIRA_ISSUE_MACRO_NAME).iterator().next();
        jiraMacroSearchPanelDialog = openJiraIssuesDialogFromMacroPlaceholder(macroPlaceholder);
        assertEquals(jiraMacroSearchPanelDialog.getMaxIssuesTxt().getValue(), "5");
    }

    @Test
    public void checkDefaultValue()
    {
        jiraMacroSearchPanelDialog = openJiraIssuesDialog();
        jiraMacroSearchPanelDialog.showDisplayOption();
        String value = jiraMacroSearchPanelDialog.getMaxIssuesTxt().getValue();
        assertEquals("20", value);
    }

    @Test
    public void checkEmptyDefaultValue()
    {
        jiraMacroSearchPanelDialog = openJiraIssuesDialog();
        jiraMacroSearchPanelDialog.showDisplayOption();
        jiraMacroSearchPanelDialog.getMaxIssuesTxt().clear();
        jiraMacroSearchPanelDialog.getMaxIssuesTxt().javascript().execute("jQuery(arguments[0]).trigger('blur')");
        String value = jiraMacroSearchPanelDialog.getMaxIssuesTxt().getValue();
        assertEquals("1000", value);
    }

    @Test
    public void checkMaxIssueHappyCase()

    {
        jiraMacroSearchPanelDialog = openJiraIssuesDialog();
        jiraMacroSearchPanelDialog.showDisplayOption();
        jiraMacroSearchPanelDialog.fillMaxIssues("1");
        List<PageElement> issuses = jiraMacroSearchPanelDialog.insertAndSave();
        assertNotNull(issuses);
        assertEquals(1, issuses.size());
    }
}
