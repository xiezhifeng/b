package it.com.atlassian.confluence.extra.jira.selenium;

import java.io.IOException;

import org.apache.commons.httpclient.HttpException;
import org.json.JSONException;

import it.com.atlassian.confluence.extra.jira.AbstractJiraMacrosPluginTestCase;

import com.atlassian.selenium.SeleniumAssertions;
import com.atlassian.selenium.SeleniumClient;
import com.atlassian.selenium.SeleniumConfiguration;
import com.atlassian.selenium.browsers.AutoInstallClient;

public class OauthJiraIssuesTestCase extends AbstractJiraMacrosPluginTestCase
{
    protected SeleniumClient client = AutoInstallClient.seleniumClient();
    protected SeleniumAssertions assertThat = AutoInstallClient.assertThat();

    protected SeleniumConfiguration config = AutoInstallClient.seleniumConfiguration();
    
    protected String baseUrl = System.getProperty("baseurl.conf1");
    
    protected void login()
    {
        client.open("login.action");
        client.type("//input[@name = 'os_username']", getConfluenceWebTester().getAdminUserName());
        client.type("//input[@name = 'os_password']", getConfluenceWebTester().getAdminPassword());
        client.click("//input[@name = 'login']");
        client.waitForPageToLoad();
    }
    
    public void testOAuthJiraIssues() throws HttpException, IOException, JSONException
    {
        createConfluenceOauthConsumerInJira();
        enableOauthWithApplink(setupAppLink());
        
        long testPageId = createPage(testSpaceKey, "testGetJiraIssuesTrusted",
                "{jiraissues:url=" + getJiraIssuesXmlUrl() + "|cache=off}");
        
        login();
        
        client.open("pages/viewpage.action?pageId=" + testPageId);
        assertThat.elementPresentByTimeout("css=.oauth-init");
        client.click("css=.oauth-init");
        // make the oauth popup the active window
        client.selectPopUp("");
        client.waitForPageToLoad();
        if (client.isElementPresent("//input[@name = 'os_username']"))
        {
            client.type("//input[@name = 'os_username']", "admin");
            client.type("//input[@name = 'os_password']", "admin");
            client.click("login");
        }
        
        assertThat.elementPresentByTimeout("//input[@name = 'approve']");
        client.click("//input[@name = 'approve']");
        
        // go back to the original window
        client.selectWindow("null");
        client.waitForAjaxWithJquery();
        
        assertThat.elementPresentByTimeout("//a[@href='" + jiraWebTester.getTestContext().getBaseUrl() + "browse/TP-2']");
        assertThat.elementPresentByTimeout("//a[@href='" + jiraWebTester.getTestContext().getBaseUrl() + "browse/TP-1']");
        
        client.click("logout-link");
    }
    
    public void testOAuthJiraIssuesStatic() throws HttpException, IOException, JSONException
    {
        createConfluenceOauthConsumerInJira();
        enableOauthWithApplink(setupAppLink());
        
        long testPageId = createPage(testSpaceKey, "testGetJiraIssuesTrusted",
                "{jiraissues:url=" + getJiraIssuesXmlUrl() + "|cache=off|renderMode=static}");
        
        login();
        
        client.open("pages/viewpage.action?pageId=" + testPageId);
        assertThat.elementPresentByTimeout("css=.static-oauth-init");
        client.click("css=.static-oauth-init");
        // make the oauth popup the active window
        client.selectPopUp("");
        client.waitForPageToLoad();      
        
        if (client.isElementPresent("//input[@name = 'os_username']"))
        {
            client.type("//input[@name = 'os_username']", "admin");
            client.type("//input[@name = 'os_password']", "admin");
            client.click("login");
        }
        
        assertThat.elementPresentByTimeout("//input[@name = 'approve']");
        client.click("//input[@name = 'approve']");
        
        // go back to the original window
        client.selectWindow("null");
        client.waitForPageToLoad();
        
        assertThat.elementPresentByTimeout("//a[@href='" + jiraWebTester.getTestContext().getBaseUrl() + "browse/TP-2']");
        assertThat.elementPresentByTimeout("//a[@href='" + jiraWebTester.getTestContext().getBaseUrl() + "browse/TP-1']");
        
        client.click("logout-link");
    }
    
    public void testOauthSingleIssue() throws HttpException, IOException, JSONException
    {
        createConfluenceOauthConsumerInJira();
        enableOauthWithApplink(setupAppLink());
        
        long testPageId = createPage(testSpaceKey, "testGetJiraIssuesTrusted",
                "{jiraissues:TP-1}");
        
        login();
        
        client.open("pages/viewpage.action?pageId=" + testPageId);
        assertThat.elementPresentByTimeout("css=.oauth-init");
        client.click("css=.oauth-init");
        // make the oauth popup the active window
        client.selectPopUp("");
        client.waitForPageToLoad();
        if (client.isElementPresent("//input[@name = 'os_username']"))
        {
            client.type("//input[@name = 'os_username']", "admin");
            client.type("//input[@name = 'os_password']", "admin");
            client.click("login");
        }
        
        assertThat.elementPresentByTimeout("//input[@name = 'approve']");
        client.click("//input[@name = 'approve']");
        
        // go back to the original window
        client.selectWindow("null");
        client.waitForPageToLoad();
                
        assertThat.elementPresentByTimeout("//a[@href='" + jiraWebTester.getTestContext().getBaseUrl() + "browse/TP-1']");
        assertThat.elementPresentByTimeout("css=span.jira-status");
        assertThat.textPresentByTimeout("Bug 01");
        
        client.click("logout-link");
    }
    
