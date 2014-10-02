package it.com.atlassian.confluence.plugins.jira.selenium;

import com.atlassian.confluence.it.Space;
import com.atlassian.confluence.it.SpacePermission;
import com.thoughtworks.selenium.Wait;

public class AnonymousViewJiraIssuesTestCase extends AbstractJiraDialogTestCase
{

    public void testAnonymousCanNotViewIssue() throws InterruptedException
    {
        setupTestData("TP-1", "AnonymousDoNotPermissionView");

        assertThat.elementPresent("//span[@class = 'jira-issue TP-1']");
        assertThat.elementDoesNotContainText("//span[@class = 'jira-issue TP-1']", "Bug -1");
        assertThat.elementDoesNotContainText("//span[@class = 'jira-issue TP-1']", "Open");
    }

    /*public void testAnonymousCanViewSomeIssues()
    {
        setupTestData("status=open", "AnonymousViewTable");
        assertThat.elementPresentByTimeout("//table[@class = 'aui']", 10000);
        assertThat.elementDoesNotContainText("//table[@class = 'aui']", "TP-1");
    }

    public void testAnonymousCanViewIssue() throws InterruptedException
    {
        setupTestData("TST-1", "AnonymousCanView");

        assertThat.elementPresent("//span[@class = 'jira-issue']");
        assertThat.elementContainsText("//span[@class = 'jira-issue']", "Test bug");
        assertThat.elementContainsText("//span[@class = 'jira-issue']", "Open");
    }
*/
    private void setupTestData(String searchValue, String pageName)
    {
        login();
        rpc.enableAnonymousAccess();
        Space space = new Space("ds","ds");;
        rpc.grantAnonymousPermission(SpacePermission.VIEW, space );

        client.open("pages/createpage.action?spaceKey=ds");
        client.type("//input[@id='content-title']", pageName);
        JiraConnectorDialog dialog = JiraConnectorDialog.openDialog(client);
        dialog.performSearch(searchValue).clickInsert();

        client.selectFrame("wysiwygTextarea_ifr");
        Wait wait = new Wait("Checking Jira link") {
            public boolean until()
            {
                return client.isElementPresent("//img[@class='editor-inline-macro']");
            }
        };
        wait.wait("Couldn't find new Jira link", 5000);

        client.selectFrame("relative=top");
        client.click("//button[@id='rte-button-publish']");
        client.waitForPageToLoad();
        logout();
        client.open("display/ds/" + pageName);
    }
}