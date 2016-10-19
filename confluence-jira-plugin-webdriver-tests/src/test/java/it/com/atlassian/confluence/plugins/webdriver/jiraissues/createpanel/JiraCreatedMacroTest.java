package it.com.atlassian.confluence.plugins.webdriver.jiraissues.createpanel;

import com.atlassian.pageobjects.elements.query.Poller;
import org.hamcrest.Matchers;
import org.junit.Ignore;
import org.junit.Test;

public class JiraCreatedMacroTest extends AbstractJiraCreatedPanelTest
{

    @Ignore("CONFDEV-38148 fails intermittently in JiraMacroCreatePanelDialog.selectIssueType when the expected project and issue type are already selected")
    @Test
    public void testDisplayUnsupportedFieldsMessage() throws Exception
    {
        jiraMacroCreatePanelDialog.selectProject("Special Project 1");
        jiraMacroCreatePanelDialog.selectIssueType("Bug");

        // Check display unsupported fields message
        String unsupportedMessage = "The required field Flagged is not available in this form.";
        Poller.waitUntil(jiraMacroCreatePanelDialog.getJiraErrorMessages(), Matchers.containsString(unsupportedMessage));

        Poller.waitUntilFalse("Insert button is disabled when there are unsupported fields",
                jiraMacroCreatePanelDialog.isInsertButtonEnabled());

        jiraMacroCreatePanelDialog.getSummaryElement().type("Test input summary");
        Poller.waitUntilFalse("Insert button is still disabled when input summary",
                jiraMacroCreatePanelDialog.isInsertButtonEnabled());

        // Select a project which has not un supported field then Insert Button must be enabled.
        jiraMacroCreatePanelDialog.selectProject(PROJECT_TSTT);
        Poller.waitUntilTrue("Insert button is enable when switch back to a project which hasn't unsupported fields",
                jiraMacroCreatePanelDialog.isInsertButtonEnabled());
    }
}
