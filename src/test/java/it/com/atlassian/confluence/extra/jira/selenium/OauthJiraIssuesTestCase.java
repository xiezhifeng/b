package it.com.atlassian.confluence.extra.jira.selenium;

import java.io.IOException;

import com.atlassian.selenium.SeleniumAssertions;
import com.atlassian.selenium.SeleniumClient;
import com.atlassian.selenium.SeleniumConfiguration;
import com.atlassian.selenium.browsers.AutoInstallClient;

import org.apache.commons.httpclient.HttpException;
import org.joda.time.DateTimeConstants;
import org.json.JSONException;

import it.com.atlassian.confluence.extra.jira.AbstractJiraMacrosPluginTestCase;

public class OauthJiraIssuesTestCase extends AbstractJiraMacrosPluginTestCase
{
    private static final String SELENIUM_BROWSER_BOT_PREFIX = "selenium.browserbot.getCurrentWindow().";
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

        assertThatDisplayUrlIsUsed("TP-2");
        assertThatDisplayUrlIsUsed("TP-1");
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
        client.waitForPopUp("", "5000");
        client.selectPopUp("");
        
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
        
        assertThat.elementPresentByTimeout("//a[@href='" + jiraWebTester.getTestContext().getBaseUrl() + "browse/TP-2']");
        assertThat.elementPresentByTimeout("//a[@href='" + jiraWebTester.getTestContext().getBaseUrl() + "browse/TP-1']");
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
        client.waitForPopUp("", "5000");
        client.selectPopUp("");

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

        assertTrue(waitForElementPresent("css=span.jira-status", 5));
        assertThatDisplayUrlIsUsed("TP-1");
        assertThat.textPresentByTimeout("Bug 01");
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
        client.waitForPopUp("", "5000");
        client.selectPopUp("");

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

        assertTrue(waitForElementPresent("css=span.jira-status", 5));
        assertThatDisplayUrlIsUsed("TP-1");
        assertThat.textPresentByTimeout("Bug 01");
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
                
        assertTrue(waitForElementPresent("css=span.jira-status", 5));
        assertThatDisplayUrlIsUsed("TP-2");
        assertThat.textPresentByTimeout("New Feature 01");
    }
    
    public void testAnonSingleStaticIssue() throws HttpException, IOException, JSONException
    {
        setupAppLink();
        
        long testPageId = createPage(testSpaceKey, "testGetJiraIssuesTrusted",
        "{jiraissues:TP-2|renderMode:static}");

        login();
        
        client.open("pages/viewpage.action?pageId=" + testPageId);

        assertTrue(waitForElementPresent("css=span.jira-status", 5));
        assertThatDisplayUrlIsUsed("TP-2");
        assertThat.textPresentByTimeout("New Feature 01");
    }

    public void testViewInJiraWithIssueKeyAsDefaultParam() throws IOException, JSONException, InterruptedException
    {
        setupAppLink();

        long testPageId = createPage(testSpaceKey, "testViewInJira", "{jira:TP-2}");

        login();

        client.open("pages/editpage.action?pageId=" + testPageId);
        client.waitForPageToLoad();
        client.waitForFrameToLoad("wysiwygTextarea_ifr", "10000");

        String editorTextAreaSelector = "css=#wysiwygTextarea_ifr";
        client.selectFrame(editorTextAreaSelector);

        long waitForPanelShowStart =  System.currentTimeMillis();

        do
        {
            client.click("css=img[data-macro-name='jira']");

            client.selectFrame("relative=top");
            if (0 == Integer.parseInt(client.getEval(";(function() { return " + SELENIUM_BROWSER_BOT_PREFIX + "jQuery('a.macro-property-panel-view-in-jira').length; })();")))
            {
                client.selectFrame(editorTextAreaSelector);
                client.click("css=#tinymce"); // Hide the property panels
            }
            else
            {
                break; // W00t
            }

        } while (System.currentTimeMillis() - waitForPanelShowStart < DateTimeConstants.MILLIS_PER_SECOND * 10);

        client.click("css=a.macro-property-panel-view-in-jira"); // If it's not here after 10 seconds this deserves to fail
        if (Boolean.valueOf(client.getEval(";(function() { return " + SELENIUM_BROWSER_BOT_PREFIX + "jQuery.browser === 'msie';} )();")).equals(true))
            client.selectWindow("_blank");
        else
            client.selectWindow("confluence-goto-jiralink-" + testPageId);

        client.selectWindow("null");
    }

    public void testViewInJiraWithIssueKeyAndServerMacroParams() throws IOException, JSONException, InterruptedException
    {
        setupAppLink();

        long testPageId = createPage(testSpaceKey, "testViewInJiraWithIssueKeyAndServerMacroParams", "{jira:key=TP-2|server=testjira}");

        login();

        client.open("pages/editpage.action?pageId=" + testPageId);
        client.waitForPageToLoad();
        client.waitForFrameToLoad("wysiwygTextarea_ifr", "10000");

        String editorTextAreaSelector = "css=#wysiwygTextarea_ifr";
        client.selectFrame(editorTextAreaSelector);

        long waitForPanelShowStart =  System.currentTimeMillis();

        do
        {
            client.click("css=img[data-macro-name='jira']");

            client.selectFrame("relative=top");
            if (0 == Integer.parseInt(client.getEval(";(function() { return " + SELENIUM_BROWSER_BOT_PREFIX + "jQuery('a.macro-property-panel-view-in-jira').length; })();")))
            {
                client.selectFrame(editorTextAreaSelector);
                client.click("css=#tinymce"); // Hide the property panels
            }
            else
            {
                break; // W00t
            }

        } while (System.currentTimeMillis() - waitForPanelShowStart < DateTimeConstants.MILLIS_PER_SECOND * 10);

        client.click("css=a.macro-property-panel-view-in-jira"); // If it's not here after 10 seconds this deserves to fail
        if (Boolean.valueOf(client.getEval(";(function() { return " + SELENIUM_BROWSER_BOT_PREFIX + "jQuery.browser === 'msie';} )();")).equals(true))
            client.selectWindow("_blank");
        else
            client.selectWindow("confluence-goto-jiralink-" + testPageId);

        // TODO: replace by jiraDisplayUrl when jira-connector plugin with displayUrl is released
        assertEquals(jiraBaseUrl + "/browse/TP-2", client.getLocation());

        client.selectWindow("null");
    }

    @Override
    protected void tearDown() throws Exception
    {        
        super.tearDown();
        // logout already called in AbstractConfluencePluginWebTestCase.tearDown
    }
    protected void logout()
    {
        if (client.isElementPresent("logout-link"))
        {
            client.click("logout-link");
        }
    }

    private void assertThatDisplayUrlIsUsed(String issueKey)
    {
        assertThat.elementPresentByTimeout("//a[@href='" + jiraDisplayUrl + "/browse/" + issueKey + "']");
    }

    private boolean waitForElementPresent(String locator, int timeout)
    {
        int count = timeout;
        while (!client.isElementPresent(locator) || count < 0)
        {
            try
            {
                Thread.sleep(1000);
                --count;
            }
            catch (InterruptedException e)
            {
                //
            }

        }

        return client.isElementPresent(locator);
    }
}
