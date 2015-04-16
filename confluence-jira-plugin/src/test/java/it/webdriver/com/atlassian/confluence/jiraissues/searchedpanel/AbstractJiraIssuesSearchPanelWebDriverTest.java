package it.webdriver.com.atlassian.confluence.jiraissues.searchedpanel;

import com.atlassian.confluence.pageobjects.component.dialog.MacroBrowserDialog;
import com.atlassian.confluence.pageobjects.component.editor.EditorContent;
import com.atlassian.confluence.pageobjects.component.editor.MacroPlaceholder;
import com.atlassian.confluence.pageobjects.page.content.EditContentPage;
import com.atlassian.confluence.pageobjects.page.content.EditorPreview;
import it.webdriver.com.atlassian.confluence.AbstractJiraODWebDriverTest;
import it.webdriver.com.atlassian.confluence.pageobjects.DisplayOptionPanel;
import it.webdriver.com.atlassian.confluence.pageobjects.JiraIssuesDialog;
import it.webdriver.com.atlassian.confluence.pageobjects.JiraIssuesPage;
import it.webdriver.com.atlassian.confluence.pageobjects.JiraMacroPropertyPanel;
import org.junit.After;
import org.junit.Assert;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public abstract class AbstractJiraIssuesSearchPanelWebDriverTest extends AbstractJiraODWebDriverTest
{

    protected static final List<String> LIST_TEST_COLUMN = Arrays.asList("Issue Type", "Resolved", "Summary", "Key");

    protected static List<String> LIST_DEFAULT_COLUMN = Arrays.asList("Key", "Summary", "Issue Type", "Created", "Updated", "Due Date", "Assignee", "Reporter", "Priority", "Status", "Resolution");

    protected JiraIssuesDialog jiraIssuesDialog;

    @After
    public void closeDialog() throws Exception
    {
        closeDialog(jiraIssuesDialog);
    }

    protected JiraIssuesDialog openJiraIssuesDialog()
    {
        MacroBrowserDialog macroBrowserDialog = openMacroBrowser();
        macroBrowserDialog.searchForFirst("embed jira issues").select();
        jiraIssuesDialog =  product.getPageBinder().bind(JiraIssuesDialog.class);
        return jiraIssuesDialog;
    }

    protected JiraIssuesDialog search(String searchValue)
    {
        openJiraIssuesDialog();
        jiraIssuesDialog.inputJqlSearch(searchValue);
        return jiraIssuesDialog.clickSearchButton();
    }

    protected JiraIssuesDialog openJiraIssuesDialogFromMacroPlaceholder(MacroPlaceholder macroPlaceholder)
    {
        macroPlaceholder.click();
        getJiraMacroPropertyPanel(macroPlaceholder).editMacro();
        return product.getPageBinder().bind(JiraIssuesDialog.class);
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
        jiraIssuesDialog = openJiraIssuesDialog();
        if (withPasteAction)
        {
            jiraIssuesDialog.pasteJqlSearch(jql);
        }
        else
        {
            jiraIssuesDialog.inputJqlSearch(jql);
        }

        jiraIssuesDialog.clickSearchButton();

        EditContentPage editContentPage = jiraIssuesDialog.clickInsertDialog();
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
        jiraIssuesDialog = openJiraIssuesDialog();
        jiraIssuesDialog.inputJqlSearch(jql);
        jiraIssuesDialog.clickSearchButton();
        jiraIssuesDialog.openDisplayOption();

        //clean all column default and add new list column
        jiraIssuesDialog.cleanAllOptionColumn();
        DisplayOptionPanel displayOptionPanel = jiraIssuesDialog.getDisplayOptionPanel();
        for(String columnName : columnNames)
        {
            displayOptionPanel.addColumn(columnName);
        }

        EditContentPage editPage = jiraIssuesDialog.clickInsertDialog();
        waitUntilInlineMacroAppearsInEditor(editPage, JIRA_ISSUE_MACRO_NAME);
        EditorContent editorContent = editPage.getContent();
        List<MacroPlaceholder> listMacroChart = editorContent.macroPlaceholderFor(JIRA_ISSUE_MACRO_NAME);
        assertEquals(1, listMacroChart.size());

        return editPage;
    }

    protected String getPreviewContent()
    {
        EditorPreview preview = editContentPage.getEditor().clickPreview();
        preview.waitUntilLoaded();

        WebDriver driver = product.getTester().getDriver();
        driver.switchTo().frame("editor-preview-iframe");
        return driver.findElement(By.className("wiki-content")).getText();
    }

    protected MacroPlaceholder convertToMacroPlaceholder(String jiraIssuesMacro)
    {
        EditorContent content = editContentPage.getEditor().getContent();
        content.type(jiraIssuesMacro);
        return editContentPage.getEditor().getContent().macroPlaceholderFor(OLD_JIRA_ISSUE_MACRO_NAME).iterator().next();
    }

    protected void convertJiraIssuesToJiraMacro(String jiraIssuesMacro, String jql)
    {
        MacroPlaceholder macroPlaceholder = convertToMacroPlaceholder(jiraIssuesMacro);

        JiraIssuesDialog dialog = openJiraIssuesDialogFromMacroPlaceholder(macroPlaceholder);
        dialog.clickSearchButton();
        Assert.assertEquals(dialog.getJqlSearch().trim(), jql);

        dialog.clickInsertDialog();
        waitUntilInlineMacroAppearsInEditor(editContentPage, JIRA_ISSUE_MACRO_NAME);
    }

    protected String getMacroParams()
    {
        MacroPlaceholder macroPlaceholder  = editContentPage.getEditor().getContent().macroPlaceholderFor(JIRA_ISSUE_MACRO_NAME).iterator().next();
        return macroPlaceholder.getAttribute("data-macro-parameters");
    }
}
