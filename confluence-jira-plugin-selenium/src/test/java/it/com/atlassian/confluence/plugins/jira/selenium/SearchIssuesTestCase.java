package it.com.atlassian.confluence.plugins.jira.selenium;

public class SearchIssuesTestCase extends AbstractJiraPanelTestCase
{
    public void testSearchWithButton()
    {
        openJiraDialog();
        client.click("//li/button[text()='Search']");

        client.typeKeys("css=input[name='jiraSearch']", "test");

        client.click("css=div.jira-search-form button");

        client.waitForAjaxWithJquery(SeleniumTestConstants.ACTION_WAIT);
        assertEquals("TSTT-1", client.getText("xpath=//div[@id='my-jira-search']/div[2]/table/tbody/tr[2]/td[2]"));
        assertEquals("TST-1", client.getText("xpath=//div[@id='my-jira-search']/div[2]/table/tbody/tr[3]/td[2]"));
    }

    public void testSearchWithEnter()
    {
        openJiraDialog();
        client.click("//li/button[text()='Search']");

        client.typeKeys("css=input[name='jiraSearch']", "test");

        client.keyDown("css=input[name='jiraSearch']", "\\13");
        client.keyUp("css=input[name='jiraSearch']", "\\13");

        client.waitForAjaxWithJquery(SeleniumTestConstants.ACTION_WAIT);
        assertEquals("TSTT-1", client.getText("xpath=//div[@id='my-jira-search']/div[2]/table/tbody/tr[2]/td[2]"));
        assertEquals("TST-1", client.getText("xpath=//div[@id='my-jira-search']/div[2]/table/tbody/tr[3]/td[2]"));
    }

    public void testSearchWithJQL()
    {
        openJiraDialog();
        client.click("//li/button[text()='Search']");

        client.typeKeys("css=input[name='jiraSearch']", "project=TP");

        client.click("css=div.jira-search-form button");

        client.waitForAjaxWithJquery(SeleniumTestConstants.ACTION_WAIT);
        assertEquals("TP-2", client.getText("xpath=//div[@id='my-jira-search']/div[2]/table/tbody/tr[2]/td[2]"));
        assertEquals("TP-1", client.getText("xpath=//div[@id='my-jira-search']/div[2]/table/tbody/tr[3]/td[2]"));
    }

    public void testSearchForAlphanumericIssueKey()
    {
        final JiraConnectorDialog dialog = JiraConnectorDialog.openDialog(client);
        final String key = dialog.performSearch("T2T-1", 1);
        assertEquals("T2T-1", key);
    }

    public void testSearchWithFilterHaveJQL()
    {
        openJiraDialog();
        client.click("//li/button[text()='Search']");

        client.type("css=input[name='jiraSearch']", JIRA_DISPLAY_URL + "/issues/?filter=10000");

        client.click("css=div.jira-search-form button");

        client.waitForAjaxWithJquery(SeleniumTestConstants.ACTION_WAIT);
        assertThat.elementPresent("//table[@class='my-result aui data-table']");
        assertThat.elementContainsText("//table[@class='my-result aui data-table']", "TSTT-5");
        assertThat.elementContainsText("//table[@class='my-result aui data-table']", "TSTT-4");
    }

    public void testSearchWithFilterEmptyJQL()
    {
        openJiraDialog();
        client.click("//li/button[text()='Search']");

        client.type("css=input[name='jiraSearch']", JIRA_DISPLAY_URL + "/issues/?filter=10001");

        client.click("css=div.jira-search-form button");

        client.waitForAjaxWithJquery(SeleniumTestConstants.ACTION_WAIT);
        assertThat.elementPresent("//table[@class='my-result aui data-table']");
        assertThat.elementContainsText("//table[@class='my-result aui data-table']", "TSTT-5");
        assertThat.elementContainsText("//table[@class='my-result aui data-table']", "TSTT-4");
    }

    public void testSearchWithFilterNotExist()
    {
        openJiraDialog();
        client.click("//li/button[text()='Search']");

        client.type("css=input[name='jiraSearch']", JIRA_DISPLAY_URL + "/issues/?filter=10002");

        client.click("css=div.jira-search-form button");

        client.waitForAjaxWithJquery(SeleniumTestConstants.ACTION_WAIT);
        assertThat.elementPresent("//div[@class='aui-message warning']");
        assertThat.elementContainsText("//div[@class='aui-message warning']",
                "The JIRA server didn't understand your search query.");
    }
}
