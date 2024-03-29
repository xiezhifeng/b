package it.com.atlassian.confluence.plugins.webdriver.jiraissues.searchpanel.macrobrowser;

import com.atlassian.confluence.webdriver.pageobjects.component.editor.MacroPlaceholder;
import it.com.atlassian.confluence.plugins.webdriver.jiraissues.searchpanel.AbstractJiraIssueMacroSearchPanelTest;
import it.com.atlassian.confluence.plugins.webdriver.pageobjects.DisplayOptionPanel;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class JiraIssuesMaxCheckedMacroBrowserTest extends AbstractJiraIssueMacroSearchPanelTest
{
    @Test
    public void checkMaxIssueValidNumber() throws Exception
    {
        // Invalid number
        jiraMacroSearchPanelDialog = openJiraIssueSearchPanelDialogFromMacroBrowser();
        jiraMacroSearchPanelDialog.fillMaxIssues("100kdkdkd");
        assertTrue(jiraMacroSearchPanelDialog.hasMaxIssuesErrorMsg());
    }

    @Test
    public void checkMaxIssueAboveRange() throws Exception
    {
        // Out of range
        jiraMacroSearchPanelDialog = openJiraIssueSearchPanelDialogFromMacroBrowser();
        jiraMacroSearchPanelDialog.fillMaxIssues("1000000");
        assertTrue(jiraMacroSearchPanelDialog.hasMaxIssuesErrorMsg());
    }

    @Test
    public void checkMaxIssueBelowRange() throws Exception
    {
        // Out of range
        jiraMacroSearchPanelDialog = openJiraIssueSearchPanelDialogFromMacroBrowser();
        jiraMacroSearchPanelDialog.fillMaxIssues("-10");
        assertTrue(jiraMacroSearchPanelDialog.hasMaxIssuesErrorMsg());
    }

    @Test
    public void checkMaxIssueDisplayOption() throws Exception
    {
        // behaviour when click difference display option
        jiraMacroSearchPanelDialog = openJiraIssueSearchPanelDialogFromMacroBrowser();
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
        jiraMacroSearchPanelDialog = openJiraIssueSearchPanelDialogFromMacroBrowser();
        jiraMacroSearchPanelDialog.fillMaxIssues("5");
        jiraMacroSearchPanelDialog.clickInsertDialog();
        editContentPage.getEditor().getContent().waitForInlineMacro(JIRA_ISSUE_MACRO_NAME);

        MacroPlaceholder macroPlaceholder  = editContentPage.getEditor().getContent().macroPlaceholderFor(JIRA_ISSUE_MACRO_NAME).iterator().next();
        jiraMacroSearchPanelDialog = openJiraIssuesDialogFromMacroPlaceholder(macroPlaceholder);
        assertEquals(jiraMacroSearchPanelDialog.getMaxIssuesTxt().getValue(), "5");
    }

    @Test
    public void checkDefaultValue() throws Exception
    {
        jiraMacroSearchPanelDialog = openJiraIssueSearchPanelDialogFromMacroBrowser();
        jiraMacroSearchPanelDialog.showDisplayOption();
        String value = jiraMacroSearchPanelDialog.getMaxIssuesTxt().getValue();
        assertEquals("20", value);
    }

    @Test
    public void checkEmptyDefaultValue() throws Exception
    {
        jiraMacroSearchPanelDialog = openJiraIssueSearchPanelDialogFromMacroBrowser();
        jiraMacroSearchPanelDialog.showDisplayOption();
        jiraMacroSearchPanelDialog.getMaxIssuesTxt().clear();
        jiraMacroSearchPanelDialog.getMaxIssuesTxt().javascript().execute("jQuery(arguments[0]).trigger('blur')");
        String value = jiraMacroSearchPanelDialog.getMaxIssuesTxt().getValue();
        assertEquals("1000", value);
    }
}
