package it.com.atlassian.confluence.plugins.webdriver.jiraissues.searchpanel;

import com.atlassian.confluence.webdriver.pageobjects.component.editor.MacroPlaceholder;
import com.atlassian.pageobjects.elements.PageElement;

import org.junit.Ignore;
import org.junit.Test;

import static com.atlassian.pageobjects.elements.query.Poller.waitUntilTrue;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;

public class VerifyOldMacroWithoutSavingTest extends AbstractJiraIssuesSearchPanelWithoutSavingTest
{
    @Test
    public void testConvertJiraIssueToJiraWithXML()
    {
        String jiraMacro = "{jiraissues:" + JIRA_DISPLAY_URL + "/sr/jira.issueviews:searchrequest-xml/temp/SearchRequest.xml?jqlQuery=project+%3D+TP}";
        convertJiraIssuesToJiraMacro(editPage, jiraMacro, "project = TP", OLD_JIRA_ISSUE_MACRO_NAME);
        assertThat(getMacroParams(editPage, JIRA_ISSUE_MACRO_NAME), containsString("jqlQuery= project \\= TP"));
    }

    @Test
    @Ignore("This is a flaky test")
    public void testConvertJiraIssueToJiraWithKey() {
        String jiraIssuesMacro = "{jiraissues:key=TP-1}";
        convertJiraIssuesToJiraMacro(editPage, jiraIssuesMacro, "key = TP-1", OLD_JIRA_ISSUE_MACRO_NAME);
        assertThat(getMacroParams(editPage, JIRA_ISSUE_MACRO_NAME), containsString("key=TP-1"));
    }

    @Test
    public void testNoSummaryButtonInTableIssue()
    {
        MacroPlaceholder macroPlaceholder = createMacroPlaceholderFromQueryString(editPage, "{jiraissues:status=open}", OLD_JIRA_ISSUE_MACRO_NAME);
        PageElement showSummary = getJiraMacroPropertyPanel(macroPlaceholder).getPropertyPanel(".macro-property-panel-show-summary.hidden");
        waitUntilTrue(showSummary.timed().isPresent());
    }

    @Test
    @Ignore("This is a flaky test")
    public void testConvertJiraIssueToJiraWithColumns()
    {
        convertJiraIssuesToJiraMacro(editPage, "{jiraissues:status=open|columns=key,summary,type}", "status = open", OLD_JIRA_ISSUE_MACRO_NAME);
        assertThat(getMacroParams(editPage, JIRA_ISSUE_MACRO_NAME), containsString("columns=key,summary,type"));
    }

    @Test
    @Ignore("This is a flaky test")
    public void testConvertJiraIssueToJiraWithCount()
    {
        convertJiraIssuesToJiraMacro(editPage, "{jiraissues:status=open|count=true}", "status = open", OLD_JIRA_ISSUE_MACRO_NAME);
        assertThat(getMacroParams(editPage, JIRA_ISSUE_MACRO_NAME), containsString("count=true"));
    }
}