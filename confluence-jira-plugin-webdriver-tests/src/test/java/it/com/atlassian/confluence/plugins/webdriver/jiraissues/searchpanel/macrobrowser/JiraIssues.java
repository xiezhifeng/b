package it.com.atlassian.confluence.plugins.webdriver.jiraissues.searchpanel.macrobrowser;

import java.util.List;

import it.com.atlassian.confluence.plugins.webdriver.jiraissues.searchpanel.AbstractJiraIssuesSearchPanelWithoutSavingTest;
import it.com.atlassian.confluence.plugins.webdriver.pageobjects.DisplayOptionPanel;
import it.com.atlassian.confluence.plugins.webdriver.pageobjects.jirachart.PieChartDialog;
import com.atlassian.confluence.webdriver.pageobjects.component.editor.MacroPlaceholder;
import com.atlassian.pageobjects.elements.query.Poller;

import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class JiraIssues extends AbstractJiraIssuesSearchPanelWithoutSavingTest
{
    protected PieChartDialog pieChartDialog;

    @After
    public void tearDown() throws Exception
    {
        closeDialog(pieChartDialog);
        super.tearDown();
    }

    @Test
    public void testDialogValidation() throws Exception
    {
        dialogSearchPanel = openJiraIssueSearchPanelDialogFromMacroBrowser(editPage);
        dialogSearchPanel.pasteJqlSearch("status = open");
        dialogSearchPanel.fillMaxIssues("20a");
        dialogSearchPanel.uncheckKey("TSTT-5");
        Poller.waitUntilFalse("Insert button is disabled", dialogSearchPanel.isInsertButtonEnabledTimed());
    }

    @Test
    public void testColumnsAreDisableInCountMode() throws Exception
    {
        dialogSearchPanel = openJiraIssueSearchPanelDialogFromMacroBrowser(editPage);
        dialogSearchPanel.pasteJqlSearch("status = open");
        dialogSearchPanel.clickSearchButton();

        dialogSearchPanel.openDisplayOption();
        dialogSearchPanel.getDisplayOptionPanel().clickDisplayTotalCount();
        dialogSearchPanel.clickInsertDialog();
        editPage.getEditor().getContent().waitForInlineMacro(JIRA_ISSUE_MACRO_NAME);
        MacroPlaceholder macroPlaceholder = editPage.getEditor().getContent().macroPlaceholderFor(JIRA_ISSUE_MACRO_NAME).iterator().next();
        dialogSearchPanel = openJiraIssuesDialogFromMacroPlaceholder(editPage, macroPlaceholder);

        Poller.waitUntilTrue(dialogSearchPanel.getDisplayOptionPanel().isColumnsDisabled());
    }

    /**
     * check JQL search field when input filter URL convert to JQL
     */
    @Test
    public void checkPasteFilterUrlInJQLSearchField() throws Exception
    {
        dialogSearchPanel = openJiraIssueSearchPanelDialogFromMacroBrowser(editPage);
        String filterQuery = "filter=10001";
        String filterURL = "http://127.0.0.1:11990/jira/issues/?" + filterQuery;
        dialogSearchPanel.pasteJqlSearch(filterURL);

        Poller.waitUntilTrue(dialogSearchPanel.getJqlSearchElement().timed().isEnabled());
        Poller.waitUntilTrue(dialogSearchPanel.getSearchButton().timed().isEnabled());
        dialogSearchPanel.clickJqlSearch();

        Poller.waitUntil(dialogSearchPanel.getJqlSearchElement().timed().getValue(), Matchers.equalToIgnoringCase(filterQuery));
    }

    /**
     * check JQL search field when input filter JQL convert to JQL
     */
    @Test
    public void checkPasteFilterJqlInJQLSearchField() throws Exception
    {
        dialogSearchPanel = openJiraIssueSearchPanelDialogFromMacroBrowser(editPage);
        String filterQuery = "filter=10001";
        dialogSearchPanel.pasteJqlSearch(filterQuery);

        Poller.waitUntilTrue(dialogSearchPanel.getJqlSearchElement().timed().isEnabled());
        Poller.waitUntilTrue(dialogSearchPanel.getSearchButton().timed().isEnabled());
        dialogSearchPanel.clickJqlSearch();

        Poller.waitUntil(dialogSearchPanel.getJqlSearchElement().timed().getValue(), Matchers.equalToIgnoringCase(filterQuery));
    }

    @Test
    public void checkColumnInDialog() throws Exception
    {
        insertJiraIssueMacroWithEditColumn(LIST_TEST_COLUMN, "status=open");
        Poller.waitUntilTrue(editPage.getEditor().getContent().htmlContains("data-macro-parameters=\"columns=type,resolutiondate,summary,key"));
    }

    @Test
    public void checkColumnKeepingAfterSearch() throws Exception
    {
        dialogSearchPanel = openJiraIssueSearchPanelDialogFromMacroBrowser(editPage);
        dialogSearchPanel.inputJqlSearch("status = open");
        dialogSearchPanel.clickSearchButton();
        dialogSearchPanel.openDisplayOption();

        DisplayOptionPanel displayOptionPanel = dialogSearchPanel.getDisplayOptionPanel();
        List<String>  firstSelectedColumns = displayOptionPanel.getSelectedColumns();
        displayOptionPanel.removeSelectedColumn("Resolution");
        displayOptionPanel.removeSelectedColumn("Status");

        //Search again and check list columns after removed "Resolution" and "Status" columns
        dialogSearchPanel.clickSearchButton();
        dialogSearchPanel.openDisplayOption();
        List<String>  removedSelectedColumns = displayOptionPanel.getSelectedColumns();
        assertEquals(firstSelectedColumns.size() - 2, removedSelectedColumns.size());
        assertFalse(removedSelectedColumns.contains("Resolution"));
        assertFalse(removedSelectedColumns.contains("Status"));

        //Search again and check list columns after add "Status" column

        displayOptionPanel.addColumn("Status");
        dialogSearchPanel.clickSearchButton();
        dialogSearchPanel.openDisplayOption();
        List<String>  addedSelectedColumns = displayOptionPanel.getSelectedColumns();
        assertTrue(addedSelectedColumns.contains("Status"));
    }


    @Test
    public void testCanInsertMacroWhenChangeTab() throws Exception
    {
        dialogSearchPanel = openJiraIssueSearchPanelDialogFromMacroBrowser(editPage);
        dialogSearchPanel.inputJqlSearch("status = open");
        dialogSearchPanel.clickSearchButton();

        //change to create issue panel to make disable insert button
        dialogSearchPanel.selectTabItem(2);

        //back again search panel
        dialogSearchPanel.selectTabItem(1);
        Poller.waitUntilTrue(dialogSearchPanel.isInsertButtonEnabledTimed());
        dialogSearchPanel.clickInsertDialog();
        editPage.getEditor().getContent().waitForInlineMacro(JIRA_ISSUE_MACRO_NAME);
        assertEquals(editPage.getEditor().getContent().macroPlaceholderFor(JIRA_ISSUE_MACRO_NAME).size(), 1);
    }

    @Test
    public void checkTableOptionEnableWhenChooseOneIssue() throws Exception
    {
        dialogSearchPanel = openJiraIssueSearchPanelDialogFromMacroBrowser(editPage);
        dialogSearchPanel.inputJqlSearch("status=open");
        dialogSearchPanel.clickSearchButton();

        dialogSearchPanel.clickSelectAllIssueOption();
        dialogSearchPanel.clickSelectIssueOption("TP-1");

        dialogSearchPanel.openDisplayOption();
        DisplayOptionPanel displayOptionPanel = dialogSearchPanel.getDisplayOptionPanel();
        assertTrue(displayOptionPanel.isInsertSingleIssueEnable());
        assertTrue(displayOptionPanel.isInsertTableIssueEnable());
        assertFalse(displayOptionPanel.isInsertCountIssueEnable());

        dialogSearchPanel.clickSelectIssueOption("TP-2");
        assertTrue(displayOptionPanel.isInsertCountIssueEnable());
        assertTrue(displayOptionPanel.isInsertTableIssueEnable());
        assertFalse(displayOptionPanel.isInsertSingleIssueEnable());
    }
}
