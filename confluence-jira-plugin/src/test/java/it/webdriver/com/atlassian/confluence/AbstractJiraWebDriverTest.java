package it.webdriver.com.atlassian.confluence;

import java.io.IOException;
import java.util.Collections;

import com.atlassian.confluence.it.Page;
import com.atlassian.confluence.it.User;
import com.atlassian.confluence.pageobjects.component.dialog.Dialog;
import com.atlassian.confluence.pageobjects.component.dialog.MacroBrowserDialog;
import com.atlassian.confluence.pageobjects.page.content.EditContentPage;
import com.atlassian.confluence.webdriver.AbstractInjectableWebDriverTest;
import com.atlassian.confluence.webdriver.WebDriverConfiguration;
import com.atlassian.webdriver.AtlassianWebDriver;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableSet;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.junit.After;
import org.junit.Before;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.atlassian.confluence.it.TestProperties.isOnDemandMode;
import static com.atlassian.pageobjects.elements.query.Poller.by;
import static com.atlassian.pageobjects.elements.query.Poller.waitUntil;
import static it.webdriver.com.atlassian.confluence.helper.ApplinkHelper.ApplinkMode.TRUSTED;
import static it.webdriver.com.atlassian.confluence.helper.ApplinkHelper.removeAllApplink;
import static it.webdriver.com.atlassian.confluence.helper.ApplinkHelper.setupAppLink;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.commons.httpclient.HttpStatus.SC_MOVED_TEMPORARILY;
import static org.apache.commons.httpclient.HttpStatus.SC_OK;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public abstract class AbstractJiraWebDriverTest extends AbstractInjectableWebDriverTest
{
    public static final String JIRA_BASE_URL = System.getProperty("baseurl.jira", "http://localhost:11990/jira");

    public static final String JIRA_DISPLAY_URL = JIRA_BASE_URL.replace("localhost", "127.0.0.1");

    public static final String JIRA_ISSUE_MACRO_NAME = "jira";

    public static final String OLD_JIRA_ISSUE_MACRO_NAME = "jiraissues";

    private final Logger LOGGER = LoggerFactory.getLogger(getClass());

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

        if (!isOnDemandMode())
        {
            removeAllApplink();
            setupAppLink(TRUSTED);

        }
        editContentPage = product.loginAndEdit(User.ADMIN, Page.TEST);
    }

    @After
    public void tearDown() throws Exception
    {
        // Determine whether or not we are still inside the editor by checking if the RTE 'Cancel' button is present
        if (editContentPage != null && editContentPage.isCancelVisibleNow())
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
                waitUntil("Macro browser is not visible", macroBrowserDialog.isVisibleTimed(), is(true), by(30, SECONDS));
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

    public void waitUntilInlineMacroAppearsInEditor(final EditContentPage editContentPage, final String macroName)
    {
        waitUntil(
                "Macro [" + macroName + "] could not be found on editor page",
                editContentPage.getContent().getRenderedContent().hasInlineMacro(macroName, Collections.<String>emptyList()),
                is(true),
                by(30, SECONDS)
        );
    }

    protected void waitForAjaxRequest(final AtlassianWebDriver webDriver)
    {
        webDriver.waitUntil(new Function<WebDriver, Boolean>()
        {
            @Override
            public Boolean apply(final WebDriver input)
            {
                return (Boolean) ((JavascriptExecutor) input).executeScript("return jQuery.active == 0;");
            }
        });
    }

}
