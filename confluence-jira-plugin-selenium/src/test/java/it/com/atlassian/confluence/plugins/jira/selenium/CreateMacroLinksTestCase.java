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
        String numberCount = client.getText("css=.wiki-content .static-jira-issues_count");
        assertTrue(numberCount.contains("2 issues"));
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
    }

    /**
     * test search with no result and allow insert
     */
    public void testSearchNoResult()
    {
        openJiraDialog();

        String searchNoResult="InvalidDesc";
        client.click("//li/button[text()='Search']");
        client.type("css=input[name='jiraSearch']", searchNoResult);
        client.clickAndWaitForAjaxWithJquery("css=div.jira-search-form button");

        //get value result in dialog 
        String resultNoResult = client.getText("css=#my-jira-search .data-table .aui-message");
        assertTrue(resultNoResult.contains("No search results found."));
    }

    /**
     * test disable option with select 1 issue from multi result issue
     */
    public void testDisabledOptionWithMultipleIssues()
    {
        openJiraDialog();

        String searchStr="key in (TP-1, TP-2)";
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
        client.click("css=.jql-display-opts-open");

        // check disabled option
        assertThat.attributeContainsValue("css=#opt-total", "disabled", "true");
        //assertThat.attributeContainsValue("css=.select2-container-multi", "class", "select2-container-disabled");
        // check macro param with selected key

        client.click("css=button.insert-issue-button");
        validateParamInLinkMacro("key=TP-1");
    }

    private void searchAndInsertLinkMacroWithParam(String paramName, String searchStr, String... expected)
    {
        client.click("//li/button[text()='Search']");

        client.type("css=input[name='jiraSearch']", searchStr);

        client.clickAndWaitForAjaxWithJquery("css=div.jira-search-form button");

//        for(int i = 0; i < expected.length; i++ ) {
//            assertEquals(expected[i], client.getTable("css=#my-jira-search table.my-result." + (i + 1) + ".1"));
//        }

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
            if (client.isElementPresent("css=a.jql-display-opts-open")) {
                client.click("css=a.jql-display-opts-open");
            }
            while (client.isElementPresent("css=a.select2-search-choice-close")) {
                client.click("css=.select2-input");
                client.keyPress("css=.select2-input", "\\8");
                client.keyPress("css=.select2-input", "\\8");
            }
            String columnsArray[] = columns.split(",");
            for (int i = 0; i < columnsArray.length; i++) {
                if(!"".equals(columnsArray[i])) {
                    client.typeWithFullKeyEvents("css=.select2-input", columnsArray[i].trim());
                    client.mouseDown("//li[contains(@class,'select2-result-selectable')]");
                    client.mouseUp("//li[contains(@class,'select2-result-selectable')]");
                }
            }
        }

        client.clickAndWaitForAjaxWithJquery("css=button.insert-issue-button", 3000);
    }

}
