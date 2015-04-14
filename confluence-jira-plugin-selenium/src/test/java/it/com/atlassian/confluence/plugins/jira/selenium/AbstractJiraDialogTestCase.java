package it.com.atlassian.confluence.plugins.jira.selenium;

import com.atlassian.confluence.it.User;
import com.atlassian.confluence.it.plugin.UploadablePlugin;
import com.atlassian.confluence.it.rpc.ConfluenceRpc;
import com.atlassian.confluence.plugin.functest.AbstractConfluencePluginWebTestCase;
import com.atlassian.selenium.SeleniumAssertions;
import com.atlassian.selenium.SeleniumClient;
import com.atlassian.selenium.browsers.AutoInstallClient;
import com.thoughtworks.selenium.SeleniumException;
import net.sourceforge.jwebunit.junit.WebTester;
import net.sourceforge.jwebunit.util.TestingEngineRegistry;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.*;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;

import java.io.File;
import java.io.IOException;
import java.util.ResourceBundle;

public class AbstractJiraDialogTestCase extends AbstractConfluencePluginWebTestCase
{
    private static final Logger LOG = Logger.getLogger(AbstractJiraDialogTestCase.class);
    
    protected final static String TEST_SPACE_KEY = "ds";

	protected static final String JIM_VERSION_KEY = "project.version";
	protected static final String JIRA_DISPLAY_URL = "http://127.0.0.1:11990/jira";

    protected WebTester jiraWebTester;

    protected SeleniumClient client = AutoInstallClient.seleniumClient();
    protected SeleniumAssertions assertThat = AutoInstallClient.assertThat();
    
    protected String jiraBaseUrl = System.getProperty("baseurl.jira", "http://localhost:11990/jira");
    protected String jiraDisplayUrl = jiraBaseUrl.replace("localhost", "127.0.0.1");

    protected String loginURL = "login.action?language=en_US";
    
    protected static ConfluenceRpc rpc;
    private static boolean installed = false;
    
    static {
        
        // prevent AutoInstallClient from using the wrong default ...
        LOG.debug("***** setting system properties");
        String confluenceBaseUrl = System.getProperty("baseurl", "http://localhost:1990/confluence");
        System.setProperty("baseurl", confluenceBaseUrl);
        // default was 3.5.9 which does not work on master anymore
        String defaultBrowser = System.getProperty("selenium.browser", "*firefox");
        System.setProperty("selenium.browser", defaultBrowser);
    }

    @Override
    protected void setUp() throws Exception
    {
        LOG.debug("***** setting up");
        super.setUp();
        setupRPC();
        installJIMIfNecessary();
        setupJiraWebTester();
        setupAppLink();
        loginToJira("admin", "admin");
    }
    
    private void setupRPC()
    {
        if (rpc == null) {
            rpc = ConfluenceRpc.newInstance(getConfluenceWebTester().getBaseUrl());
            User adminUser = new User(
                    getConfluenceWebTester().getAdminUserName(),
                    getConfluenceWebTester().getAdminPassword(),
                    null,
                    null);
            rpc.logIn(adminUser);
        }
    }
    
    private void installJIMIfNecessary() throws Exception
    {
        if(!installed)
        {
            rpc.getPluginHelper().installPlugin(new UploadablePlugin()
            {
                @Override
                public String getKey()
                {
                    return "com.atlassian.confluence.plugins:confluence-jira-plugin";
                }
                
                @Override
                public String getDisplayName()
                {
                    return "Jira Issue Macros Under Test";
                }
                
                @Override
                public File getFile()
                {
                    File file = new File("../confluence-jira-plugin/target/confluence-jira-plugin-" + ResourceBundle.getBundle("maven").getString(JIM_VERSION_KEY) + ".jar");
                    LOG.info("Installing JIM plugin to test: "+file.getAbsolutePath());
                    return file;
                }
            });
            installed = true;
        }
    }

    @Override
    public void restoreData() {
        // don't need to restore site-export.zip anymore
    }
    
    static String idAppLink = null;
    
    protected void setupAppLink() throws IOException, JSONException
    {
        String authArgs = getAuthQueryString();
        final HttpClient client = new HttpClient();
        doWebSudo(client);
        removeApplink(client, authArgs);
        idAppLink = createAppLink(client, authArgs);
        enableApplinkTrustedApp(client, getBasicQueryString(), idAppLink);
    }

    private void enableApplinkTrustedApp(HttpClient client, String authArgs, String idAppLink) throws HttpException, IOException
    {
        PostMethod setTrustMethod = new PostMethod(getConfluenceWebTester().getBaseUrl() + "/plugins/servlet/applinks/auth/conf/trusted/outbound-non-ual/" + idAppLink + authArgs);
        setTrustMethod.addParameter("action", "ENABLE");
        setTrustMethod.addRequestHeader("X-Atlassian-Token", "no-check");
        int status = client.executeMethod(setTrustMethod);
        Assert.assertTrue("Cannot enable Trusted AppLink", status == 200);
    }

    private String getAuthQueryString()
    {
        final String adminUserName = User.ADMIN.getUsername();
        final String adminPassword = User.ADMIN.getPassword();
        return "?os_username=" + adminUserName + "&os_password=" + adminPassword;
    }