    public void testOauthStaticSingleIssue() throws HttpException, IOException, JSONException
    {
        createConfluenceOauthConsumerInJira();
        enableOauthWithApplink(setupAppLink());
        
        long testPageId = createPage(testSpaceKey, "testGetJiraIssuesTrusted",
                "{jiraissues:TP-1|renderMode:static}");
        
        login();
        
        client.open("pages/viewpage.action?pageId=" + testPageId);
        assertThat.elementPresentByTimeout("css=.oauth-init");
        client.click("css=.oauth-init");
        // make the oauth popup the active window
        client.selectPopUp("");
        client.waitForPageToLoad();
        if (client.isElementPresent("//input[@name = 'os_username']"))
        {
            client.type("//input[@name = 'os_username']", "admin");
            client.type("//input[@name = 'os_password']", "admin");
            client.click("login");
        }
        
        assertThat.elementPresentByTimeout("//input[@name = 'approve']");
        client.click("//input[@name = 'approve']");
        
        // go back to the original window
        client.selectWindow("null");
        client.waitForPageToLoad();
                
        assertThat.elementPresentByTimeout("//a[@href='" + jiraWebTester.getTestContext().getBaseUrl() + "browse/TP-1']");
        assertThat.elementPresentByTimeout("css=span.jira-status");
        assertThat.textPresentByTimeout("Bug 01");
        
        client.click("logout-link");
    }
    
    // CONF-24178
    public void testOauthMultipleIssuesSameKey() throws HttpException, IOException, JSONException
    {
        createConfluenceOauthConsumerInJira();
        enableOauthWithApplink(setupAppLink());
        
        long testPageId = createPage(testSpaceKey, "testGetJiraIssuesTrusted",
                "{jiraissues:TP-1}\n{jiraissues:TP-1}\n{jiraissues:TP-1}");
        
        login();
        
        client.open("pages/viewpage.action?pageId=" + testPageId);
        Number approveCount = client.getXpathCount("//a[text() = 'Authenticate']");
        
        assertEquals(3, approveCount.intValue());
    }
    
    public void testAnonSingleIssue() throws HttpException, IOException, JSONException
    {
        setupAppLink();
        
        long testPageId = createPage(testSpaceKey, "testGetJiraIssuesTrusted",
        "{jiraissues:TP-2}");

        login();
        
        client.open("pages/viewpage.action?pageId=" + testPageId);
        client.waitForPageToLoad();
                
        assertThat.elementPresentByTimeout("//a[@href='" + jiraWebTester.getTestContext().getBaseUrl() + "browse/TP-2']");
        assertThat.elementPresentByTimeout("css=span.jira-status");
        assertThat.textPresentByTimeout("New Feature 01");
        
        client.click("logout-link");
    }
    
    public void testAnonSingleStaticIssue() throws HttpException, IOException, JSONException
    {
        setupAppLink();
        
        long testPageId = createPage(testSpaceKey, "testGetJiraIssuesTrusted",
        "{jiraissues:TP-2|renderMode:static}");

        login();
        
        client.open("pages/viewpage.action?pageId=" + testPageId);
        client.waitForPageToLoad();
                
        assertThat.elementPresentByTimeout("//a[@href='" + jiraWebTester.getTestContext().getBaseUrl() + "browse/TP-2']");
        assertThat.elementPresentByTimeout("css=span.jira-status");
        assertThat.textPresentByTimeout("New Feature 01");
        
        client.click("logout-link");
    }

    public void testViewInJira() throws IOException, JSONException, InterruptedException
    {
        setupAppLink();

        long testPageId = createPage(testSpaceKey, "testViewInJira", "{jira:TP-2}");

        login();

        client.open("pages/editpage.action?pageId=" + testPageId);
        client.waitForPageToLoad();
        client.waitForFrameToLoad("wysiwygTextarea_ifr", "10000");
        client.selectFrame("css=#wysiwygTextarea_ifr");
        client.click("css=img[data-macro-name='jira']");
        client.selectFrame("relative=top");
        // Panel loads via AJAX?
        client.waitForCondition(";(function() { return jQuery('.a.macro-property-panel-view-in-jira').length; })();");
        client.click("css=a.macro-property-panel-view-in-jira");
        if (Boolean.valueOf(client.getEval(";(function() { return jQuery.browser === 'msie';} )();")).equals(true))
            client.selectWindow("_blank");
        else
            client.selectWindow("confluence-goto-jiralink-" + testPageId);
    }

    @Override
    protected void tearDown() throws Exception
    {        
        super.tearDown();
        logout();
    }
    protected void logout()
    {
        if (client.isElementPresent("logout-link"))
        {
            client.click("logout-link");
        }
    }
}
