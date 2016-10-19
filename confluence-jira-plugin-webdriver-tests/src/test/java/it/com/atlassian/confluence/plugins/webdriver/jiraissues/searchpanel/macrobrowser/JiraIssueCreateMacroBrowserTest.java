package it.com.atlassian.confluence.plugins.webdriver.jiraissues.searchpanel.macrobrowser;

import it.com.atlassian.confluence.plugins.webdriver.jiraissues.searchpanel.AbstractJiraIssueMacroSearchPanelTest;
import it.com.atlassian.confluence.plugins.webdriver.pageobjects.DisplayOptionPanel;
import org.junit.Test;

import static com.atlassian.pageobjects.elements.query.Poller.waitUntilTrue;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class JiraIssueCreateMacroBrowserTest extends AbstractJiraIssueMacroSearchPanelTest
{

    @Test
    public void testCreateLinkMacroWithDefault() throws Exception
    {
        String searchStr = "project = TP";
        editContentPage = openJiraIssueSearchPanelAndStartSearch(searchStr).clickInsertDialog();
        editContentPage.getEditor().getContent().waitForInlineMacro(JIRA_ISSUE_MACRO_NAME);
        waitUntilTrue(editContentPage.getEditor().getContent().htmlContains("/confluence/download/resources/confluence.extra.jira/jira-table.png"));
    }

    @Test
    public void testSearchNoResult() throws Exception
    {
        openJiraIssueSearchPanelAndStartSearch("InvalidValue");
        waitUntilTrue(jiraMacroSearchPanelDialog.hasInfoMessage());
        assertThat(jiraMacroSearchPanelDialog.getInfoMessage(), containsString("No search results found."));
    }

    @Test
    public void testDisableOption() throws Exception
    {
        openJiraIssueSearchPanelAndStartSearch("TP-2");
        jiraMacroSearchPanelDialog.openDisplayOption();
        DisplayOptionPanel displayOptionPanel = jiraMacroSearchPanelDialog.getDisplayOptionPanel();
        assertTrue(displayOptionPanel.isInsertTableIssueEnable());
        assertFalse(displayOptionPanel.isInsertCountIssueEnable());
    }

    @Test
    public void testDisabledOptionWithMultipleIssues() throws Exception
    {
        openJiraIssueSearchPanelAndStartSearch("key in (TP-1, TP-2)");

        jiraMacroSearchPanelDialog.clickSelectIssueOption("TP-1");
        assertFalse(jiraMacroSearchPanelDialog.isSelectAllIssueOptionChecked());

        jiraMacroSearchPanelDialog.clickSelectIssueOption("TP-1");
        assertTrue(jiraMacroSearchPanelDialog.isSelectAllIssueOptionChecked());

        jiraMacroSearchPanelDialog.clickSelectAllIssueOption();
        jiraMacroSearchPanelDialog.clickSelectIssueOption("TP-1");

        jiraMacroSearchPanelDialog.openDisplayOption();
        DisplayOptionPanel displayOptionPanel = jiraMacroSearchPanelDialog.getDisplayOptionPanel();
        assertTrue(displayOptionPanel.isInsertTableIssueEnable());
        assertFalse(displayOptionPanel.isInsertCountIssueEnable());
    }

    @Test
    public void testRemoveColumnWithTwoTimesBackSpace() throws Exception
    {
        openJiraIssueSearchPanelAndStartSearch("key in (TP-1, TP-2)");
        jiraMacroSearchPanelDialog.openDisplayOption();
        DisplayOptionPanel displayOptionPanel = jiraMacroSearchPanelDialog.getDisplayOptionPanel();
        assertEquals(11, displayOptionPanel.getSelectedColumns().size());

        displayOptionPanel.typeSelect2Input("\u0008\u0008");
        assertEquals(10, displayOptionPanel.getSelectedColumns().size());
    }

    @Test
    public void testAddColumnByKey() throws Exception
    {
        openJiraIssueSearchPanelAndStartSearch("key in (TP-1, TP-2)");
        jiraMacroSearchPanelDialog.openDisplayOption();
        DisplayOptionPanel displayOptionPanel = jiraMacroSearchPanelDialog.getDisplayOptionPanel();
        assertEquals(11, displayOptionPanel.getSelectedColumns().size());

        displayOptionPanel.typeSelect2Input("Security");
        displayOptionPanel.sendReturnKeyToAddedColoumn();

        assertEquals(12, displayOptionPanel.getSelectedColumns().size());
    }
}
