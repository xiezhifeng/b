package it.com.atlassian.confluence.plugins.jira.selenium;

import com.atlassian.confluence.it.User;
import com.atlassian.confluence.it.plugin.Plugin;
import com.atlassian.confluence.it.plugin.PluginHelper;
import com.atlassian.confluence.it.plugin.SimplePlugin;
import com.atlassian.confluence.it.rpc.ConfluenceRpc;
import com.atlassian.confluence.plugin.functest.AbstractConfluencePluginWebTestCase;
import com.atlassian.selenium.SeleniumAssertions;
import com.atlassian.selenium.SeleniumClient;
import com.atlassian.selenium.browsers.AutoInstallClient;
import net.sourceforge.jwebunit.junit.WebTester;
import net.sourceforge.jwebunit.util.TestingEngineRegistry;

import java.io.IOException;

public class AbstractJiraDialogTestCase extends AbstractConfluencePluginWebTestCase
{
    protected final static String TEST_SPACE_KEY = "tst";

    protected WebTester jiraWebTester;

    protected SeleniumClient client = AutoInstallClient.seleniumClient();
    protected SeleniumAssertions assertThat = AutoInstallClient.assertThat();

    private static boolean legacyPluginDisabled = false;
    
    
    private static final String[] LEGACY_PLUGIN_IDS =
    				new String[] {"com.atlassian.confluence.plugins.jira.jira-connector"};

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
        if (!legacyPluginDisabled)
        {
        	disablePlugin(LEGACY_PLUGIN_IDS);
        	legacyPluginDisabled = true;
        	
        }
        super.installPlugin();
    }

    
    
    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        setupJiraWebTester();
        loginToJira("admin", "admin");
    }
    
    
   

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
        String authArgs = "?os_username=" + adminUserName + "&os_password=" + adminPassword;
        return authArgs;
    }
    protected void logout()
    {
        if (client.isElementPresent("logout-link"))
        {
            client.click("logout-link");
        }
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

    private void disablePlugin(String... pluginIds)
    {
        try {
			ConfluenceRpc rpc = ConfluenceRpc.newInstance(getConfluenceWebTester().getBaseUrl());
			User adminUser = new User(
					getConfluenceWebTester().getAdminUserName(),
					getConfluenceWebTester().getAdminPassword(),
					null,
					null);
			rpc.logIn(adminUser);

			PluginHelper pluginHelper = rpc.getPluginHelper();
			for (String pluginId : pluginIds)
			{
				Plugin plugin = new SimplePlugin(pluginId, null);
				pluginHelper.disablePlugin(plugin);
			}
		} catch (Exception e) {
			// probably rpc-funct-test plugin not installed, ignore
		}
    }
}
