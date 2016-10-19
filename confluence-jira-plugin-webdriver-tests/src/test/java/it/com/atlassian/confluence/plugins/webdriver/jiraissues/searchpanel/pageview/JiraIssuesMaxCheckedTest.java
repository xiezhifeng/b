package it.com.atlassian.confluence.plugins.webdriver.jiraissues.searchpanel.pageview;

import com.atlassian.pageobjects.elements.PageElement;
import it.com.atlassian.confluence.plugins.webdriver.jiraissues.searchpanel.AbstractJiraIssueMacroSearchPanelTest;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class JiraIssuesMaxCheckedTest extends AbstractJiraIssueMacroSearchPanelTest
{
    @Test
    public void checkMaxIssueHappyCase() throws Exception
    {
        jiraMacroSearchPanelDialog = openJiraIssueSearchPanelDialogFromMacroBrowser();
        jiraMacroSearchPanelDialog.showDisplayOption();
        jiraMacroSearchPanelDialog.fillMaxIssues("1");
        List<PageElement> issues = jiraMacroSearchPanelDialog.insertAndSave();
        assertNotNull(issues);
        assertEquals(1, issues.size());
    }
}
