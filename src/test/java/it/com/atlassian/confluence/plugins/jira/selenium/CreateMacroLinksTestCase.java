package it.com.atlassian.confluence.plugins.jira.selenium;


/**
 * This class contains tests for search issue, check option table/count, input value columns
 */
public class CreateMacroLinksTestCase extends AbstractJiraPanelTestCase
{
    
    private static final String COUNT_PARAM = "count";
    private static final String TABLE_PARAM = "table";
    private static final String COLUMNS_PARAM = "columns";
    
    static String searchStr = "project = TP";
	static String[] expected = new String[]{"TP-2", "TP-1"};
	static String columns = "key, summary";
	
    @Override
    public void setUp() throws Exception
    {
        super.setUp();
        client.waitForPageToLoad();
    }

    @Override
    public void tearDown() throws Exception
    {
        super.tearDown();
    }

    /**
     * create macro link and insert to RTE
     */
    public void testCreateLinkMacro() 
    {
    	openJiraDialog();
    	String paramName = "";
    	searchAndInsertLinkMacroWithParam(paramName, searchStr, expected);
    }
    
    /**
     * select option table in macro dialog and insert macro link in the Editor
     */
    public void testCreateLinkMacroWithParamTable() 
    {
    	openJiraDialog();
    	String paramName = TABLE_PARAM;
    	searchAndInsertLinkMacroWithParam(paramName, searchStr, expected);
    }
    
    /**
     * select option count in macro dialog and insert macro link in the Editor
     * Create page and check result count in view page
     */
    public void testCreateLinkMacroWithParamCount() 
    {
    	openJiraDialog();
    	String paramName = COUNT_PARAM;
    	searchAndInsertLinkMacroWithParam(paramName, searchStr, expected);
    	
    	//add title for page
    	client.click("css=#content-title");
        final String contentId = client.getEval("window.AJS.Confluence.Editor.getContentId()");
        client.type("css=#content-title", "Test " + contentId);

	   // Save page in default location
        client.clickAndWaitForAjaxWithJquery("css=#rte-button-publish");
        client.waitForPageToLoad();
        
        //check exist count in page view
        String numberCount = client.getText("css=#main-content .jiraissues_count");
        assertTrue(numberCount.contains("2 issues"));
        
        //click edit page
        client.clickAndWaitForAjaxWithJquery("css=#editPageLink");
        
    	validateParamInLinkMacro("count=true");
    }
    
    /**
     * input value to columns field in macro dialog and insert macro link in the Editor
     * Check param column of macro placeholder in Editor
     */
    public void testCreateLinkMacroWithParamColumns() 
    {
    	openJiraDialog();
    	String paramName = COLUMNS_PARAM;
    	searchAndInsertLinkMacroWithParam(paramName, searchStr, expected);
    	validateParamInLinkMacro("columns=key,summary");
    }
    
    /**
     * input value to columns field in macro dialog and insert macro link in the Editor, 
     * create page with this macro and Edit page check macro param columns
     */
    public void testCreatePageWithParamColumnMacro() 
    {
    	openJiraDialog();
    	
    	String paramName = COLUMNS_PARAM;
    	searchAndInsertLinkMacroWithParam(paramName, searchStr, expected);
    	    	
    	//add title for page
    	client.click("css=#content-title");
        final String contentId = client.getEval("window.AJS.Confluence.Editor.getContentId()");
        client.type("css=#content-title", "Test " + contentId);

        // Save page in default location
        client.clickAndWaitForAjaxWithJquery("css=#rte-button-publish");
        client.waitForPageToLoad();
        
        //click edit page
        client.clickAndWaitForAjaxWithJquery("css=#editPageLink");
        
    	validateParamInLinkMacro("columns=key,summary");
    }
    
    /**
     * test search with no result and allow insert
     */
    public void testSearchNoResult() 
    {
    	openJiraDialog();
    	
    	String searchNoResult="TP-10";
    	client.click("//li/button[text()='Search']");
        client.type("css=input[name='jiraSearch']", searchNoResult);
        client.clickAndWaitForAjaxWithJquery("css=div.jira-search-form button");
        
        //get value result in dialog 
        String resultNoResult = client.getText("css=#my-jira-search .data-table .aui-message");
		assertTrue(resultNoResult.contains("No search results found."));

		client.clickAndWaitForAjaxWithJquery("css=button.insert-issue-button", 3000);
        validateParamInLinkMacro("jqlQuery");
    }
    
