package it.com.atlassian.confluence.plugins.jira.selenium;

public class CreateIssueTestCase extends AbstractJiraPanelTestCase
{

//    {JRA-9|server=testjira}
    public void testProjectsLoaded()
    {

        openJiraDialog();
        client.click("//button[text()='Create New Issue']");
        client.waitForAjaxWithJquery();
        client.setSpeed("1000");

        assertThat.elementPresent("//option[text()='Test Project']");
        assertThat.elementPresent("//option[text()='Test Project 1']");
        assertThat.elementPresent("//option[text()='Test Project 2']");
        assertThat.elementPresent("//option[text()='Special Project 1']");

        client.selectFrame("relative=top");
        client.select("css=select.project-select","index=4");

        assertThat.elementPresent("//option[@value='1']");
        assertThat.elementPresent("//option[@value='2']");
        assertThat.elementPresent("//option[@value='3']");
        assertThat.elementPresent("//option[@value='4']");
        assertThat.elementNotPresent("//option[@value='5']");
        
        // index 1 is the old index 2 because we removed the initial "select project" from the options after
        // something is selected

        client.selectFrame("relative=top");
        client.select("css=select.project-select","index=4");
        client.setSpeed("0");
        assertThat.elementPresentByTimeout("//option[text()='Trivial Task']");

    }

    public void testComponents()
    {
        openJiraDialog();
        client.click("//button[text()='Create New Issue']");
        client.waitForAjaxWithJquery();

        client.selectFrame("relative=top");
        client.select("css=select.project-select","index=2");

        client.waitForAjaxWithJquery();
        final String componentId = "id=components";
        assertThat.elementPresentByTimeout(componentId);
        client.select(componentId,"index=0");
        assertThat.elementContainsText(componentId, "test-component");
    }

    public void testCreateIssue()
    {
        openJiraDialog();
        client.click("//button[text()='Create New Issue']");
        client.waitForAjaxWithJquery();

        client.selectFrame("relative=top");
        client.select("css=select.project-select","index=4");

        client.waitForAjaxWithJquery();

        // Try to type spaces
        client.typeKeys("css=input.issue-summary", "     ");
        client.click("css=button.insert-issue-button");
        assertThat.elementContainsText("css=div.jira-error", "Required fields: Summary");

        // Type correct value
        client.typeKeys("css=input.issue-summary", "blah");
        client.typeKeys("css=input[name='reporter']", "admin");
        assertThat.elementPresent("css=button.insert-issue-button:enabled");
        client.click("css=button.insert-issue-button");

        client.waitForAjaxWithJquery();

        // dialog should close
        assertThat.elementNotVisible("css=div.aui-dialog");

        //look for JIRA issue in RTE
        client.selectFrame("wysiwygTextarea_ifr");
        assertThat.elementPresentByTimeout("//img[@class='editor-inline-macro' and @data-macro-name='jira']");

        client.selectFrame("relative=top");
    }

    public void testIssueTypeIsSubTaskNotExist()
    {
        openJiraDialog();
        client.click("//button[text()='Create New Issue']");
        client.waitForAjaxWithJquery();
        client.setSpeed("1000");

        client.selectFrame("relative=top");
        client.select("css=select.project-select","index=2");
        assertThat.elementNotPresentByTimeout("//option[text()='Technical task']");
    }
}
