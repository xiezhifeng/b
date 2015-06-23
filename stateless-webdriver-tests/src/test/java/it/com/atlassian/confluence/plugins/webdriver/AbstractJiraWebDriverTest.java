package it.com.atlassian.confluence.plugins.webdriver;

import java.io.IOException;
import java.util.Collections;

import javax.inject.Inject;

import com.atlassian.confluence.api.model.content.Content;
import com.atlassian.confluence.it.User;
import com.atlassian.confluence.plugins.helper.ApplinkHelper;
import com.atlassian.confluence.test.api.model.person.UserWithDetails;
import com.atlassian.confluence.test.properties.TestProperties;
import com.atlassian.confluence.test.rest.api.ConfluenceRestClient;
import com.atlassian.confluence.test.rpc.api.ConfluenceRpcClient;
import com.atlassian.confluence.test.rpc.api.permissions.GlobalPermission;
import com.atlassian.confluence.test.rpc.api.permissions.SpacePermission;
import com.atlassian.confluence.test.stateless.ConfluenceStatelessTestRunner;
import com.atlassian.confluence.test.stateless.fixtures.Fixture;
import com.atlassian.confluence.test.stateless.fixtures.GroupFixture;
import com.atlassian.confluence.test.stateless.fixtures.SpaceFixture;
import com.atlassian.confluence.test.stateless.fixtures.UserFixture;
import com.atlassian.confluence.webdriver.pageobjects.ConfluenceTestedProduct;
import com.atlassian.confluence.webdriver.pageobjects.component.dialog.Dialog;
import com.atlassian.confluence.webdriver.pageobjects.component.dialog.MacroBrowserDialog;
import com.atlassian.confluence.webdriver.pageobjects.page.NoOpPage;
import com.atlassian.confluence.webdriver.pageobjects.page.content.EditContentPage;
import com.atlassian.confluence.webdriver.pageobjects.page.content.ViewPage;
import com.atlassian.pageobjects.PageBinder;
import com.atlassian.pageobjects.elements.query.Poller;
import com.atlassian.webdriver.AtlassianWebDriver;
import com.atlassian.webdriver.testing.annotation.TestedProductClass;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableSet;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.atlassian.pageobjects.elements.query.Poller.by;
import static com.atlassian.pageobjects.elements.query.Poller.waitUntil;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.commons.httpclient.HttpStatus.SC_MOVED_TEMPORARILY;
import static org.apache.commons.httpclient.HttpStatus.SC_OK;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

@RunWith(ConfluenceStatelessTestRunner.class)
@TestedProductClass(ConfluenceTestedProduct.class)
public class AbstractJiraWebDriverTest
{
    public static final String JIRA_BASE_URL = System.getProperty("baseurl.jira", "http://localhost:11990/jira");
    public static final String JIRA_DISPLAY_URL = JIRA_BASE_URL.replace("localhost", "127.0.0.1");
    public static final String JIRA_ISSUE_MACRO_NAME = "jira";
    public static final String OLD_JIRA_ISSUE_MACRO_NAME = "jiraissues";
    public static final int RETRY_TIME = 8;
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractJiraWebDriverTest.class);

    @Inject protected static ConfluenceTestedProduct product;
    @Inject protected static PageBinder pageBinder;
    @Inject protected static ConfluenceRestClient restClient;
    @Inject protected static ConfluenceRpcClient rpcClient;

    @Fixture
    public static GroupFixture group = GroupFixture.groupFixture()
            .globalPermission(GlobalPermission.CONFLUENCE_ADMIN)
            .build();

    @Fixture public static UserFixture user = UserFixture.userFixture()
            .group(group)
            .build();

    @Fixture public static SpaceFixture space = SpaceFixture.spaceFixture()
            .permission(user, SpacePermission.VIEW, SpacePermission.PAGE_EDIT, SpacePermission.BLOG_EDIT)
            .build();

    protected static Content testPageContent;

    @BeforeClass
    public static void start() throws Exception
    {
        doWebSudo(new HttpClient());

        if (!TestProperties.isOnDemandMode())
        {
            ApplinkHelper.setupAppLink(ApplinkHelper.ApplinkMode.BASIC, new HttpClient(), getAuthQueryString(), getBasicQueryString());
        }

        //login once, so that we don't repeatedly login and waste time - this test doesn't need it
        product.login(user.get(), NoOpPage.class);
    }

    @After
    public void tearDown() throws Exception
    {
    }

    public static String getAuthQueryString()
    {
        return "?os_username=" + User.ADMIN.getUsername() + "&os_password=" + User.ADMIN.getPassword();
    }

    protected static String getBasicQueryString()
    {
        final String adminUserName = User.ADMIN.getUsername();
        final String adminPassword = User.ADMIN.getPassword();
        return "?username=" + adminUserName + "&password1=" + adminPassword + "&password2=" + adminPassword;
    }

    protected static void doWebSudo(final HttpClient client) throws IOException
    {
        final PostMethod l = new PostMethod(System.getProperty("baseurl.confluence") + "/doauthenticate.action" + getAuthQueryString());
        l.addParameter("password", user.get().getPassword());
        final int status = client.executeMethod(l);
        assertThat("WebSudo auth returned unexpected status", ImmutableSet.of(SC_MOVED_TEMPORARILY, SC_OK), hasItem(status));
    }

    protected static EditContentPage gotoEditTestPage(UserWithDetails user)
    {
        testPageContent = space.get().getHomepageRef().get();
        EditContentPage editPage = product.loginAndEdit(user, testPageContent);

        Poller.waitUntilTrue("Edit page is ready", editPage.getEditor().isEditorCurrentlyActive());
        editPage.getEditor().getContent().clear();

        return editPage;
    }

    protected void closeDialog(final Dialog dialog)
    {
        if (dialog != null && dialog.isVisible())
        {
            // for some reason jiraIssuesDialog.clickCancelAndWaitUntilClosed() throws compilation issue against 5.5-SNAPSHOT as of Feb 27 2014
            dialog.clickCancel();
            dialog.waitUntilHidden();
        }
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

    protected void waitUntilInlineMacroAppearsInEditor(final EditContentPage editContentPage, final String macroName)
    {
        waitUntil(
                "Macro [" + macroName + "] could not be found on editor page",
                editContentPage.getEditor().getContent().hasInlineMacro(macroName, Collections.<String>emptyList()),
                is(true),
                by(30, SECONDS)
        );
    }

    protected MacroBrowserDialog openMacroBrowser(EditContentPage editPage)
    {
        MacroBrowserDialog macroBrowserDialog = null;
        int retry = 1;
        AssertionError assertionError = null;
        while (macroBrowserDialog == null && retry <= RETRY_TIME)
        {
            try
            {
                macroBrowserDialog = editPage.getEditor().openMacroBrowser();
                waitUntil("Macro browser is not visible", macroBrowserDialog.isVisibleTimed(), is(true));
            }
            catch (final AssertionError e)
            {
                assertionError = e;
                LOGGER.warn("Couldn't bind MacroBrower, retrying {} time", retry);
                retry++;
            }
        }

        if (macroBrowserDialog == null && assertionError != null)
        {
            throw assertionError;
        }
        return macroBrowserDialog;
    }

}
