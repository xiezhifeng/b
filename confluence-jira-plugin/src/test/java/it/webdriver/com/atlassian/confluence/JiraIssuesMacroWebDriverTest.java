package it.webdriver.com.atlassian.confluence;

import com.atlassian.confluence.it.Page;
import com.atlassian.confluence.it.User;
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
        jiraMacroDialog.selectProject("10000");
        jiraMacroDialog.selectIssueType("6");
        jiraMacroDialog.setEpicName("TEST EPIC");
        jiraMacroDialog.setSummary("SUMMARY");
        EditContentPage editContentPage = jiraMacroDialog.insertIssue();
        waitForMacroOnEditor(editContentPage, "jira");
        List<MacroPlaceholder> listMacroChart = editContentPage.getContent().macroPlaceholderFor("jira");
        Assert.assertEquals(1, listMacroChart.size());
    }

    /**
     * check JQL search field when input filter URL convert to JQL
     */
    @Test
    public void checkPasteFilterUrlInJQLSearchField()
    {
        JiraMacroDialog jiraMacroDialog = openSelectMacroDialog();
        String filterQuery = "filter=10001";
        String filterURL = this.jiraBaseUrl + "/issues/?" + filterQuery;
        jiraMacroDialog.pasteJqlSearch(filterURL);

        Poller.waitUntilTrue(jiraMacroDialog.getJQLSearchElement().timed().isEnabled());
        Poller.waitUntilTrue(jiraMacroDialog.getSearchButton().timed().isEnabled());
        jiraMacroDialog.clickJqlSearch();

        Assert.assertEquals(filterQuery, jiraMacroDialog.getJqlSearch());
    }

    /**
     * check JQL search field when input filter JQL convert to JQL
     */
    @Test
    public void checkPasteFilterJqlInJQLSearchField()
    {
        JiraMacroDialog jiraMacroDialog = openSelectMacroDialog();
        String filterQuery = "filter=10001";
        jiraMacroDialog.pasteJqlSearch(filterQuery);

        Poller.waitUntilTrue(jiraMacroDialog.getJQLSearchElement().timed().isEnabled());
        Poller.waitUntilTrue(jiraMacroDialog.getSearchButton().timed().isEnabled());
        jiraMacroDialog.clickJqlSearch();

        Assert.assertEquals(filterQuery, jiraMacroDialog.getJqlSearch());
    }
}
