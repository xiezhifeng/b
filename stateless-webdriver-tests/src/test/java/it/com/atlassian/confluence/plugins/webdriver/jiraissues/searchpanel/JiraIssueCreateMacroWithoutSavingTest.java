package it.com.atlassian.confluence.plugins.webdriver.jiraissues.searchpanel;

import com.atlassian.confluence.plugins.pageobjects.DisplayOptionPanel;
import com.atlassian.pageobjects.elements.query.Poller;

import org.junit.Assert;
import org.junit.Test;

public class JiraIssueCreateMacroWithoutSavingTest extends AbstractJiraIssuesSearchPanelWithoutSavingTest
{
    private static String searchStr = "project = TP";

    @Test
    public void testCreateLinkMacroWithDefault() throws Exception
    {
        editPage = openJiraIssueSearchPanelAndStartSearch(searchStr).clickInsertDialog();
        editPage.getEditor().getContent().waitForInlineMacro(JIRA_ISSUE_MACRO_NAME);
        Poller.waitUntilTrue(editPage.getEditor().getContent().htmlContains("/confluence/download/resources/confluence.extra.jira/jira-table.png"));
    }

    @Test
    public void testSearchNoResult() throws Exception
    {
        openJiraIssueSearchPanelAndStartSearch("InvalidValue");
        Assert.assertTrue(jiraMacroSearchPanelDialog.getInfoMessage().contains("No search results found."));
    }

    @Test
    public void testDisableOption() throws Exception
    {
        openJiraIssueSearchPanelAndStartSearch("TP-2");
        jiraMacroSearchPanelDialog.openDisplayOption();
        DisplayOptionPanel displayOptionPanel = jiraMacroSearchPanelDialog.getDisplayOptionPanel();
        Assert.assertTrue(displayOptionPanel.isInsertTableIssueEnable());
        Assert.assertFalse(displayOptionPanel.isInsertCountIssueEnable());
    }

    @Test
    public void testDisabledOptionWithMultipleIssues() throws Exception
    {
        openJiraIssueSearchPanelAndStartSearch("key in (TP-1, TP-2)");

        jiraMacroSearchPanelDialog.clickSelectIssueOption("TP-1");
        Assert.assertFalse(jiraMacroSearchPanelDialog.isSelectAllIssueOptionChecked());

        jiraMacroSearchPanelDialog.clickSelectIssueOption("TP-1");
        Assert.assertTrue(jiraMacroSearchPanelDialog.isSelectAllIssueOptionChecked());

        jiraMacroSearchPanelDialog.clickSelectAllIssueOption();
        jiraMacroSearchPanelDialog.clickSelectIssueOption("TP-1");

        jiraMacroSearchPanelDialog.openDisplayOption();
        DisplayOptionPanel displayOptionPanel = jiraMacroSearchPanelDialog.getDisplayOptionPanel();
        Assert.assertTrue(displayOptionPanel.isInsertTableIssueEnable());
        Assert.assertFalse(displayOptionPanel.isInsertCountIssueEnable());
    }

    @Test
    public void testRemoveColumnWithTwoTimesBackSpace() throws Exception
    {
        openJiraIssueSearchPanelAndStartSearch("key in (TP-1, TP-2)");
        jiraMacroSearchPanelDialog.openDisplayOption();
        DisplayOptionPanel displayOptionPanel = jiraMacroSearchPanelDialog.getDisplayOptionPanel();
        Assert.assertEquals(11, displayOptionPanel.getSelectedColumns().size());

        displayOptionPanel.typeSelect2Input("\u0008\u0008");
        Assert.assertEquals(10, displayOptionPanel.getSelectedColumns().size());
    }

    @Test
    public void testAddColumnByKey() throws Exception
    {
        openJiraIssueSearchPanelAndStartSearch("key in (TP-1, TP-2)");
        jiraMacroSearchPanelDialog.openDisplayOption();
        DisplayOptionPanel displayOptionPanel = jiraMacroSearchPanelDialog.getDisplayOptionPanel();
        Assert.assertEquals(11, displayOptionPanel.getSelectedColumns().size());

        displayOptionPanel.typeSelect2Input("Security");
        displayOptionPanel.sendReturnKeyToAddedColoumn();

        Assert.assertEquals(12, displayOptionPanel.getSelectedColumns().size());
    }
}
