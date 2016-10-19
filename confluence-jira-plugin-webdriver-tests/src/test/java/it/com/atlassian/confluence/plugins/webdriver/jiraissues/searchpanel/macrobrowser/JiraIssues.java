package it.com.atlassian.confluence.plugins.webdriver.jiraissues.searchpanel.macrobrowser;

import com.atlassian.confluence.webdriver.pageobjects.component.editor.EditorContent;
import com.atlassian.confluence.webdriver.pageobjects.component.editor.MacroPlaceholder;
import com.atlassian.confluence.webdriver.pageobjects.page.content.EditContentPage;
import com.atlassian.pageobjects.elements.query.Poller;
import com.google.common.collect.ImmutableList;
import it.com.atlassian.confluence.plugins.webdriver.jiraissues.searchpanel.AbstractJiraIssueMacroSearchPanelTest;
import it.com.atlassian.confluence.plugins.webdriver.pageobjects.DisplayOptionPanel;
import it.com.atlassian.confluence.plugins.webdriver.pageobjects.jirachart.PieChartDialog;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Test;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class JiraIssues extends AbstractJiraIssueMacroSearchPanelTest
{
    private static final List<String> LIST_TEST_COLUMN = ImmutableList.of("Issue Type", "Resolved", "Summary", "Key");
    private PieChartDialog pieChartDialog;

    @After
    public void clearChartDialog() throws Exception
    {
        closeDialog(pieChartDialog);
    }

    @Test
    public void testJiraChartMacroLink() throws Exception
    {
        jiraMacroSearchPanelDialog = openJiraIssueSearchPanelDialogFromMacroBrowser();
        checkNotNull(jiraMacroSearchPanelDialog.getJiraChartMacroAnchor());
        assertEquals(jiraMacroSearchPanelDialog.getJiraChartMacroAnchor().getAttribute("class"), "item-button jira-left-panel-link");
        pieChartDialog = jiraMacroSearchPanelDialog.clickJiraChartMacroAnchor();
        assertEquals(pieChartDialog.getJiraIssuesMacroAnchor().getAttribute("class"), "item-button jira-left-panel-link");
    }

    @Test
    public void testDialogValidation() throws Exception
    {
        jiraMacroSearchPanelDialog = openJiraIssueSearchPanelDialogFromMacroBrowser();
        jiraMacroSearchPanelDialog.pasteJqlSearch("status = open");
        jiraMacroSearchPanelDialog.fillMaxIssues("20a");
        jiraMacroSearchPanelDialog.uncheckKey("TSTT-5");
        Poller.waitUntilTrue(jiraMacroSearchPanelDialog.hasInsertButton());
        assertFalse("Insert button is disabled", jiraMacroSearchPanelDialog.isInsertable());
    }

    @Test
    public void testColumnsAreDisableInCountMode() throws Exception
    {
        jiraMacroSearchPanelDialog = openJiraIssueSearchPanelDialogFromMacroBrowser();
        jiraMacroSearchPanelDialog.pasteJqlSearch("status = open");
        jiraMacroSearchPanelDialog.clickSearchButton();

        jiraMacroSearchPanelDialog.openDisplayOption();
        jiraMacroSearchPanelDialog.getDisplayOptionPanel().clickDisplayTotalCount();
        jiraMacroSearchPanelDialog.clickInsertDialog();
        editContentPage.getEditor().getContent().waitForInlineMacro(JIRA_ISSUE_MACRO_NAME);
        MacroPlaceholder macroPlaceholder = editContentPage.getEditor().getContent().macroPlaceholderFor(JIRA_ISSUE_MACRO_NAME).iterator().next();
        jiraMacroSearchPanelDialog = openJiraIssuesDialogFromMacroPlaceholder(macroPlaceholder);

        Poller.waitUntilTrue(jiraMacroSearchPanelDialog.getDisplayOptionPanel().isColumnsDisabled());
    }

    /**
     * check JQL search field when input filter URL convert to JQL
     */
    @Test
    public void checkPasteFilterUrlInJQLSearchField() throws Exception
    {
        jiraMacroSearchPanelDialog = openJiraIssueSearchPanelDialogFromMacroBrowser();
        String filterQuery = "filter=10001";
        String filterURL = "http://127.0.0.1:11990/jira/issues/?" + filterQuery;
        jiraMacroSearchPanelDialog.pasteJqlSearch(filterURL);

        Poller.waitUntilTrue(jiraMacroSearchPanelDialog.getJqlSearchElement().timed().isEnabled());
        Poller.waitUntilTrue(jiraMacroSearchPanelDialog.getSearchButton().timed().isEnabled());
        jiraMacroSearchPanelDialog.clickJqlSearch();

        Poller.waitUntil(jiraMacroSearchPanelDialog.getJqlSearchElement().timed().getValue(), Matchers.equalToIgnoringCase(filterQuery));
    }

    /**
     * check JQL search field when input filter JQL convert to JQL
     */
    @Test
    public void checkPasteFilterJqlInJQLSearchField() throws Exception
    {
        jiraMacroSearchPanelDialog = openJiraIssueSearchPanelDialogFromMacroBrowser();
        String filterQuery = "filter=10001";
        jiraMacroSearchPanelDialog.pasteJqlSearch(filterQuery);

        Poller.waitUntilTrue(jiraMacroSearchPanelDialog.getJqlSearchElement().timed().isEnabled());
        Poller.waitUntilTrue(jiraMacroSearchPanelDialog.getSearchButton().timed().isEnabled());
        jiraMacroSearchPanelDialog.clickJqlSearch();

        Poller.waitUntil(jiraMacroSearchPanelDialog.getJqlSearchElement().timed().getValue(), Matchers.equalToIgnoringCase(filterQuery));
    }

    @Test
    public void checkColumnInDialog() throws Exception
    {
        insertJiraIssueMacroWithEditColumn(LIST_TEST_COLUMN, "status=open");
        Poller.waitUntilTrue(editContentPage.getEditor().getContent().htmlContains("data-macro-parameters=\"columns=type,resolutiondate,summary,key"));
    }

    @Test
    public void checkColumnKeepingAfterSearch() throws Exception
    {
        jiraMacroSearchPanelDialog = openJiraIssueSearchPanelDialogFromMacroBrowser();
        jiraMacroSearchPanelDialog.inputJqlSearch("status = open");
        jiraMacroSearchPanelDialog.clickSearchButton();
        jiraMacroSearchPanelDialog.openDisplayOption();

        DisplayOptionPanel displayOptionPanel = jiraMacroSearchPanelDialog.getDisplayOptionPanel();
        List<String>  firstSelectedColumns = displayOptionPanel.getSelectedColumns();
        displayOptionPanel.removeSelectedColumn("Resolution");
        displayOptionPanel.removeSelectedColumn("Status");

        //Search again and check list columns after removed "Resolution" and "Status" columns
        jiraMacroSearchPanelDialog.clickSearchButton();
        jiraMacroSearchPanelDialog.openDisplayOption();
        List<String>  removedSelectedColumns = displayOptionPanel.getSelectedColumns();
        assertEquals(firstSelectedColumns.size() - 2, removedSelectedColumns.size());
        assertFalse(removedSelectedColumns.contains("Resolution"));
        assertFalse(removedSelectedColumns.contains("Status"));

        //Search again and check list columns after add "Status" column

        displayOptionPanel.addColumn("Status");
        jiraMacroSearchPanelDialog.clickSearchButton();
        jiraMacroSearchPanelDialog.openDisplayOption();
        List<String>  addedSelectedColumns = displayOptionPanel.getSelectedColumns();
        assertTrue(addedSelectedColumns.contains("Status"));
    }


    @Test
    public void testCanInsertMacroWhenChangeTab() throws Exception
    {
        jiraMacroSearchPanelDialog = openJiraIssueSearchPanelDialogFromMacroBrowser();
        jiraMacroSearchPanelDialog.inputJqlSearch("status = open");
        jiraMacroSearchPanelDialog.clickSearchButton();

        //change to create issue panel to make disable insert button
        jiraMacroSearchPanelDialog.selectMenuItem(2);

        //back again search panel
        jiraMacroSearchPanelDialog.selectMenuItem(1);
        Poller.waitUntilTrue(jiraMacroSearchPanelDialog.getInsertButton().timed().isEnabled());
        jiraMacroSearchPanelDialog.clickInsertDialog();
        editContentPage.getEditor().getContent().waitForInlineMacro(JIRA_ISSUE_MACRO_NAME);
        assertEquals(editContentPage.getEditor().getContent().macroPlaceholderFor(JIRA_ISSUE_MACRO_NAME).size(), 1);
    }

    @Test
    public void checkTableOptionEnableWhenChooseOneIssue() throws Exception
    {
        jiraMacroSearchPanelDialog = openJiraIssueSearchPanelDialogFromMacroBrowser();
        jiraMacroSearchPanelDialog.inputJqlSearch("status=open");
        jiraMacroSearchPanelDialog.clickSearchButton();

        jiraMacroSearchPanelDialog.clickSelectAllIssueOption();
        jiraMacroSearchPanelDialog.clickSelectIssueOption("TP-1");

        jiraMacroSearchPanelDialog.openDisplayOption();
        DisplayOptionPanel displayOptionPanel = jiraMacroSearchPanelDialog.getDisplayOptionPanel();
        assertTrue(displayOptionPanel.isInsertSingleIssueEnable());
        assertTrue(displayOptionPanel.isInsertTableIssueEnable());
        assertFalse(displayOptionPanel.isInsertCountIssueEnable());

        jiraMacroSearchPanelDialog.clickSelectIssueOption("TP-2");
        assertTrue(displayOptionPanel.isInsertCountIssueEnable());
        assertTrue(displayOptionPanel.isInsertTableIssueEnable());
        assertFalse(displayOptionPanel.isInsertSingleIssueEnable());
    }

    private EditContentPage insertJiraIssueMacroWithEditColumn(List<String> columnNames, String jql) throws Exception {
        jiraMacroSearchPanelDialog = openJiraIssueSearchPanelDialogFromMacroBrowser();
        jiraMacroSearchPanelDialog.inputJqlSearch(jql);
        jiraMacroSearchPanelDialog.clickSearchButton();
        jiraMacroSearchPanelDialog.openDisplayOption();

        //clean all column default and add new list column
        jiraMacroSearchPanelDialog.cleanAllOptionColumn();
        DisplayOptionPanel displayOptionPanel = jiraMacroSearchPanelDialog.getDisplayOptionPanel();
        columnNames.forEach(displayOptionPanel::addColumn);

        EditContentPage editPage = jiraMacroSearchPanelDialog.clickInsertDialog();
        editPage.getEditor().getContent().waitForInlineMacro(JIRA_ISSUE_MACRO_NAME);
        EditorContent editorContent = editPage.getEditor().getContent();
        List<MacroPlaceholder> listMacroChart = editorContent.macroPlaceholderFor(JIRA_ISSUE_MACRO_NAME);
        assertEquals(1, listMacroChart.size());

        return editPage;
    }
}