    private String getBasicQueryString()
    {
        final String adminUserName = User.ADMIN.getUsername();
        final String adminPassword = User.ADMIN.getPassword();
        return "?username=" + adminUserName + "&password1=" + adminPassword + "&password2=" + adminPassword;
    }

    private JSONArray getListAppLink(HttpClient client, String authArgs) throws IOException, JSONException
    {
        final GetMethod m = new GetMethod(getConfluenceWebTester().getBaseUrl() + "/rest/applinks/1.0/applicationlink" + authArgs);
        m.setRequestHeader("Accept", "application/json, text/javascript, */*");

        final int status = client.executeMethod(m);
        Assert.assertEquals(HttpStatus.SC_OK, status);

        final JSONObject jsonObj = new JSONObject(m.getResponseBodyAsString());
        return jsonObj.getJSONArray("applicationLinks");
    }

    private void doWebSudo(HttpClient client) throws IOException
    {
        final PostMethod l = new PostMethod(getConfluenceWebTester().getBaseUrl() + "/confluence/doauthenticate.action" + getAuthQueryString());
        l.addParameter("password", User.ADMIN.getPassword());
        final int status = client.executeMethod(l);
        Assert.assertEquals(HttpStatus.SC_MOVED_TEMPORARILY, status);
    }

    private String createAppLink(HttpClient client, String authArgs) throws IOException, JSONException
    {
        final PostMethod m = new PostMethod(getConfluenceWebTester().getBaseUrl() + "/rest/applinks/1.0/applicationlinkForm/createAppLink" + authArgs);

        m.setRequestHeader("Accept", "application/json, text/javascript, */*");
        final String reqBody = "{\"applicationLink\":{\"typeId\":\"jira\",\"name\":\"testjira\",\"rpcUrl\":\"" + jiraBaseUrl + "\",\"displayUrl\":\"" + jiraDisplayUrl + "\",\"isPrimary\":true},\"username\":\"admin\",\"password\":\"admin\",\"createTwoWayLink\":false,\"customRpcURL\":false,\"rpcUrl\":\"\",\"configFormValues\":{\"trustEachOther\":false,\"shareUserbase\":false}}";
        final StringRequestEntity reqEntity = new StringRequestEntity(reqBody,"application/json", "UTF-8");
        m.setRequestEntity(reqEntity);

        final int status = client.executeMethod(m);
        Assert.assertEquals(HttpStatus.SC_OK, status);

        final JSONObject jsonObj = new JSONObject(m.getResponseBodyAsString());
        return jsonObj.getJSONObject("applicationLink").getString("id");
    }

    private void setupJiraWebTester() throws IOException
    {
        LOG.debug("***** setupJiraWebTester");
        jiraWebTester = new WebTester();
        jiraWebTester.setTestingEngineKey(TestingEngineRegistry.TESTING_ENGINE_HTMLUNIT);
        jiraWebTester.setScriptingEnabled(false);
        jiraWebTester.getTestContext().setBaseUrl(System.getProperty("baseurl.jira", "http://localhost:11990/jira"));

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
        client.open(this.loginURL);
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

    public void removeApplink() throws IOException, JSONException
    {
        final HttpClient client = new HttpClient();
        doWebSudo(client);
        removeApplink(client, getAuthQueryString());
    }

    public void removeApplink(HttpClient client, String authArgs) throws JSONException, IOException
    {
        JSONArray applinks = getListAppLink(client, authArgs);
        for(int i = 0; i< applinks.length(); i++)
        {
            final String applinkId = applinks.getJSONObject(i).getString("id");
            final DeleteMethod deleteMethod = new DeleteMethod(getConfluenceWebTester().getBaseUrl() + "/rest/applinks/1.0/applicationlink/" + applinkId + authArgs);
            deleteMethod.setRequestHeader("Accept", "application/json, text/javascript, */*");
            final int status = client.executeMethod(deleteMethod);
            Assert.assertEquals(HttpStatus.SC_OK, status);
        }
    }

    protected String getDefaultServerId()
    {
        String authArgs = getAuthQueryString();
        final HttpClient client = new HttpClient();
        String serverId = "";
        try
        {
            JSONArray jiraservers = getListAppLink(client, authArgs);
            for (int i = 0; i < jiraservers.length(); ++i)
            {
                JSONObject jiraServer = jiraservers.getJSONObject(i);
                if (jiraServer.getString("isPrimary").equals("true"))
                {
                    serverId = jiraServer.getString("id");
                    break;
                }
            }
        }
        catch (Exception e)
        {
            assertTrue(false);
        }
        return serverId;
    }
    
    protected void createPageWithJiraMacro(String markup, String pageTitle) {
        client.type("//input[@id='content-title']", pageTitle);
        client.selectFrame("wysiwygTextarea_ifr");
        client.typeWithFullKeyEvents("css=#tinymce", markup);
        assertThat.elementPresentByTimeout("css=img.editor-inline-macro", 10000);
        client.selectFrame("relative=top");
        client.click("//button[@id='rte-button-publish']");
        client.waitForPageToLoad();
    }
}
