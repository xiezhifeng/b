package it.webdriver.com.atlassian.confluence;

import com.atlassian.confluence.it.Page;
import com.atlassian.confluence.it.User;
import com.atlassian.confluence.pageobjects.component.editor.EditorContent;
import com.atlassian.confluence.pageobjects.component.editor.MacroPlaceholder;
import com.atlassian.confluence.pageobjects.page.content.EditContentPage;
import com.atlassian.pageobjects.elements.query.Poller;
import it.webdriver.com.atlassian.confluence.pageobjects.JiraMacroDialog;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class JiraIssuesMacroWebDriverTest extends AbstractJiraWebDriverTest
{
    private JiraMacroDialog openSelectMacroDialog()
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
    }
}
