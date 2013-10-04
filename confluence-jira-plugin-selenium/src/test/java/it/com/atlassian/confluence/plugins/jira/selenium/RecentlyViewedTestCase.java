package it.com.atlassian.confluence.plugins.jira.selenium;

import org.apache.log4j.Logger;

public class RecentlyViewedTestCase extends AbstractJiraPanelTestCase
{

    private static final Logger LOG = Logger.getLogger(RecentlyViewedTestCase.class);
    
    public void testRecentlyViewedIssuesAppear()
    {
        LOG.debug("*****testRecentlyViewedIssuesAppear");
        jiraWebTester.gotoPage("/browse/TP-1");
        jiraWebTester.gotoPage("/browse/TP-2");
        jiraWebTester.gotoPage("/browse/TST-1");

        openJiraDialog();
        LOG.debug("*****finished openJiraDialog");
        client.click("//li/button[text()='Recently Viewed']");
        client.waitForAjaxWithJquery(SeleniumTestConstants.ACTION_WAIT);
        assertThat.elementPresent("//table[@class='my-result aui data-table']");
        assertThat.elementContainsText("//table[@class='my-result aui data-table']", "TST-1");
        assertThat.elementContainsText("//table[@class='my-result aui data-table']", "TP-2");
        assertThat.elementContainsText("//table[@class='my-result aui data-table']", "TP-1");
    }

    public void testUpDownEnterKeyboardInTable()
    {
        openJiraDialog();
        client.click("//li/button[text()='Recently Viewed']");
        client.waitForAjaxWithJquery();
        String top = client.getTable("css=table.my-result.1.0");
        String middle = client.getTable("css=table.my-result.3.0");

        assertThat.elementPresent("css=table.my-result tr.selected");
        assertThat.elementContainsText("css=table.my-result tr.selected", top);

        // for mozilla browsers
        client.keyPress("css=table.my-result tr.selected", "\\40");
        client.keyPress("css=table.my-result tr.selected", "\\40");

        // for non-mozilla browsers
        client.keyDown("css=table.my-result tr.selected", "\\40");
        client.keyDown("css=table.my-result tr.selected", "\\40");

        assertThat.elementContainsText("css=table.my-result tr.selected",
                middle);

        // for mozilla browsers
        client.keyPress("css=table.my-result tr.selected", "\\38");
        client.keyPress("css=table.my-result tr.selected", "\\38");

        // for non-mozilla browsers
        client.keyDown("css=table.my-result tr.selected", "\\38");
        client.keyDown("css=table.my-result tr.selected", "\\38");

        assertThat.elementContainsText("css=table.my-result tr.selected", top);

        client.keyPress("css=table.my-result tr.selected", "\\13");
        client.keyDown("css=table.my-result tr.selected", "\\13");

        assertThat.elementNotVisible("css=div.aui-dialog");
        client.selectFrame("wysiwygTextarea_ifr");
        assertThat.elementPresentByTimeout("//img[@class='editor-inline-macro' and @data-macro-name='jira']");

        client.selectFrame("relative=top");
    }

}
