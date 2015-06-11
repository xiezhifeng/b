package it.webdriver.com.atlassian.confluence.jiraissues.searchedpanel;

import com.atlassian.confluence.it.Page;
import com.atlassian.confluence.it.Space;
import com.atlassian.confluence.it.SpacePermission;
import it.webdriver.com.atlassian.confluence.pageobjects.JiraIssuesPage;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class JiraIssuesAnonymousViewWebDriverTest extends AbstractJiraIssuesSearchPanelWebDriverTest
{

    @Before
    public void start() throws Exception
    {
        super.start();
        rpc.enableAnonymousAccess();
        rpc.grantAnonymousPermission(SpacePermission.VIEW, Space.TEST);
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
        Assert.assertEquals("Number of issues", "2 issues", jiraIssuesPage.getNumberOfIssuesText());
        product.logOut();
        product.viewPage(Page.TEST.getIdAsString());

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
        createPageWithJiraIssueMacro(key);
        product.logOut();
        product.viewPage(Page.TEST.getIdAsString());
        return product.getPageBinder().bind(JiraIssuesPage.class);
    }

}
