package it.com.atlassian.confluence.plugins.jira.selenium;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.MultivaluedMap;

import net.sourceforge.jwebunit.junit.WebTester;
import net.sourceforge.jwebunit.util.TestingEngineRegistry;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.atlassian.confluence.plugin.functest.AbstractConfluencePluginWebTestCase;
import com.atlassian.selenium.SeleniumAssertions;
import com.atlassian.selenium.SeleniumClient;
import com.atlassian.selenium.browsers.AutoInstallClient;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.MultivaluedMapImpl;

public class AbstractJiraDialogTestCase extends AbstractConfluencePluginWebTestCase
{
    protected final static String TEST_SPACE_KEY = "tst";
    private static final String APPLINK_WS = "http://localhost:1990/confluence/rest/applinks/1.0/applicationlink";

    protected WebTester jiraWebTester;

    protected SeleniumClient client = AutoInstallClient.seleniumClient();
    protected SeleniumAssertions assertThat = AutoInstallClient.assertThat();

    static {
        // prevent AutoInstallClient from using the wrong default ...
        String confluenceBaseUrl = System.getProperty("baseurl", "http://localhost:1990/confluence");
        System.setProperty("baseurl", confluenceBaseUrl);
        // default was 3.5.9 which does not work on master anymore
        String defaultBrowser = System.getProperty("selenium.browser", "firefox-3.6");
        System.setProperty("selenium.browser", defaultBrowser);
    }

    @Override
    public void installPlugin()
    {
        super.installPlugin();
    }
    

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        setupJiraWebTester();
        loginToJira("admin", "admin");
        installCustomConfluencePaste();
    }
    
    private void installCustomConfluencePaste() throws URISyntaxException
    {
        URL url = AbstractJiraDialogTestCase.class.getClassLoader().getResource("confluence-paste-5.2-SNAPSHOT.jar");
        File f = new File(url.toURI());
        getConfluenceWebTester().installPlugin(f);
    }

    /*@Override
    public void restoreData() {
        //check to make sure the data restoring only happens once
        //to make the test run faster. 
        if(!dataInstalled) {
            super.restoreData();
            dataInstalled = true;
        }
    }*/

    private void setupJiraWebTester() throws IOException
    {
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
        client.type("//input[@name = 'os_username']", getConfluenceWebTester().getAdminUserName());
        client.type("//input[@name = 'os_password']", getConfluenceWebTester().getAdminPassword());
        client.click("//input[@name = 'login']");
        client.waitForPageToLoad();
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
        WebResource webResource = null;

        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        queryParams.add("os_username", getConfluenceWebTester().getAdminUserName());
        queryParams.add("os_password", getConfluenceWebTester().getAdminPassword());

        List<String> ids  = new ArrayList<String>();

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

        //delete all server config in applink
        for(String id: ids)
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
}
