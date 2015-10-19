package it.com.atlassian.confluence.plugins.webdriver.jiraissues.searchpanel.macrobrowser;

import it.com.atlassian.confluence.plugins.webdriver.jiraissues.searchpanel.AbstractJiraIssuesSearchPanelWithoutSavingTest;
import it.com.atlassian.confluence.plugins.webdriver.pageobjects.DisplayOptionPanel;
import it.com.atlassian.confluence.plugins.webdriver.pageobjects.jiraissuefillter.JiraMacroSearchPanelDialog;

import com.atlassian.pageobjects.elements.query.Poller;

import org.junit.Assert;
import org.junit.Test;

public class JiraIssueCreateMacro extends AbstractJiraIssuesSearchPanelWithoutSavingTest
{
    private static String searchStr = "project = TP";

    @Test
    public void testCreateLinkMacroWithDefault() throws Exception
    {
        JiraMacroSearchPanelDialog dialog = openJiraIssueSearchPanelAndStartSearch(searchStr);
        editPage = dialog.clickInsertDialog();
        dialog.waitUntilHidden();

        editPage.getEditor().getContent().waitForInlineMacro(JIRA_ISSUE_MACRO_NAME);
        Poller.waitUntilTrue(editPage.getEditor().getContent().htmlContains("/confluence/download/resources/confluence.extra.jira/jira-table.png"));
    }

    @Test
    public void testSearchNoResult() throws Exception
    {
        openJiraIssueSearchPanelAndStartSearch("InvalidValue");
        Poller.waitUntilTrue(dialogSearchPanel.hasInfoMessage());
        Assert.assertTrue(dialogSearchPanel.getInfoMessage().contains("No search results found."));
    }

    @Test
    public void testDisableOption() throws Exception
    {
        openJiraIssueSearchPanelAndStartSearch("TP-2");
        dialogSearchPanel.openDisplayOption();
        DisplayOptionPanel displayOptionPanel = dialogSearchPanel.getDisplayOptionPanel();
        Assert.assertTrue(displayOptionPanel.isInsertTableIssueEnable());
        Assert.assertFalse(displayOptionPanel.isInsertCountIssueEnable());
    }

    @Test
    public void testDisabledOptionWithMultipleIssues() throws Exception
    {
        openJiraIssueSearchPanelAndStartSearch("key in (TP-1, TP-2)");

        dialogSearchPanel.clickSelectIssueOption("TP-1");
        Assert.assertFalse(dialogSearchPanel.isSelectAllIssueOptionChecked());

        dialogSearchPanel.clickSelectIssueOption("TP-1");
        Assert.assertTrue(dialogSearchPanel.isSelectAllIssueOptionChecked());

        dialogSearchPanel.clickSelectAllIssueOption();
        dialogSearchPanel.clickSelectIssueOption("TP-1");

        dialogSearchPanel.openDisplayOption();
        DisplayOptionPanel displayOptionPanel = dialogSearchPanel.getDisplayOptionPanel();
        Assert.assertTrue(displayOptionPanel.isInsertTableIssueEnable());
        Assert.assertFalse(displayOptionPanel.isInsertCountIssueEnable());
    }

    @Test
    public void testRemoveColumnWithTwoTimesBackSpace() throws Exception
    {
        openJiraIssueSearchPanelAndStartSearch("key in (TP-1, TP-2)");
        dialogSearchPanel.openDisplayOption();
        DisplayOptionPanel displayOptionPanel = dialogSearchPanel.getDisplayOptionPanel();
        Assert.assertEquals(11, displayOptionPanel.getSelectedColumns().size());

        displayOptionPanel.typeSelect2Input("\u0008\u0008");
        Assert.assertEquals(10, displayOptionPanel.getSelectedColumns().size());
    }

    @Test
    public void testAddColumnByKey() throws Exception
    {
        openJiraIssueSearchPanelAndStartSearch("key in (TP-1, TP-2)");
        dialogSearchPanel.openDisplayOption();
        DisplayOptionPanel displayOptionPanel = dialogSearchPanel.getDisplayOptionPanel();
        Assert.assertEquals(11, displayOptionPanel.getSelectedColumns().size());

        displayOptionPanel.typeSelect2Input("Security");
        displayOptionPanel.sendReturnKeyToAddedColoumn();

        Assert.assertEquals(12, displayOptionPanel.getSelectedColumns().size());
    }
}
