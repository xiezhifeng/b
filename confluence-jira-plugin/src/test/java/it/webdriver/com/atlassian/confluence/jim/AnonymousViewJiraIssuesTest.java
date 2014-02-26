package it.webdriver.com.atlassian.confluence.jim;

import static org.junit.Assert.assertTrue;
import it.webdriver.com.atlassian.confluence.pageobjects.JiraIssuesPage;

import org.junit.Test;

import com.atlassian.confluence.it.Space;
import com.atlassian.confluence.it.SpacePermission;
import com.atlassian.confluence.pageobjects.page.content.EditContentPage;
import com.atlassian.confluence.pageobjects.page.content.ViewPage;
import com.atlassian.pageobjects.elements.PageElement;

public class AnonymousViewJiraIssuesTest extends AbstractJIMTest
{

    private ViewPage viewPage;

    @Override
    public void setup() throws Exception
    {
        super.setup();
        viewPage = setupTestData();
    }

    @Override
    public void tearDown()
    {
        super.tearDown();
        rpc.removePage(viewPage.getPageId());
    }

    @Test
    public void testAnonymousPermissions() throws InterruptedException
    {
        JiraIssuesPage issuePage = bindCurrentPageToJiraIssues();

        PageElement tp1Issue = issuePage.getSingleIssue("TP-1");
        assertTrue(tp1Issue.isPresent());
        assertTrue("summary of a restricted issue is leaked in single issue", !tp1Issue.getText().contains("Bug -1"));
        assertTrue("status of a restricted issue is leaked single issue", !tp1Issue.getText().contains("Open"));

        PageElement issueTable = issuePage.getIssuesTableElement();
        assertTrue("restricted issue is leaked in table mode", !issueTable.getText().contains("TP-1"));

        PageElement anonymousIssue = issuePage.getSingleIssue("TST-1");
        assertTrue("summary of anonymous issue couldn't be rendered", anonymousIssue.getText().contains("Test bug"));
        assertTrue("status of anonymous issue couldn't be rendered", anonymousIssue.getText().contains("Open"));
    }

    private ViewPage setupTestData()
    {
        rpc.enableAnonymousAccess();
        Space space = new Space("tst", "tst");
        rpc.grantAnonymousPermission(SpacePermission.VIEW, space);

        // anonymous doesn't have permission on TP-1
        openJiraIssuesDialog().inputJqlSearch("TP-1").clickSearchButton().clickInsertDialog();
        openJiraIssuesDialog().inputJqlSearch("status=open").clickSearchButton().clickInsertDialog();
        EditContentPage editContentPage = openJiraIssuesDialog().inputJqlSearch("TST-1").clickSearchButton().clickInsertDialog();
        ViewPage viewPage = editContentPage.setTitle("testAnonymousPermission " + System.currentTimeMillis()).save();
        long pageId = viewPage.getPageId();

        product.logOut();
        return product.viewPage(String.valueOf(pageId));
    }
}
