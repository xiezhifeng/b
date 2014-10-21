package it.webdriver.com.atlassian.confluence;

import static org.hamcrest.core.Is.is;
import it.webdriver.com.atlassian.confluence.helper.ApplinkHelper;
import it.webdriver.com.atlassian.confluence.jiracharts.JiraChartWebDriverTest;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

import com.atlassian.confluence.it.Page;
import com.atlassian.confluence.it.TestProperties;
import com.atlassian.confluence.it.User;
import com.atlassian.confluence.pageobjects.component.dialog.Dialog;
import com.atlassian.confluence.pageobjects.component.dialog.MacroBrowserDialog;
import com.atlassian.confluence.pageobjects.page.content.EditContentPage;
import com.atlassian.confluence.webdriver.AbstractInjectableWebDriverTest;
import com.atlassian.confluence.webdriver.WebDriverConfiguration;

import com.atlassian.pageobjects.elements.query.Poller;
import com.atlassian.webdriver.AtlassianWebDriver;

import com.google.common.base.Function;

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

public abstract class AbstractJiraWebDriverTest extends AbstractInjectableWebDriverTest
{
    public static final String JIRA_BASE_URL = System.getProperty("baseurl.jira", "http://localhost:11990/jira");

    public static final String JIRA_DISPLAY_URL = JIRA_BASE_URL.replace("localhost", "127.0.0.1");

    public static final String JIRA_ISSUE_MACRO_NAME = "jira";

    private static final Logger LOGGER = LoggerFactory.getLogger(JiraChartWebDriverTest.class);

    protected String authArgs;
    protected final HttpClient client = new HttpClient();
    private static final int RETRY_TIME = 8;

    private static final String CREATED_VS_RESOLVED_DARK_FEATURE = "jirachart.createdvsresolved";
    private static final String TWO_DIMENSIONAL_DARK_FEATURE = "jirachart.twodimensional";

    protected EditContentPage editContentPage;

    @Override
    @Before
    public void start() throws Exception
    {
        int i = 0;
        Exception ex = null;
        while (i < RETRY_TIME)
        {
            try
            {
                ex = null;
                super.start();
                break;
            }
            catch (final Exception e)
            {
                ex = e;
                i++;
            }
        }
        if (i == RETRY_TIME && ex != null)
        {
            throw ex;
        }
        setup();
    }

    protected void setup() throws Exception
    {
        darkFeaturesHelper.enableSiteFeature(CREATED_VS_RESOLVED_DARK_FEATURE);
        darkFeaturesHelper.enableSiteFeature(TWO_DIMENSIONAL_DARK_FEATURE);
        authArgs = getAuthQueryString();
        doWebSudo(client);

        if (!TestProperties.isOnDemandMode())
        {
            ApplinkHelper.removeAllAppLink(client, authArgs);
            ApplinkHelper.setupAppLink(ApplinkHelper.ApplinkMode.TRUSTED, client, authArgs);

        }
        editContentPage = product.loginAndEdit(User.ADMIN, Page.TEST);
    }

    @After
    public void tearDown() throws Exception
    {
        // Determine whether or not we are still inside the editor by checking if the RTE 'Cancel' button is present
        if (editContentPage != null && editContentPage.getEditor().isCancelVisibleNow())
        {
            editContentPage.cancel();
        }
        darkFeaturesHelper.disableSiteFeature(CREATED_VS_RESOLVED_DARK_FEATURE);
    }

    public void closeDialog(final Dialog dialog)
    {
        if (dialog != null && dialog.isVisible())
        {
            // for some reason jiraIssuesDialog.clickCancelAndWaitUntilClosed() throws compilation issue against 5.5-SNAPSHOT as of Feb 27 2014
            dialog.clickCancel();
            dialog.waitUntilHidden();
        }
    }

    protected MacroBrowserDialog openMacroBrowser()
    {
        MacroBrowserDialog macroBrowserDialog = null;
        int retry = 1;
        AssertionError assertionError = null;
        while (macroBrowserDialog == null && retry <= RETRY_TIME)
        {
            try
            {
                macroBrowserDialog = editContentPage.openMacroBrowser();
                Poller.waitUntil(macroBrowserDialog.isVisibleTimed(), is(true), Poller.by(30, TimeUnit.SECONDS));
            }
            catch (final AssertionError e)
            {
                assertionError = e;
            }
            LOGGER.warn("Couldn't bind MacroBrower, retrying {} time", retry);
            retry++;
        }

        if (macroBrowserDialog == null && assertionError != null)
        {
            throw assertionError;
        }
        return macroBrowserDialog;
    }



    protected String getAuthQueryString()
    {
        final String adminUserName = User.ADMIN.getUsername();
        final String adminPassword = User.ADMIN.getPassword();
        return "?os_username=" + adminUserName + "&os_password=" + adminPassword;
    }

    protected void doWebSudo(final HttpClient client) throws IOException
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
                Poller.by(30, TimeUnit.SECONDS)
                );
    }

    @SuppressWarnings("deprecation")
    protected void waitForAjaxRequest(final AtlassianWebDriver webDriver)
    {
        webDriver.waitUntil(new Function<WebDriver, Boolean>()
        {
            @Override
            public Boolean apply(@Nullable final WebDriver input)
            {
                return (Boolean) ((JavascriptExecutor) input).executeScript("return jQuery.active == 0;");
            }
        });
    }

}
