package it.com.atlassian.confluence.plugins.webdriver.jiraissues.searchpanel;

import com.atlassian.confluence.webdriver.pageobjects.component.editor.EditorContent;
import com.atlassian.confluence.webdriver.pageobjects.component.editor.MacroPlaceholder;
import com.atlassian.confluence.webdriver.pageobjects.page.content.EditContentPage;
import com.google.common.collect.ImmutableList;
import it.com.atlassian.confluence.plugins.webdriver.AbstractJiraIssueMacroTest;
import it.com.atlassian.confluence.plugins.webdriver.pageobjects.DisplayOptionPanel;
import it.com.atlassian.confluence.plugins.webdriver.pageobjects.JiraIssuesPage;
import it.com.atlassian.confluence.plugins.webdriver.pageobjects.JiraMacroPropertyPanel;
import it.com.atlassian.confluence.plugins.webdriver.pageobjects.jiraissuefillter.JiraMacroSearchPanelDialog;
import org.junit.After;

import java.util.List;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class AbstractJiraIssueMacroSearchPanelTest extends AbstractJiraIssueMacroTest {

    protected static final String JIRA_BASE_URL = System.getProperty("baseurl.jira", "http://localhost:11990/jira");
    protected static final String JIRA_DISPLAY_URL = JIRA_BASE_URL.replace("localhost", "127.0.0.1");
    protected static final String OLD_JIRA_ISSUE_MACRO_NAME = "jiraissues";

    protected static final List<String> LIST_MULTIVALUE_COLUMN = ImmutableList.of("Summary", "Issue Type", "Key", "Component/s", "Fix Version/s");
    protected static final List<String> LIST_TEST_COLUMN = ImmutableList.of("Issue Type", "Resolved", "Summary", "Key");
    protected static final List<String> LIST_URL_TEST_COLUMN = ImmutableList.of("Summary", "Issue Type", "URL", "Key");
    protected static List<String> LIST_DEFAULT_COLUMN = ImmutableList.of("Key", "Summary", "Issue Type", "Created", "Updated", "Due Date", "Assignee", "Reporter", "Priority", "Status", "Resolution");

    protected JiraMacroSearchPanelDialog jiraMacroSearchPanelDialog;

    @After
    public final void clear() {
        closeDialog(jiraMacroSearchPanelDialog);
    }

    protected JiraMacroSearchPanelDialog openJiraIssueSearchPanelAndStartSearch(String searchValue) throws Exception {
        jiraMacroSearchPanelDialog = openJiraIssueSearchPanelDialogFromMacroBrowser();
        jiraMacroSearchPanelDialog.inputJqlSearch(searchValue);
        return jiraMacroSearchPanelDialog.clickSearchButton();
    }

    protected MacroPlaceholder createMacroPlaceholderFromQueryString(String jiraIssuesMacro, String macroName) {
        EditorContent content = editContentPage.getEditor().getContent();
        content.type(jiraIssuesMacro);
        content.waitForInlineMacro(macroName);
        final List<MacroPlaceholder> macroPlaceholders = content.macroPlaceholderFor(macroName);
        assertThat("No macro placeholder found", macroPlaceholders, hasSize(greaterThanOrEqualTo(1)));
        return macroPlaceholders.iterator().next();
    }

    protected JiraMacroSearchPanelDialog openJiraIssuesDialogFromMacroPlaceholder(MacroPlaceholder macroPlaceholder) {
        editContentPage.getEditor().getContent().doubleClickEditInlineMacro(macroPlaceholder.getAttribute("data-macro-name"));
        return pageBinder.bind(JiraMacroSearchPanelDialog.class);
    }

    protected JiraMacroPropertyPanel getJiraMacroPropertyPanel(MacroPlaceholder macroPlaceholder) {
        macroPlaceholder.click();
        return pageBinder.bind(JiraMacroPropertyPanel.class);
    }

    protected JiraIssuesPage bindCurrentPageToJiraIssues() {
        return pageBinder.bind(JiraIssuesPage.class);
    }

    protected JiraIssuesPage createPageWithJiraIssueMacro(String jql) throws Exception {
        return createPageWithJiraIssueMacro(jql, false);
    }

    protected JiraIssuesPage createPageWithJiraIssueMacro(String jql, boolean withPasteAction) throws Exception {
        EditContentPage editContentPage = addJiraIssueMacroToPage(jql, withPasteAction);

        editContentPage.save();
        return bindCurrentPageToJiraIssues();
    }

    EditContentPage addJiraIssueMacroToPage(String jql, boolean withPasteAction) throws Exception
    {
        jiraMacroSearchPanelDialog = openJiraIssueSearchPanelDialogFromMacroBrowser();
        if (withPasteAction)
        {
            jiraMacroSearchPanelDialog.pasteJqlSearch(jql);
        }
        else
        {
            jiraMacroSearchPanelDialog.inputJqlSearch(jql);
        }

        jiraMacroSearchPanelDialog.clickSearchButton();

        EditContentPage editContentPage = jiraMacroSearchPanelDialog.clickInsertDialog();
        editContentPage.getEditor().getContent().waitForInlineMacro(JIRA_ISSUE_MACRO_NAME);

        return editContentPage;
    }

    protected EditContentPage insertJiraIssueMacroWithEditColumn(List<String> columnNames, String jql) throws Exception
    {
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