package it.webdriver.com.atlassian.confluence;

import com.atlassian.confluence.it.Page;
import com.atlassian.confluence.it.User;
import com.atlassian.confluence.pageobjects.page.content.EditContentPage;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.Poller;
import it.webdriver.com.atlassian.confluence.pageobjects.JiraIssuesDialog;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class JiraIssuesWebDriverTest extends AbstractJiraWebDriverTest
{
    private static final String TITLE_DIALOG_JIRA_ISSUE = "Insert JIRA Issue";

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
    public void checkMaxIssueValidNumber()
    {
        // Invalid number
        JiraIssuesDialog jiraIssueDialog = openSelectMacroDialog();
        jiraIssueDialog.fillMaxIssues("100kdkdkd");
        Assert.assertTrue(jiraIssueDialog.hasMaxIssuesErrorMsg());
    }

    @Test
    public void checkMaxIssueAboveRange()
    {
        // Out of range
        JiraIssuesDialog jiraIssueDialog = openSelectMacroDialog();
        jiraIssueDialog.fillMaxIssues("1000000");
        Assert.assertTrue(jiraIssueDialog.hasMaxIssuesErrorMsg());
    }

    @Test
    public void checkMaxIssueBelowRange()
    {
        // Out of range
        JiraIssuesDialog jiraIssueDialog = openSelectMacroDialog();
        jiraIssueDialog.fillMaxIssues("-10");
        Assert.assertTrue(jiraIssueDialog.hasMaxIssuesErrorMsg());
    }

    @Test
    public void checkMaxIssueDisplayOption()
    {
        // behaviour when click difference display option
        JiraIssuesDialog jiraIssueDialog = openSelectMacroDialog();
        jiraIssueDialog.fillMaxIssues("-10");
        Assert.assertTrue(jiraIssueDialog.hasMaxIssuesErrorMsg());
        jiraIssueDialog.clickDisplaySingle();
        Assert.assertFalse(jiraIssueDialog.getMaxIssuesTxt().isEnabled());
        jiraIssueDialog.clickDisplayTotalCount();
        Assert.assertFalse(jiraIssueDialog.getMaxIssuesTxt().isEnabled());
        jiraIssueDialog.clickDisplayTable();
        Assert.assertTrue(jiraIssueDialog.getMaxIssuesTxt().isEnabled());
    }

    @Test
    public void checkDefaultValue()
    {
        JiraIssuesDialog jiraIssueDialog = openSelectMacroDialog();
        jiraIssueDialog.showDisplayOption();
        String value = jiraIssueDialog.getMaxIssuesTxt().getValue();
        Assert.assertEquals("20", value);
    }

    @Test
    public void checkEmptyDefaultValue()
    {
        JiraIssuesDialog jiraIssueDialog = openSelectMacroDialog();
        jiraIssueDialog.showDisplayOption();
        jiraIssueDialog.getMaxIssuesTxt().clear();
        String value = jiraIssueDialog.getMaxIssuesTxt().getValue();
        Assert.assertEquals("1000", value);
    }

    @Test
    public void checkMaxIssueHappyCase()
    {
        JiraIssuesDialog jiraIssueDialog = openSelectMacroDialog();
        jiraIssueDialog.showDisplayOption();
        jiraIssueDialog.fillMaxIssues("1");
        List<PageElement> issuses = jiraIssueDialog.insertAndSave();
        Assert.assertNotNull(issuses);
        Assert.assertEquals(1, issuses.size());
    }

}
