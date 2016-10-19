package it.com.atlassian.confluence.plugins.webdriver.jiraissues.searchpanel.pageview;

import com.atlassian.confluence.webdriver.pageobjects.component.editor.MacroPlaceholder;
import com.atlassian.confluence.webdriver.pageobjects.page.content.ViewPage;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.Poller;
import it.com.atlassian.confluence.plugins.webdriver.jiraissues.searchpanel.AbstractJiraIssueMacroSearchPanelTest;
import it.com.atlassian.confluence.plugins.webdriver.pageobjects.JiraIssuesPage;
import org.hamcrest.Matchers;
import org.junit.Test;

import static com.atlassian.pageobjects.elements.query.Poller.waitUntilTrue;

public class VerifyOldMacro extends AbstractJiraIssueMacroSearchPanelTest
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
        Poller.waitUntil(viewPage.getRenderedContent().getTextTimed(), Matchers.containsString("Bug 01"));
    }

    @Test
    public void testConvertJiraIssueToJiraWithSummary()
    {
        createMacroPlaceholderFromQueryString("{jiraissues:key=TP-1|showSummary=true}", OLD_JIRA_ISSUE_MACRO_NAME);

        ViewPage viewPage = editContentPage.save();
        Poller.waitUntil(viewPage.getRenderedContent().getTextTimed(), Matchers.containsString("Bug 01"));
    }

    @Test
    public void testConvertJiraIssueToJiraWithoutSummary()
    {
        createMacroPlaceholderFromQueryString("{jiraissues:key=TP-1|showSummary=false}", OLD_JIRA_ISSUE_MACRO_NAME);

        ViewPage viewPage = editContentPage.save();
        Poller.waitUntil(viewPage.getRenderedContent().getTextTimed(), Matchers.containsString("OPEN"));
        Poller.waitUntil(viewPage.getRenderedContent().getTextTimed(), Matchers.not(Matchers.containsString("Bug 01")));
    }

}
