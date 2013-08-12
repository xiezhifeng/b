package it.com.atlassian.confluence.plugins.jira.selenium;

import java.io.IOException;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.atlassian.confluence.plugin.functest.JWebUnitConfluenceWebTester;

public class PasteUrlDialogTestCase extends AbstractJiraPanelTestCase
{

    @Override
    public void setUp() throws Exception
    {
        super.setUp();
        client.waitForPageToLoad();
    }

    @Override
    public void tearDown() throws Exception
    {
        super.tearDown();
    }

    /**
     * Test for Paste an url that contain server not existed in the system
     */
    public void testPasteUrlWithNoJiraServer()
    {
        openJiraDialog();
        // jira server url do not existed
        String pasteUrl = "http://anotherserver.com/jira/browse/TST-1";

        client.click("//li/button[text()='Search']");

        client.type("css=input[name='jiraSearch']", pasteUrl);

        client.clickAndWaitForAjaxWithJquery("css=div.jira-search-form button");
        // warning popup is displayed
        assertThat.elementVisible("css=a#open_applinks");
        // can not click insert jira button
        assertThat.attributeContainsValue("css=button.insert-issue-button",
                "disabled", "true");
    }

    /**
     * Paste an URL that contain server existed in the system and user has the
     * right to search on it
     */
    public void testPasteUrlWithJiraServer() throws HttpException, IOException,
            JSONException
    {

        // create another jira app link with primary true for default selected
        // in select jira servers list
        String serverName = "JIRA TEST SERVER1";
        String serverUrl = "http://jira.test.com";
        String serverDisplayUrl = "http://jira.test.com";
        addJiraAppLink(serverName, serverUrl, serverDisplayUrl, false);

        client.refresh();
        client.waitForPageToLoad();

        // url for paste
        String pasteServerUrl = "http://localhost:11990/jira";
        // get server id match with paste server url
        String pasteServerId = getServerId(pasteServerUrl);

        // search url using paste into search input
        String pasteSearchUrl = pasteServerUrl + "/browse/TST-1";

        openJiraDialog();

        client.click("//li/button[text()='Search']");

        client.type("css=input[name='jiraSearch']", (pasteSearchUrl));
        client.clickAndWaitForAjaxWithJquery("css=div.jira-search-form button");

        // check auto select jira server match with paste server url
        String selectedServerId = client
                .getSelectedValue("css=#my-jira-search select.select");
        assertTrue(selectedServerId.equals(pasteServerId));

        client.clickAndWaitForAjaxWithJquery("css=button.insert-issue-button",
                3000);

        // validate insert issue
        validateParamInLinkMacro("key=TST-1");
    }

    /*
     * Paste an URL that contain server existed in the system and user has no
     * right to search on it
     */
    public void testPasteUrlWithJiraServerNoPermission() throws HttpException,
            IOException, JSONException
    {

        // create another jira app link
        String serverName = "JIRA TEST SERVER2";
        String serverUrl = "http://jira.test.com";
        String serverDisplayUrl = "http://jira.test.com";
        String serverId = addJiraAppLink(serverName, serverUrl,
                serverDisplayUrl, false);
        // set Server using Oauth
        enableOauthWithApplink(serverId);

        // refresh when add new server
        client.refresh();
        client.waitForPageToLoad();

        openJiraDialog();

        String pasteSearchUrl = serverUrl + "/browse/TST-1";

        client.click("//li/button[text()='Search']");

        client.type("css=input[name='jiraSearch']", pasteSearchUrl);
        client.clickAndWaitForAjaxWithJquery("css=div.jira-search-form button");

        // check display Log & Approve link
        assertThat.elementVisible("css=a.oauth-init");

        // check can not insert jira issue
        assertThat.attributeContainsValue("css=button.insert-issue-button",
                "disabled", "true");
    }

    /*
     * Verify the ability to search by XML
     */
    public void testPasteXmlUrl() throws HttpException, IOException,
            JSONException
    {

        // xml url for paste
        String pasteXmlUrl = "http://localhost:11990/jira/si/jira.issueviews:issue-xml/TST-1/TST-1.xml";

        openJiraDialog();

        client.click("//li/button[text()='Search']");

        client.type("css=input[name='jiraSearch']", pasteXmlUrl);
        client.clickAndWaitForAjaxWithJquery("css=div.jira-search-form button");

        client.clickAndWaitForAjaxWithJquery("css=button.insert-issue-button",
                5000);

        // validate insert issue
        validateParamInLinkMacro("key=TST-1");
    }

    private String getServerId(String serverUrl) throws HttpException,
            IOException, JSONException
    {
        String serverId = "";
        JSONArray jiraservers = getJiraServers();

        for (int i = 0; i < jiraservers.length(); ++i)
        {

            JSONObject jiraServer = jiraservers.getJSONObject(i);
            String jiraServerUrl = jiraServer.getString("url").toLowerCase();

            if (jiraServerUrl.equals(serverUrl))
            {
                serverId = jiraServer.getString("id");
            }
        }
        return serverId;
    }

    private JSONArray getJiraServers() throws HttpException, IOException,
            JSONException
    {

        String adminUserName = getConfluenceWebTester().getAdminUserName();
        String adminPassword = getConfluenceWebTester().getAdminPassword();
        String authArgs = getAuthQueryString(adminUserName, adminPassword);

        HttpClient client = new HttpClient();
        final String baseUrl = ((JWebUnitConfluenceWebTester) tester)
                .getBaseUrl();

        final String url = baseUrl + "/rest/jiraanywhere/1.0/servers"
                + authArgs;
        GetMethod m = new GetMethod(url);

        m.setRequestHeader("Accept", "application/json, text/javascript, */*");

        int status = client.executeMethod(m);
        assertEquals("Got status " + status + " when retrieving " + url, 200,
                status);

        final String responseBody = m.getResponseBodyAsString();

        m.releaseConnection();

        return new JSONArray(responseBody);
    }
}
