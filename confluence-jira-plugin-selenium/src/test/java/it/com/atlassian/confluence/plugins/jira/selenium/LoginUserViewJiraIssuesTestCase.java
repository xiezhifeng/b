package it.com.atlassian.confluence.plugins.jira.selenium;

import com.thoughtworks.selenium.Wait;

public class LoginUserViewJiraIssuesTestCase extends AbstractJiraPanelTestCase
{
    private static final String JIRA_URL = System.getProperty("baseurl.jira", "http://localhost:11990/jira");

    public void testUserViewIssueWhenNotHavePermission() throws InterruptedException
    {
        setupTestData("UserCanNotView", false);
        assertThat.elementPresent("//span[@class = 'jira-issue TP-10']");
        assertThat.elementDoesNotContainText("//span[@class = 'jira-issue TP-10']", "Open");
        assertThat.elementDoesNotContainText("//span[@class = 'jira-issue TP-10']", "Test bug");
    }

    public void testUserViewIssueWhenNotMapping() throws Exception
    {
        setupTestData("UserNotMapping", false);
        doWebSudo(httpClient);
        removeApplink(httpClient, getAuthQueryString());
        String serverId = addJiraAppLink("Applink Test", JIRA_URL, JIRA_URL, true);
        enableOauthWithApplink(serverId);

        client.open("display/" + TEST_SPACE_KEY + "/UserNotMapping");
        client.waitForPageToLoad(5000);

        assertThat.elementPresent("//span[@class = 'jira-issue lock-jira-issue TP-10']");
        assertThat.elementContainsText("//span[@class = 'jira-issue lock-jira-issue TP-10']", "Authenticate");
    }

    private void setupTestData(String pageName, boolean searchMode) throws InterruptedException
    {
        client.type("//input[@id='content-title']", pageName);

        if(searchMode)
        {
            JiraConnectorDialog dialog = JiraConnectorDialog.openDialog(client);
            dialog.performSearch("TP-1").clickInsert();
        }
        else
        {
            client.selectFrame("wysiwygTextarea_ifr");
            assertThat.elementPresentByTimeout("css=#tinymce", SeleniumTestConstants.PAGE_LOAD_WAIT);
            client.typeWithFullKeyEvents("css=#tinymce", "{jira:key=TP-10|cache=off}");
        }

        Wait wait = new Wait("Checking Jira link") {
            public boolean until()
            {
                return client.isElementPresent("//img[@class='editor-inline-macro']");
            }
        };
        wait.wait("Couldn't find new Jira link", SeleniumTestConstants.ACTION_WAIT);

        client.selectFrame("relative=top");
        client.click("//button[@value= 'Save']");
        client.waitForPageToLoad(SeleniumTestConstants.PAGE_LOAD_WAIT);
    }
}
