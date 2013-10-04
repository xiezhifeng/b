package it.webdriver.com.atlassian.confluence;

import java.util.List;

import it.webdriver.com.atlassian.confluence.pageobjects.JiraIssuesDialog;

import org.junit.Assert;
import org.junit.Test;

import com.atlassian.confluence.it.Page;
import com.atlassian.confluence.it.User;
import com.atlassian.confluence.pageobjects.component.editor.EditorContent;
import com.atlassian.confluence.pageobjects.component.editor.MacroPlaceholder;
import com.atlassian.confluence.pageobjects.page.content.EditContentPage;
import com.atlassian.pageobjects.elements.query.Poller;

public class JiraIssuesWebDriverTest extends AbstractJiraWebDriverTest
{
    private static final String TITLE_DIALOG_JIRA_ISSUE = "Insert JIRA Issue";
    
    private static final String[] LIST_VALUE_COLUMN = {"Issue Type", "Resolved", "Summary", "Key"};
    
    private JiraIssuesDialog openSelectMacroDialog()
    {
        EditContentPage editPage = product.loginAndEdit(User.ADMIN, Page.TEST);
        editPage.openMacroBrowser();
        JiraIssuesDialog jiraIssuesDialog = product.getPageBinder().bind(JiraIssuesDialog.class);
        jiraIssuesDialog.open();

        Assert.assertTrue(TITLE_DIALOG_JIRA_ISSUE.equals(jiraIssuesDialog.getTitleDialog()));

        return jiraIssuesDialog;
    }

    /**
     * check JQL search field when input filter URL convert to JQL
     */
    @Test
    public void checkPasteFilterUrlInJQLSearchField()
    {
        JiraIssuesDialog jiraIssueDialog = openSelectMacroDialog();
        String filterQuery = "filter=10001";
        String filterURL = this.jiraBaseUrl + "/issues/?" + filterQuery;
        jiraIssueDialog.pasteJqlSearch(filterURL);

        Poller.waitUntilTrue(jiraIssueDialog.getJQLSearchElement().timed().isEnabled());
        Poller.waitUntilTrue(jiraIssueDialog.getSearchButton().timed().isEnabled());
        jiraIssueDialog.clickJqlSearch();

        Assert.assertEquals(filterQuery, jiraIssueDialog.getJqlSearch());
    }

    /**
     * check JQL search field when input filter JQL convert to JQL
     */
    @Test
    public void checkPasteFilterJqlInJQLSearchField()
    {
        JiraIssuesDialog jiraIssueDialog = openSelectMacroDialog();
        String filterQuery = "filter=10001";
        jiraIssueDialog.pasteJqlSearch(filterQuery);

        Poller.waitUntilTrue(jiraIssueDialog.getJQLSearchElement().timed().isEnabled());
        Poller.waitUntilTrue(jiraIssueDialog.getSearchButton().timed().isEnabled());
        jiraIssueDialog.clickJqlSearch();

        Assert.assertEquals(filterQuery, jiraIssueDialog.getJqlSearch());
    }
    
    @Test
    public void checkColumnInDialog() 
    {
        JiraIssuesDialog jiraIssueDialog = openSelectMacroDialog();
        jiraIssueDialog.inputJqlSearch("status = open");
        
        jiraIssueDialog.clickSearchButton();
        jiraIssueDialog.openDisplayOption();
        
        //clean all column default and add new list column
        jiraIssueDialog.cleanAllOptionColumn();
        for(int i = 0; i< LIST_VALUE_COLUMN.length; i++)
        {
            jiraIssueDialog.clickSelected2Element(jiraIssueDialog.getColumnContainer());
            jiraIssueDialog.selectOption(jiraIssueDialog.getColumnDropDown(), LIST_VALUE_COLUMN[i]);
        }
        
        EditContentPage editPage = jiraIssueDialog.clickInsertDialog();
        EditorContent editorContent = editPage.getContent();
        List<MacroPlaceholder> listMacroChart = editorContent.macroPlaceholderFor("jira");
        Assert.assertEquals(1, listMacroChart.size());
        String htmlMacro = editorContent.getHtml();
        Assert.assertTrue(htmlMacro.contains("data-macro-parameters=\"columns=type,resolutiondate,summary,key"));
       
    }

}
