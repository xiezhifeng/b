package it.webdriver.com.atlassian.confluence.jiraissues.searchedpanel;

import com.atlassian.confluence.pageobjects.component.dialog.MacroBrowserDialog;
import com.atlassian.confluence.pageobjects.component.editor.MacroPlaceholder;
import it.webdriver.com.atlassian.confluence.AbstractJiraSLATWebDriverTest;
import it.webdriver.com.atlassian.confluence.pageobjects.JiraIssuesDialog;
import it.webdriver.com.atlassian.confluence.pageobjects.JiraMacroPropertyPanel;
import org.junit.After;

public class AbstractJiraIssuesSearchPanelWebDriverTest extends AbstractJiraSLATWebDriverTest
{

    public JiraIssuesDialog jiraIssuesDialog;

    @After
    public void closeDialog() throws Exception
    {
        closeDialog(jiraIssuesDialog);
    }

    public JiraIssuesDialog openJiraIssuesDialog()
    {
        MacroBrowserDialog macroBrowserDialog = openMacroBrowser();
        macroBrowserDialog.searchForFirst("embed jira issues").select();
        jiraIssuesDialog =  product.getPageBinder().bind(JiraIssuesDialog.class);
        return jiraIssuesDialog;
    }

    public JiraIssuesDialog search(String searchValue)
    {
        openJiraIssuesDialog();
        jiraIssuesDialog.inputJqlSearch(searchValue);
        return jiraIssuesDialog.clickSearchButton();
    }

    public JiraIssuesDialog openJiraIssuesDialogFromMacroPlaceholder(MacroPlaceholder macroPlaceholder)
    {
        macroPlaceholder.click();
        product.getPageBinder().bind(JiraMacroPropertyPanel.class).edit();
        return product.getPageBinder().bind(JiraIssuesDialog.class);
    }
}
