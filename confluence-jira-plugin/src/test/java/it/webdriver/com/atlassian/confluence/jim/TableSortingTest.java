package it.webdriver.com.atlassian.confluence.jim;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import it.webdriver.com.atlassian.confluence.pageobjects.JiraIssuesPage;

import org.junit.Test;

public class TableSortingTest extends AbstractJIMTest
{

    @Test
    public void testSortIssueTable()
    {
        JiraIssuesPage page = createPageWithTableJiraIssueMacroAndJQL("project = TSTT");
        String KeyValueAtFirstTimeLoad = page.getFirstRowValueOfSummay();
        page.clickHeaderIssueTable("Summary");
        String keyAfterSort = page.getFirstRowValueOfSummay();
        assertNotSame(KeyValueAtFirstTimeLoad, keyAfterSort);
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
        waitUntilInlineMacroAppearsInEditor(editContentPage, "jira");
        editContentPage.save();
        JiraIssuesPage page = product.getPageBinder().bind(JiraIssuesPage.class);
        String keyValueAtFirstTime = page.getFirstRowValueOfSummay();
        page.clickHeaderIssueTable("Linked Issues");
        String keyAfterSort = page.getFirstRowValueOfSummay();
        assertEquals(keyValueAtFirstTime, keyAfterSort);
    }
    
}
