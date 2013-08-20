package it.com.atlassian.confluence.plugins.jira.selenium;


/**
 * Test if default language changing in JIRA affects JIM or not
 */
public class ChangeJiraDefaultLanguageTestCase extends AbstractJiraPanelTestCase
{

    @Override
    public void setUp() throws Exception
    {
        super.setUp();
        changeJiraDefaultLanguage();
    }

    @Override
    public void tearDown() throws Exception
    {
        super.tearDown();
        restoreJiraDefaultLanguage();
    }
    
    public void testChangeJiraDefaultLanguage() 
    {
        openJiraDialog();
        JiraConnectorDialog dialog = JiraConnectorDialog.openDialog(client);
        dialog.performSearch("status  =  open").clickInsert();
        validateParamInLinkMacro("columns=key,summary");
        client.clickAndWaitForAjaxWithJquery("css=#rte-button-preview");
        assertThat.elementPresentByTimeout("css=.wiki-content table", 5000);
        assertThat.elementContainsText("css=.wiki-content table", "Ouvertes"); // "Ouvertes" = "Open" in French 
    }

    private void changeJiraDefaultLanguage()
    {
        setJiraLanguage("fr_FR");
    }
    
    private void restoreJiraDefaultLanguage()
    {
        setJiraLanguage("-1");
    }
    
    private void setJiraLanguage(String locale) 
    {
        jiraWebTester.gotoPage("/secure/admin/EditApplicationProperties!default.jspa");
        jiraWebTester.setWorkingForm("jiraform");
        jiraWebTester.selectOptionByValue("defaultLocale", locale);
        jiraWebTester.submit();
    }
}
