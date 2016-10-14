package it.com.atlassian.confluence.plugins.webdriver.jiraissues.searchpanel.pageview;

import java.io.IOException;
import java.util.List;

import com.atlassian.pageobjects.elements.query.Poller;
import it.com.atlassian.confluence.plugins.webdriver.helper.ApplinkHelper;
import it.com.atlassian.confluence.plugins.webdriver.jiraissues.searchpanel.AbstractJiraIssuesSearchPanelTest;
import it.com.atlassian.confluence.plugins.webdriver.pageobjects.DisplayOptionPanel;
import it.com.atlassian.confluence.plugins.webdriver.pageobjects.JiraIssuesPage;
import com.atlassian.confluence.test.properties.TestProperties;
import com.atlassian.pageobjects.elements.PageElement;

import org.apache.commons.httpclient.HttpClient;
import org.apache.http.impl.client.CloseableHttpClient;
import org.json.JSONException;
import org.junit.Assert;
import org.junit.Test;

public class JiraIssueCreateMacro extends AbstractJiraIssuesSearchPanelTest
{
    private static String searchStr = "project = TP";

    @Test
    public void testCreateLinkMacroWithParamCount() throws Exception
    {
        openJiraIssueSearchPanelAndStartSearch(searchStr);
        jiraMacroSearchPanelDialog.openDisplayOption();
        DisplayOptionPanel displayOptionPanel = jiraMacroSearchPanelDialog.getDisplayOptionPanel();
        displayOptionPanel.clickDisplayTotalCount();

        editPage = jiraMacroSearchPanelDialog.clickInsertDialog();
        editPage.getEditor().getContent().waitForInlineMacro(JIRA_ISSUE_MACRO_NAME);
        editPage.save();

        JiraIssuesPage jiraIssuesPage = bindCurrentPageToJiraIssues();
        Assert.assertEquals(2, jiraIssuesPage.getIssueCount());
    }

    @Test
    public void testCreatePageWithParamColumnMacro() throws Exception
    {
        openJiraIssueSearchPanelAndStartSearch(searchStr);
        jiraMacroSearchPanelDialog.openDisplayOption();
        DisplayOptionPanel displayOptionPanel = jiraMacroSearchPanelDialog.getDisplayOptionPanel();
        displayOptionPanel.removeAllColumns();
        displayOptionPanel.addColumn("Key", "Summary");

        editPage = jiraMacroSearchPanelDialog.clickInsertDialog();
        editPage.getEditor().getContent().waitForInlineMacro(JIRA_ISSUE_MACRO_NAME);
        editPage.save();

        JiraIssuesPage jiraIssuesPage = bindCurrentPageToJiraIssues();
        List<PageElement> columns = jiraIssuesPage.getIssuesTableColumns();

        Assert.assertEquals(2, columns.size());
        Assert.assertTrue(columns.get(0).getText().contains("Key"));
        Assert.assertTrue(columns.get(1).getText().contains("Summary"));
    }

    @Test
    public void testUserViewIssueWhenNotHavePermission() throws InterruptedException
    {
        editPage.getEditor().getContent().setContent("{jira:key=TP-10|cache=off}");
        editPage.getEditor().getContent().waitForInlineMacro(JIRA_ISSUE_MACRO_NAME);
        editPage.save();
        JiraIssuesPage jiraIssuesPage = bindCurrentPageToJiraIssues();
        Assert.assertTrue(jiraIssuesPage.getErrorMessage().hasClass("jim-error-message-single"));
    }

    @Test
    public void testUserViewIssueWhenNotMapping() throws JSONException, IOException
    {
        String authArgs = getAuthQueryString();
        ApplinkHelper.removeAllAppLink(client, authArgs);
        String applinkId = ApplinkHelper.createAppLink(client, "jiratest", authArgs, JIRA_BASE_URL, JIRA_DISPLAY_URL, true);
        ApplinkHelper.enableApplinkOauthMode(client, applinkId, authArgs);

        editPage.getEditor().getContent().setContent("{jira:key=TP-10|cache=off}");
        editPage.getEditor().getContent().waitForInlineMacro(JIRA_ISSUE_MACRO_NAME);
        editPage.save();

        JiraIssuesPage jiraIssuesPage = bindCurrentPageToJiraIssues();
        Poller.waitUntilTrue(jiraIssuesPage.isSingleContainText("TP-10 - Authenticate to see issue details"));

        resetupAppLink(client, authArgs);
    }

    private void resetupAppLink(CloseableHttpClient client, String authArg) throws JSONException, IOException
    {
        ApplinkHelper.removeAllAppLink(client, authArg);

        doWebSudo(client);

        if (!TestProperties.isOnDemandMode())
        {
            ApplinkHelper.setupAppLink(ApplinkHelper.ApplinkMode.BASIC, client, authArg, getBasicQueryString());
        }
    }
}
