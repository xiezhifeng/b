package it.com.atlassian.confluence.plugins.webdriver.jiraissues.searchpanel;

import java.util.List;

import it.com.atlassian.confluence.plugins.webdriver.pageobjects.DisplayOptionPanel;
import it.com.atlassian.confluence.plugins.webdriver.pageobjects.JiraIssuesPage;
import it.com.atlassian.confluence.plugins.webdriver.pageobjects.JiraMacroPropertyPanel;
import it.com.atlassian.confluence.plugins.webdriver.pageobjects.jiraissuefillter.JiraMacroSearchPanelDialog;
import com.atlassian.confluence.webdriver.pageobjects.component.editor.EditorContent;
import com.atlassian.confluence.webdriver.pageobjects.component.editor.MacroPlaceholder;
import com.atlassian.confluence.webdriver.pageobjects.page.content.EditContentPage;
import com.atlassian.confluence.webdriver.pageobjects.page.content.ViewPage;

import com.atlassian.pageobjects.elements.query.Poller;
import com.google.common.collect.ImmutableList;

import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;

import it.com.atlassian.confluence.plugins.webdriver.AbstractJiraTest;
import org.junit.BeforeClass;

import static org.junit.Assert.assertEquals;

public abstract class AbstractJiraIssuesSearchPanelTest extends AbstractJiraTest
{
    protected static final List<String> LIST_TEST_COLUMN = ImmutableList.of("Issue Type", "Resolved", "Summary", "Key");
    protected static List<String> LIST_DEFAULT_COLUMN = ImmutableList.of("Key", "Summary", "Issue Type", "Created", "Updated", "Due Date", "Assignee", "Reporter", "Priority", "Status", "Resolution");

    protected JiraMacroSearchPanelDialog jiraMacroSearchPanelDialog;
    protected static EditContentPage editPage;
    protected ViewPage viewPage;

    @BeforeClass
    public static void init() throws Exception
    {
        AbstractJiraTest.start();
        editPage = gotoEditTestPage(user.get());
    }

    @Before
    public void setup() throws Exception
    {
        if (editPage == null)
        {
            editPage = gotoEditTestPage(user.get());
        }
        else
        {
            if (editPage.getEditor().isCancelVisibleNow())
            {
                // in editor page.
                editPage.getEditor().getContent().clear();
            }
            else
            {
                // in view page, and then need to go to edit page.
                editPage = gotoEditTestPage(user.get());
            }
        }
    }

    @After
    public void tearDown() throws Exception
    {
        closeDialog(jiraMacroSearchPanelDialog);
    }

    @AfterClass
    public static void clean() throws Exception
    {
        cancelEditPage(editPage);
    }

    protected JiraMacroSearchPanelDialog openJiraIssueSearchPanelAndStartSearch(String searchValue) throws Exception
    {
        jiraMacroSearchPanelDialog = openJiraIssueSearchPanelDialogFromMacroBrowser(editPage);
        jiraMacroSearchPanelDialog.inputJqlSearch(searchValue);
        return jiraMacroSearchPanelDialog.clickSearchButton();
    }

    protected JiraMacroPropertyPanel getJiraMacroPropertyPanel(MacroPlaceholder macroPlaceholder)
    {
        macroPlaceholder.click();
        return pageBinder.bind(JiraMacroPropertyPanel.class);
    }

    protected JiraIssuesPage createPageWithJiraIssueMacro(String jql) throws Exception
    {
        return createPageWithJiraIssueMacro(jql, false);
    }

    protected JiraIssuesPage createPageWithJiraIssueMacro(String jql, boolean withPasteAction) throws Exception
    {
        jiraMacroSearchPanelDialog = openJiraIssueSearchPanelDialogFromMacroBrowser(editPage);
        if (withPasteAction)
        {
            jiraMacroSearchPanelDialog.pasteJqlSearch(jql);
        }
        else
        {
            jiraMacroSearchPanelDialog.inputJqlSearch(jql);
        }

        jiraMacroSearchPanelDialog.clickSearchButton();
        jiraMacroSearchPanelDialog.clickInsertDialog();

        editPage.getEditor().getContent().waitForInlineMacro(JIRA_ISSUE_MACRO_NAME);
        editPage.save();

        return bindCurrentPageToJiraIssues();
    }

    protected JiraIssuesPage bindCurrentPageToJiraIssues()
    {
        return pageBinder.bind(JiraIssuesPage.class);
    }

    protected EditContentPage insertJiraIssueMacroWithEditColumn(List<String> columnNames, String jql) throws Exception
    {
        jiraMacroSearchPanelDialog = openJiraIssueSearchPanelDialogFromMacroBrowser(editPage);
        jiraMacroSearchPanelDialog.inputJqlSearch(jql);
        jiraMacroSearchPanelDialog.clickSearchButton();
        jiraMacroSearchPanelDialog.openDisplayOption();

        //clean all column default and add new list column
        jiraMacroSearchPanelDialog.cleanAllOptionColumn();
        DisplayOptionPanel displayOptionPanel = jiraMacroSearchPanelDialog.getDisplayOptionPanel();
        for(String columnName : columnNames)
        {
            displayOptionPanel.addColumn(columnName);
        }

        EditContentPage editPage = jiraMacroSearchPanelDialog.clickInsertDialog();
        editPage.getEditor().getContent().waitForInlineMacro(JIRA_ISSUE_MACRO_NAME);
        EditorContent editorContent = editPage.getEditor().getContent();
        List<MacroPlaceholder> listMacroChart = editorContent.macroPlaceholderFor(JIRA_ISSUE_MACRO_NAME);
        assertEquals(1, listMacroChart.size());

        return editPage;
    }

    protected void convertJiraIssuesToJiraMacro(EditContentPage editPage, String jiraIssuesMacro, String jql, String macroName)
    {
        MacroPlaceholder macroPlaceholder = createMacroPlaceholderFromQueryString(editPage, jiraIssuesMacro, macroName);

        JiraMacroSearchPanelDialog dialog = openJiraIssuesDialogFromMacroPlaceholder(editPage, macroPlaceholder);
        dialog.clickSearchButton();
        Poller.waitUntil(dialog.getJqlSearchElement().timed().getValue(), Matchers.containsString(jql));

        dialog.clickInsertDialog();
    }
}
