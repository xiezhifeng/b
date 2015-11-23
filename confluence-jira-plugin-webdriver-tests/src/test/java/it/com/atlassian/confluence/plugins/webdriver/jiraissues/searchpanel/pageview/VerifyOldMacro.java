package it.com.atlassian.confluence.plugins.webdriver.jiraissues.searchpanel.pageview;

import it.com.atlassian.confluence.plugins.webdriver.jiraissues.searchpanel.AbstractJiraIssuesSearchPanelTest;
import it.com.atlassian.confluence.plugins.webdriver.pageobjects.JiraIssuesPage;
import com.atlassian.confluence.webdriver.pageobjects.component.editor.MacroPlaceholder;
import com.atlassian.confluence.webdriver.pageobjects.page.content.ViewPage;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.Poller;

import org.hamcrest.Matchers;
import org.junit.Test;

import static com.atlassian.pageobjects.elements.query.Poller.waitUntilTrue;

public class VerifyOldMacro extends AbstractJiraIssuesSearchPanelTest
{
    @Test
    public void testVerifyJiraIssuesWithRenderDynamic()
    {
        createMacroPlaceholderFromQueryString(editPage, "{jiraissues:status=open|width=400|renderMode=dynamic}", OLD_JIRA_ISSUE_MACRO_NAME);
        editPage.getEditor().getContent().waitForInlineMacro(OLD_JIRA_ISSUE_MACRO_NAME);
        viewPage = editPage.save();
        JiraIssuesPage jiraIssuesPage = bindCurrentPageToJiraIssues();
        waitUntilTrue(jiraIssuesPage.getDynamicJiraIssueTable().timed().isVisible());
    }

    @Test
    public void testClickShowSummaryFromHideStatus()
    {
        MacroPlaceholder macroPlaceholder = createMacroPlaceholderFromQueryString(editPage, "{jiraissues:key=TP-1|showSummary=false}", OLD_JIRA_ISSUE_MACRO_NAME);
        PageElement showSummary = getJiraMacroPropertyPanel(macroPlaceholder).getPropertyPanel(".macro-property-panel-show-summary");
        waitUntilTrue(showSummary.timed().isVisible());

        showSummary.click();

        ViewPage viewPage = editPage.save();
        Poller.waitUntil(viewPage.getRenderedContent().getTextTimed(), Matchers.containsString("Bug 01"));
    }

    @Test
    public void testConvertJiraIssueToJiraWithSummary()
    {
        createMacroPlaceholderFromQueryString(editPage, "{jiraissues:key=TP-1|showSummary=true}", OLD_JIRA_ISSUE_MACRO_NAME);

        ViewPage viewPage = editPage.save();
        Poller.waitUntil(viewPage.getRenderedContent().getTextTimed(), Matchers.containsString("Bug 01"));
    }

    @Test
    public void testConvertJiraIssueToJiraWithoutSummary()
    {
        createMacroPlaceholderFromQueryString(editPage, "{jiraissues:key=TP-1|showSummary=false}", OLD_JIRA_ISSUE_MACRO_NAME);

        ViewPage viewPage = editPage.save();
        Poller.waitUntil(viewPage.getRenderedContent().getTextTimed(), Matchers.containsString("OPEN"));
        Poller.waitUntil(viewPage.getRenderedContent().getTextTimed(), Matchers.not(Matchers.containsString("Bug 01")));
    }
}
