package it.webdriver.com.atlassian.confluence.jiraissues.searchedpanel;

import com.atlassian.confluence.pageobjects.component.dialog.MacroBrowserDialog;
import it.webdriver.com.atlassian.confluence.AbstractJiraWebDriverTest;
import it.webdriver.com.atlassian.confluence.pageobjects.JiraIssuesDialog;
import org.junit.After;

public class AbstractJiraIssuesSearchPanelWebDriverTest extends AbstractJiraWebDriverTest
{

    public JiraIssuesDialog jiraIssuesDialog;

    @After
    public void tearDown() throws Exception
    {
        if (jiraIssuesDialog != null && jiraIssuesDialog.isVisible())
        {
            // for some reason jiraIssuesDialog.clickCancelAndWaitUntilClosed() throws compilation issue against 5.5-SNAPSHOT as of Feb 27 2014
            jiraIssuesDialog.clickCancel();
            jiraIssuesDialog.waitUntilHidden();
        }
        super.tearDown();
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
}
