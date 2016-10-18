package it.com.atlassian.confluence.plugins.webdriver.jiraissues.searchpanel.macrobrowser;

import com.atlassian.confluence.webdriver.pageobjects.component.editor.MacroPlaceholder;
import com.atlassian.pageobjects.elements.PageElement;
import it.com.atlassian.confluence.plugins.webdriver.jiraissues.searchpanel.AbstractJiraIssuesSearchPanelTest;
import org.junit.Assert;
import org.junit.Test;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;

public class VerifyOldMacro extends AbstractJiraIssuesSearchPanelTest
{
    @Test
    public void testConvertJiraIssueToJiraWithXML()
    {
        String jiraMacro = "{jiraissues:" + JIRA_DISPLAY_URL + "/sr/jira.issueviews:searchrequest-xml/temp/SearchRequest.xml?jqlQuery=project+%3D+TP}";
        convertJiraIssuesToJiraMacro(editPage, jiraMacro, "project = TP", OLD_JIRA_ISSUE_MACRO_NAME);
        assertThat(getMacroParams(editPage, JIRA_ISSUE_MACRO_NAME), containsString("jqlQuery= project \\= TP"));
    }

    @Test
    public void testConvertJiraIssueToJiraWithKey() {
        String jiraIssuesMacro = "{jiraissues:key=TP-1}";
        convertJiraIssuesToJiraMacro(editPage, jiraIssuesMacro, "key = TP-1", OLD_JIRA_ISSUE_MACRO_NAME);
        assertThat(getMacroParams(editPage, JIRA_ISSUE_MACRO_NAME), containsString("key=TP-1"));
    }

    @Test
    public void testNoSummaryButtonInTableIssue()
    {
        MacroPlaceholder macroPlaceholder = createMacroPlaceholderFromQueryString(editPage, "{jiraissues:status=open}", OLD_JIRA_ISSUE_MACRO_NAME);
        PageElement showSummary = getJiraMacroPropertyPanel(macroPlaceholder).getPropertyPanel(".macro-property-panel-show-summary");
        Assert.assertTrue(!showSummary.isPresent() || showSummary.hasClass("hidden"));
    }

    @Test
    public void testConvertJiraIssueToJiraWithColumns()
    {
        convertJiraIssuesToJiraMacro(editPage, "{jiraissues:status=open|columns=key,summary,type}", "status = open", OLD_JIRA_ISSUE_MACRO_NAME);
        assertThat(getMacroParams(editPage, JIRA_ISSUE_MACRO_NAME), containsString("columns=key,summary,type"));
    }

    @Test
    public void testConvertJiraIssueToJiraWithCount()
    {
        convertJiraIssuesToJiraMacro(editPage, "{jiraissues:status=open|count=true}", "status = open", OLD_JIRA_ISSUE_MACRO_NAME);
        assertThat(getMacroParams(editPage, JIRA_ISSUE_MACRO_NAME), containsString("count=true"));
    }
}
