package it.com.atlassian.confluence.plugins.webdriver.jiraissues.searchpanel.pageview;

import java.util.List;

import com.atlassian.pageobjects.elements.PageElement;

import it.com.atlassian.confluence.plugins.webdriver.jiraissues.searchpanel.AbstractJiraIssuesSearchPanelTest;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class JiraIssuesMaxChecked extends AbstractJiraIssuesSearchPanelTest
{
    @Test
    public void checkMaxIssueHappyCase() throws Exception
    {
        dialogSearchPanel = openJiraIssueSearchPanelDialogFromMacroBrowser(editPage);
        dialogSearchPanel.showDisplayOption();
        dialogSearchPanel.fillMaxIssues("1");
        List<PageElement> issuses = dialogSearchPanel.insertAndSave();
        assertNotNull(issuses);
        assertEquals(1, issuses.size());
    }
}
