package it.com.atlassian.confluence.plugins.webdriver.jiraissues.searchpanel.pageview;

import java.util.List;

import com.atlassian.pageobjects.elements.PageElement;

import it.com.atlassian.confluence.plugins.webdriver.jiraissues.searchpanel.AbstractJiraIssuesSearchPanelTest;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class JiraIssuesMaxCheckedTest extends AbstractJiraIssuesSearchPanelTest
{
    @Test
    public void checkMaxIssueHappyCase() throws Exception
    {
        jiraMacroSearchPanelDialog = openJiraIssueSearchPanelDialogFromMacroBrowser(editPage);
        jiraMacroSearchPanelDialog.showDisplayOption();
        jiraMacroSearchPanelDialog.fillMaxIssues("1");
        List<PageElement> issuses = jiraMacroSearchPanelDialog.insertAndSave();
        assertNotNull(issuses);
        assertEquals(1, issuses.size());
    }
}
