package it.com.atlassian.confluence.plugins.webdriver.jiraissues.searchpanel;

import com.atlassian.confluence.plugins.pageobjects.DisplayOptionPanel;
import com.atlassian.confluence.webdriver.pageobjects.component.editor.MacroPlaceholder;
import com.atlassian.pageobjects.elements.PageElement;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class JiraIssuesMaxCheckedTest extends AbstractJiraIssuesSearchPanelTest
{
    @Test
    public void checkMaxIssueValidNumber() throws Exception
    {
        // Invalid number
        jiraMacroSearchPanelDialog = openJiraIssueSearchPanelDialogFromMacroBrowser(editPage);
        jiraMacroSearchPanelDialog.fillMaxIssues("100kdkdkd");
        assertTrue(jiraMacroSearchPanelDialog.hasMaxIssuesErrorMsg());
    }

    @Test
    public void checkMaxIssueAboveRange() throws Exception
    {
        // Out of range
        jiraMacroSearchPanelDialog = openJiraIssueSearchPanelDialogFromMacroBrowser(editPage);
        jiraMacroSearchPanelDialog.fillMaxIssues("1000000");
        assertTrue(jiraMacroSearchPanelDialog.hasMaxIssuesErrorMsg());
    }

    @Test
    public void checkMaxIssueBelowRange() throws Exception
    {
        // Out of range
        jiraMacroSearchPanelDialog = openJiraIssueSearchPanelDialogFromMacroBrowser(editPage);
        jiraMacroSearchPanelDialog.fillMaxIssues("-10");
        assertTrue(jiraMacroSearchPanelDialog.hasMaxIssuesErrorMsg());
    }

    @Test
    public void checkMaxIssueDisplayOption() throws Exception
    {
        // behaviour when click difference display option
        jiraMacroSearchPanelDialog = openJiraIssueSearchPanelDialogFromMacroBrowser(editPage);
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
    public void checkMaxIssueNumberKeeping() throws Exception
    {
        jiraMacroSearchPanelDialog = openJiraIssueSearchPanelDialogFromMacroBrowser(editPage);
        jiraMacroSearchPanelDialog.fillMaxIssues("5");
        jiraMacroSearchPanelDialog.clickInsertDialog();
        waitUntilInlineMacroAppearsInEditor(editPage, JIRA_ISSUE_MACRO_NAME);

        MacroPlaceholder macroPlaceholder  = editPage.getEditor().getContent().macroPlaceholderFor(JIRA_ISSUE_MACRO_NAME).iterator().next();
        jiraMacroSearchPanelDialog = openJiraIssuesDialogFromMacroPlaceholder(editPage, macroPlaceholder);
        assertEquals(jiraMacroSearchPanelDialog.getMaxIssuesTxt().getValue(), "5");
    }

    @Test
    public void checkDefaultValue() throws Exception
    {
        jiraMacroSearchPanelDialog = openJiraIssueSearchPanelDialogFromMacroBrowser(editPage);
        jiraMacroSearchPanelDialog.showDisplayOption();
        String value = jiraMacroSearchPanelDialog.getMaxIssuesTxt().getValue();
        assertEquals("20", value);
    }

    @Test
    public void checkEmptyDefaultValue() throws Exception
    {
        jiraMacroSearchPanelDialog = openJiraIssueSearchPanelDialogFromMacroBrowser(editPage);
        jiraMacroSearchPanelDialog.showDisplayOption();
        jiraMacroSearchPanelDialog.getMaxIssuesTxt().clear();
        jiraMacroSearchPanelDialog.getMaxIssuesTxt().javascript().execute("jQuery(arguments[0]).trigger('blur')");
        String value = jiraMacroSearchPanelDialog.getMaxIssuesTxt().getValue();
        assertEquals("1000", value);
    }

    @Test
    public void checkMaxIssueHappyCase() throws Exception

    {
        jiraMacroSearchPanelDialog = openJiraIssueSearchPanelDialogFromMacroBrowser(editPage);
        jiraMacroSearchPanelDialog.showDisplayOption();
        jiraMacroSearchPanelDialog.fillMaxIssues("1");
        List<PageElement> issuses = jiraMacroSearchPanelDialog.insertAndSave();
        assertNotNull(issuses);
        assertEquals(1, issuses.size());
    }
}
