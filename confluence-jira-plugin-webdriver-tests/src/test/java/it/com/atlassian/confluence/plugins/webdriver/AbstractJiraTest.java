package it.com.atlassian.confluence.plugins.webdriver;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import com.atlassian.confluence.api.model.content.Content;
import com.atlassian.confluence.it.User;
import it.com.atlassian.confluence.plugins.webdriver.helper.ApplinkHelper;
import it.com.atlassian.confluence.plugins.webdriver.pageobjects.JiraIssuesPage;
import it.com.atlassian.confluence.plugins.webdriver.pageobjects.jirachart.CreatedVsResolvedChartDialog;
import it.com.atlassian.confluence.plugins.webdriver.pageobjects.jirachart.JiraChartViewPage;
import it.com.atlassian.confluence.plugins.webdriver.pageobjects.jirachart.PieChartDialog;
import it.com.atlassian.confluence.plugins.webdriver.pageobjects.jirachart.TwoDimensionalChartDialog;
import it.com.atlassian.confluence.plugins.webdriver.pageobjects.jiraissuefillter.JiraMacroCreatePanelDialog;
import it.com.atlassian.confluence.plugins.webdriver.pageobjects.jiraissuefillter.JiraMacroRecentPanelDialog;
import it.com.atlassian.confluence.plugins.webdriver.pageobjects.jiraissuefillter.JiraMacroSearchPanelDialog;
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
import com.atlassian.confluence.webdriver.pageobjects.component.dialog.MacroForm;
import com.atlassian.confluence.webdriver.pageobjects.component.dialog.MacroItem;
import com.atlassian.confluence.webdriver.pageobjects.component.editor.EditorContent;
import com.atlassian.confluence.webdriver.pageobjects.component.editor.MacroPlaceholder;
import com.atlassian.confluence.webdriver.pageobjects.page.NoOpPage;
import com.atlassian.confluence.webdriver.pageobjects.page.content.EditContentPage;
import com.atlassian.confluence.webdriver.pageobjects.page.content.ViewPage;
import com.atlassian.pageobjects.PageBinder;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.Poller;
import com.atlassian.webdriver.testing.annotation.TestedProductClass;
import com.atlassian.webdriver.utils.element.WebDriverPoller;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableSet;
import it.com.atlassian.confluence.plugins.webdriver.pageobjects.sprint.JiraSprintMacroDialog;
import it.com.atlassian.confluence.plugins.webdriver.pageobjects.sprint.JiraSprintMacroPage;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.commons.httpclient.HttpStatus.SC_MOVED_TEMPORARILY;
import static org.apache.commons.httpclient.HttpStatus.SC_OK;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@RunWith(ConfluenceStatelessTestRunner.class)
@TestedProductClass(ConfluenceTestedProduct.class)
public class AbstractJiraTest
{
    public static final String JIRA_BASE_URL = System.getProperty("baseurl.jira", "http://localhost:11990/jira");
    public static final String JIRA_DISPLAY_URL = JIRA_BASE_URL.replace("localhost", "127.0.0.1");
    public static final String JIRA_ISSUE_MACRO_NAME = "jira";
    public static final String JIRA_CHART_MACRO_NAME = "jirachart";
    public static final String OLD_JIRA_ISSUE_MACRO_NAME = "jiraissues";
    public static final String JIRA_SPRINT_MACRO_NAME = "jirasprint";

    private static final int RETRY_TIME = 8;

    protected static final String PROJECT_TSTT = "Test Project";
    protected static final String PROJECT_TP = "Test Project 1";
    protected static final String PROJECT_TST = "Test Project 2";

    protected Map<String, String> internalJiraProjects = Collections.unmodifiableMap(new HashMap<String, String>() {
        {
            put(PROJECT_TSTT, "10011");
            put(PROJECT_TP, "10000");
            put(PROJECT_TST, "10010");
        }
    });

    private final Logger log = LoggerFactory.getLogger(AbstractJiraTest.class);
    
    @Inject protected static ConfluenceTestedProduct product;
    @Inject protected static PageBinder pageBinder;
    @Inject protected static ConfluenceRestClient restClient;
    @Inject protected static ConfluenceRpcClient rpcClient;
    @Inject protected WebDriverPoller poller;

