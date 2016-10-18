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
import com.atlassian.confluence.webdriver.pageobjects.component.dialog.MacroForm;
import com.atlassian.confluence.webdriver.pageobjects.component.dialog.MacroItem;
import com.atlassian.confluence.webdriver.pageobjects.page.NoOpPage;
import com.atlassian.confluence.webdriver.pageobjects.page.content.EditContentPage;
import com.atlassian.confluence.webdriver.pageobjects.page.content.ViewPage;
import com.atlassian.pageobjects.PageBinder;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.Poller;
import com.atlassian.webdriver.testing.annotation.TestedProductClass;
import com.google.common.collect.ImmutableSet;
import it.com.atlassian.confluence.plugins.webdriver.helper.ApplinkHelper;
import it.com.atlassian.confluence.plugins.webdriver.pageobjects.jirachart.CreatedVsResolvedChartDialog;
import it.com.atlassian.confluence.plugins.webdriver.pageobjects.jirachart.JiraChartViewPage;
import it.com.atlassian.confluence.plugins.webdriver.pageobjects.jirachart.PieChartDialog;
import it.com.atlassian.confluence.plugins.webdriver.pageobjects.jirachart.TwoDimensionalChartDialog;
import it.com.atlassian.confluence.plugins.webdriver.pageobjects.jiraissuefillter.JiraMacroRecentPanelDialog;
import it.com.atlassian.confluence.plugins.webdriver.pageobjects.jiraissuefillter.JiraMacroSearchPanelDialog;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.openqa.selenium.By;

import javax.inject.Inject;
import java.io.IOException;

import static org.apache.commons.httpclient.HttpStatus.SC_MOVED_TEMPORARILY;
import static org.apache.commons.httpclient.HttpStatus.SC_OK;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@RunWith(ConfluenceStatelessTestRunner.class)
@TestedProductClass(ConfluenceTestedProduct.class)
public abstract class AbstractJiraIssueMacroTest {

    public static final String JIRA_BASE_URL = System.getProperty("baseurl.jira", "http://localhost:11990/jira");
    public static final String JIRA_DISPLAY_URL = JIRA_BASE_URL.replace("localhost", "127.0.0.1");
    public static final String JIRA_ISSUE_MACRO_NAME = "jira";
    public static final String JIRA_CHART_MACRO_NAME = "jirachart";
    public static final String OLD_JIRA_ISSUE_MACRO_NAME = "jiraissues";

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
    private static SpaceFixture space = SpaceFixture.spaceFixture()
            .permission(user, SpacePermission.VIEW, SpacePermission.PAGE_EDIT, SpacePermission.BLOG_EDIT)
            .build();

    protected EditContentPage editPage;
    protected JiraMacroSearchPanelDialog jiraMacroSearchPanelDialog;
    protected JiraMacroRecentPanelDialog dialogJiraRecentView;
    protected CreatedVsResolvedChartDialog dialogCreatedVsResolvedChart;
    protected TwoDimensionalChartDialog dialogTwoDimensionalChart;
    protected PieChartDialog dialogPieChart;
    protected JiraMacroSearchPanelDialog dialogSearchPanel;
    protected JiraChartViewPage pageJiraChartView;

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
        editPage.dismissEditorNotifications();
    }

    @After
    public void clear() throws Exception {
        cancelEditPage(editPage);
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

    protected static String getBasicQueryString() {
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

    protected PieChartDialog openPieChartDialog(boolean isAutoAuthentication) {
        MacroBrowserDialog macroBrowserDialog = openMacroBrowser(editPage);

        // "searchForFirst" method is flaky test. It types and search too fast.
        // macroBrowserDialog.searchForFirst("jira chart").select();

        // Although, `MacroBrowserDialog` has `searchFor` method to do search. But it's flaky test.
        // Here we tried to clearn field search first then try to search the searching term.
        PageElement searchFiled = macroBrowserDialog.getDialog().find(By.id("macro-browser-search"));
        searchFiled.clear();

        Iterable<MacroItem> macroItems = macroBrowserDialog.searchFor("jira chart");
        Poller.waitUntil(searchFiled.timed().getValue(), Matchers.equalToIgnoringCase("jira chart"));

        MacroForm macroForm = macroItems.iterator().next().select();
        macroForm.waitUntilHidden();

        PieChartDialog dialogPieChart = pageBinder.bind(PieChartDialog.class);

        if (isAutoAuthentication) {
            if (dialogPieChart.needAuthentication()) {
                // going to authenticate
                dialogPieChart.doOAuthenticate();
            }
        }
        return dialogPieChart;
    }

    protected PieChartDialog openPieChartAndSearch() {
        dialogPieChart = openPieChartDialog(true);
        dialogPieChart.inputJqlSearch("status = open");
        dialogPieChart.clickPreviewButton();

        Assert.assertTrue(dialogPieChart.hadImageInDialog());
        return dialogPieChart;
    }

    protected CreatedVsResolvedChartDialog openJiraChartCreatedVsResolvedPanelDialog() {
        PieChartDialog pieChartDialog = openPieChartDialog(true);
        pieChartDialog.selectMenuItem("Created vs Resolved");
        return pageBinder.bind(CreatedVsResolvedChartDialog.class);
    }

    protected CreatedVsResolvedChartDialog openAndSelectAndSearchCreatedVsResolvedChartMacroToEditor() {
        dialogCreatedVsResolvedChart = openJiraChartCreatedVsResolvedPanelDialog();
        dialogCreatedVsResolvedChart.inputJqlSearch("status = open");
        dialogCreatedVsResolvedChart.clickPreviewButton();
        assertTrue(dialogCreatedVsResolvedChart.hadChartImage());
        return dialogCreatedVsResolvedChart;
    }

    protected TwoDimensionalChartDialog openTwoDimensionalChartDialog() {
        PieChartDialog pieChartDialog = openPieChartDialog(true);
        pieChartDialog.selectMenuItem("Two Dimensional");
        return pageBinder.bind(TwoDimensionalChartDialog.class);
    }

    protected void cancelEditPage(EditContentPage editPage) {
        // in editor page.
        if (editPage != null && editPage.getEditor().isCancelVisibleNow()) {
            ViewPage viewPage = editPage.cancel();
            viewPage.doWait();
        }
    }
}