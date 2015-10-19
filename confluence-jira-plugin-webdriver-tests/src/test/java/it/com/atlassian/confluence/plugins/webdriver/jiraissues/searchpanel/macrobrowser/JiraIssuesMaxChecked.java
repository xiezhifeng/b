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
        dialogSearchPanel = openJiraIssueSearchPanelDialogFromMacroBrowser(editPage);
        dialogSearchPanel.fillMaxIssues("100kdkdkd");
        assertTrue(dialogSearchPanel.hasMaxIssuesErrorMsg());
    }

    @Test
    public void checkMaxIssueAboveRange() throws Exception
    {
        // Out of range
        dialogSearchPanel = openJiraIssueSearchPanelDialogFromMacroBrowser(editPage);
        dialogSearchPanel.fillMaxIssues("1000000");
        assertTrue(dialogSearchPanel.hasMaxIssuesErrorMsg());
    }

    @Test
    public void checkMaxIssueBelowRange() throws Exception
    {
        // Out of range
        dialogSearchPanel = openJiraIssueSearchPanelDialogFromMacroBrowser(editPage);
        dialogSearchPanel.fillMaxIssues("-10");
        assertTrue(dialogSearchPanel.hasMaxIssuesErrorMsg());
    }

    @Test
    public void checkMaxIssueDisplayOption() throws Exception
    {
        // behaviour when click difference display option
        dialogSearchPanel = openJiraIssueSearchPanelDialogFromMacroBrowser(editPage);
        dialogSearchPanel.fillMaxIssues("-10");
        assertTrue(dialogSearchPanel.hasMaxIssuesErrorMsg());
        DisplayOptionPanel displayOptionPanel = dialogSearchPanel.getDisplayOptionPanel();
        displayOptionPanel.clickDisplaySingle();
        displayOptionPanel.clickDisplayTotalCount();
        displayOptionPanel.clickDisplayTable();
        assertTrue(dialogSearchPanel.hasMaxIssuesErrorMsg());
    }

    @Test
    public void checkMaxIssueNumberKeeping() throws Exception
    {
        dialogSearchPanel = openJiraIssueSearchPanelDialogFromMacroBrowser(editPage);
        dialogSearchPanel.fillMaxIssues("5");
        dialogSearchPanel.clickInsertDialog();
        editPage.getEditor().getContent().waitForInlineMacro(JIRA_ISSUE_MACRO_NAME);

        MacroPlaceholder macroPlaceholder  = editPage.getEditor().getContent().macroPlaceholderFor(JIRA_ISSUE_MACRO_NAME).iterator().next();
        dialogSearchPanel = openJiraIssuesDialogFromMacroPlaceholder(editPage, macroPlaceholder);
        assertEquals(dialogSearchPanel.getMaxIssuesTxt().getValue(), "5");
    }

    @Test
    public void checkDefaultValue() throws Exception
    {
        dialogSearchPanel = openJiraIssueSearchPanelDialogFromMacroBrowser(editPage);
        dialogSearchPanel.showDisplayOption();
        String value = dialogSearchPanel.getMaxIssuesTxt().getValue();
        assertEquals("20", value);
    }

    @Test
    public void checkEmptyDefaultValue() throws Exception
    {
        dialogSearchPanel = openJiraIssueSearchPanelDialogFromMacroBrowser(editPage);
        dialogSearchPanel.showDisplayOption();
        dialogSearchPanel.getMaxIssuesTxt().clear();
        dialogSearchPanel.getMaxIssuesTxt().javascript().execute("jQuery(arguments[0]).trigger('blur')");
        String value = dialogSearchPanel.getMaxIssuesTxt().getValue();
        assertEquals("1000", value);
    }
}
