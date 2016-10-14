package it.com.atlassian.confluence.plugins.webdriver.jiraissues.searchpanel.pageview;

import com.atlassian.pageobjects.elements.query.Poller;
import it.com.atlassian.confluence.plugins.webdriver.helper.ApplinkHelper;
import it.com.atlassian.confluence.plugins.webdriver.jiraissues.searchpanel.AbstractJiraIssuesSearchPanelTest;
import it.com.atlassian.confluence.plugins.webdriver.pageobjects.JiraIssuesPage;
import org.apache.commons.lang3.StringUtils;
import org.hamcrest.core.StringContains;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class JiraIssuesSearch extends AbstractJiraIssuesSearchPanelTest
{
    private String globalTestAppLinkId;

    @After
    public void deleteAppLink() throws Exception
    {
        if (StringUtils.isNotEmpty(globalTestAppLinkId))
        {
            ApplinkHelper.deleteApplink(client, globalTestAppLinkId, getAuthQueryString());
        }
        globalTestAppLinkId = "";
    }

    @Test
    public void testPasteUrlWithJiraServerNoPermission() throws Exception
    {
        editPage.cancel();
        //create oath applink
        String jiraURL = "http://jira.test.com";
        String authArgs = getAuthQueryString();
        String appLinkId = ApplinkHelper.createAppLink(client, "TEST", authArgs, jiraURL, jiraURL, false);
        globalTestAppLinkId = appLinkId;
        ApplinkHelper.enableApplinkOauthMode(client, appLinkId, authArgs);
        editPage = gotoEditTestPage(user.get());
        
        jiraMacroSearchPanelDialog = openJiraIssueSearchPanelDialogFromMacroBrowser(editPage);
        jiraMacroSearchPanelDialog.pasteJqlSearch(jiraURL + "/browse/TST-1");

        Poller.waitUntilTrue(jiraMacroSearchPanelDialog.hasInfoMessage());
        Assert.assertThat(jiraMacroSearchPanelDialog.getInfoMessage(), StringContains.containsString("Login & Approve to retrieve data from TEST"));
        Assert.assertFalse(jiraMacroSearchPanelDialog.getSearchButton().isEnabled());
    }
}
