package it.com.atlassian.confluence.plugins.jira.selenium;

public class AbstractJiraPanelTestCase extends AbstractJiraDialogTestCase
{
    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        login();

        client.open("pages/createpage.action?spaceKey=" + TEST_SPACE_KEY);
    }

    protected void openJiraDialog()
    {
        assertThat.elementPresentByTimeout("jiralink", 10000);
        client.click("jiralink");
        assertThat.textPresentByTimeout("Insert JIRA Issue", 1000);
    }

}
