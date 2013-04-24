package com.atlassian.confluence.extra.jira;

import com.atlassian.confluence.macro.ImagePlaceholder;
import junit.framework.TestCase;

import java.util.HashMap;
import java.util.Map;

public class TestMacroPlaceHolder extends TestCase
{
    private static final String JIRA_TABLE_DISPLAY_PLACEHOLDER_IMG_PATH = "/download/resources/confluence.extra.jira/jira-table.png";

    private JiraIssuesMacro jiraIssuesMacro;

    public void testGetTableImagePlaceholder() {
        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("jqlQuery", "status=open");
        jiraIssuesMacro = new JiraIssuesMacro();
        ImagePlaceholder imagePlaceholder = jiraIssuesMacro.getImagePlaceholder(parameters, null);
        assertEquals(imagePlaceholder.getUrl(), JIRA_TABLE_DISPLAY_PLACEHOLDER_IMG_PATH);
    }

}
