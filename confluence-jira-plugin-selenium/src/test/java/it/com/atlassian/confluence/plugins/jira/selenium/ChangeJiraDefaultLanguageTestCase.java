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
    }

    @Override
    public void tearDown() throws Exception
    {
        super.tearDown();
        restoreJiraDefaultLanguage();
    }
    
    /**
     * Make sure when JIRA default language is changed, user can still create new JIM properly
     */
    public void testNewJimWhenChangeJiraDefaultLanguage() 
    {
        changeJiraDefaultLanguage();
        openJiraDialog();
        JiraConnectorDialog dialog = JiraConnectorDialog.openDialog(client);
        dialog.performSearch("project = tstt");
        
        client.waitForCondition("window.AJS.Editor.JiraConnector.servers " +
        		"&& window.AJS.Editor.JiraConnector.servers[0] " +
        		"&& window.AJS.Editor.JiraConnector.servers[0].columns ", 5000);
        
        client.setSpeed("500");
        client.click("css=a.jql-display-opts-open");
        client.setSpeed("100");
        // try "Date Customfield" custom column
        client.typeWithFullKeyEvents("css=.select2-input", "Date Cus");
        // click on custom field
        client.mouseDown("//li[contains(@class,'select2-result-selectable')]");
        client.mouseUp("//li[contains(@class,'select2-result-selectable')]");
        
        dialog.clickInsert();
        client.setSpeed("0");
        
        validateParamInLinkMacro("columns=key,summary");
        client.clickAndWaitForAjaxWithJquery("css=#rte-button-preview");
        assertThat.elementPresentByTimeout("css=.wiki-content table", 10000);
        assertThat.elementContainsText("css=.wiki-content table", "Ouvertes"); // "Ouvertes" = "Open" in French 

        // custom field column present
        assertThat.elementContainsText("css=.wiki-content table", "date customfield"); 

        // custom field column has value
        assertThat.elementContainsText("css=.wiki-content table", "Dec 25, 2009");
    }
    
    /**
     * test for CONF-28740
     */
    public void testVerifyExistingJimWhenChangeDefaultLanguage() 
    {
        // create a macro
        String pageTitle = "testVerifyExistingJimWhenChangeDefaultLanguage" + System.currentTimeMillis();
        createPageWithJiraMacro("{jira:status=open|cache=off|columns=key,type,created,updated,due,status,fixversions,date customfield}", pageTitle);
        
        // change language
        changeJiraDefaultLanguage();
        
        // verify macro in view mode
        client.open(rpc.getBaseUrl() + "/display/ds/" + pageTitle);
        
        assertThat.elementPresentByTimeout("css=.wiki-content table", 10000);
        assertThat.elementContainsText("css=.wiki-content table", "Ouvertes"); 
        assertThat.elementContainsText("css=.wiki-content table", "date customfield"); 
        assertThat.elementContainsText("css=.wiki-content table", "Dec 25");
        assertThat.elementPresent("css=.wiki-content table img");
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
        jiraWebTester.setTextField("baseURL", jiraBaseUrl);
        jiraWebTester.submit();
        //jiraWebTester.assertTextPresent("générale");
    }
    
}
