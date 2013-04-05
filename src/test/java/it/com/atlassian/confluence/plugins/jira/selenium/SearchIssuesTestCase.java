package it.com.atlassian.confluence.plugins.jira.selenium;

public class SearchIssuesTestCase extends AbstractJiraPanelTestCase
{
    public void testSearchWithButton()
    {
        openJiraDialog();
        client.click("//li/button[text()='Search']");

        client.typeKeys("css=input[name='jiraSearch']", "test");

        client.click("css=div.jira-search-form button");

        client.waitForAjaxWithJquery(3000);
        assertEquals("TSTT-1", client.getTable("css=#my-jira-search table.my-result.1.0"));
        assertEquals("TST-1", client.getTable("css=#my-jira-search table.my-result.2.0"));
    }

    public void testSearchWithEnter()
    {
        openJiraDialog();
        client.click("//li/button[text()='Search']");

        client.typeKeys("css=input[name='jiraSearch']", "test");

        client.keyDown("css=input[name='jiraSearch']", "\\13");
        client.keyUp("css=input[name='jiraSearch']", "\\13");

        client.waitForAjaxWithJquery(3000);
        assertEquals("TSTT-1", client.getTable("css=#my-jira-search table.my-result.1.0"));
        assertEquals("TST-1", client.getTable("css=#my-jira-search table.my-result.2.0"));
    }

    public void testSearchWithJQL()
    {
        openJiraDialog();
        client.click("//li/button[text()='Search']");

        client.typeKeys("css=input[name='jiraSearch']", "project=TP");

        client.click("css=div.jira-search-form button");

        client.waitForAjaxWithJquery();
        assertEquals("TP-2", client.getTable("css=#my-jira-search table.my-result.1.0"));
        assertEquals("TP-1", client.getTable("css=#my-jira-search table.my-result.2.0"));
    }

    public void testSearchForAlphanumericIssueKey()
    {
        JiraConnectorDialog dialog = JiraConnectorDialog.openDialog(client);
        String key = dialog.performSearch("T2T-1", 1);
        assertEquals("T2T-1", key);
    }
}
