package it.com.atlassian.confluence.plugins.webdriver.jiraissues.searchpanel;

import java.util.List;

import com.atlassian.confluence.it.TestProperties;
import com.atlassian.pageobjects.elements.query.Poller;
import com.atlassian.test.categories.OnDemandSuiteTest;

import org.apache.commons.httpclient.HttpStatus;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static it.com.atlassian.confluence.plugins.webdriver.helper.JiraRestHelper.createJiraFilter;
import static it.com.atlassian.confluence.plugins.webdriver.helper.JiraRestHelper.deleteJiraFilter;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@Category(OnDemandSuiteTest.class)
@Ignore
public class JiraIssuesSearchOnDemandSuiteTest extends AbstractJiraIssuesSearchPanelWithoutSavingTest
{
    @Test
    public void testSearchWithButton() throws Exception
    {
        openJiraIssueSearchPanelAndStartSearch("TST-1");
        Poller.waitUntilTrue(jiraMacroSearchPanelDialog.isIssueExistInSearchResult("TST-1"));
    }

    @Test
    public void testSearchWithFilterHaveJQL() throws Exception
    {
        String filterId = "10000";

        if (TestProperties.isOnDemandMode())
        {
            filterId = createJiraFilter("All Open Bugs", "status=open", "", client);
            checkNotNull(filterId);
        }

        openJiraIssueSearchPanelAndStartSearch(JIRA_DISPLAY_URL + "/issues/?filter=" + filterId);
        Poller.waitUntilTrue(jiraMacroSearchPanelDialog.isIssueExistInSearchResult("TSTT-5"));
        Poller.waitUntilTrue(jiraMacroSearchPanelDialog.isIssueExistInSearchResult("TSTT-4"));
        assertEquals(deleteJiraFilter(filterId, client), HttpStatus.SC_NO_CONTENT);
    }

    @Test
    public void checkColumnLoadDefaultWhenInsert() throws Exception
    {
        insertJiraIssueMacroWithEditColumn(LIST_TEST_COLUMN, "status=open");
        jiraMacroSearchPanelDialog = openJiraIssueSearchPanelDialogFromMacroBrowser(editPage);

        assertTrue(jiraMacroSearchPanelDialog.getJqlSearchElement().getValue().equals(""));
        assertFalse(jiraMacroSearchPanelDialog.getIssuesTable().isPresent());

        jiraMacroSearchPanelDialog.inputJqlSearch("status = open");
        jiraMacroSearchPanelDialog.clickSearchButton();
        jiraMacroSearchPanelDialog.openDisplayOption();

        List<String> columns = jiraMacroSearchPanelDialog.getDisplayOptionPanel().getSelectedColumns();
        assertEquals(columns.toString(), LIST_DEFAULT_COLUMN.toString());
    }
}
