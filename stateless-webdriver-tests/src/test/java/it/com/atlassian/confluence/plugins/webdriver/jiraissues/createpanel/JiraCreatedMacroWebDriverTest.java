package it.com.atlassian.confluence.plugins.webdriver.jiraissues.createpanel;

import javax.inject.Inject;

import com.atlassian.confluence.api.model.content.Content;
import com.atlassian.confluence.plugins.helper.ApplinkHelper;
import com.atlassian.confluence.plugins.webdriver.page.JiraCreatedMacroDialog;
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
import com.atlassian.confluence.webdriver.pageobjects.page.NoOpPage;
import com.atlassian.confluence.webdriver.pageobjects.page.content.EditContentPage;
import com.atlassian.confluence.webdriver.pageobjects.page.content.ViewPage;
import com.atlassian.pageobjects.PageBinder;
import com.atlassian.pageobjects.elements.query.Poller;
import com.atlassian.webdriver.testing.annotation.TestedProductClass;

import com.google.common.collect.ImmutableSet;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import java.io.IOException;

import static org.apache.commons.httpclient.HttpStatus.SC_MOVED_TEMPORARILY;
import static org.apache.commons.httpclient.HttpStatus.SC_OK;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@RunWith(ConfluenceStatelessTestRunner.class)
@TestedProductClass(ConfluenceTestedProduct.class)
public class JiraCreatedMacroWebDriverTest
{
    @Inject protected static ConfluenceTestedProduct product;
    @Inject protected static PageBinder pageBinder;
    @Inject protected static ConfluenceRestClient restClient;
    @Inject protected static ConfluenceRpcClient rpcClient;

    @Fixture public static GroupFixture group = GroupFixture.groupFixture()
            .globalPermission(GlobalPermission.CONFLUENCE_ADMIN)
            .build();

    @Fixture public static UserFixture user = UserFixture.userFixture()
            .group(group)
            .build();

    @Fixture public static SpaceFixture space = SpaceFixture.spaceFixture()
            .permission(user, SpacePermission.VIEW, SpacePermission.PAGE_EDIT, SpacePermission.BLOG_EDIT)
            .build();

    protected static Content testPageContent;
    protected static ViewPage viewPage;
    protected static EditContentPage editPage;

    @BeforeClass
    public static void start() throws Exception
    {
        doWebSudo(new HttpClient());

        //login once, so that we don't repeatedly login and waste time - this test doesn't need it
        product.login(user.get(), NoOpPage.class);

        if (!TestProperties.isOnDemandMode())
        {
            ApplinkHelper.setupAppLink(ApplinkHelper.ApplinkMode.BASIC, new HttpClient(), getAuthQueryString(), getBasicQueryString());
        }

        gotoEditTestPage();
    }

    protected static void gotoEditTestPage()
    {
        testPageContent = space.get().getHomepageRef().get();
        viewPage = product.loginAndView(user.get(), testPageContent);
        editPage = viewPage.edit();
        Poller.waitUntilTrue("Edit page is ready", editPage.getEditor().isEditorCurrentlyActive());
        editPage.getEditor().getContent().clear();
    }

    @Test
    public void testComponentsVisible()
    {
        JiraCreatedMacroDialog jiraCreatedMacroDialog = openJiraCreatedMacroDialog(true);
        jiraCreatedMacroDialog.selectProject("Jira integration plugin");
        assertTrue(jiraCreatedMacroDialog.getComponents().isVisible());
    }

    protected JiraCreatedMacroDialog openJiraCreatedMacroDialog(boolean isFromMenu)
    {
        JiraCreatedMacroDialog jiraCreatedMacroDialog;

        if (isFromMenu)
        {
            editPage.getEditor().openInsertMenu();
            jiraCreatedMacroDialog = product.getPageBinder().bind(JiraCreatedMacroDialog.class);
            jiraCreatedMacroDialog.open();
            jiraCreatedMacroDialog.selectMenuItem("Create New Issue");
        }
        else
        {
            WebDriver driver  = product.getTester().getDriver();
            driver.switchTo().frame("wysiwygTextarea_ifr");
            driver.findElement(By.id("tinymce")).sendKeys("{ji");
            driver.switchTo().defaultContent();
            driver.findElement(By.cssSelector(".autocomplete-macro-jira")).click();
            jiraCreatedMacroDialog = product.getPageBinder().bind(JiraCreatedMacroDialog.class);
        }

        return jiraCreatedMacroDialog;
    }

    public static String getAuthQueryString()
    {
        return "?os_username=" + "admin" + "&os_password=" + "admin";
    }

    private static String getBasicQueryString()
    {
        final String adminUserName = "admin";
        final String adminPassword = "admin";
        return "?username=" + adminUserName + "&password1=" + adminPassword + "&password2=" + adminPassword;
    }

    protected static void doWebSudo(final HttpClient client) throws IOException
    {
        final PostMethod l = new PostMethod(System.getProperty("baseurl.confluence") + "/doauthenticate.action" + getAuthQueryString());
        l.addParameter("password", user.get().getPassword());
        final int status = client.executeMethod(l);
        assertThat("WebSudo auth returned unexpected status", ImmutableSet.of(SC_MOVED_TEMPORARILY, SC_OK), hasItem(status));
    }
}
