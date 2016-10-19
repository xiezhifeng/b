package it.com.atlassian.confluence.plugins.webdriver.jiraissues.searchpanel.macrobrowser;

import com.atlassian.confluence.webdriver.pageobjects.component.editor.MacroPlaceholder;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.Poller;
import it.com.atlassian.confluence.plugins.webdriver.jiraissues.searchpanel.AbstractJiraIssueMacroSearchPanelTest;
import it.com.atlassian.confluence.plugins.webdriver.pageobjects.jiraissuefillter.JiraMacroSearchPanelDialog;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;

public class VerifyOldMacroBrowserTest extends AbstractJiraIssueMacroSearchPanelTest
{
    private static final String OLD_JIRA_ISSUE_MACRO_NAME = "jiraissues";

    @Test
    public void testConvertJiraIssueToJiraWithXML()
    {
        String jiraMacro = "{jiraissues:" + JIRA_DISPLAY_URL + "/sr/jira.issueviews:searchrequest-xml/temp/SearchRequest.xml?jqlQuery=project+%3D+TP}";
        convertJiraIssuesToJiraMacro(jiraMacro, "project = TP", OLD_JIRA_ISSUE_MACRO_NAME);
        assertThat(getMacroParams(JIRA_ISSUE_MACRO_NAME), containsString("jqlQuery= project \\= TP"));
    }

    @Test
    public void testConvertJiraIssueToJiraWithKey() {
        String jiraIssuesMacro = "{jiraissues:key=TP-1}";
        convertJiraIssuesToJiraMacro(jiraIssuesMacro, "key = TP-1", OLD_JIRA_ISSUE_MACRO_NAME);
        assertThat(getMacroParams(JIRA_ISSUE_MACRO_NAME), containsString("key=TP-1"));
    }

    @Test
    public void testNoSummaryButtonInTableIssue()
    {
        MacroPlaceholder macroPlaceholder = createMacroPlaceholderFromQueryString("{jiraissues:status=open}", OLD_JIRA_ISSUE_MACRO_NAME);
        PageElement showSummary = getJiraMacroPropertyPanel(macroPlaceholder).getPropertyPanel(".macro-property-panel-show-summary");
        Assert.assertTrue(!showSummary.isPresent() || showSummary.hasClass("hidden"));
    }

    @Test
    public void testConvertJiraIssueToJiraWithColumns()
    {
        convertJiraIssuesToJiraMacro("{jiraissues:status=open|columns=key,summary,type}", "status = open", OLD_JIRA_ISSUE_MACRO_NAME);
        assertThat(getMacroParams(JIRA_ISSUE_MACRO_NAME), containsString("columns=key,summary,type"));
    }

    @Test
    public void testConvertJiraIssueToJiraWithCount()
    {
        convertJiraIssuesToJiraMacro("{jiraissues:status=open|count=true}", "status = open", OLD_JIRA_ISSUE_MACRO_NAME);
        assertThat(getMacroParams(JIRA_ISSUE_MACRO_NAME), containsString("count=true"));
    }

    private void convertJiraIssuesToJiraMacro(String jiraIssuesMacro, String jql, String macroName) {
        MacroPlaceholder macroPlaceholder = createMacroPlaceholderFromQueryString(jiraIssuesMacro, macroName);

        JiraMacroSearchPanelDialog dialog = openJiraIssuesDialogFromMacroPlaceholder(macroPlaceholder);
        dialog.clickSearchButton();
        Poller.waitUntil(dialog.getJqlSearchElement().timed().getValue(), Matchers.containsString(jql));
        dialog.clickInsertDialog();
    }

    private String getMacroParams(String macroName) {
        MacroPlaceholder macroPlaceholder = editContentPage.getEditor().getContent().macroPlaceholderFor(macroName).iterator().next();
        return macroPlaceholder.getAttribute("data-macro-parameters");
    }
}
