package it.com.atlassian.confluence.plugins.webdriver.jiraissues.searchpanel.macrobrowser;

import com.atlassian.pageobjects.elements.query.Poller;
import it.com.atlassian.confluence.plugins.webdriver.jiraissues.searchpanel.AbstractJiraIssueMacroSearchPanelTest;
import it.com.atlassian.confluence.plugins.webdriver.pageobjects.DisplayOptionPanel;
import org.junit.Assert;
import org.junit.Test;

public class JiraIssueCreateMacro extends AbstractJiraIssueMacroSearchPanelTest
{

    @Test
    public void testCreateLinkMacroWithDefault() throws Exception
    {
        String searchStr = "project = TP";
        editContentPage = openJiraIssueSearchPanelAndStartSearch(searchStr).clickInsertDialog();
        editContentPage.getEditor().getContent().waitForInlineMacro(JIRA_ISSUE_MACRO_NAME);
        Poller.waitUntilTrue(editContentPage.getEditor().getContent().htmlContains("/confluence/download/resources/confluence.extra.jira/jira-table.png"));
    }

    @Test
    public void testSearchNoResult() throws Exception
    {
        openJiraIssueSearchPanelAndStartSearch("InvalidValue");
        Poller.waitUntilTrue(jiraMacroSearchPanelDialog.hasInfoMessage());
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
