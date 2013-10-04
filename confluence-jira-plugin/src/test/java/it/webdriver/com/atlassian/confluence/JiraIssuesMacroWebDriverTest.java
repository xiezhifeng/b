package it.webdriver.com.atlassian.confluence;

import com.atlassian.confluence.it.Page;
import com.atlassian.confluence.it.User;
import com.atlassian.confluence.pageobjects.component.editor.MacroPlaceholder;
import com.atlassian.confluence.pageobjects.page.content.EditContentPage;
import com.atlassian.confluence.pageobjects.page.content.ViewPage;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.Poller;
import it.webdriver.com.atlassian.confluence.pageobjects.JiraMacroDialog;
import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;

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
        EditContentPage editContentPage = jiraMacroDialog.clickInsertDialog();
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

    @Test
    public void testRefreshCacheHaveDataChange()
    {
        ViewPage viewPage = createPageWithTableJiraIssueMacro();
        EditContentPage editContentPage = insertNewIssue(viewPage);
        viewPage = editContentPage.save();
        validateCacheResult(viewPage, 1);
    }

    @Test
    public void testRefreshCacheHaveSameData()
    {
        ViewPage viewPage = createPageWithTableJiraIssueMacro();
        validateCacheResult(viewPage, 0);
    }

    private void validateCacheResult(ViewPage viewPage, int numOfNewIssues)
    {
        PageElement mainContent = viewPage.getMainContent();
        int numberOfIssues = getNumberIssue(mainContent);
        clickRefreshedIcon(mainContent);
        Poller.waitUntilTrue(mainContent.find(By.cssSelector("table.aui")).timed().isVisible());
        Assert.assertTrue(numberOfIssues + numOfNewIssues == getNumberIssue(mainContent));
    }

    private ViewPage createPageWithTableJiraIssueMacro()
    {
        JiraMacroDialog jiraMacroDialog = openSelectMacroDialog();
        jiraMacroDialog.inputJqlSearch("status=open");
        jiraMacroDialog.clickSearchButton();
        EditContentPage editContentPage = jiraMacroDialog.clickInsertDialog();
        waitForMacroOnEditor(editContentPage, "jira");
        return editContentPage.save();
    }

    private EditContentPage insertNewIssue(ViewPage viewPage)
    {
        EditContentPage editContentPage = viewPage.edit();
        editContentPage.openInsertMenu();
        JiraMacroDialog jiraMacroDialog = product.getPageBinder().bind(JiraMacroDialog.class);
        jiraMacroDialog.open();
        jiraMacroDialog.selectMenuItem("Create New Issue");
        jiraMacroDialog.selectProject("10000");
        jiraMacroDialog.selectIssueType("1");
        jiraMacroDialog.setSummary("TEST CACHE");
        editContentPage = jiraMacroDialog.clickInsertDialog();
        waitForMacroOnEditor(editContentPage, "jira");
        return editContentPage;
    }

    private int getNumberIssue(PageElement mainContent)
    {
        return mainContent.findAll(By.cssSelector("table.aui .rowNormal")).size()
                + mainContent.findAll(By.cssSelector("table.aui .rowAlternate")).size();
    }

    private void clickRefreshedIcon(PageElement mainContent)
    {
        PageElement refreshedIcon = mainContent.find(By.cssSelector(".icon-refresh"));
        refreshedIcon.click();
    }
}
