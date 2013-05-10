package it.com.atlassian.confluence.plugins.jira.selenium;

import com.thoughtworks.selenium.Wait;

public class VerifyOldPluginTestCase extends AbstractJiraPanelTestCase {
    
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

    public void testConvertJiraIssueToJiraWithKey() {
        String jiraIssuesMacro = "{jiraissues:TP-1}";
        convertJiraIssuesToJiraMacro(jiraIssuesMacro, "TP-1");
        validateParamInLinkMacro("key=TP-1");
    }

    public void testConvertJiraIssueToJiraWithXML() {
        String jiraIssuesMacro = "{jiraissues:http://localhost:11990/jira/sr/jira.issueviews:searchrequest-xml/temp/SearchRequest.xml?jqlQuery=project+%3D+TP}";
        convertJiraIssuesToJiraMacro(jiraIssuesMacro, "project = TP");
        validateParamInLinkMacro("jqlQuery=project \\= TP");
    }

    public void testConvertJiraIssueToJiraWithColumns() {
        String jiraIssuesMacro = "{jiraissues:status=open|columns=key,summary,type}";
        convertJiraIssuesToJiraMacro(jiraIssuesMacro, "status = open");
        validateParamInLinkMacro("columns=key,summary,type");
    }

    public void testConvertJiraIssueToJiraWithCount() {
        String jiraIssuesMacro = "{jiraissues:status=open|count=true}";
        convertJiraIssuesToJiraMacro(jiraIssuesMacro, "status = open");
        validateParamInLinkMacro("count=true");
    }
    
    public void testVerifyJiraIssuesWithRenderDynamic() {
        //add title for page
        client.click("css=#content-title");
        final String contentId = client.getEval("window.AJS.Confluence.Editor.getContentId()");
        client.type("css=#content-title", "Test Jira issue " + contentId);

        //select frame RTE
        client.selectFrame("wysiwygTextarea_ifr");
        client.typeKeys("css=#tinymce", "{jiraissues:status=open|width=400|renderMode=dynamic}");
        validateParamInLinkJiraIssuesMacro("renderMode=dynamic");
        client.selectFrame("relative=top");

        waitForCheckElement("css=#rte-button-publish");
       // Save page in default location
        client.click("css=#rte-button-publish");
        waitForCheckElement("//div[@class='jiraissues_table']");
        
        Number num = client.getElementWidth("//div[@class='jiraissues_table']");
        assertEquals(400, num);
        client.clickAndWaitForAjaxWithJquery("css=#editPageLink", 5000);
    }

    private void convertJiraIssuesToJiraMacro(String jiraIssuesMacro, String inputField) {
        client.selectFrame("wysiwygTextarea_ifr");
        client.typeKeys("css=#tinymce", jiraIssuesMacro);
        waitForCheckElement("css=img.editor-inline-macro");
        
        //click to edit open dialog jira macro
        client.doubleClick("css=img.editor-inline-macro");
        client.selectFrame("relative=top");
        assertThat.textPresentByTimeout("Insert JIRA Issue", 5000);
        client.click("//li/button[text()='Search']");
        assertEquals(inputField, client.getValue("css=input[name='jiraSearch']"));
        client.clickAndWaitForAjaxWithJquery("css=button.insert-issue-button", 5000);
    }

    private void waitForCheckElement(final String locator) {
        Wait wait = new Wait("Wait For check Element") {
            public boolean until() {
                return client.isElementPresent(locator);
            }
        };
        wait.wait("Couldn't Check See Element", 5000);
    }

    private void validateParamInLinkJiraIssuesMacro(String paramMarco) {
        String parameters = getJiraIssuesMacroParameters();
        assertTrue(parameters.contains(paramMarco));
    }

    private String getJiraIssuesMacroParameters() {
        // look macro link in RTE
        assertThat.elementPresentByTimeout("//img[@class='editor-inline-macro' and @data-macro-name='jiraissues']");
        String attributeValue = client.getAttribute("xpath=//img[@class='editor-inline-macro' and @data-macro-name='jiraissues']/@data-macro-parameters");
        return attributeValue;
    }
}
