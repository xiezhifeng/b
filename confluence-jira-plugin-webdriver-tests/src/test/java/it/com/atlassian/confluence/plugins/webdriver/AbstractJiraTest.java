package it.com.atlassian.confluence.plugins.webdriver;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import com.atlassian.confluence.api.model.content.Content;
import com.atlassian.confluence.it.User;
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
import com.atlassian.pageobjects.elements.query.Conditions;
import com.atlassian.pageobjects.elements.query.Poller;
import com.atlassian.webdriver.testing.annotation.TestedProductClass;
import com.atlassian.webdriver.utils.element.WebDriverPoller;

import com.google.common.base.Function;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableSet;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import it.com.atlassian.confluence.plugins.webdriver.helper.ApplinkHelper;
import it.com.atlassian.confluence.plugins.webdriver.pageobjects.AbstractJiraIssueMacroDialog;
import it.com.atlassian.confluence.plugins.webdriver.pageobjects.JiraIssuesPage;
import it.com.atlassian.confluence.plugins.webdriver.pageobjects.jirachart.CreatedVsResolvedChartDialog;
import it.com.atlassian.confluence.plugins.webdriver.pageobjects.jirachart.JiraChartViewPage;
import it.com.atlassian.confluence.plugins.webdriver.pageobjects.jirachart.PieChartDialog;
import it.com.atlassian.confluence.plugins.webdriver.pageobjects.jirachart.TwoDimensionalChartDialog;
import it.com.atlassian.confluence.plugins.webdriver.pageobjects.jiraissuefillter.JiraMacroCreatePanelDialog;
import it.com.atlassian.confluence.plugins.webdriver.pageobjects.jiraissuefillter.JiraMacroSearchPanelDialog;
import it.com.atlassian.confluence.plugins.webdriver.pageobjects.sprint.JiraSprintMacroDialog;
import it.com.atlassian.confluence.plugins.webdriver.pageobjects.sprint.JiraSprintMacroPage;

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

    protected static EditContentPage editPage;
    protected static JiraChartViewPage pageJiraChartView;
    protected static ViewPage viewPage;

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
            ApplinkHelper.setupAppLink(ApplinkHelper.ApplinkMode.BASIC, client, ApplinkHelper.getAuthQueryString(), ApplinkHelper.getBasicQueryString());
        }

        //login once, so that we don't repeatedly login and waste time - this test doesn't need it
        product.login(user.get(), NoOpPage.class);
    }

    protected String getProjectId(String projectName)
    {
        return internalJiraProjects.get(projectName);
    }

    protected static void doWebSudo(final HttpClient client) throws IOException
    {
        final PostMethod l = new PostMethod(System.getProperty("baseurl.confluence") + "/doauthenticate.action" + ApplinkHelper.getAuthQueryString());
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

    protected void closeDialog(AbstractJiraIssueMacroDialog dialog)
    {
        if (dialog != null && dialog.isVisible())
        {
            dialog.clickCancel();
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

    protected String getMacroParams(EditContentPage editPage, String macroName)
    {
        EditorContent editorContent =  editPage.getEditor().getContent();
        List<MacroPlaceholder> macroPlaceholders = editorContent.macroPlaceholderFor(macroName);
        MacroPlaceholder macroPlaceholder = macroPlaceholders.iterator().next();
        return macroPlaceholder.getAttribute("data-macro-parameters");
    }

    protected JiraMacroSearchPanelDialog openJiraIssueSearchPanelDialogFromMacroBrowser(EditContentPage editPage) throws Exception
    {
        openDialogFromMacroBrowser(editPage, "embed jira issues");
        JiraMacroSearchPanelDialog dialog = pageBinder.bind(JiraMacroSearchPanelDialog.class);
        dialog.waitUntilVisible();
        return dialog;
    }

    protected JiraSprintMacroDialog openSprintDialogFromMacroBrowser(EditContentPage editPage) throws Exception
    {
        JiraMacroSearchPanelDialog dialog = openJiraIssueSearchPanelDialogFromMacroBrowser(editPage);
        dialog.selectPanel("Sprint");
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
        dialog.selectTabItem("Create New Issue");
        return pageBinder.bind(JiraMacroCreatePanelDialog.class);
    }

    protected JiraIssuesPage createPageWithJiraIssueMacro(String jql) throws Exception
    {
        return createPageWithJiraIssueMacro(jql, false);
    }

    protected JiraIssuesPage createPageWithJiraIssueMacro(String jql, boolean withPasteAction) throws Exception
    {
        JiraMacroSearchPanelDialog dialog = openJiraIssueSearchPanelDialogFromMacroBrowser(editPage);
        if (withPasteAction)
        {
            dialog.pasteJqlSearch(jql);
        }
        else
        {
            dialog.inputJqlSearch(jql);
        }

        dialog.clickSearchButton();

        Poller.waitUntilTrue(dialog.isInsertButtonEnabledTimed());
        EditContentPage editContentPage = dialog.clickInsertDialog();
        dialog.waitUntilHidden();

        editPage.getEditor().getContent().waitForInlineMacro(JIRA_ISSUE_MACRO_NAME);
        editContentPage.save();
        return pageBinder.bind(JiraIssuesPage.class);
    }

    /**
     * Try to close/dismiss any alert dialog if it appears in editor page.
     * Somehow there is sometimes alert popup editor page. For instance,
     * when users click a link to do authentication in editor, it will open a new tab/window and trigger "unload" event
     * to trigger an alert popup in editor page.
     */
    public void closeExistingAlertIfHave()
    {
        try
        {
            WebDriver driver = product.getTester().getDriver();
            WebDriverWait wait = new WebDriverWait(driver, 1);
            wait.until(ExpectedConditions.alertIsPresent());
            driver.switchTo().alert().dismiss();

            Poller.waitUntilTrue("alert did not disappear after dismissing", Conditions.forSupplier(new Supplier<Boolean>()
            {
                @Override
                public Boolean get()
                {
                    return ExpectedConditions.alertIsPresent().apply(product.getTester().getDriver()) == null;
                }
            }));
        }
        catch (Exception ignored)
        {
            log.error("There is no existing alert", ignored);
        }
    }

    protected MacroBrowserDialog openMacroBrowser(EditContentPage editPage)
    {
        editPage.doWaitUntilTinyMceIsInit();
        return editPage.getEditor().openMacroBrowser();
    }

    protected void openDialogFromMacroBrowser(EditContentPage editContentPage, String macroName)
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

    /**
     * Make sure we are in editing mode to run some tests.
     * This method is usually called on before running a test.
     */
    public void getReadyOnEditTestPage()
    {
        if (editPage == null)
        {
            editPage = gotoEditTestPage(user.get());
        }
        else
        {
            if (editPage.getEditor().isCancelVisibleNow())
            {
                // in editor page.
                editPage.getEditor().getContent().clear();
            }
            else
            {
                // in view page, and then need to go to edit page.
                editPage = gotoEditTestPage(user.get());
            }
        }
    }

    protected PieChartDialog openPieChartDialog(boolean isAutoAuthentication)
    {
        openDialogFromMacroBrowser(editPage, "jira chart");
        PieChartDialog dialog = pageBinder.bind(PieChartDialog.class);
        dialog.waitUntilVisible();

        if (isAutoAuthentication)
        {
            if (dialog.needAuthentication())
            {
                // going to authenticate
                dialog.doOAuthenticate();
                dialog = pageBinder.bind(PieChartDialog.class);
            }
        }

        return dialog;
    }

    protected PieChartDialog openPieChartAndSearch()
    {
        PieChartDialog dialog = openPieChartDialog(true);
        dialog.inputJqlSearch("status = open");
        dialog.clickPreviewButton();

        Assert.assertTrue(dialog.hadImageInDialog());
        return dialog;
    }

    protected CreatedVsResolvedChartDialog openAndSelectAndSearchCreatedVsResolvedChartMacroToEditor()
    {
        CreatedVsResolvedChartDialog dialog = openJiraChartCreatedVsResolvedChartDialog();
        dialog.inputJqlSearch("status = open");
        dialog.clickPreviewButton();
        assertTrue(dialog.hadChartImage());
        return dialog;
    }

    protected CreatedVsResolvedChartDialog openJiraChartCreatedVsResolvedChartDialog()
    {
        PieChartDialog dialog = openPieChartDialog(true);
        dialog.selectTabItem("Created vs Resolved");
        closeExistingAlertIfHave();
        return pageBinder.bind(CreatedVsResolvedChartDialog.class);
    }

    protected TwoDimensionalChartDialog openTwoDimensionalChartDialog()
    {
        PieChartDialog dialog = openPieChartDialog(true);
        dialog.selectTabItem("Two Dimensional");
        closeExistingAlertIfHave();
        return pageBinder.bind(TwoDimensionalChartDialog.class);
    }
}
