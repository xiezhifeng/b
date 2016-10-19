package it.com.atlassian.confluence.plugins.webdriver;

import com.atlassian.confluence.api.model.content.Content;
import com.atlassian.confluence.it.User;
import com.atlassian.confluence.test.api.model.person.UserWithDetails;
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
import com.atlassian.webdriver.utils.element.WebDriverPoller;
import com.google.common.collect.ImmutableSet;
import it.com.atlassian.confluence.plugins.webdriver.helper.ApplinkHelper;
import it.com.atlassian.confluence.plugins.webdriver.pageobjects.JiraIssuesPage;
import it.com.atlassian.confluence.plugins.webdriver.pageobjects.jiraissuefillter.JiraMacroCreatePanelDialog;
import it.com.atlassian.confluence.plugins.webdriver.pageobjects.jiraissuefillter.JiraMacroSearchPanelDialog;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.hamcrest.Matchers;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.openqa.selenium.By;

import javax.inject.Inject;
import java.io.IOException;

import static org.apache.commons.httpclient.HttpStatus.SC_MOVED_TEMPORARILY;
import static org.apache.commons.httpclient.HttpStatus.SC_OK;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertThat;

@RunWith(ConfluenceStatelessTestRunner.class)
@TestedProductClass(ConfluenceTestedProduct.class)
public abstract class AbstractJiraTest
{
    public static final String JIRA_BASE_URL = System.getProperty("baseurl.jira", "http://localhost:11990/jira");
    public static final String JIRA_DISPLAY_URL = JIRA_BASE_URL.replace("localhost", "127.0.0.1");
    public static final String JIRA_ISSUE_MACRO_NAME = "jira";

    protected static final String PROJECT_TSTT = "Test Project";

    @Inject protected static ConfluenceTestedProduct product;
    @Inject protected static PageBinder pageBinder;
    @Inject protected static ConfluenceRestClient restClient;
    @Inject protected static ConfluenceRpcClient rpcClient;
    @Inject protected WebDriverPoller poller;

    protected JiraMacroCreatePanelDialog jiraMacroCreatePanelDialog;
    protected static EditContentPage editPage;
    protected JiraMacroSearchPanelDialog jiraMacroSearchPanelDialog;

    public static final HttpClient client = new HttpClient();

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

    @BeforeClass
    public static void start() throws Exception
    {
        doWebSudo(client);
        ApplinkHelper.setupAppLink(ApplinkHelper.ApplinkMode.BASIC, client, getAuthQueryString(), getBasicQueryString());
        //login once, so that we don't repeatedly login and waste time - this test doesn't need it
        product.login(user.get(), NoOpPage.class);
    }

    private MacroBrowserDialog openMacroBrowser(EditContentPage editPage)
    {
        editPage.doWaitUntilTinyMceIsInit();
        return editPage.getEditor().openMacroBrowser();
    }

    public static String getAuthQueryString()
    {
        return "?os_username=" + User.ADMIN.getUsername() + "&os_password=" + User.ADMIN.getPassword();
    }

    private static String getBasicQueryString()
    {
        final String adminUserName = User.ADMIN.getUsername();
        final String adminPassword = User.ADMIN.getPassword();
        return "?username=" + adminUserName + "&password1=" + adminPassword + "&password2=" + adminPassword;
    }

    private static void doWebSudo(final HttpClient client) throws IOException
    {
        final PostMethod l = new PostMethod(System.getProperty("baseurl.confluence") + "/doauthenticate.action" + getAuthQueryString());
        l.addParameter("password", User.ADMIN.getPassword());
        final int status = client.executeMethod(l);
        assertThat("WebSudo auth returned unexpected status", ImmutableSet.of(SC_MOVED_TEMPORARILY, SC_OK), hasItem(status));
    }

    protected static EditContentPage gotoEditTestPage(UserWithDetails user)
    {
        Content testPageContent = space.get().getHomepageRef().get();
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

    protected JiraMacroSearchPanelDialog openJiraIssueSearchPanelDialogFromMacroBrowser(EditContentPage editPage) throws Exception
    {
        JiraMacroSearchPanelDialog dialog;

        MacroBrowserDialog macroBrowserDialog = openMacroBrowser(editPage);

        // Although, `MacroBrowserDialog` has `searchFor` method to do search. But it's flaky test.
        // Here we tried to clearn field search first then try to search the searching term.
        PageElement searchFiled = macroBrowserDialog.getDialog().find(By.id("macro-browser-search"));
        searchFiled.clear();
        Iterable<MacroItem> macroItems = macroBrowserDialog.searchFor("embed jira issues");
        Poller.waitUntil(searchFiled.timed().getValue(), Matchers.equalToIgnoringCase("embed jira issues"));

        MacroForm macroForm = macroItems.iterator().next().select();
        macroForm.waitUntilHidden();

        dialog = pageBinder.bind(JiraMacroSearchPanelDialog.class);

        return dialog;
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

    protected JiraIssuesPage createPageWithJiraIssueMacro(String jql) throws Exception
    {
        return createPageWithJiraIssueMacro(jql, false);
    }

    private JiraIssuesPage createPageWithJiraIssueMacro(String jql, boolean withPasteAction) throws Exception
    {
        EditContentPage editContentPage = addJiraIssueMacroToPage(jql, withPasteAction);

        editContentPage.save();
        return bindCurrentPageToJiraIssues();
    }

    private EditContentPage addJiraIssueMacroToPage(String jql, boolean withPasteAction) throws Exception
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

        return editContentPage;
    }

    protected JiraIssuesPage bindCurrentPageToJiraIssues()
    {
        return pageBinder.bind(JiraIssuesPage.class);
    }

}
