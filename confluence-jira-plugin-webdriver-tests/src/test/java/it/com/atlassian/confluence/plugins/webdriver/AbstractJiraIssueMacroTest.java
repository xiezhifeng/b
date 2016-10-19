package it.com.atlassian.confluence.plugins.webdriver;

import com.atlassian.confluence.api.model.content.Content;
import com.atlassian.confluence.it.User;
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
import com.atlassian.webdriver.testing.annotation.TestedProductClass;
import com.google.common.collect.ImmutableSet;
import it.com.atlassian.confluence.plugins.webdriver.helper.ApplinkHelper;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import javax.inject.Inject;

import java.io.IOException;

import static org.apache.commons.httpclient.HttpStatus.SC_MOVED_TEMPORARILY;
import static org.apache.commons.httpclient.HttpStatus.SC_OK;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertThat;

@RunWith(ConfluenceStatelessTestRunner.class)
@TestedProductClass(ConfluenceTestedProduct.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public abstract class AbstractJiraIssueMacroTest {

    @Inject
    protected static ConfluenceTestedProduct product;
    @Inject
    protected static PageBinder pageBinder;

    protected static final HttpClient client = new HttpClient();

    protected static EditContentPage editContentPage;

    @Fixture
    private static GroupFixture group = GroupFixture.groupFixture()
            .globalPermission(GlobalPermission.CONFLUENCE_ADMIN)
            .build();

    @Fixture
    protected static UserFixture user = UserFixture.userFixture()
            .group(group)
            .build();

    @Fixture
    private static SpaceFixture space = SpaceFixture.spaceFixture()
            .permission(user, SpacePermission.VIEW, SpacePermission.PAGE_EDIT, SpacePermission.BLOG_EDIT)
            .build();

    @BeforeClass
    public static void start() throws Exception {
        webSudo();
        ApplinkHelper.setupAppLink(
                ApplinkHelper.ApplinkMode.BASIC, client, getAuthQueryString(), getBasicQueryString()
        );
        product.login(user.get(), NoOpPage.class);
    }

    @Before
    public void alwaysSetup() throws Exception {
        setupEditPage();
    }

    @After
    public void alwaysClear() throws Exception {
        cancelEditPage();
    }

    @AfterClass
    public static void teardown() {

    }

    private void setupEditPage() {
        if (editContentPage == null || !editContentPage.getEditor().isCancelVisibleNow()) {
            Content content = space.get().getHomepageRef().get();
            editContentPage = product.viewPage(content).edit();
        }
        Poller.waitUntilTrue("Edit page is ready", editContentPage.getEditor().isEditorCurrentlyActive());
        editContentPage.getEditor().getContent().clear();
    }

    protected void cancelEditPage() {
        if (editContentPage != null && editContentPage.getEditor().isCancelVisibleNow()) {
            ViewPage viewPage = editContentPage.cancel();
            viewPage.doWait();
        }
    }

    protected static void webSudo() throws IOException {
        final PostMethod request = new PostMethod(
                System.getProperty("baseurl.confluence") + "/doauthenticate.action" + getAuthQueryString()
        );
        request.addParameter("password", User.ADMIN.getPassword());
        final int status = client.executeMethod(request);
        assertThat(
                "WebSudo auth returned unexpected status",
                ImmutableSet.of(SC_MOVED_TEMPORARILY, SC_OK),
                hasItem(status)
        );
    }

    protected static String getAuthQueryString() {
        return "?os_username=" + User.ADMIN.getUsername() + "&os_password=" + User.ADMIN.getPassword();
    }

    private static String getBasicQueryString() {
        final String adminUserName = User.ADMIN.getUsername();
        final String adminPassword = User.ADMIN.getPassword();
        return "?username=" + adminUserName + "&password1=" + adminPassword + "&password2=" + adminPassword;
    }

    protected void closeDialog(final Dialog dialog) {
        if (dialog != null && dialog.isVisible()) {
            dialog.clickCancel();
            dialog.waitUntilHidden();
        }
    }

    protected MacroBrowserDialog openMacroBrowser(final EditContentPage editPage) {
        editPage.doWaitUntilTinyMceIsInit();
        return editPage.getEditor().openMacroBrowser();
    }
}