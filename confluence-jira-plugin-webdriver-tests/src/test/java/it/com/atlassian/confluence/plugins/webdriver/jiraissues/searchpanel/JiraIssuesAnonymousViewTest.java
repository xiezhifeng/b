package it.com.atlassian.confluence.plugins.webdriver.jiraissues.searchpanel;

import com.atlassian.confluence.test.rpc.api.ConfluenceRpcClient;
import com.atlassian.confluence.test.rpc.api.permissions.SpacePermission;
import com.atlassian.confluence.webdriver.pageobjects.page.content.ViewPage;
import com.atlassian.pageobjects.elements.query.Poller;
import it.com.atlassian.confluence.plugins.webdriver.pageobjects.JiraIssuesPage;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.inject.Inject;

public class JiraIssuesAnonymousViewTest extends AbstractJiraIssueMacroSearchPanelTest
{
    @Inject private static ConfluenceRpcClient rpcClient;

    @BeforeClass
    public static void start() throws Exception
    {
        AbstractJiraIssueMacroSearchPanelTest.start();
        rpcClient.getAdminSession().getSystemComponent().enableAnonymousAccess();
        rpcClient.getAdminSession().getPermissionsComponent().grantAnonymousPermission(SpacePermission.VIEW, space.get());
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

    private JiraIssuesPage setupSingleIssuePage(String key) throws Exception
    {
        ViewPage viewPage = createPageWithJiraIssueMacro(key);
        String pageId = String.valueOf(viewPage.getPageId());
        product.logOut();
        product.viewPage(pageId);
        return pageBinder.bind(JiraIssuesPage.class);
    }

}
