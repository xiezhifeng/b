package it.com.atlassian.confluence.plugins.webdriver.jiraissues.searchpanel;

import com.atlassian.confluence.it.SpacePermission;
import com.atlassian.confluence.it.rpc.ConfluenceRpc;
import com.atlassian.confluence.plugins.pageobjects.JiraIssuesPage;
import com.atlassian.confluence.webdriver.pageobjects.page.content.ViewPage;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class JiraIssuesAnonymousViewTest extends AbstractJiraIssuesSearchPanelTest
{
    protected static ConfluenceRpc rpc = ConfluenceRpc.newInstance(System.getProperty("baseurl.confluence"), ConfluenceRpc.Version.V2_WITH_WIKI_MARKUP);

    @Before
    public void setup() throws Exception
    {
        // enable anonymous access for each page, which is pretty slow. TODO: enable BeforeClass
        rpc.enableAnonymousAccess();
        rpc.executeOnCurrentNode("addAnonymousPermissionToSpace", SpacePermission.VIEW.getValue(), space.get().getKey());
        super.setup();
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
        Assert.assertTrue(jiraIssuesPage.isSingleContainText("TST-1"));
        Assert.assertTrue(jiraIssuesPage.isSingleContainText("Test bug"));
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
