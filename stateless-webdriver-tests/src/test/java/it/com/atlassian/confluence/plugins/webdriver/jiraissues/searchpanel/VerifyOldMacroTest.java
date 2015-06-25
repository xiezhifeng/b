package it.com.atlassian.confluence.plugins.webdriver.jiraissues.searchpanel;

import com.atlassian.confluence.plugins.pageobjects.JiraIssuesPage;
import com.atlassian.confluence.webdriver.pageobjects.component.editor.MacroPlaceholder;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.Poller;

import org.junit.Test;

import static com.atlassian.pageobjects.elements.query.Poller.waitUntilTrue;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

public class VerifyOldMacroTest extends AbstractJiraIssuesSearchPanelTest
{
    @Test
    public void testConvertJiraIssueToJiraWithXML()
    {
        String jiraMacro = "{jiraissues:" + JIRA_DISPLAY_URL + "/sr/jira.issueviews:searchrequest-xml/temp/SearchRequest.xml?jqlQuery=project+%3D+TP}";
        convertJiraIssuesToJiraMacro(editPage, jiraMacro, "project = TP", OLD_JIRA_ISSUE_MACRO_NAME);
        assertThat(getMacroParams(editPage), containsString("jqlQuery= project \\= TP"));
    }

    @Test
    public void testConvertJiraIssueToJiraWithKey() {
        String jiraIssuesMacro = "{jiraissues:key=TP-1}";
        convertJiraIssuesToJiraMacro(editPage,jiraIssuesMacro, "key = TP-1", OLD_JIRA_ISSUE_MACRO_NAME);
        assertThat(getMacroParams(editPage), containsString("key=TP-1"));
    }

    @Test
    public void testNoSummaryButtonInTableIssue()
    {
        MacroPlaceholder macroPlaceholder = createMacroPlaceholderFromQueryString(editPage, "{jiraissues:status=open}", OLD_JIRA_ISSUE_MACRO_NAME);
        PageElement showSummary = getJiraMacroPropertyPanel(macroPlaceholder).getPropertyPanel(".macro-property-panel-show-summary.hidden");
        waitUntilTrue(showSummary.timed().isPresent());
    }

    @Test
    public void testClickShowSummaryFromHideStatus()
    {
        MacroPlaceholder macroPlaceholder = createMacroPlaceholderFromQueryString(editPage, "{jiraissues:key=TP-1|showSummary=false}", OLD_JIRA_ISSUE_MACRO_NAME);
        PageElement showSummary = getJiraMacroPropertyPanel(macroPlaceholder).getPropertyPanel(".macro-property-panel-show-summary");
        waitUntilTrue(showSummary.timed().isVisible());

        showSummary.click();

        Poller.waitUntilTrue(getEditorPreview().containsContent("Bug 01"));
    }

    @Test
    public void testConvertJiraIssueToJiraWithSummary()
    {
        createMacroPlaceholderFromQueryString(editPage, "{jiraissues:key=TP-1|showSummary=true}", OLD_JIRA_ISSUE_MACRO_NAME);
        Poller.waitUntilTrue(getEditorPreview().containsContent("Bug 01"));
    }

    @Test
    public void testConvertJiraIssueToJiraWithoutSummary()
    {
        createMacroPlaceholderFromQueryString(editPage, "{jiraissues:key=TP-1|showSummary=false}", OLD_JIRA_ISSUE_MACRO_NAME);
        Poller.waitUntilFalse(getEditorPreview().containsContent("Bug 01"));
    }

    @Test
    public void testConvertJiraIssueToJiraWithColumns()
    {
        convertJiraIssuesToJiraMacro(editPage, "{jiraissues:status=open|columns=key,summary,type}", "status = open", OLD_JIRA_ISSUE_MACRO_NAME);
        assertThat(getMacroParams(editPage), containsString("columns=key,summary,type"));
    }

    @Test
    public void testConvertJiraIssueToJiraWithCount()
    {
        convertJiraIssuesToJiraMacro(editPage, "{jiraissues:status=open|count=true}", "status = open", OLD_JIRA_ISSUE_MACRO_NAME);
        assertThat(getMacroParams(editPage), containsString("count=true"));
    }

    @Test
    public void testVerifyJiraIssuesWithRenderDynamic()
    {
        createMacroPlaceholderFromQueryString(editPage, "{jiraissues:status=open|width=400|renderMode=dynamic}", OLD_JIRA_ISSUE_MACRO_NAME);
        editPage.getEditor().getContent().waitForInlineMacro(OLD_JIRA_ISSUE_MACRO_NAME);
        viewPage = editPage.save();
        JiraIssuesPage jiraIssuesPage = bindCurrentPageToJiraIssues();
        waitUntilTrue(jiraIssuesPage.getDynamicJiraIssueTable().timed().isVisible());
    }

}
