package it.webdriver.com.atlassian.confluence.jim;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.atlassian.pageobjects.elements.query.Poller;

public class JiraIssuesSearchDialogTest extends AbstractJIMTest
{

    @Test
    public void testIssuesCheckBoxBehaviors()
    {
        
    }
    
    /**
     * Verify if filter is converted properly in search box
     */
    @Test
    public void checkPasteFilterUrlInJQLSearchField()
    {
        jiraIssuesDialog = openJiraIssuesDialog();
        String filterQuery = "filter=10001";
        String filterURL = "http://127.0.0.1:11990/jira/issues/?" + filterQuery;
        jiraIssuesDialog.pasteJqlSearch(filterURL);

        Poller.waitUntilTrue(jiraIssuesDialog.getJQLSearchElement().timed().isEnabled());
        Poller.waitUntilTrue(jiraIssuesDialog.getSearchButton().timed().isEnabled());
        jiraIssuesDialog.clickJqlSearch();

        assertEquals(filterQuery, jiraIssuesDialog.getJqlSearch());
    }
    
}
