package it.webdriver.com.atlassian.confluence.jiraissues.searchedpanel;

import com.atlassian.confluence.pageobjects.component.editor.MacroPlaceholder;
import com.atlassian.pageobjects.elements.PageElement;

import org.junit.Test;

import it.webdriver.com.atlassian.confluence.pageobjects.JiraIssuesPage;
import jdk.nashorn.internal.ir.annotations.Ignore;

import static com.atlassian.pageobjects.elements.query.Poller.waitUntilTrue;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

@Ignore
public class VerifyOldMacroWebDriverTest extends AbstractJiraIssuesSearchPanelWebDriverTest
{
    @Test
    public void testConvertJiraIssueToJiraWithXML()
    {
        String jiraMacro = "{jiraissues:http://127.0.0.1:11990/jira/sr/jira.issueviews:searchrequest-xml/temp/SearchRequest.xml?jqlQuery=project+%3D+TP}";
        convertJiraIssuesToJiraMacro(jiraMacro, "project = TP");
        assertThat(getMacroParams(), containsString("jqlQuery= project \\= TP"));
    }

    @Test
    public void testConvertJiraIssueToJiraWithKey() {
        String jiraIssuesMacro = "{jiraissues:key=TP-1}";
        convertJiraIssuesToJiraMacro(jiraIssuesMacro, "key = TP-1");
        assertThat(getMacroParams(), containsString("key=TP-1"));
    }

    @Test
    public void testNoSummaryButtonInTableIssue()
    {
        MacroPlaceholder macroPlaceholder = convertToMacroPlaceholder("{jiraissues:status=open}");
        PageElement showSummary = getJiraMacroPropertyPanel(macroPlaceholder).getPropertyPanel(".macro-property-panel-show-summary.hidden");
        waitUntilTrue(showSummary.timed().isPresent());
    }

    @Test
    public void testClickShowSummaryFromHideStatus()
    {
        MacroPlaceholder macroPlaceholder = convertToMacroPlaceholder("{jiraissues:key=TP-1|showSummary=false}");
        PageElement showSummary = getJiraMacroPropertyPanel(macroPlaceholder).getPropertyPanel(".macro-property-panel-show-summary");
        waitUntilTrue(showSummary.timed().isVisible());

        showSummary.click();
        assertThat(getPreviewContent(), containsString("Bug 01"));
    }

    @Test
    public void testConvertJiraIssueToJiraWithSummary()
    {
        convertToMacroPlaceholder("{jiraissues:key=TP-1|showSummary=true}");
        assertThat(getPreviewContent(), containsString("Bug 01"));
    }

    @Test
    public void testConvertJiraIssueToJiraWithoutSummary()
    {
        convertToMacroPlaceholder("{jiraissues:key=TP-1|showSummary=false}");
        assertThat(getPreviewContent(), not(containsString("Bug 01")));
    }

    @Test
    public void testConvertJiraIssueToJiraWithColumns()
    {
        convertJiraIssuesToJiraMacro("{jiraissues:status=open|columns=key,summary,type}", "status = open");
        assertThat(getMacroParams(), containsString("columns=key,summary,type"));
    }

    @Test
    public void testConvertJiraIssueToJiraWithCount()
    {
        convertJiraIssuesToJiraMacro("{jiraissues:status=open|count=true}", "status = open");
        assertThat(getMacroParams(), containsString("count=true"));
    }

    @Test
    public void testVerifyJiraIssuesWithRenderDynamic()
    {
        convertToMacroPlaceholder("{jiraissues:status=open|width=400|renderMode=dynamic}");
        waitUntilInlineMacroAppearsInEditor(editContentPage, OLD_JIRA_ISSUE_MACRO_NAME);
        editContentPage.save();
        JiraIssuesPage jiraIssuesPage = bindCurrentPageToJiraIssues();
        waitUntilTrue(jiraIssuesPage.getDynamicJiraIssueTable().timed().isVisible());
    }

}
