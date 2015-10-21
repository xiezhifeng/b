package it.com.atlassian.confluence.plugins.webdriver.jiraissues.searchpanel.macrobrowser;

import it.com.atlassian.confluence.plugins.webdriver.jiraissues.searchpanel.AbstractJiraIssuesSearchPanelWithoutSavingTest;
import it.com.atlassian.confluence.plugins.webdriver.pageobjects.DisplayOptionPanel;
import com.atlassian.confluence.webdriver.pageobjects.component.editor.MacroPlaceholder;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class JiraIssuesMaxChecked extends AbstractJiraIssuesSearchPanelWithoutSavingTest
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
        editPage.getEditor().getContent().waitForInlineMacro(JIRA_ISSUE_MACRO_NAME);

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
}
