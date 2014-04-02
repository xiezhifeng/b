package it.webdriver.com.atlassian.confluence;

import com.atlassian.confluence.it.Page;
import com.atlassian.confluence.it.TestProperties;
import com.atlassian.confluence.it.User;
import com.atlassian.confluence.pageobjects.component.dialog.MacroBrowserDialog;
import com.atlassian.confluence.pageobjects.page.content.EditContentPage;
import com.atlassian.confluence.webdriver.AbstractWebDriverTest;
import com.atlassian.confluence.webdriver.WebDriverConfiguration;
import com.atlassian.pageobjects.binder.PageBindingException;
import com.atlassian.pageobjects.elements.query.Poller;
import com.atlassian.webdriver.AtlassianWebDriver;
import com.google.common.base.Function;
import it.webdriver.com.atlassian.confluence.helper.ApplinkHelper;
import it.webdriver.com.atlassian.confluence.jiracharts.JiraChartWebDriverTest;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.PostMethod;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.core.Is.is;

public abstract class AbstractJiraWebDriverTest extends AbstractWebDriverTest
{
    public static final String JIRA_BASE_URL = System.getProperty("baseurl.jira", "http://localhost:11990/jira");

    public static final String JIRA_DISPLAY_URL = JIRA_BASE_URL.replace("localhost", "127.0.0.1");

    public static final String JIRA_ISSUE_MACRO_NAME = "jira";

    private static final Logger LOGGER = LoggerFactory.getLogger(JiraChartWebDriverTest.class);
    

    protected String authArgs;
    protected final HttpClient client = new HttpClient();
    private static final int RETRY_TIME = 8;


    protected EditContentPage editContentPage;
    
    @Before
    public void setup() throws Exception
    {
        authArgs = getAuthQueryString();
        doWebSudo(client);

        if (!TestProperties.isOnDemandMode())
        {
            // Need to set up applinks if not running against an OD instance
            ApplinkHelper.removeAllAppLink(client, authArgs);
            ApplinkHelper.setupAppLink(ApplinkHelper.ApplinkMode.TRUSTED, client, authArgs);

        }
        editContentPage = product.loginAndEdit(User.ADMIN, Page.TEST);
    }



    @After
    public void tearDown() throws Exception
    {
        // Determine whether or not we are still inside the editor by checking if the RTE 'Cancel' button is present
        if (editContentPage != null && editContentPage.getEditor().isCancelVisiableNow())
        {
            editContentPage.cancel();
        }
    }

    protected MacroBrowserDialog openMacroBrowser()
    {
        MacroBrowserDialog macroBrowserDialog = null;
        int retry = 1;
        PageBindingException ex = null;
        while (macroBrowserDialog == null && retry <= RETRY_TIME)
        {
            try
            {
                macroBrowserDialog = editContentPage.openMacroBrowser();
            }
            catch (PageBindingException e)
            {
                ex = e;
            }
            LOGGER.warn("Couldn't bind MacroBrower, retrying {} time", retry);
            retry++;
        }

        if (macroBrowserDialog == null && ex != null)
        {
            throw ex;
        }

        Poller.waitUntil(macroBrowserDialog.isVisibleTimed(), is(true), Poller.by(15, TimeUnit.SECONDS));
        return macroBrowserDialog;
    }



    private String getAuthQueryString()
    {
        final String adminUserName = User.ADMIN.getUsername();
        final String adminPassword = User.ADMIN.getPassword();
        return "?os_username=" + adminUserName + "&os_password=" + adminPassword;
    }

    private void doWebSudo(HttpClient client) throws IOException
    {
        final PostMethod l = new PostMethod(WebDriverConfiguration.getBaseUrl() + "/confluence/doauthenticate.action" + getAuthQueryString());
        l.addParameter("password", User.ADMIN.getPassword());
        final int status = client.executeMethod(l);
        Assert.assertTrue(status == HttpStatus.SC_MOVED_TEMPORARILY || status == HttpStatus.SC_OK);
    }

    public void waitUntilInlineMacroAppearsInEditor(final EditContentPage editContentPage, final String macroName)
    {
        Poller.waitUntil(
                "Macro could not be found on editor page",
                editContentPage.getContent().getRenderedContent().hasInlineMacro(macroName, Collections.EMPTY_LIST),
                is(true),
                Poller.by(10, TimeUnit.SECONDS)
        );
    }

    @SuppressWarnings("deprecation")
    protected void waitForAjaxRequest(final AtlassianWebDriver webDriver)
    {
        webDriver.waitUntil(new Function<WebDriver, Boolean>()
        {
            @Override
            public Boolean apply(@Nullable WebDriver input)
            {
                return (Boolean) ((JavascriptExecutor) input).executeScript("return jQuery.active == 0;");
            }
        });
    }
    
}
