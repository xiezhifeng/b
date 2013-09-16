package it.webdriver.com.atlassian.confluence;

import com.atlassian.confluence.it.Page;
import com.atlassian.confluence.it.User;
import com.atlassian.confluence.pageobjects.component.editor.EditorContent;
import com.atlassian.confluence.pageobjects.component.editor.MacroPlaceholder;
import com.atlassian.confluence.pageobjects.page.content.EditContentPage;
import com.atlassian.pageobjects.elements.query.Poller;
import com.atlassian.webdriver.utils.by.ByJquery;
import it.webdriver.com.atlassian.confluence.pageobjects.JiraChartDialog;
import it.webdriver.com.atlassian.confluence.pageobjects.JiraMacroDialog;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class JiraIssuesMacroWebDriverTest extends AbstractJiraWebDriverTest
{
   /* private JiraMacroDialog openSelectMacroDialog()
    {
        EditContentPage editPage = product.loginAndEdit(User.ADMIN, Page.TEST);
        editPage.openInsertMenu();
        JiraMacroDialog jiraMacroDialog = product.getPageBinder().bind(JiraMacroDialog.class);
        jiraMacroDialog.open();
        return jiraMacroDialog;
    }

    @Test
    public void testCreateEpicIssue() throws InterruptedException
    {
        JiraMacroDialog jiraMacroDialog = openSelectMacroDialog();
        jiraMacroDialog.selectMenuItem("Create New Issue");
        jiraMacroDialog.selectProject("Test Project 1");
        jiraMacroDialog.selectIssueType("Epic");
        jiraMacroDialog.setSummary("SUMMARY");
        jiraMacroDialog.setEpicName("TEST EPIC");
        EditorContent editorContent = jiraMacroDialog.insertIssue().getContent();
        wait(5000);
        List<MacroPlaceholder> listMacroChart = editorContent.macroPlaceholderFor("jira");
        Assert.assertEquals(1, listMacroChart.size());
        String htmlMacro = editorContent.getHtml();
        Assert.assertTrue(htmlMacro.contains("data-macro-name=\"jira\""));
    }*/


    /**
     * validate jira chart macro in RTE
     */
    @Test
    public void validateMacroInEditor()
    {
        EditContentPage editorPage = insertMacroToEditor().clickInsertDialog();
        EditorContent editorContent = editorPage.getContent();
        List<MacroPlaceholder> listMacroChart = editorContent.macroPlaceholderFor("jirachart");
        Assert.assertEquals(1, listMacroChart.size());
        String htmlMacro = editorContent.getHtml();
        Assert.assertTrue(htmlMacro.contains("data-macro-name=\"jirachart\""));
    }

    private JiraChartDialog insertMacroToEditor()
    {
        JiraChartDialog jiraChartDialog = openSelectMacroDialog();
        jiraChartDialog.inputJqlSearch("status = open");
        jiraChartDialog.clickPreviewButton();
        Assert.assertTrue(jiraChartDialog.hadImageInDialog());
        return jiraChartDialog;
    }

    private static final String TITLE_DIALOG_JIRA_CHART = "Insert JIRA Chart";
    private JiraChartDialog openSelectMacroDialog()
    {
        EditContentPage editPage = product.loginAndEdit(User.ADMIN, Page.TEST);
        editPage.openMacroBrowser();
        JiraChartDialog jiraChartDialog = product.getPageBinder().bind(JiraChartDialog.class);
        jiraChartDialog.open();
        Assert.assertTrue(TITLE_DIALOG_JIRA_CHART.equals(jiraChartDialog.getTitleDialog()));
        return jiraChartDialog;
    }
}
