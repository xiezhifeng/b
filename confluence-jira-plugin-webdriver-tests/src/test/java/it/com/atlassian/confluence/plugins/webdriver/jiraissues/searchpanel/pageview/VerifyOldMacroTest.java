package it.com.atlassian.confluence.plugins.webdriver.jiraissues.searchpanel.pageview;

import com.atlassian.confluence.webdriver.pageobjects.component.editor.MacroPlaceholder;
import com.atlassian.confluence.webdriver.pageobjects.page.content.ViewPage;
import com.atlassian.pageobjects.elements.PageElement;
import it.com.atlassian.confluence.plugins.webdriver.jiraissues.searchpanel.AbstractJiraIssueMacroSearchPanelTest;
import it.com.atlassian.confluence.plugins.webdriver.pageobjects.JiraIssuesPage;
import org.junit.Test;

import static com.atlassian.pageobjects.elements.query.Poller.waitUntil;
import static com.atlassian.pageobjects.elements.query.Poller.waitUntilTrue;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

public class VerifyOldMacroTest extends AbstractJiraIssueMacroSearchPanelTest
{
    @Test
    public void testVerifyJiraIssuesWithRenderDynamic()
    {
        createMacroPlaceholderFromQueryString("{jiraissues:status=open|width=400|renderMode=dynamic}", OLD_JIRA_ISSUE_MACRO_NAME);
        editContentPage.getEditor().getContent().waitForInlineMacro(OLD_JIRA_ISSUE_MACRO_NAME);
        editContentPage.save();
        JiraIssuesPage jiraIssuesPage = bindCurrentPageToJiraIssues();
        waitUntilTrue(jiraIssuesPage.getDynamicJiraIssueTable().timed().isVisible());
    }

    @Test
    public void testClickShowSummaryFromHideStatus()
    {
        MacroPlaceholder macroPlaceholder = createMacroPlaceholderFromQueryString("{jiraissues:key=TP-1|showSummary=false}", OLD_JIRA_ISSUE_MACRO_NAME);
        PageElement showSummary = getJiraMacroPropertyPanel(macroPlaceholder).getPropertyPanel(".macro-property-panel-show-summary");
        waitUntilTrue(showSummary.timed().isVisible());

        showSummary.click();

        ViewPage viewPage = editContentPage.save();
        waitUntil(viewPage.getRenderedContent().getTextTimed(), containsString("Bug 01"));
    }

    @Test
    public void testConvertJiraIssueToJiraWithSummary()
    {
        createMacroPlaceholderFromQueryString("{jiraissues:key=TP-1|showSummary=true}", OLD_JIRA_ISSUE_MACRO_NAME);

        ViewPage viewPage = editContentPage.save();
        waitUntil(viewPage.getRenderedContent().getTextTimed(), containsString("Bug 01"));
    }

    @Test
    public void testConvertJiraIssueToJiraWithoutSummary()
    {
        createMacroPlaceholderFromQueryString("{jiraissues:key=TP-1|showSummary=false}", OLD_JIRA_ISSUE_MACRO_NAME);

        ViewPage viewPage = editContentPage.save();
        waitUntil(viewPage.getRenderedContent().getTextTimed(), containsString("OPEN"));
        waitUntil(viewPage.getRenderedContent().getTextTimed(), not(containsString("Bug 01")));
    }

}