    protected JiraMacroCreatePanelDialog jiraMacroCreatePanelDialog;
    protected JiraSprintMacroDialog sprintDialog;
    protected static EditContentPage editPage;
    protected JiraMacroSearchPanelDialog jiraMacroSearchPanelDialog;
    protected JiraMacroRecentPanelDialog dialogJiraRecentView;
    protected CreatedVsResolvedChartDialog dialogCreatedVsResolvedChart = null;
    protected TwoDimensionalChartDialog dialogTwoDimensionalChart;
    protected PieChartDialog dialogPieChart;
    protected static JiraChartViewPage pageJiraChartView;
    protected static ViewPage viewPage;
    protected JiraMacroSearchPanelDialog dialogSearchPanel;

    public static final HttpClient client = new HttpClient();

    @Fixture
    public static GroupFixture group = GroupFixture.groupFixture()
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

    protected static Content testPageContent;

    @BeforeClass
    public static void start() throws Exception
    {
        doWebSudo(client);

        if (!TestProperties.isOnDemandMode())
        {
            ApplinkHelper.setupAppLink(ApplinkHelper.ApplinkMode.BASIC, client, getAuthQueryString(), getBasicQueryString());
        }

        //login once, so that we don't repeatedly login and waste time - this test doesn't need it
        product.login(user.get(), NoOpPage.class);
    }

    protected String getProjectId(String projectName)
    {
        return internalJiraProjects.get(projectName);
    }

