package com.atlassian.confluence.extra.jira;

import java.util.HashMap;
import java.util.Map;

import com.atlassian.confluence.xhtml.api.MacroDefinition;

import org.junit.Ignore;

import junit.framework.TestCase;

public class TestJiraIssuesMacroMigrator extends TestCase
{
    JiraIssuesMacroMigrator migrator;
    MacroDefinition macro;

    public void setUp()
    {
        migrator = new JiraIssuesMacroMigrator();
        macro = new MacroDefinition();
        macro.setName("jiraissues");
    }

    public void testUrlDefaultParameter()
    {
        // verify that it will move a url default parameter to an explicit url parameter to avoid the
        // JQL '=' character being interpreted incorrectly by the default migrator.
        String urlKey = "http://jira.atlassian.com/sr/jira.issueviews:searchrequest-xml/temp/SearchRequest.xml?jqlQuery";
        String urlVal = "project+%3D+JRA+AND+issuetype+in+%28Bug%2C+Improvement%2C+%22New+Feature%22%29+AND+resolution+%3D+Fixed+AND+fixVersion+%3D+%224.4.1%22+AND+status+in+%28Resolved%2C+Closed%29+ORDER+BY+priority+DESC%2C+key+DESC%2C+issuetype+ASC&tempMax=200";
        HashMap<String, String> params = new HashMap<String, String>();
        params.put(urlKey, urlVal);
        macro.setParameters(params);
        migrator.migrate(macro, null);

        Map<String, String> parameters = macro.getParameters();
        assertEquals(urlKey + '=' + urlVal, parameters.get("url"));
        assertNull(parameters.get(urlKey));
    }
}
