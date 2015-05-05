package it.webdriver.com.atlassian.confluence.jiraissues.searchedpanel;

import com.atlassian.confluence.pageobjects.component.editor.MacroPlaceholder;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.Poller;
import it.webdriver.com.atlassian.confluence.pageobjects.JiraIssuesPage;
import org.junit.Assert;
import org.junit.Test;


public class VerifyOldMacroWebDriverTest extends AbstractJiraIssuesSearchPanelWebDriverTest
{

    @Test
    public void testConvertJiraIssueToJiraWithXML()
    {
        String jiraMacro = "{jiraissues:http://127.0.0.1:11990/jira/sr/jira.issueviews:searchrequest-xml/temp/SearchRequest.xml?jqlQuery=project+%3D+TP}";
        convertJiraIssuesToJiraMacro(jiraMacro, "project = TP");
        Assert.assertTrue(getMacroParams().contains("jqlQuery= project \\= TP"));
    }

    @Test
    public void testConvertJiraIssueToJiraWithKey() {
        String jiraIssuesMacro = "{jiraissues:key=TP-1}";
        convertJiraIssuesToJiraMacro(jiraIssuesMacro, "key = TP-1");
        Assert.assertTrue(getMacroParams().contains("key=TP-1"));
    }

    @Test
    public void testNoSummaryButtonInTableIssue()
    {
        MacroPlaceholder macroPlaceholder = convertToMacroPlaceholder("{jiraissues:status=open}");
        PageElement showSummary = getJiraMacroPropertyPanel(macroPlaceholder).getPropertyPanel(".macro-property-panel-show-summary.hidden");
        Assert.assertTrue(showSummary.isPresent());
    }

    @Test
    public void testClickShowSummaryFromHideStatus()
    {
        MacroPlaceholder macroPlaceholder = convertToMacroPlaceholder("{jiraissues:key=TP-1|showSummary=false}");
        PageElement showSummary = getJiraMacroPropertyPanel(macroPlaceholder).getPropertyPanel(".macro-property-panel-show-summary");
        Assert.assertTrue(showSummary.isVisible());

        showSummary.click();
        Assert.assertTrue(getPreviewContent().contains("Bug 01"));
    }

    @Test
    public void testConvertJiraIssueToJiraWithSummary()
    {
        convertToMacroPlaceholder("{jiraissues:key=TP-1|showSummary=true}");
        Assert.assertTrue(getPreviewContent().contains("Bug 01"));
    }

    @Test
    public void testConvertJiraIssueToJiraWithoutSummary()
    {
        convertToMacroPlaceholder("{jiraissues:key=TP-1|showSummary=false}");
        Assert.assertFalse(getPreviewContent().contains("Bug 01"));
    }

    @Test
    public void testConvertJiraIssueToJiraWithColumns()
    {
        convertJiraIssuesToJiraMacro("{jiraissues:status=open|columns=key,summary,type}", "status = open");
        Assert.assertTrue(getMacroParams().contains("columns=key,summary,type"));
    }

    @Test
    public void testConvertJiraIssueToJiraWithCount()
    {
        convertJiraIssuesToJiraMacro("{jiraissues:status=open|count=true}", "status = open");
        Assert.assertTrue(getMacroParams().contains("count=true"));

    }

    @Test
    public void testVerifyJiraIssuesWithRenderDynamic()
    {
        convertToMacroPlaceholder("{jiraissues:status=open|width=400|renderMode=dynamic}");
        waitUntilInlineMacroAppearsInEditor(editContentPage, OLD_JIRA_ISSUE_MACRO_NAME);
        editContentPage.save();
        JiraIssuesPage jiraIssuesPage = bindCurrentPageToJiraIssues();
        Poller.waitUntilTrue(jiraIssuesPage.getDynamicJiraIssueTable().timed().isVisible());
    }

}
