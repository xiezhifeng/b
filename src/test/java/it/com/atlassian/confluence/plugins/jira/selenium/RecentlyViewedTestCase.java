package it.com.atlassian.confluence.plugins.jira.selenium;

import org.openqa.selenium.Keys;

import com.thoughtworks.selenium.Wait;
public class RecentlyViewedTestCase extends AbstractJiraPanelTestCase
{
    
    public void testRecentlyViewedIssuesAppear()
    {
        jiraWebTester.gotoPage("/browse/TP-1");
        jiraWebTester.gotoPage("/browse/TP-2");
        jiraWebTester.gotoPage("/browse/TST-1");
        
        openJiraDialog();
        client.click("//li/button[text()='Recently Viewed']");
        client.waitForAjaxWithJquery();
        assertThat.elementVisible("//td/span[text() = 'TST-1']");
        assertThat.elementVisible("//td/span[text() = 'TP-2']");
        assertThat.elementVisible("//td/span[text() = 'TP-1']");
        // verify there are only two rows of results.
        String s = client.getEval("window.jQuery('table.my-result tbody tr').not('tr.data-table-header').length;");
        //assertEquals("3", s);
    }
    
    public void testUpDownEnterKeyboardInTable()
    {
        jiraWebTester.gotoPage("/browse/TP-1");
        jiraWebTester.gotoPage("/browse/TP-2");
        jiraWebTester.gotoPage("/browse/TST-1");
    	
        openJiraDialog();
        
        client.click("//li/button[text()='Recently Viewed']");
        client.waitForAjaxWithJquery();
        
        String top = client.getTable("css=table.my-result.1.0");
        String middle = client.getTable("css=table.my-result.3.0");
        
        assertThat.elementPresent("css=table.my-result tr.selected");
        assertThat.elementContainsText("css=table.my-result tr.selected", top);
        
        //for mozilla browsers
        
        client.keyPress("css=table.my-result tr.selected", "\\40");
        client.keyPress("css=table.my-result tr.selected", "\\40");
        
        // for non-mozilla browsers
        client.keyDown("css=table.my-result tr.selected", "\\40");
        client.keyDown("css=table.my-result tr.selected", "\\40");
        
        assertThat.elementContainsText("css=table.my-result tr.selected", middle);
        
        //for mozilla browsers
        client.keyPress("css=table.my-result tr.selected", "\\38");
        client.keyPress("css=table.my-result tr.selected", "\\38");
        
        // for non-mozilla browsers: Actually, this piece of code cannot run on Chrome
        client.keyDown("css=table.my-result tr.selected", "\\38");
        client.keyDown("css=table.my-result tr.selected", "\\38");
        
        assertThat.elementContainsText("css=table.my-result tr.selected", top);
        
        client.keyPress("css=table.my-result tr.selected", "\\13");
        client.keyDown("css=table.my-result tr.selected", "\\13");
        
        
        assertThat.elementNotVisible("css=div.aui-dialog");
        client.selectFrame("wysiwygTextarea_ifr");
        //here, we need to wait - because the test run quickly will be failed IF we
        //check before the DOM rendering.
        Wait wait = new Wait("Checking Jira link") {
		    public boolean until() {
		        return client.isElementPresent("xpath=//img[@class='editor-inline-macro' and @data-macro-name='jira']");
		    }
		};
		wait.wait("Couldn't find new Jira link", 5000);
        assertThat.elementPresentByTimeout("//img[@class='editor-inline-macro' and @data-macro-name='jira']");
        
        client.selectFrame("relative=top");
    }
           
    
    
 
    
}