    protected MacroBrowserDialog openMacroBrowser(EditContentPage editPage)
    {
        editPage.doWaitUntilTinyMceIsInit();
        return editPage.getEditor().openMacroBrowser();
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
        l.addParameter("password", User.ADMIN.getPassword());
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
            dialog.clickCancel();
            dialog.waitUntilHidden();
        }
    }

    protected void waitForAjaxRequest()
    {
        poller.waitUntil(new Function<WebDriver, Boolean>()
        {
            @Override
            public Boolean apply(final WebDriver input)
            {
                return (Boolean) ((JavascriptExecutor) input).executeScript("return jQuery.active == 0;");
            }
        });
    }

    protected MacroPlaceholder createMacroPlaceholderFromQueryString(EditContentPage editPage, String jiraIssuesMacro, String macroName)
    {
        EditorContent content = editPage.getEditor().getContent();
        content.type(jiraIssuesMacro);
        content.waitForInlineMacro(macroName);
        final List<MacroPlaceholder> macroPlaceholders = content.macroPlaceholderFor(macroName);
        assertThat("No macro placeholder found", macroPlaceholders, hasSize(greaterThanOrEqualTo(1)));
        return macroPlaceholders.iterator().next();
    }

    protected JiraMacroSearchPanelDialog openJiraIssuesDialogFromMacroPlaceholder(EditContentPage editPage, MacroPlaceholder macroPlaceholder)
    {
        editPage.getEditor().getContent().doubleClickEditInlineMacro(macroPlaceholder.getAttribute("data-macro-name"));
        return pageBinder.bind(JiraMacroSearchPanelDialog.class);
    }

    protected JiraSprintMacroDialog openSprintDialogFromMacroPlaceholder(EditorContent editorContent, MacroPlaceholder macroPlaceholder)
    {
        editorContent.doubleClickEditInlineMacro(macroPlaceholder.getAttribute("data-macro-name"));
        return pageBinder.bind(JiraSprintMacroDialog.class);
    }

    protected String getMacroParams(EditContentPage editPage, String macroName)
    {
        MacroPlaceholder macroPlaceholder = editPage.getEditor().getContent().macroPlaceholderFor(macroName).iterator().next();
        return macroPlaceholder.getAttribute("data-macro-parameters");
    }

    protected JiraMacroSearchPanelDialog openJiraIssueSearchPanelDialogFromMacroBrowser(EditContentPage editPage) throws Exception
    {
        openDialogFromMacroBrowser(editPage, "embed jira issues");
        return pageBinder.bind(JiraMacroSearchPanelDialog.class);

    }

    protected JiraSprintMacroDialog openSprintDialogFromMacroBrowser(EditContentPage editPage) throws Exception
    {
        JiraMacroSearchPanelDialog dialog = openJiraIssueSearchPanelDialogFromMacroBrowser(editPage);
        dialog.selectMenuItem("JIRA Sprints");
        return pageBinder.bind(JiraSprintMacroDialog.class);
    }

    /**
     * Try to cancel Edit page in order to avoid browser modal dialog shows when navigating out out Edit page.
     * @param editPage
     */
    protected static void cancelEditPage(EditContentPage editPage)
    {
        // in editor page.
        if (editPage != null && editPage.getEditor().isCancelVisibleNow())
        {
            ViewPage viewPage = editPage.cancel();
            viewPage.doWait();
        }
    }

    protected JiraMacroCreatePanelDialog openJiraMacroCreateNewIssuePanelFromMenu() throws Exception
    {
        JiraMacroSearchPanelDialog dialog = openJiraIssueSearchPanelDialogFromMacroBrowser(editPage);
        dialog.selectMenuItem("Create New Issue");

        jiraMacroCreatePanelDialog = pageBinder.bind(JiraMacroCreatePanelDialog.class);
        return jiraMacroCreatePanelDialog;
    }

    protected JiraIssuesPage createPageWithJiraIssueMacro(String jql) throws Exception
    {
        return createPageWithJiraIssueMacro(jql, false);
    }

    protected JiraIssuesPage createPageWithJiraIssueMacro(String jql, boolean withPasteAction) throws Exception
    {
        jiraMacroSearchPanelDialog = openJiraIssueSearchPanelDialogFromMacroBrowser(editPage);
        if (withPasteAction)
        {
            jiraMacroSearchPanelDialog.pasteJqlSearch(jql);
        }
        else
        {
            jiraMacroSearchPanelDialog.inputJqlSearch(jql);
        }

        jiraMacroSearchPanelDialog.clickSearchButton();

        EditContentPage editContentPage = jiraMacroSearchPanelDialog.clickInsertDialog();
        editPage.getEditor().getContent().waitForInlineMacro(JIRA_ISSUE_MACRO_NAME);
        editContentPage.save();
        return bindCurrentPageToJiraIssues();
    }

    protected JiraIssuesPage bindCurrentPageToJiraIssues()
    {
        return pageBinder.bind(JiraIssuesPage.class);
    }

    protected JiraSprintMacroPage bindCurrentPageToSprintPage()
    {
        return pageBinder.bind(JiraSprintMacroPage.class);
    }

    protected PieChartDialog openPieChartDialog(boolean isAutoAuthentication)
    {
        openDialogFromMacroBrowser(editPage, "jira chart");
        PieChartDialog dialogPieChart = pageBinder.bind(PieChartDialog.class);

        if (isAutoAuthentication)
        {
            if (dialogPieChart.needAuthentication())
            {
                // going to authenticate
                dialogPieChart.doOAuthenticate();
            }
        }

        return dialogPieChart;
    }

    protected PieChartDialog openPieChartAndSearch()
    {
        dialogPieChart = openPieChartDialog(true);
        dialogPieChart.inputJqlSearch("status = open");
        dialogPieChart.clickPreviewButton();

        Assert.assertTrue(dialogPieChart.hadImageInDialog());
        return dialogPieChart;
    }

    protected CreatedVsResolvedChartDialog openAndSelectAndSearchCreatedVsResolvedChartMacroToEditor()
    {
        dialogCreatedVsResolvedChart = openJiraChartCreatedVsResolvedPanelDialog();
        dialogCreatedVsResolvedChart.inputJqlSearch("status = open");
        dialogCreatedVsResolvedChart.clickPreviewButton();
        assertTrue(dialogCreatedVsResolvedChart.hadChartImage());
        return dialogCreatedVsResolvedChart;
    }

    protected CreatedVsResolvedChartDialog openJiraChartCreatedVsResolvedPanelDialog()
    {
        PieChartDialog pieChartDialog = openPieChartDialog(true);
        pieChartDialog.selectMenuItem("Created vs Resolved");

        return pageBinder.bind(CreatedVsResolvedChartDialog.class);
    }

    protected TwoDimensionalChartDialog openTwoDimensionalChartDialog()
    {
        PieChartDialog pieChartDialog = openPieChartDialog(true);
        pieChartDialog.selectMenuItem("Two Dimensional");

        return pageBinder.bind(TwoDimensionalChartDialog.class);
    }

    private void openDialogFromMacroBrowser(EditContentPage editContentPage, String macroName)
    {
        MacroBrowserDialog macroBrowserDialog = openMacroBrowser(editContentPage);

        // "searchForFirst" method is flaky test. It types and search too fast.
        // macroBrowserDialog.searchForFirst("jira chart").select();

        // Although, `MacroBrowserDialog` has `searchFor` method to do search. But it's flaky test.
        // Here we tried to clear field search first then try to search the searching term.
        PageElement searchFiled = macroBrowserDialog.getDialog().find(By.id("macro-browser-search"));
        searchFiled.clear();

        Iterable<MacroItem> macroItems = macroBrowserDialog.searchFor(macroName);
        Poller.waitUntil(searchFiled.timed().getValue(), Matchers.equalToIgnoringCase(macroName));

        MacroForm macroForm = macroItems.iterator().next().select();
        macroForm.waitUntilHidden();
    }
}
