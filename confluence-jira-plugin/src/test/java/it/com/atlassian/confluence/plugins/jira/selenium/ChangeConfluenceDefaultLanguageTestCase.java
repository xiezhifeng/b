package it.com.atlassian.confluence.plugins.jira.selenium;


/**
 * Test for cases when user switches from default Confluence language to another language
 */
public class ChangeConfluenceDefaultLanguageTestCase extends AbstractJiraPanelTestCaseInGerman
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
    }
    
    /**
     * test for CONF-30496
     */
    public void testChangeDefaultConfLanguage() {
        JiraConnectorDialog dialog = JiraConnectorDialog.openDialog(client);
        dialog.setSearchButton("Suche").performSearch("status != closed").clickInsert();
        client.clickAndWaitForAjaxWithJquery("css=#rte-button-preview");
        assertThat.elementPresentByTimeout("css=.wiki-content table", 10000);
        assertThat.elementContainsText("css=.wiki-content table", "blah"); 
        assertThat.elementContainsText("css=.wiki-content table", "Jan 09, 2012"); 
        assertThat.elementContainsText("css=.wiki-content table", "admin"); 
        assertThat.elementContainsText("css=.wiki-content table", "Open"); 
        assertThat.elementContainsText("css=.wiki-content table", "Unresolved"); 
    }
    
}
