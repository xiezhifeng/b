package it.com.atlassian.confluence.plugins.webdriver.jiraissues.searchpanel;

import com.atlassian.confluence.it.SpacePermission;
import com.atlassian.confluence.plugins.pageobjects.JiraIssuesPage;
import com.atlassian.confluence.webdriver.pageobjects.page.content.ViewPage;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class JiraIssuesAnonymousViewTest extends AbstractJiraIssuesSearchPanelTest
{

    @Before
    public void setup() throws Exception
    {
        // enable anonymous access for each page, which is pretty slow. TODO: enable BeforeClass
        rpc.enableAnonymousAccess();
        rpc.executeOnCurrentNode("addAnonymousPermissionToSpace", SpacePermission.VIEW.getValue(), space.get().getKey());
        super.setup();
    }

    @Test
    public void testAnonymousCanNotViewIssue() throws InterruptedException
    {
        JiraIssuesPage jiraIssuesPage = setupSingleIssuePage("TP-1");
        Assert.assertTrue(jiraIssuesPage.getErrorMessage().hasClass("jim-error-message-single"));
    }

    @Test
    public void testAnonymousCanViewSomeIssues()
    {
        createPageWithJiraIssueMacro("project=TP");
        JiraIssuesPage jiraIssuesPage = product.getPageBinder().bind(JiraIssuesPage.class);
        String pageId = String.valueOf(jiraIssuesPage.getPageId());
        Assert.assertEquals("Number of issues", "2 issues", jiraIssuesPage.getNumberOfIssuesText());
        product.logOut();
        product.viewPage(pageId);

        jiraIssuesPage = product.getPageBinder().bind(JiraIssuesPage.class);
        Assert.assertEquals("Number of issues", "1 issue", jiraIssuesPage.getNumberOfIssuesText());
    }

    @Test
    public void testAnonymousCanViewIssue() throws InterruptedException
    {
        JiraIssuesPage jiraIssuesPage = setupSingleIssuePage("TST-1");
        Assert.assertTrue(jiraIssuesPage.isSingleContainText("TST-1"));
        Assert.assertTrue(jiraIssuesPage.isSingleContainText("Test bug"));
    }

    private JiraIssuesPage setupSingleIssuePage(String key)
    {
        ViewPage viewPage = createPageWithJiraIssueMacro(key);
        String pageId = String.valueOf(viewPage.getPageId());
        product.logOut();
        product.viewPage(pageId);
        return product.getPageBinder().bind(JiraIssuesPage.class);
    }

}
