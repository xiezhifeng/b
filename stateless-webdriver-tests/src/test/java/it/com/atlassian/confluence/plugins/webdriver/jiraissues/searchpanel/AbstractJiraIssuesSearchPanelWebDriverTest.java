package it.com.atlassian.confluence.plugins.webdriver.jiraissues.searchpanel;

import com.atlassian.confluence.it.rpc.ConfluenceRpc;
import com.atlassian.confluence.plugins.pageobjects.DisplayOptionPanel;
import com.atlassian.confluence.plugins.pageobjects.JiraIssuesPage;
import com.atlassian.confluence.plugins.pageobjects.JiraMacroPropertyPanel;
import com.atlassian.confluence.plugins.pageobjects.jiraissuefillter.JiraMacroSearchPanelDialog;
import com.atlassian.confluence.webdriver.pageobjects.component.dialog.MacroBrowserDialog;
import com.atlassian.confluence.webdriver.pageobjects.component.editor.EditorContent;
import com.atlassian.confluence.webdriver.pageobjects.component.editor.MacroPlaceholder;
import com.atlassian.confluence.webdriver.pageobjects.page.content.EditContentPage;
import com.atlassian.confluence.webdriver.pageobjects.page.content.EditorPreview;
import com.google.common.collect.ImmutableList;

import it.com.atlassian.confluence.plugins.webdriver.AbstractJiraODTest;
import org.junit.After;
import org.junit.Before;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import java.util.List;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public abstract class AbstractJiraIssuesSearchPanelWebDriverTest extends AbstractJiraODTest
{
    protected static final List<String> LIST_TEST_COLUMN = ImmutableList.of("Issue Type", "Resolved", "Summary", "Key");
    protected static List<String> LIST_DEFAULT_COLUMN = ImmutableList.of("Key", "Summary", "Issue Type", "Created", "Updated", "Due Date", "Assignee", "Reporter", "Priority", "Status", "Resolution");

    protected JiraMacroSearchPanelDialog jiraMacroSearchPanelDialog;
    protected static EditContentPage editPage;
    protected static ConfluenceRpc rpc = ConfluenceRpc.newInstance(System.getProperty("baseurl.confluence"), ConfluenceRpc.Version.V2_WITH_WIKI_MARKUP);

    @Before
    public void setup() throws Exception
    {
        editPage = gotoEditTestPage(user.get());
    }

    @After
    public void tearDown() throws Exception
    {
        closeDialog(jiraMacroSearchPanelDialog);
        if (editPage != null && editPage.getEditor().isCancelVisibleNow()) {
            editPage.getEditor().clickCancel();
        }
    }

    protected JiraMacroSearchPanelDialog openJiraIssuesDialog()
    {
        MacroBrowserDialog macroBrowserDialog = openMacroBrowser(editPage);
        macroBrowserDialog.searchForFirst("embed jira issues").select();
        jiraMacroSearchPanelDialog =  product.getPageBinder().bind(JiraMacroSearchPanelDialog.class);
        return jiraMacroSearchPanelDialog;
    }

    protected JiraMacroSearchPanelDialog search(String searchValue)
    {
        openJiraIssuesDialog();
        jiraMacroSearchPanelDialog.inputJqlSearch(searchValue);
        return jiraMacroSearchPanelDialog.clickSearchButton();
    }

    protected JiraMacroSearchPanelDialog openJiraIssuesDialogFromMacroPlaceholder(MacroPlaceholder macroPlaceholder)
    {
        editPage.getEditor().getContent().doubleClickEditInlineMacro(macroPlaceholder.getAttribute("data-macro-name"));
        return product.getPageBinder().bind(JiraMacroSearchPanelDialog.class);
    }

    protected JiraMacroPropertyPanel getJiraMacroPropertyPanel(MacroPlaceholder macroPlaceholder)
    {
        macroPlaceholder.click();
        return product.getPageBinder().bind(JiraMacroPropertyPanel.class);
    }

    protected JiraIssuesPage createPageWithJiraIssueMacro(String jql)
    {
        return createPageWithJiraIssueMacro(jql, false);
    }

    protected JiraIssuesPage createPageWithJiraIssueMacro(String jql, boolean withPasteAction)
    {
        jiraMacroSearchPanelDialog = openJiraIssuesDialog();
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
        waitUntilInlineMacroAppearsInEditor(editContentPage, JIRA_ISSUE_MACRO_NAME);
        editContentPage.save();
        return bindCurrentPageToJiraIssues();
    }

    protected JiraIssuesPage bindCurrentPageToJiraIssues()
    {
        return product.getPageBinder().bind(JiraIssuesPage.class);
    }

    protected EditContentPage insertJiraIssueMacroWithEditColumn(List<String> columnNames, String jql)
    {
        jiraMacroSearchPanelDialog = openJiraIssuesDialog();
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
        waitUntilInlineMacroAppearsInEditor(editPage, JIRA_ISSUE_MACRO_NAME);
        EditorContent editorContent = editPage.getEditor().getContent();
        List<MacroPlaceholder> listMacroChart = editorContent.macroPlaceholderFor(JIRA_ISSUE_MACRO_NAME);
        assertEquals(1, listMacroChart.size());

        return editPage;
    }

    protected String getPreviewContent()
    {
        EditorPreview preview = editPage.getEditor().clickPreview();
        preview.waitUntilLoaded();

        WebDriver driver = product.getTester().getDriver();
        driver.switchTo().frame("editor-preview-iframe");
        return driver.findElement(By.className("wiki-content")).getText();
    }

    protected MacroPlaceholder convertToMacroPlaceholder(String jiraIssuesMacro)
    {
        EditorContent content = editPage.getEditor().getContent();
        content.type(jiraIssuesMacro);
        final List<MacroPlaceholder> macroPlaceholders = content.macroPlaceholderFor(OLD_JIRA_ISSUE_MACRO_NAME);
        assertThat("No macro placeholder found", macroPlaceholders, hasSize(greaterThanOrEqualTo(1)));
        return macroPlaceholders.iterator().next();
    }

    protected void convertJiraIssuesToJiraMacro(String jiraIssuesMacro, String jql)
    {
        MacroPlaceholder macroPlaceholder = convertToMacroPlaceholder(jiraIssuesMacro);

        JiraMacroSearchPanelDialog dialog = openJiraIssuesDialogFromMacroPlaceholder(macroPlaceholder);
        dialog.clickSearchButton();
        assertEquals(dialog.getJqlSearch().trim(), jql);

        dialog.clickInsertDialog();
        waitUntilInlineMacroAppearsInEditor(editPage, JIRA_ISSUE_MACRO_NAME);
    }

    protected String getMacroParams()
    {
        MacroPlaceholder macroPlaceholder  = editPage.getEditor().getContent().macroPlaceholderFor(JIRA_ISSUE_MACRO_NAME).iterator().next();
        return macroPlaceholder.getAttribute("data-macro-parameters");
    }
}
