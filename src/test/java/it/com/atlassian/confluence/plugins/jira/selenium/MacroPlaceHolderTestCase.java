package it.com.atlassian.confluence.plugins.jira.selenium;

import com.thoughtworks.selenium.Wait;

public class MacroPlaceHolderTestCase extends AbstractJiraDialogTestCase
{
    
    /**
     * Get the image source attribute (src) of the macro place holder
     * based on the XPath expression (ex: "xpath=//img[@class='editor-inline-macro' and @data-macro-name='jira']")
     * @param xpathExp XPath string expression (start with: xpath=)
     * @return image source attribute value
     */
    private String getImageSourceOfMacroElement(final String xpathExp)
    {
        client.selectFrame("wysiwygTextarea_ifr");
        Wait wait = new Wait("Checking Jira link") {
            public boolean until()
            {
                return client.isElementPresent(xpathExp);
            }
        };
        wait.wait("Couldn't find new Jira link", 5000);
        assertThat.elementVisible(xpathExp);
        return client.getAttribute(xpathExp + "/@src");
    }

    public void testPlaceHolderWhenMacroContainsOneIssue() throws Exception {
        login();
        client.open("pages/createpage.action?spaceKey=" + TEST_SPACE_KEY);
        JiraConnectorDialog dialog = JiraConnectorDialog.openDialog(client);
        dialog.performSearch("TST-1").clickInsert();
        String imgSrcAttValueOfMacro = getImageSourceOfMacroElement("xpath=//img[@class='editor-inline-macro' and @data-macro-name='jira']");
        assertTrue(imgSrcAttValueOfMacro.contains("/plugins/servlet/confluence/placeholder/macro"));
        client.selectFrame("relative=top");
    }

    public void testPlaceHolderWhenMacroContainsMultiIssues() throws Exception {
        login();
        client.open("pages/createpage.action?spaceKey=" + TEST_SPACE_KEY);
        JiraConnectorDialog dialog = JiraConnectorDialog.openDialog(client);
        dialog.performSearch("key in (TSTT-1, TST-1)").clickInsert();
        String imgSrcAttValueOfMacro = getImageSourceOfMacroElement("xpath=//img[@class='editor-inline-macro' and @data-macro-name='jira']");
        assertTrue(imgSrcAttValueOfMacro.contains("/confluence/download/resources/confluence.extra.jira/jira-table.png"));
        client.selectFrame("relative=top");
    }

    public void testPlaceHolderWhenMacroContainsJQL() throws Exception {
        login();
        client.open("pages/createpage.action?spaceKey=" + TEST_SPACE_KEY);
        JiraConnectorDialog dialog = JiraConnectorDialog.openDialog(client);
        dialog.performSearch("project = 'Alphanumeric Key Test'").clickInsert();
        String imgSrcAttValueOfMacro = getImageSourceOfMacroElement("xpath=//img[@class='editor-inline-macro' and @data-macro-name='jira']");
        assertTrue(imgSrcAttValueOfMacro.contains("/confluence/download/resources/confluence.extra.jira/jira-table.png"));
        client.selectFrame("relative=top");
    }

    public void testPlaceHolderCountWhenMacroContainsMultiIssues()
    {
        login();
        client.open("pages/createpage.action?spaceKey=" + TEST_SPACE_KEY);
        JiraConnectorDialog dialog = JiraConnectorDialog.openDialog(client);
        dialog.performSearch("key in (TSTT-1, TST-1)");
        dialog.checkTotalIssueCount();
        dialog.clickInsert();
        client.selectFrame("wysiwygTextarea_ifr");
        Wait wait = new Wait("Checking Jira link")
        {
            public boolean until()
            {
                return client.isElementPresent("xpath=//img[@class='editor-inline-macro' and @data-macro-name='jira']");
            }
        };
        wait.wait("Couldn't find new Jira link", 5000);
        assertThat.elementVisible("xpath=//img[@class='editor-inline-macro' and @data-macro-name='jira']");
        String attributeValue = client.getAttribute("xpath=//img[@class='editor-inline-macro' and @data-macro-name='jira']/@src");
        assertTrue(attributeValue.contains("/confluence/plugins/servlet/count-image-generator?totalIssues=2"));
        client.selectFrame("relative=top");
    }
}