    /**
     * Test display with 1 issue result
     */
    public void testDisableOption()
    {
    	openJiraDialog();
    	String searchStr="TP-1";
    	
    	client.click("//li/button[text()='Search']");
    	client.type("css=input[name='jiraSearch']", searchStr);
    	client.clickAndWaitForAjaxWithJquery("css=div.jira-search-form button");
    	
    	// check disable option
    	assertThat.attributeContainsValue("css=#opt-total", "disabled", "true");
    	assertThat.attributeContainsValue("css=#opt-table", "disabled", "true");
    	assertThat.attributeContainsValue("css=input[name='columns-display']", "disabled", "true");
    	
    	client.clickAndWaitForAjaxWithJquery("css=button.insert-issue-button", 3000);
    	validateParamInLinkMacro("key=TP-1");
    }
    
    /**
     * test disable option with select 1 issue from multi result issue
     */
    public void testDisabledOptionWithMultipleIssues() 
    {
    	openJiraDialog();
    	
    	String searchStr="TP-1, TP-2";
    	client.click("//li/button[text()='Search']");
        client.type("css=input[name='jiraSearch']", searchStr);        
        client.clickAndWaitForAjaxWithJquery("css=div.jira-search-form button");
        
        // uncheck 1 issue in table
        client.click("css=input[value='TP-1']");
        
        // check checkbox All need uncheck
        assertFalse(client.isChecked("css=input[name='jira-issue-all']"));
        
        // check & uncheck checkbox all
        client.click("css=input[value='TP-1']");
        client.click("css=input[name='jira-issue-all']");
        
        // check 1 issue
        client.click("css=input[value='TP-1']");
        
        // check disabled option
        assertThat.attributeContainsValue("css=#opt-total", "disabled", "true");
     	assertThat.attributeContainsValue("css=#opt-table", "disabled", "true");
     	assertThat.attributeContainsValue("css=input[name='columns-display']", "disabled", "true");
        
     	// check macro param with selected key     	
		client.clickAndWaitForAjaxWithJquery("css=button.insert-issue-button", 3000);
        validateParamInLinkMacro("key=TP-1");
    }
    
    /**
     * validate param in data-macro-parameters from the macro placeholder in the Editor
     * @param paramMarco
     */
    private void validateParamInLinkMacro(String paramMarco) 
    {
    	String parameters = getJiraMacroParameters();
    	assertTrue(parameters.contains(paramMarco));
    }
    
    private void searchAndInsertLinkMacroWithParam(String paramName, String searchStr, String... expected) 
    {
    	client.click("//li/button[text()='Search']");

        client.type("css=input[name='jiraSearch']", searchStr);

        client.clickAndWaitForAjaxWithJquery("css=div.jira-search-form button");
        
        for(int i = 0; i < expected.length; i++ ) {
            assertEquals(expected[i], client.getTable("css=#my-jira-search table.my-result." + (i + 1) + ".1"));
        }

        //click to open option
        client.click("css=#my-jira-search .jql-display-opts-open");
        
        if(paramName.equals(COUNT_PARAM)) {
        	client.check("insert-advanced", "insert-count");
        } 
        
        if(paramName.equals(TABLE_PARAM)) {
        	client.check("insert-advanced", "insert-table");
        }
        
        if(paramName.equals(COLUMNS_PARAM)) {
        	client.check("insert-advanced", "insert-table");
            client.type("css=input[name='columns-display']", columns);
        }
        
        client.clickAndWaitForAjaxWithJquery("css=button.insert-issue-button", 3000);
    }
    
    /**
     * 
     * @return the value of the data-macro-parameters attribute from the macro placeholder in the Editor. Only the first found macro
     * is used.
     */
    private String getJiraMacroParameters()
    {
    	//look macro link in RTE
        client.selectFrame("wysiwygTextarea_ifr");
        // debug
        assertThat.elementVisible("xpath=//img[@class='editor-inline-macro' and @data-macro-name='jira']");
        String attributeValue = client.getAttribute("xpath=//img[@class='editor-inline-macro' and @data-macro-name='jira']/@data-macro-parameters");
        client.selectFrame("relative=top");
        return attributeValue;
    }

}
