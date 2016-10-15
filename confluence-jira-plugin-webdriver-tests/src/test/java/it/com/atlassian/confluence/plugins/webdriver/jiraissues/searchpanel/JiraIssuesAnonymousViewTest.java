package it.com.atlassian.confluence.plugins.webdriver.jiraissues.searchpanel;

import com.atlassian.confluence.it.SpacePermission;
import com.atlassian.confluence.it.rpc.ConfluenceRpc;
import com.atlassian.pageobjects.elements.query.Poller;
import it.com.atlassian.confluence.plugins.webdriver.pageobjects.JiraIssuesPage;
import com.atlassian.confluence.webdriver.pageobjects.page.content.ViewPage;

import org.junit.After;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class JiraIssuesAnonymousViewTest extends AbstractJiraIssuesSearchPanelTest
{
    protected static ConfluenceRpc rpc = ConfluenceRpc.newInstance(System.getProperty("baseurl.confluence"), ConfluenceRpc.Version.V2_WITH_WIKI_MARKUP);

    @BeforeClass
    public static void init() throws Exception
    {
        // enable anonymous access for each page, which is pretty slow. TODO: enable BeforeClass
        rpc.enableAnonymousAccess();
        rpc.executeOnCurrentNode("addAnonymousPermissionToSpace", SpacePermission.VIEW.getValue(), space.get().getKey());
    }

    @After
    public void reLoginSoAbstractClassWorksUnhappyFace() {
        product.loginAndView(user.get(), page.get());
    }

    @Test
    public void testAnonymousCanNotViewIssue() throws Exception
    {
        JiraIssuesPage jiraIssuesPage = setupSingleIssuePage("TP-1");
        Assert.assertTrue(jiraIssuesPage.getErrorMessage().hasClass("jim-error-message-single"));
    }

    @Test
    public void testAnonymousCanViewSomeIssues() throws Exception
    {
        createPageWithJiraIssueMacro("project=TP");
        JiraIssuesPage jiraIssuesPage = pageBinder.bind(JiraIssuesPage.class);
        String pageId = String.valueOf(jiraIssuesPage.getPageId());
        Assert.assertEquals("Number of issues", "2 issues", jiraIssuesPage.getNumberOfIssuesText());
        product.logOut();
        product.viewPage(pageId);

        jiraIssuesPage = pageBinder.bind(JiraIssuesPage.class);
        Assert.assertEquals("Number of issues", "1 issue", jiraIssuesPage.getNumberOfIssuesText());

    }

    @Test
    public void testAnonymousCanViewIssue() throws Exception
    {
        JiraIssuesPage jiraIssuesPage = setupSingleIssuePage("TST-1");
        Poller.waitUntilTrue(jiraIssuesPage.isSingleContainText("TST-1"));
        Poller.waitUntilTrue(jiraIssuesPage.isSingleContainText("Test bug"));
    }

    protected JiraIssuesPage setupSingleIssuePage(String key) throws Exception
    {
        ViewPage viewPage = createPageWithJiraIssueMacro(key);
        String pageId = String.valueOf(viewPage.getPageId());
        product.logOut();
        product.viewPage(pageId);
        return pageBinder.bind(JiraIssuesPage.class);
    }

}
