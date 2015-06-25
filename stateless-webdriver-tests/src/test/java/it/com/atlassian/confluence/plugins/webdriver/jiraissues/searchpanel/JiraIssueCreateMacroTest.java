package it.com.atlassian.confluence.plugins.webdriver.jiraissues.searchpanel;

import com.atlassian.confluence.plugins.helper.ApplinkHelper;
import com.atlassian.confluence.plugins.pageobjects.DisplayOptionPanel;
import com.atlassian.confluence.plugins.pageobjects.JiraIssuesPage;
import com.atlassian.confluence.test.properties.TestProperties;
import com.atlassian.pageobjects.elements.PageElement;
import org.apache.commons.httpclient.HttpClient;
import org.json.JSONException;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertTrue;

public class JiraIssueCreateMacroTest extends AbstractJiraIssuesSearchPanelTest
{

    private static String searchStr = "project = TP";

    @Test
    public void testCreateLinkMacroWithDefault()
    {
        editPage = search(searchStr).clickInsertDialog();
        waitUntilInlineMacroAppearsInEditor(editPage, JIRA_ISSUE_MACRO_NAME);
        String htmlContent = editPage.getEditor().getContent().getTimedHtml().now();
        assertTrue(htmlContent.contains("/confluence/download/resources/confluence.extra.jira/jira-table.png"));
    }

    @Test
    public void testCreateLinkMacroWithParamCount()
    {
        search(searchStr);
        jiraMacroSearchPanelDialog.openDisplayOption();
        DisplayOptionPanel displayOptionPanel = jiraMacroSearchPanelDialog.getDisplayOptionPanel();
        displayOptionPanel.clickDisplayTotalCount();

        editPage = jiraMacroSearchPanelDialog.clickInsertDialog();
        waitUntilInlineMacroAppearsInEditor(editPage, JIRA_ISSUE_MACRO_NAME);
        editPage.save();

        JiraIssuesPage jiraIssuesPage = bindCurrentPageToJiraIssues();
        Assert.assertEquals(2, jiraIssuesPage.getIssueCount());
    }

    @Test
    public void testCreatePageWithParamColumnMacro()
    {
        search(searchStr);
        jiraMacroSearchPanelDialog.openDisplayOption();
        DisplayOptionPanel displayOptionPanel = jiraMacroSearchPanelDialog.getDisplayOptionPanel();
        displayOptionPanel.removeAllColumns();
        displayOptionPanel.addColumn("Key", "Summary");

        editPage = jiraMacroSearchPanelDialog.clickInsertDialog();
        waitUntilInlineMacroAppearsInEditor(editPage, JIRA_ISSUE_MACRO_NAME);
        editPage.save();

        JiraIssuesPage jiraIssuesPage = bindCurrentPageToJiraIssues();
        List<PageElement> columns = jiraIssuesPage.getIssuesTableColumns();

        Assert.assertEquals(2, columns.size());
        Assert.assertTrue(columns.get(0).getText().contains("Key"));
        Assert.assertTrue(columns.get(1).getText().contains("Summary"));
    }


    @Test
    public void testSearchNoResult()
    {
        search("InvalidValue");
        Assert.assertTrue(jiraMacroSearchPanelDialog.getInfoMessage().contains("No search results found."));
    }

    @Test
    public void testDisableOption()
    {
        search("TP-2");
        jiraMacroSearchPanelDialog.openDisplayOption();
        DisplayOptionPanel displayOptionPanel = jiraMacroSearchPanelDialog.getDisplayOptionPanel();
        Assert.assertTrue(displayOptionPanel.isInsertTableIssueEnable());
        Assert.assertFalse(displayOptionPanel.isInsertCountIssueEnable());
    }

    @Test
    public void testDisabledOptionWithMultipleIssues()
    {
        search("key in (TP-1, TP-2)");

        jiraMacroSearchPanelDialog.clickSelectIssueOption("TP-1");
        Assert.assertFalse(jiraMacroSearchPanelDialog.isSelectAllIssueOptionChecked());

        jiraMacroSearchPanelDialog.clickSelectIssueOption("TP-1");
        Assert.assertTrue(jiraMacroSearchPanelDialog.isSelectAllIssueOptionChecked());

        jiraMacroSearchPanelDialog.clickSelectAllIssueOption();
        jiraMacroSearchPanelDialog.clickSelectIssueOption("TP-1");

        jiraMacroSearchPanelDialog.openDisplayOption();
        DisplayOptionPanel displayOptionPanel = jiraMacroSearchPanelDialog.getDisplayOptionPanel();
        Assert.assertTrue(displayOptionPanel.isInsertTableIssueEnable());
        Assert.assertFalse(displayOptionPanel.isInsertCountIssueEnable());
    }

    @Test
    public void testRemoveColumnWithTwoTimesBackSpace()
    {
        search("key in (TP-1, TP-2)");
        jiraMacroSearchPanelDialog.openDisplayOption();
        DisplayOptionPanel displayOptionPanel = jiraMacroSearchPanelDialog.getDisplayOptionPanel();
        Assert.assertEquals(11, displayOptionPanel.getSelectedColumns().size());

        displayOptionPanel.typeSelect2Input("\u0008\u0008");
        Assert.assertEquals(10, displayOptionPanel.getSelectedColumns().size());
    }

    @Test
    public void testAddColumnByKey()
    {
        search("key in (TP-1, TP-2)");
        jiraMacroSearchPanelDialog.openDisplayOption();
        DisplayOptionPanel displayOptionPanel = jiraMacroSearchPanelDialog.getDisplayOptionPanel();
        Assert.assertEquals(11, displayOptionPanel.getSelectedColumns().size());

        displayOptionPanel.typeSelect2Input("Security");
        displayOptionPanel.sendReturnKeyToAddedColoumn();

        Assert.assertEquals(12, displayOptionPanel.getSelectedColumns().size());
    }

    @Test
    public void testUserViewIssueWhenNotHavePermission() throws InterruptedException
    {
        editPage.getEditor().getContent().setContent("{jira:key=TP-10|cache=off}");
        waitUntilInlineMacroAppearsInEditor(editPage, JIRA_ISSUE_MACRO_NAME);
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
        waitUntilInlineMacroAppearsInEditor(editPage, JIRA_ISSUE_MACRO_NAME);
        editPage.save();

        JiraIssuesPage jiraIssuesPage = bindCurrentPageToJiraIssues();
        Assert.assertTrue(jiraIssuesPage.isSingleContainText("TP-10 - Authenticate to see issue details"));

        resetupAppLink(client, authArgs);
    }

    private void resetupAppLink(HttpClient client, String authArg) throws JSONException, IOException
    {
        ApplinkHelper.removeAllAppLink(client, authArg);

        doWebSudo(client);

        if (!TestProperties.isOnDemandMode())
        {
            ApplinkHelper.setupAppLink(ApplinkHelper.ApplinkMode.BASIC, client, authArg, getBasicQueryString());
        }
    }
}
