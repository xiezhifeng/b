package it.com.atlassian.confluence.plugins.webdriver.jiraissues.searchpanel.pageview;

import java.io.IOException;

import com.atlassian.pageobjects.elements.query.Poller;
import it.com.atlassian.confluence.plugins.webdriver.helper.ApplinkHelper;
import it.com.atlassian.confluence.plugins.webdriver.jiraissues.searchpanel.AbstractJiraIssuesSearchPanelTest;
import it.com.atlassian.confluence.plugins.webdriver.pageobjects.JiraIssuesPage;

import org.apache.commons.lang3.StringUtils;
import org.hamcrest.core.StringContains;
import org.json.JSONException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class JiraIssuesSearch extends AbstractJiraIssuesSearchPanelTest
{
    private String globalTestAppLinkId;

    @After
    public void tearDown() throws Exception
    {
        if (StringUtils.isNotEmpty(globalTestAppLinkId))
        {
            ApplinkHelper.deleteApplink(client, globalTestAppLinkId, getAuthQueryString());
        }
        globalTestAppLinkId = "";
        super.tearDown();
    }

    @Test
    public void testColumnNotSupportSortableInIssueTable() throws Exception
    {
        dialogSearchPanel = openJiraIssueSearchPanelDialogFromMacroBrowser(editPage);
        dialogSearchPanel.inputJqlSearch("status = open");
        dialogSearchPanel.clickSearchButton();
        dialogSearchPanel.openDisplayOption();
        dialogSearchPanel.getDisplayOptionPanel().addColumn("Linked Issues");
        dialogSearchPanel.clickInsertDialog();
        editPage.getEditor().getContent().waitForInlineMacro(JIRA_ISSUE_MACRO_NAME);
        editPage.getEditor().clickSaveAndWaitForPageChange();
        JiraIssuesPage page = pageBinder.bind(JiraIssuesPage.class);
        String keyValueAtFirstTime = page.getFirstRowValueOfSummay();
        page.clickColumnHeaderIssueTable("Linked Issues",null);
        String keyAfterSort = page.getFirstRowValueOfSummay();
        assertEquals(keyValueAtFirstTime, keyAfterSort);
    }

    @Test
    public void testPasteXmlUrl() throws Exception
    {
        JiraIssuesPage jiraIssuesPage = createPageWithJiraIssueMacro(JIRA_DISPLAY_URL + "/si/jira.issueviews:issue-xml/TST-1/TST-1.xml", true);
        Poller.waitUntilTrue(jiraIssuesPage.isSingleContainText("Test bug"));
    }

    @Test
    public void testPasteUrlWithJiraServer() throws Exception
    {
        //create another primary applink
        String jiraURL = "http://jira.test.com";
        String authArgs = getAuthQueryString();
        globalTestAppLinkId = ApplinkHelper.createAppLink(client, "TEST", authArgs, jiraURL, jiraURL, true);

        JiraIssuesPage jiraIssuesPage = createPageWithJiraIssueMacro(JIRA_DISPLAY_URL + "/browse/TST-1", true);
        Poller.waitUntilTrue(jiraIssuesPage.isSingleContainText("Test bug"));
    }

    @Test
    public void testPasteUrlWithJiraServerNoPermission() throws Exception
    {
        //create oath applink
        String jiraURL = "http://jira.test.com";
        String authArgs = getAuthQueryString();
        String appLinkId = ApplinkHelper.createAppLink(client, "TEST", authArgs, jiraURL, jiraURL, false);
        globalTestAppLinkId = appLinkId;
        ApplinkHelper.enableApplinkOauthMode(client, appLinkId, authArgs);

        product.refresh();

        closeDialog(dialogSearchPanel);
        dialogSearchPanel = openJiraIssueSearchPanelDialogFromMacroBrowser(editPage);
        dialogSearchPanel.pasteJqlSearch(jiraURL + "/browse/TST-1");

        Poller.waitUntilTrue(dialogSearchPanel.hasInfoMessage());
        Assert.assertThat(dialogSearchPanel.getInfoMessage(), StringContains.containsString("Login & Approve to retrieve data from TEST"));
        Assert.assertFalse(dialogSearchPanel.getSearchButton().isEnabled());
    }
}
