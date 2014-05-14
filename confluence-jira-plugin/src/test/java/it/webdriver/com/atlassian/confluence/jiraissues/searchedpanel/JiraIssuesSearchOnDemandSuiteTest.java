package it.webdriver.com.atlassian.confluence.jiraissues.searchedpanel;

import java.util.List;

import com.atlassian.confluence.it.TestProperties;
import com.atlassian.test.categories.OnDemandSuiteTest;

import org.apache.commons.httpclient.HttpStatus;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import it.webdriver.com.atlassian.confluence.pageobjects.JiraIssuesPage;

import static com.google.common.base.Preconditions.checkNotNull;
import static it.webdriver.com.atlassian.confluence.helper.JiraRestHelper.createJiraFilter;
import static it.webdriver.com.atlassian.confluence.helper.JiraRestHelper.deleteJiraFilter;
import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@Category(OnDemandSuiteTest.class)
public class JiraIssuesSearchOnDemandSuiteTest extends AbstractJiraIssuesSearchPanelWebDriverTest
{
    @Test
    public void testSearchWithButton()
    {
        search("TST-1");
        //assertTrue(jiraIssuesDialog.isIssueExistInSearchResult("TSTT-1"));
        assertTrue(jiraIssuesDialog.isIssueExistInSearchResult("TST-1"));
    }

    @Test
    public void testSearchWithFilterHaveJQL()
    {
        String filterId = "10000";

        if (TestProperties.isOnDemandMode())
        {
            filterId = createJiraFilter("All Open Bugs", "status=open", "", client);
            checkNotNull(filterId);
        }

        search(JIRA_DISPLAY_URL + "/issues/?filter=" + filterId);
        assertTrue(jiraIssuesDialog.isIssueExistInSearchResult("TSTT-5"));
        assertTrue(jiraIssuesDialog.isIssueExistInSearchResult("TSTT-4"));
        assertEquals(deleteJiraFilter(filterId, client), HttpStatus.SC_NO_CONTENT);
    }

    @Test
    public void testColumnNotSupportSortableInIssueTable()
    {
        jiraIssuesDialog = openJiraIssuesDialog();
        jiraIssuesDialog.inputJqlSearch("status = open");
        jiraIssuesDialog.clickSearchButton();
        jiraIssuesDialog.openDisplayOption();
        jiraIssuesDialog.getDisplayOptionPanel().addColumn("Linked Issues");
        jiraIssuesDialog.clickInsertDialog();
        waitUntilInlineMacroAppearsInEditor(editContentPage, JIRA_ISSUE_MACRO_NAME);
        editContentPage.getEditor().clickSaveAndWaitForPageChange();
        JiraIssuesPage page = product.getPageBinder().bind(JiraIssuesPage.class);
        String keyValueAtFirstTime = page.getFirstRowValueOfSummay();
        page.clickColumnHeaderIssueTable("Linked Issues");
        String keyAfterSort = page.getFirstRowValueOfSummay();
        Assert.assertEquals(keyValueAtFirstTime, keyAfterSort);
    }

    @Test
    public void checkColumnLoadDefaultWhenInsert()
    {
        insertJiraIssueMacroWithEditColumn(LIST_TEST_COLUMN, "status=open");
        jiraIssuesDialog = openJiraIssuesDialog();

        assertTrue(jiraIssuesDialog.getJqlSearch().equals(""));
        assertFalse(jiraIssuesDialog.getIssuesTable().isPresent());

        jiraIssuesDialog.inputJqlSearch("status = open");
        jiraIssuesDialog.clickSearchButton();
        jiraIssuesDialog.openDisplayOption();

        List<String> columns = jiraIssuesDialog.getDisplayOptionPanel().getSelectedColumns();
        Assert.assertEquals(columns.toString(), LIST_DEFAULT_COLUMN.toString());
    }
}