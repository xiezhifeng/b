package it.webdriver.com.atlassian.confluence.jiraissues.searchedpanel;

import com.atlassian.confluence.pageobjects.page.content.EditContentPage;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.Poller;
import it.webdriver.com.atlassian.confluence.helper.ApplinkHelper;
import it.webdriver.com.atlassian.confluence.pageobjects.DisplayOptionPanel;
import it.webdriver.com.atlassian.confluence.pageobjects.JiraIssuesPage;
import org.json.JSONException;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertTrue;

public class JiraIssueCreateMacroWebDriverTest extends AbstractJiraIssuesSearchPanelWebDriverTest
{

    private static String searchStr = "project = TP";

    @Test
    public void testCreateLinkMacroWithDefault()
    {
        EditContentPage editContentPage = search(searchStr).clickInsertDialog();
        waitUntilInlineMacroAppearsInEditor(editContentPage, JIRA_ISSUE_MACRO_NAME);
        String htmlContent = editContentPage.getEditor().getContent().getTimedHtml().now();
        assertTrue(htmlContent.contains("/confluence/download/resources/confluence.extra.jira/jira-table.png"));
    }

    @Test
    public void testCreateLinkMacroWithParamCount()
    {
        search(searchStr);
        jiraIssuesDialog.openDisplayOption();
        DisplayOptionPanel displayOptionPanel = jiraIssuesDialog.getDisplayOptionPanel();
        displayOptionPanel.clickDisplayTotalCount();

        EditContentPage editPage = jiraIssuesDialog.clickInsertDialog();
        waitUntilInlineMacroAppearsInEditor(editContentPage, JIRA_ISSUE_MACRO_NAME);
        editPage.save();

        JiraIssuesPage jiraIssuesPage = bindCurrentPageToJiraIssues();
        Assert.assertEquals(2, jiraIssuesPage.getIssueCount());
    }

    @Test
    public void testCreatePageWithParamColumnMacro()
    {
        search(searchStr);
        jiraIssuesDialog.openDisplayOption();
        DisplayOptionPanel displayOptionPanel = jiraIssuesDialog.getDisplayOptionPanel();
        displayOptionPanel.removeAllColumns();
        displayOptionPanel.addColumn("Key", "Summary");

        EditContentPage editPage = jiraIssuesDialog.clickInsertDialog();
        waitUntilInlineMacroAppearsInEditor(editContentPage, JIRA_ISSUE_MACRO_NAME);
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
        Assert.assertTrue(jiraIssuesDialog.getInfoMessage().contains("No search results found."));
    }

    @Test
    public void testDisableOption()
    {
        search("TP-2");
        DisplayOptionPanel displayOptionPanel = jiraIssuesDialog.getDisplayOptionPanel();
        Assert.assertTrue(displayOptionPanel.isInsertTableIssueEnable());
        Assert.assertFalse(displayOptionPanel.isInsertCountIssueEnable());
    }

    @Test
    public void testDisabledOptionWithMultipleIssues()
    {
        search("key in (TP-1, TP-2)");

        jiraIssuesDialog.clickSelectIssueOption("TP-1");
        Assert.assertFalse(jiraIssuesDialog.isSelectAllIssueOptionChecked());

        jiraIssuesDialog.clickSelectIssueOption("TP-1");
        Assert.assertTrue(jiraIssuesDialog.isSelectAllIssueOptionChecked());

        jiraIssuesDialog.clickSelectAllIssueOption();
        jiraIssuesDialog.clickSelectIssueOption("TP-1");

        DisplayOptionPanel displayOptionPanel = jiraIssuesDialog.getDisplayOptionPanel();
        Assert.assertTrue(displayOptionPanel.isInsertTableIssueEnable());
        Assert.assertFalse(displayOptionPanel.isInsertCountIssueEnable());
    }

    @Test
    public void testRemoveColumnWithTwoTimesBackSpace()
    {
        search("key in (TP-1, TP-2)");
        jiraIssuesDialog.openDisplayOption();
        DisplayOptionPanel displayOptionPanel = jiraIssuesDialog.getDisplayOptionPanel();
        Assert.assertEquals(11, displayOptionPanel.getSelectedColumns().size());

        displayOptionPanel.typeSelect2Input("\u0008\u0008");
        Assert.assertEquals(10, displayOptionPanel.getSelectedColumns().size());
    }

    @Test
    public void testAddColumnByKey()
    {
        search("key in (TP-1, TP-2)");
        jiraIssuesDialog.openDisplayOption();
        DisplayOptionPanel displayOptionPanel = jiraIssuesDialog.getDisplayOptionPanel();
        Assert.assertEquals(11, displayOptionPanel.getSelectedColumns().size());

        displayOptionPanel.typeSelect2Input("Security");
        displayOptionPanel.sendReturnKeyToAddedColoumn();

        Assert.assertEquals(12, displayOptionPanel.getSelectedColumns().size());
    }

    @Test
    public void testUserViewIssueWhenNotHavePermission() throws InterruptedException
    {
        editContentPage.getEditor().getContent().setContent("{jira:key=TP-10|cache=off}");
        waitUntilInlineMacroAppearsInEditor(editContentPage, JIRA_ISSUE_MACRO_NAME);
        editContentPage.save();
        JiraIssuesPage jiraIssuesPage = bindCurrentPageToJiraIssues();
        Assert.assertTrue(jiraIssuesPage.getErrorMessage().hasClass("jim-error-message-single"));
    }

    @Test
    public void testUserViewIssueWhenNotMapping() throws JSONException, IOException
    {
        ApplinkHelper.removeAllAppLink(client, authArgs);
        String applinkId = ApplinkHelper.createAppLink(client, "jiratest", authArgs, JIRA_BASE_URL, JIRA_DISPLAY_URL, true);
        ApplinkHelper.enableApplinkOauthMode(client, applinkId, authArgs);

        editContentPage.getEditor().getContent().setContent("{jira:key=TP-10|cache=off}");
        waitUntilInlineMacroAppearsInEditor(editContentPage, JIRA_ISSUE_MACRO_NAME);
        editContentPage.save();

        JiraIssuesPage jiraIssuesPage = bindCurrentPageToJiraIssues();
        Assert.assertTrue(jiraIssuesPage.isSingleContainText("TP-10 - Authenticate to see issue details"));
    }

}
