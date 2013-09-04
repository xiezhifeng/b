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
        dialog.performSearch("project = tstt");
        
        client.waitForCondition("window.AJS.Editor.JiraConnector.servers " +
        		"&& window.AJS.Editor.JiraConnector.servers[0] " +
        		"&& window.AJS.Editor.JiraConnector.servers[0].columns ", 5000);
        
        client.click("css=a.jql-display-opts-open");
        // try "Date Customfield" custom column
        client.typeWithFullKeyEvents("css=.select2-input", "Date Cus");
        // click on custom field
        client.mouseDown("//li[contains(@class,'select2-result-selectable')]");
        client.mouseUp("//li[contains(@class,'select2-result-selectable')]");
        
        dialog.clickInsert();
        
        validateParamInLinkMacro("columns=key,summary");
        client.clickAndWaitForAjaxWithJquery("css=#rte-button-preview");
        assertThat.elementPresentByTimeout("css=.wiki-content table", 10000);
        assertThat.elementContainsText("css=.wiki-content table", "Ouvertes"); // "Ouvertes" = "Open" in French 

        // custom field column present
        assertThat.elementContainsText("css=.wiki-content table", "date customfield"); 

        // custom field column has value
        assertThat.elementContainsText("css=.wiki-content table", "25 déc. 2009"); 
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
        jiraWebTester.gotoPage("/"); // buy confluence some seconds ?
    }
}
