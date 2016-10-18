package it.com.atlassian.confluence.plugins.webdriver;

import com.atlassian.confluence.api.model.content.Content;
import com.atlassian.confluence.it.User;
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
import com.atlassian.webdriver.testing.annotation.TestedProductClass;
import com.google.common.collect.ImmutableSet;
import it.com.atlassian.confluence.plugins.webdriver.helper.ApplinkHelper;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

import javax.inject.Inject;
import java.io.IOException;

import static org.apache.commons.httpclient.HttpStatus.SC_MOVED_TEMPORARILY;
import static org.apache.commons.httpclient.HttpStatus.SC_OK;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertThat;

@RunWith(ConfluenceStatelessTestRunner.class)
@TestedProductClass(ConfluenceTestedProduct.class)
public class AbstractJiraIssueMacroTest {

    @Inject protected static ConfluenceTestedProduct product;
    @Inject protected static PageBinder pageBinder;
    @Inject protected static ConfluenceRestClient restClient;
    @Inject protected static ConfluenceRpcClient rpcClient;

    protected static final HttpClient client = new HttpClient();

    @Fixture
    private static GroupFixture group = GroupFixture.groupFixture()
            .globalPermission(GlobalPermission.CONFLUENCE_ADMIN)
            .build();

    @Fixture
    public static UserFixture user = UserFixture.userFixture()
            .group(group)
            .build();

    @Fixture
    public static SpaceFixture space = SpaceFixture.spaceFixture()
            .permission(user, SpacePermission.VIEW, SpacePermission.PAGE_EDIT, SpacePermission.BLOG_EDIT)
            .build();

    protected EditContentPage editPage;

    @BeforeClass
    public static void start() throws Exception {
        doWebSudo(client);
        ApplinkHelper.setupAppLink(ApplinkHelper.ApplinkMode.BASIC, client, getAuthQueryString(), getBasicQueryString());
        product.login(user.get(), NoOpPage.class);
    }

    @AfterClass
    public static void teardown() throws Exception {
        ApplinkHelper.removeAllAppLink(client, getAuthQueryString());
    }

    @Before
    public void setup() throws Exception {
        Content content = space.get().getHomepageRef().get();
        editPage = product.viewPage(content).edit();
        Poller.waitUntilTrue("Edit page is ready", editPage.getEditor().isEditorCurrentlyActive());
        editPage.getEditor().getContent().clear();
    }

    protected static void doWebSudo(final HttpClient client) throws IOException {
        final PostMethod l = new PostMethod(System.getProperty("baseurl.confluence") + "/doauthenticate.action" + getAuthQueryString());
        l.addParameter("password", User.ADMIN.getPassword());
        final int status = client.executeMethod(l);
        assertThat("WebSudo auth returned unexpected status", ImmutableSet.of(SC_MOVED_TEMPORARILY, SC_OK), hasItem(status));
    }

    protected static String getAuthQueryString() {
        return "?os_username=" + User.ADMIN.getUsername() + "&os_password=" + User.ADMIN.getPassword();
    }

    private static String getBasicQueryString() {
        final String adminUserName = User.ADMIN.getUsername();
        final String adminPassword = User.ADMIN.getPassword();
        return "?username=" + adminUserName + "&password1=" + adminPassword + "&password2=" + adminPassword;
    }

    protected MacroBrowserDialog openMacroBrowser(EditContentPage editPage) {
        editPage.doWaitUntilTinyMceIsInit();
        return editPage.getEditor().openMacroBrowser();
    }

    protected void closeDialog(final Dialog dialog) {
        if (dialog != null && dialog.isVisible())
        {
            dialog.clickCancel();
            dialog.waitUntilHidden();
        }
    }

    protected static void cancelEditPage(EditContentPage editPage) {
        // in editor page.
        if (editPage != null && editPage.getEditor().isCancelVisibleNow()) {
            ViewPage viewPage = editPage.cancel();
            viewPage.doWait();
        }
    }
}