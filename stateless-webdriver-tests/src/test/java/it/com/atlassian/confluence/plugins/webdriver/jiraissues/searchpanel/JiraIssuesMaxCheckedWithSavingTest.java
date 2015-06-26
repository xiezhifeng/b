package it.com.atlassian.confluence.plugins.webdriver.jiraissues.searchpanel;

import java.util.List;

import com.atlassian.pageobjects.elements.PageElement;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class JiraIssuesMaxCheckedWithSavingTest extends AbstractJiraIssuesSearchPanelTest
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
