package com.atlassian.confluence.extra.jira;

import com.atlassian.renderer.RenderContext;
import junit.framework.TestCase;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class TestJiraIssuesMacro extends TestCase
{
    public void testCreateContextMapForTemplate() throws Exception
    {
        Map params = new HashMap();
        params.put("url", "http://localhost:8080/sr/jira.issueviews:searchrequest-xml/temp/SearchRequest.xml?pid=10000&sorter/field=issuekey&sorter/order=ASC");
        params.put("columns", "type,summary");

        Map expectedContextMap = new HashMap();
        expectedContextMap.put("useTrustedConnection", Boolean.FALSE);
        expectedContextMap.put("showTrustWarnings", Boolean.FALSE);
        expectedContextMap.put("startOn", new Integer(0));
        expectedContextMap.put("clickableUrl", "http://localhost:8080/secure/IssueNavigator.jspa?reset=true&pid=10000&sorter/field=issuekey&sorter/order=ASC");
        expectedContextMap.put("resultsPerPage", new Integer(500));
        expectedContextMap.put("retrieverUrlHtml", "/plugins/servlet/issue-retriever?url=http%3A%2F%2Flocalhost%3A8080%2Fsr%2Fjira.issueviews%3Asearchrequest-xml%2Ftemp%2FSearchRequest.xml%3Fpid%3D10000&columns=type&columns=summary&useTrustedConnection=false");
        expectedContextMap.put("sortOrder", "asc");
        expectedContextMap.put("sortField", "issuekey");
        Set cols = new LinkedHashSet();
        cols.add("type");
        cols.add("summary");
        expectedContextMap.put("columns", cols);
        expectedContextMap.put("useCache", Boolean.TRUE);
        expectedContextMap.put("height", new Integer(480));

        JiraIssuesMacro jiraIssuesMacro = new JiraIssuesMacro();
        Map contextMap =  new HashMap();
        RenderContext renderContext = new RenderContext();
        jiraIssuesMacro.createContextMapFromParams(params,renderContext,contextMap,false);
        assertEquals(expectedContextMap, contextMap);

        contextMap =  new HashMap();
        params.put("url", "http://localhost:8080/sr/jira.issueviews:searchrequest-xml/temp/SearchRequest.xml?pid=10000");
        params.put("cache", "off");
        params.put("columns", "type,summary,key,reporter");
        params.put("height", "300");
        cols.add("key");
        cols.add("reporter");
        expectedContextMap.put("clickableUrl", "http://localhost:8080/secure/IssueNavigator.jspa?reset=true&pid=10000");
        expectedContextMap.put("sortOrder", "desc");
        expectedContextMap.put("sortField", null);
        expectedContextMap.put("useCache", Boolean.FALSE);
        expectedContextMap.put("retrieverUrlHtml",
                               "/plugins/servlet/issue-retriever?url=http%3A%2F%2Flocalhost%3A8080%2Fsr%2Fjira.issueviews%3Asearchrequest-xml%2Ftemp%2FSearchRequest.xml%3Fpid%3D10000&columns=type&columns=summary&columns=key&columns=reporter&useTrustedConnection=false");
        expectedContextMap.put("height", new Integer(300));
        jiraIssuesMacro.createContextMapFromParams(params,renderContext,contextMap,false);
        assertEquals(expectedContextMap, contextMap);
    }

    public void testFilterOutParam()
    {
        String expectedUrl = "http://localhost:8080/sr/jira.issueviews:searchrequest-xml/temp/SearchRequest.xml?pid=10000&sorter/field=issuekey&sorter/order=DESC";
        String filter = "tempMax=";
        String value;

        StringBuffer urlWithParamAtEnd = new StringBuffer("http://localhost:8080/sr/jira.issueviews:searchrequest-xml/temp/SearchRequest.xml?pid=10000&sorter/field=issuekey&sorter/order=DESC&tempMax=259");
        value = JiraIssuesMacro.filterOutParam(urlWithParamAtEnd, filter);
        assertEquals("259", value);
        assertEquals(expectedUrl, urlWithParamAtEnd.toString());

        StringBuffer urlWithParamAtBeginning = new StringBuffer("http://localhost:8080/sr/jira.issueviews:searchrequest-xml/temp/SearchRequest.xml?tempMax=1&pid=10000&sorter/field=issuekey&sorter/order=DESC");
        value = JiraIssuesMacro.filterOutParam(urlWithParamAtBeginning, filter);
        assertEquals("1", value);
        assertEquals(expectedUrl, urlWithParamAtBeginning.toString());

        StringBuffer urlWithParamInMiddle = new StringBuffer("http://localhost:8080/sr/jira.issueviews:searchrequest-xml/temp/SearchRequest.xml?pid=10000&sorter/field=issuekey&tempMax=30&sorter/order=DESC");
        value = JiraIssuesMacro.filterOutParam(urlWithParamInMiddle, filter);
        assertEquals("30", value);
        assertEquals(expectedUrl, urlWithParamInMiddle.toString());

    }

    // testing transformation of urls from xml to issue navigator styles
    public void testMakeClickableUrl()
    {
        assertEquals("http://jira.atlassian.com/secure/IssueNavigator.jspa?reset=true&pid=11011&pid=11772",
            JiraIssuesMacro.makeClickableUrl("http://jira.atlassian.com/sr/jira.issueviews:searchrequest-xml/temp/SearchRequest.xml?pid=11011&pid=11772"));

        assertEquals("http://jira.atlassian.com/secure/IssueNavigator.jspa?reset=true",
            JiraIssuesMacro.makeClickableUrl("http://jira.atlassian.com/sr/jira.issueviews:searchrequest-xml/temp/SearchRequest.xml"));

        assertEquals("http://jira.atlassian.com/secure/IssueNavigator.jspa?requestId=15701&tempMax=200",
            JiraIssuesMacro.makeClickableUrl("http://jira.atlassian.com/sr/jira.issueviews:searchrequest-xml/15701/SearchRequest-15701.xml?tempMax=200"));

        assertEquals("http://jira.atlassian.com/secure/IssueNavigator.jspa?requestId=15701",
            JiraIssuesMacro.makeClickableUrl("http://jira.atlassian.com/sr/jira.issueviews:searchrequest-xml/15701/SearchRequest-15701.xml"));
    }

    public void testPrepareDisplayColumns()
    {
        JiraIssuesMacro jiraIssuesMacro = new JiraIssuesMacro();

        Set defaultColumns = new LinkedHashSet();
        defaultColumns.add("type");
        defaultColumns.add("key");
        defaultColumns.add("summary");
        defaultColumns.add("assignee");
        defaultColumns.add("reporter");
        defaultColumns.add("priority");
        defaultColumns.add("status");
        defaultColumns.add("resolution");
        defaultColumns.add("created");
        defaultColumns.add("updated");
        defaultColumns.add("due");

        Set threeColumns = new LinkedHashSet();
        threeColumns.add("key");
        threeColumns.add("summary");
        threeColumns.add("assignee");

        // make sure get default columns when have empty column list
        assertEquals(defaultColumns,jiraIssuesMacro.prepareDisplayColumns(""));

        // make sure get columns properly
        assertEquals(threeColumns,jiraIssuesMacro.prepareDisplayColumns("key,summary,assignee"));
        assertEquals(threeColumns,jiraIssuesMacro.prepareDisplayColumns("key;summary;assignee"));

        // make sure empty columns are removed
        assertEquals(threeColumns,jiraIssuesMacro.prepareDisplayColumns(";key;summary;;assignee"));
        assertEquals(threeColumns,jiraIssuesMacro.prepareDisplayColumns("key;summary;assignee;"));

        // make sure if all empty columns are removed, get default columns
        assertEquals(defaultColumns,jiraIssuesMacro.prepareDisplayColumns(";"));
    }
}