package it.com.atlassian.confluence.plugins.jira.selenium;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.MultivaluedMap;

import net.sourceforge.jwebunit.junit.WebTester;
import net.sourceforge.jwebunit.util.TestingEngineRegistry;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.atlassian.confluence.plugin.functest.AbstractConfluencePluginWebTestCase;
import com.atlassian.confluence.plugin.functest.JWebUnitConfluenceWebTester;
import com.atlassian.selenium.SeleniumAssertions;
import com.atlassian.selenium.SeleniumClient;
import com.atlassian.selenium.browsers.AutoInstallClient;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import com.thoughtworks.selenium.SeleniumException;

public class AbstractJiraDialogTestCase extends AbstractConfluencePluginWebTestCase
{
    private static final Logger LOG = Logger.getLogger(AbstractJiraDialogTestCase.class);
    
    protected static final String TEST_SPACE_KEY = "ds";
    protected static final String JIRA_URL = System.getProperty("baseurl.jira1", "http://localhost:11990/jira");

    private static final String APPLINK_WS = "http://localhost:1990/confluence/rest/applinks/1.0/applicationlink";

    protected WebTester jiraWebTester;

    protected SeleniumClient client = AutoInstallClient.seleniumClient();
    protected SeleniumAssertions assertThat = AutoInstallClient.assertThat();

    static {
        // prevent AutoInstallClient from using the wrong default ...
        LOG.debug("***** setting system properties");
        String confluenceBaseUrl = System.getProperty("baseurl", "http://localhost:1990/confluence");
        System.setProperty("baseurl", confluenceBaseUrl);
        // default was 3.5.9 which does not work on master anymore
        String defaultBrowser = System.getProperty("selenium.browser", "firefox-3.6");
        System.setProperty("selenium.browser", defaultBrowser);
    }

    @Override
    protected void setUp() throws Exception
    {
        LOG.debug("***** setting up");
        super.setUp();
        setupJiraWebTester();
        loginToJira("admin", "admin");
        //requireApplink();
    }
    
    private void setupJiraWebTester() throws IOException
    {
        LOG.debug("***** setupJiraWebTester");
        jiraWebTester = new WebTester();
        jiraWebTester.setTestingEngineKey(TestingEngineRegistry.TESTING_ENGINE_HTMLUNIT);
        jiraWebTester.setScriptingEnabled(false);
        jiraWebTester.getTestContext().setBaseUrl(System.getProperty("baseurl.jira1", "http://localhost:11990/jira"));

        jiraWebTester.beginAt("/");
    }

    protected void loginToJira(String userName, String password)
    {
        jiraWebTester.gotoPage("/login.jsp");
        jiraWebTester.setWorkingForm("login-form");
        jiraWebTester.setTextField("os_username", userName);
        jiraWebTester.setTextField("os_password", password);
        jiraWebTester.submit();

        assertLinkPresentWithText("Log Out");
    }

    protected String getAuthQueryString(String adminUserName, String adminPassword)
    {
        return  "?os_username=" + adminUserName + "&os_password=" + adminPassword;
    }
    protected void logout()
    {
        if (client.isElementPresent("logout-link"))
            client.click("logout-link");
    }
    protected void login()
    {
        client.open("login.action");
        client.waitForPageToLoad();
        try
        {
            client.type("//input[@name = 'os_username']", getConfluenceWebTester().getAdminUserName());
        } catch (SeleniumException e)
        {
            // already logged in, no need to have further process
            if (e.getMessage().contains("//input[@name = 'os_username'] not found"))
            {
                return;
            }
        }
        client.type("//input[@name = 'os_password']", getConfluenceWebTester().getAdminPassword());
        client.click("//input[@name = 'login']");
        client.waitForPageToLoad();
    }
    
    public void restoreData()
    {
        if (getAllApplinkIds().isEmpty()) {
            super.restoreData();
        }
    }
    
    public void forceRestoreData() {
        super.restoreData();
    }

