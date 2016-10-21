package it.com.atlassian.confluence.plugins.webdriver.jiraissues.searchpanel.pageview;

import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.Poller;
import it.com.atlassian.confluence.plugins.webdriver.helper.ApplinkHelper;
import it.com.atlassian.confluence.plugins.webdriver.jiraissues.searchpanel.AbstractJiraIssueMacroSearchPanelTest;
import it.com.atlassian.confluence.plugins.webdriver.pageobjects.DisplayOptionPanel;
import it.com.atlassian.confluence.plugins.webdriver.pageobjects.JiraIssuesPage;
import org.json.JSONException;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class JiraIssueCreateMacroTest extends AbstractJiraIssueMacroSearchPanelTest
{
    private static String searchStr = "project = TP";

    @Test
    public void testCreateLinkMacroWithParamCount() throws Exception
    {
        openJiraIssueSearchPanelAndStartSearch(searchStr);
        jiraMacroSearchPanelDialog.openDisplayOption();
        DisplayOptionPanel displayOptionPanel = jiraMacroSearchPanelDialog.getDisplayOptionPanel();
        displayOptionPanel.clickDisplayTotalCount();

        editContentPage = jiraMacroSearchPanelDialog.clickInsertDialog();
        editContentPage.getEditor().getContent().waitForInlineMacro(JIRA_ISSUE_MACRO_NAME);
        editContentPage.save();

        JiraIssuesPage jiraIssuesPage = bindCurrentPageToJiraIssues();
        assertEquals(2, jiraIssuesPage.getIssueCount());
    }

    @Test
    public void testCreatePageWithParamColumnMacro() throws Exception
    {
        openJiraIssueSearchPanelAndStartSearch(searchStr);
        jiraMacroSearchPanelDialog.openDisplayOption();
        DisplayOptionPanel displayOptionPanel = jiraMacroSearchPanelDialog.getDisplayOptionPanel();
        displayOptionPanel.removeAllColumns();
        displayOptionPanel.addColumn("Key", "Summary");

        editContentPage = jiraMacroSearchPanelDialog.clickInsertDialog();
        editContentPage.getEditor().getContent().waitForInlineMacro(JIRA_ISSUE_MACRO_NAME);
        editContentPage.save();

        JiraIssuesPage jiraIssuesPage = bindCurrentPageToJiraIssues();
        List<PageElement> columns = jiraIssuesPage.getIssuesTableColumns();

        assertEquals(2, columns.size());
        assertTrue(columns.get(0).getText().contains("Key"));
        assertTrue(columns.get(1).getText().contains("Summary"));
    }

    @Test
    public void testUserViewIssueWhenNotHavePermission() throws InterruptedException
    {
        editContentPage.getEditor().getContent().setContent("{jira:key=TP-10|cache=off}");
        editContentPage.getEditor().getContent().waitForInlineMacro(JIRA_ISSUE_MACRO_NAME);
        editContentPage.save();
        JiraIssuesPage jiraIssuesPage = bindCurrentPageToJiraIssues();
        assertTrue(jiraIssuesPage.getErrorMessage().hasClass("jim-error-message-single"));
    }

    @Test
    public void testUserViewIssueWhenNotMapping() throws JSONException, IOException
    {
        String authArgs = getAuthQueryString();
        ApplinkHelper.removeAllAppLink(client, authArgs);
        String appLinkId = ApplinkHelper.createAppLink(client, "jiratest", authArgs, JIRA_BASE_URL, JIRA_DISPLAY_URL, true);
        ApplinkHelper.enableApplinkOauthMode(client, appLinkId, authArgs);

        editContentPage.getEditor().getContent().setContent("{jira:key=TP-10|cache=off}");
        editContentPage.getEditor().getContent().waitForInlineMacro(JIRA_ISSUE_MACRO_NAME);
        editContentPage.save();

        JiraIssuesPage jiraIssuesPage = bindCurrentPageToJiraIssues();
        Poller.waitUntilTrue(jiraIssuesPage.isSingleContainText("TP-10 - Authenticate to see issue details"));

        resetUpAppLink();
    }

    private void resetUpAppLink() throws JSONException, IOException
    {
        ApplinkHelper.removeAllAppLink(client, getAuthQueryString());
        webSudo();
        ApplinkHelper.setupAppLink(ApplinkHelper.ApplinkMode.BASIC, client, getAuthQueryString(), getBasicQueryString());
    }
}
