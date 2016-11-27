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
import com.atlassian.confluence.webdriver.pageobjects.component.dialog.MacroForm;
import com.atlassian.confluence.webdriver.pageobjects.component.dialog.MacroItem;
import com.atlassian.confluence.webdriver.pageobjects.page.NoOpPage;
import com.atlassian.confluence.webdriver.pageobjects.page.content.EditContentPage;
import com.atlassian.confluence.webdriver.pageobjects.page.content.ViewPage;
import com.atlassian.pageobjects.PageBinder;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.Poller;
import com.atlassian.webdriver.testing.annotation.TestedProductClass;
import com.atlassian.webdriver.utils.element.WebDriverPoller;
import com.google.common.collect.ImmutableSet;
import it.com.atlassian.confluence.plugins.webdriver.helper.ApplinkHelper;
import it.com.atlassian.confluence.plugins.webdriver.pageobjects.jiraissuefillter.JiraMacroSearchPanelDialog;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.apache.commons.httpclient.HttpStatus.SC_MOVED_TEMPORARILY;
import static org.apache.commons.httpclient.HttpStatus.SC_OK;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertThat;

@RunWith(ConfluenceStatelessTestRunner.class)
@TestedProductClass(ConfluenceTestedProduct.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public abstract class AbstractJiraIssueMacroTest {

    @Inject protected static ConfluenceTestedProduct product;
    @Inject protected static PageBinder pageBinder;
    @Inject protected static WebDriverPoller poller;

    @Fixture
    private static GroupFixture group = GroupFixture.groupFixture()
            .globalPermission(GlobalPermission.CONFLUENCE_ADMIN)
            .build();
    @Fixture
    protected static UserFixture user = UserFixture.userFixture()
            .group(group)
            .build();


    @Fixture
    protected static SpaceFixture space = SpaceFixture.spaceFixture()
            .permission(user, SpacePermission.VIEW, SpacePermission.PAGE_EDIT, SpacePermission.BLOG_EDIT)
            .build();

    protected static final HttpClient client = new HttpClient();
    protected static final String JIRA_ISSUE_MACRO_NAME = "jira";
    protected static final String PROJECT_TSTT = "Test Project";
    protected static final String PROJECT_TP = "Test Project 1";
    private static final String PROJECT_TST = "Test Project 2";
    private Map<String, String> internalJiraProjects = Collections.unmodifiableMap(new HashMap<String, String>() {
        {
            put(PROJECT_TSTT, "10011");
            put(PROJECT_TP, "10000");
            put(PROJECT_TST, "10010");
        }
    });

    protected EditContentPage editContentPage;


    @BeforeClass
    public static void start() throws Exception {
        webSudo();
        ApplinkHelper.setupAppLink(
                ApplinkHelper.ApplinkMode.BASIC, client, getAuthQueryString(), getBasicQueryString()
        );
        product.login(user.get(), NoOpPage.class);
    }

    @Before
    public final void alwaysSetup() throws Exception {
        setupEditPage();
    }

    @After
    public final void alwaysClear() throws Exception {
        cancelEditPage();
    }

    @AfterClass
    public static void teardown() throws Exception {
        webSudo();
        ApplinkHelper.removeAllAppLink(client, getAuthQueryString());
        product.logOutFast();
    }

    protected void setupEditPage() {
        if (editContentPage == null || !editContentPage.getEditor().isCancelVisibleNow()) {
            Content content = space.get().getHomepageRef().get();
            editContentPage = product.loginAndView(user.get(), content).edit();
        }
        Poller.waitUntilTrue("Edit page is ready", editContentPage.getEditor().isEditorCurrentlyActive());
        editContentPage.getEditor().getContent().clear().focus();
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

    protected static String getBasicQueryString() {
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

    protected String getProjectId(String projectName) {
        return internalJiraProjects.get(projectName);
    }

    protected void waitForAjaxRequest() {
        poller.waitUntil(
                input -> (Boolean) ((JavascriptExecutor) input).executeScript("return jQuery.active == 0;")
        );
    }

    protected JiraMacroSearchPanelDialog openJiraIssueSearchPanelDialogFromMacroBrowser() throws Exception {
        MacroBrowserDialog macroBrowserDialog = openMacroBrowser(editContentPage);

        // Although, `MacroBrowserDialog` has `searchFor` method to do search. But it's flaky test.
        // Here we tried to clearn field search first then try to search the searching term.
        PageElement searchFiled = macroBrowserDialog.getDialog().find(By.id("macro-browser-search"));
        searchFiled.clear();
        Iterable<MacroItem> macroItems = macroBrowserDialog.searchFor("embed jira issues");
        Poller.waitUntil(
                searchFiled.timed().getValue(),
                Matchers.equalToIgnoringCase("embed jira issues")
        );

        MacroForm macroForm = macroItems.iterator().next().select();
        macroForm.waitUntilHidden();

        return pageBinder.bind(JiraMacroSearchPanelDialog.class);
    }
}