    /*
     * private void disablePlugin(String... pluginIds) { try { ConfluenceRpc rpc
     * = ConfluenceRpc.newInstance(getConfluenceWebTester().getBaseUrl()); User
     * adminUser = new User( getConfluenceWebTester().getAdminUserName(),
     * getConfluenceWebTester().getAdminPassword(), null, null);
     * rpc.logIn(adminUser);
     * 
     * PluginHelper pluginHelper = rpc.getPluginHelper(); for (String pluginId :
     * pluginIds) { Plugin plugin = new SimplePlugin(pluginId, null);
     * pluginHelper.disablePlugin(plugin); } } catch (Exception e) { // probably
     * rpc-funct-test plugin not installed, ignore } }
     */

    //remove config applink
    public void removeApplink()
    {

        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        queryParams.add("os_username", getConfluenceWebTester().getAdminUserName());
        queryParams.add("os_password", getConfluenceWebTester().getAdminPassword());

        Client clientJersey = Client.create();
        WebResource webResource = clientJersey.resource(APPLINK_WS);

        //delete all server config in applink
        for(String id: getAllApplinkIds())
        {
            String response = webResource.path(id).queryParams(queryParams).accept("application/json, text/javascript, */*").delete(String.class);
            try
            {
                final JSONObject jsonObj = new JSONObject(response);
                int status = jsonObj.getInt("status-code");
                assertEquals(200, status);
            } catch (JSONException e) {
                assertTrue(false);
            }
        }
    }
    
    private Collection<String> getAllApplinkIds()
    {
        
        WebResource webResource = null;

        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        queryParams.add("os_username", getConfluenceWebTester().getAdminUserName());
        queryParams.add("os_password", getConfluenceWebTester().getAdminPassword());
        Set<String> ids  = new HashSet<String>();

        //get list server in applink
        try
        {
            Client clientJersey = Client.create();
            webResource = clientJersey.resource(APPLINK_WS);

            String result = webResource.queryParams(queryParams).accept("application/json, text/javascript, */*").get(String.class);
            final JSONObject jsonObj = new JSONObject(result);
            JSONArray jsonArray = jsonObj.getJSONArray("applicationLinks");
            for(int i = 0; i< jsonArray.length(); i++) {
                final String id = jsonArray.getJSONObject(i).getString("id");
                assertNotNull(id);
                ids.add(id);
            }
        } catch (Exception e)
        {
            assertTrue(false);
        }
        return ids;
    }

    protected String addJiraAppLink(String name, String url, String displayUrl,
            boolean isPrimary) throws HttpException, IOException, JSONException {
        return addJiraAppLink(name, url, displayUrl, isPrimary, false);
    }

        protected String addJiraAppLink(String name, String url, String displayUrl,
            boolean isPrimary, boolean isTrusted) throws HttpException, IOException, JSONException {
        final String adminUserName = getConfluenceWebTester()
                .getAdminUserName();
        final String adminPassword = getConfluenceWebTester()
                .getAdminPassword();
        final String authArgs = getAuthQueryString(adminUserName, adminPassword);

        final HttpClient client = new HttpClient();
        final String baseUrl = ((JWebUnitConfluenceWebTester) tester)
                .getBaseUrl();

        final PostMethod m = new PostMethod(baseUrl
                + "/rest/applinks/1.0/applicationlinkForm/createAppLink"
                + authArgs);

        m.setRequestHeader("Accept", "application/json, text/javascript, */*");
        // add new Jira server with set primary for selected default
        final String reqBody = "{\"applicationLink\":{\"typeId\":\"jira\",\"name\":\""
                + name
                + "\",\"rpcUrl\":\""
                + url
                + "\",\"displayUrl\":\""
                + displayUrl
                + "\",\"isPrimary\":"
                + String.valueOf(isPrimary)
                + "},\"username\":\"\",\"password\":\"\",\"createTwoWayLink\":"+String.valueOf(isTrusted)+",\"customRpcURL\":false,\"rpcUrl\":\"\",\"configFormValues\":{\"trustEachOther\":"+String.valueOf(isTrusted)+",\"shareUserbase\":"+String.valueOf(isTrusted)+"}}";
        final StringRequestEntity reqEntity = new StringRequestEntity(reqBody,
                "application/json", "UTF-8");
        m.setRequestEntity(reqEntity);

        final int status = client.executeMethod(m);
        assertEquals(200, status);

        final JSONObject jsonObj = new JSONObject(m.getResponseBodyAsString());
        final String id = jsonObj.getJSONObject("applicationLink").getString(
                "id");
        return id;
    }
    
}
