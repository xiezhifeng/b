package it.com.atlassian.confluence.plugins.webdriver.jiraissues.createpanel;

import com.atlassian.confluence.plugins.webdriver.page.JiraCreatedMacroDialog;

import org.junit.Test;

import it.com.atlassian.confluence.plugins.webdriver.AbstractJiraIssueMacroTest;

import static org.junit.Assert.assertTrue;


public class JiraCreatedMacroWebDriverTest extends AbstractJiraIssueMacroTest
{
    @Test
    public void testComponentsVisible()
    {
        JiraCreatedMacroDialog jiraCreatedMacroDialog = openJiraCreatedMacroDialog(true);
        jiraCreatedMacroDialog.selectProject("Jira integration plugin");
        assertTrue(jiraCreatedMacroDialog.getComponents().isVisible());
    }
}
