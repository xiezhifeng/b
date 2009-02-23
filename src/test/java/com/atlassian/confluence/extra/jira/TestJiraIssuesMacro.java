package com.atlassian.confluence.extra.jira;

import com.atlassian.confluence.core.ConfluenceActionSupport;
import com.atlassian.confluence.extra.jira.JiraIssuesMacro.ColumnInfo;
import junit.framework.TestCase;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TestJiraIssuesMacro extends TestCase
{
    private JiraIssuesMacro jiraIssuesMacro;

    private ConfluenceActionSupport confluenceActionSupport;

    protected void setUp() throws Exception
    {
        super.setUp();
        confluenceActionSupport = mock(ConfluenceActionSupport.class);

        jiraIssuesMacro = new JiraIssuesMacro()
        {
            protected ConfluenceActionSupport getConfluenceActionSupport()
            {
                return confluenceActionSupport;
            }
        };

    }

    public void testCreateContextMapForTemplate() throws Exception
    {
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("url", "http://localhost:8080/sr/jira.issueviews:searchrequest-xml/temp/SearchRequest.xml?pid=10000&sorter/field=issuekey&sorter/order=ASC");
        params.put("columns", "type,summary");

        Map<String, Object> expectedContextMap = new HashMap<String, Object>();
        expectedContextMap.put("useTrustedConnection", false);
        expectedContextMap.put("showTrustWarnings", false);
        expectedContextMap.put("startOn", 0);
        expectedContextMap.put("clickableUrl", "http://localhost:8080/secure/IssueNavigator.jspa?reset=true&pid=10000&sorter/field=issuekey&sorter/order=ASC");
        expectedContextMap.put("resultsPerPage", 500);
        expectedContextMap.put("retrieverUrlHtml", "/plugins/servlet/issue-retriever?url=http%3A%2F%2Flocalhost%3A8080%2Fsr%2Fjira.issueviews%3Asearchrequest-xml%2Ftemp%2FSearchRequest.xml%3Fpid%3D10000&columns=type&columns=summary&useTrustedConnection=false");
        expectedContextMap.put("sortOrder", "asc");
        expectedContextMap.put("sortField", "issuekey");
        List<ColumnInfo> cols = new ArrayList<ColumnInfo>();
        cols.add(new ColumnInfo("type"));
        cols.add(new ColumnInfo("summary"));
        expectedContextMap.put("columns", cols);
        expectedContextMap.put("useCache", true);
        expectedContextMap.put("title", "jiraissues.title");
        expectedContextMap.put("height", 480);
        expectedContextMap.put("sortEnabled", true);

        Map<String, Object> contextMap =  new HashMap<String, Object>();
        jiraIssuesMacro.createContextMapFromParams(params,contextMap,false,false);
        assertEquals(expectedContextMap, contextMap);

        contextMap =  new HashMap<String, Object>();
        params.put("url", "http://localhost:8080/sr/jira.issueviews:searchrequest-xml/temp/SearchRequest.xml?pid=10000");
        params.put("title", "Some Random & Unlikely Issues");
        params.put("cache", "off");
        params.put("columns", "type,summary,key,reporter");
        params.put("height", "300");
        cols.add(new ColumnInfo("key", "key"));
        cols.add(new ColumnInfo("reporter", "reporter"));
        expectedContextMap.put("clickableUrl", "http://localhost:8080/secure/IssueNavigator.jspa?reset=true&pid=10000");
        expectedContextMap.put("sortOrder", "desc");
        expectedContextMap.put("sortField", null);
        expectedContextMap.put("title", "Some Random &amp; Unlikely Issues");
        expectedContextMap.put("useCache", false);
        expectedContextMap.put("retrieverUrlHtml",
                               "/plugins/servlet/issue-retriever?url=http%3A%2F%2Flocalhost%3A8080%2Fsr%2Fjira.issueviews%3Asearchrequest-xml%2Ftemp%2FSearchRequest.xml%3Fpid%3D10000&columns=type&columns=summary&columns=key&columns=reporter&useTrustedConnection=false");
        expectedContextMap.put("height", 300);
        jiraIssuesMacro.createContextMapFromParams(params,contextMap,false,false);
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
        List<ColumnInfo> defaultColumns = new ArrayList<ColumnInfo>();
        defaultColumns.add(new ColumnInfo("type"));
        defaultColumns.add(new ColumnInfo("key"));
        defaultColumns.add(new ColumnInfo("summary"));
        defaultColumns.add(new ColumnInfo("assignee"));
        defaultColumns.add(new ColumnInfo("reporter"));
        defaultColumns.add(new ColumnInfo("priority"));
        defaultColumns.add(new ColumnInfo("status"));
        defaultColumns.add(new ColumnInfo("resolution"));
        defaultColumns.add(new ColumnInfo("created"));
        defaultColumns.add(new ColumnInfo("updated"));
        defaultColumns.add(new ColumnInfo("due"));

        List<ColumnInfo> threeColumns = new ArrayList<ColumnInfo>();
        threeColumns.add(new ColumnInfo("key"));
        threeColumns.add(new ColumnInfo("summary"));
        threeColumns.add(new ColumnInfo("assignee"));

        // make sure get default columns when have empty column list
        assertEquals(defaultColumns,jiraIssuesMacro.getColumnInfo(""));

        // make sure get columns properly
        assertEquals(threeColumns,jiraIssuesMacro.getColumnInfo("key,summary,assignee"));
        assertEquals(threeColumns,jiraIssuesMacro.getColumnInfo("key;summary;assignee"));

        // make sure empty columns are removed
        assertEquals(threeColumns,jiraIssuesMacro.getColumnInfo(";key;summary;;assignee"));
        assertEquals(threeColumns,jiraIssuesMacro.getColumnInfo("key;summary;assignee;"));

        // make sure if all empty columns are removed, get default columns
        assertEquals(defaultColumns,jiraIssuesMacro.getColumnInfo(";"));
    }

    public void testColumnWrapping() 
    {
        final String NOWRAP = "nowrap";
        Set<String> wrappedColumns = new HashSet<String>( Arrays.asList( "summary" ) );

        List<ColumnInfo> columnInfo = jiraIssuesMacro.getColumnInfo(null);
        
        for (ColumnInfo colInfo : columnInfo)
        {   
            boolean hasNowrap = colInfo.getHtmlClassName().contains(NOWRAP);
            if(wrappedColumns.contains(colInfo.getKey()))
            {
                assertFalse("Wrapped columns should not have nowrap class (" + colInfo.getKey() + ", " + colInfo.getHtmlClassName() +")", hasNowrap);
            }
            else 
            {
                assertTrue("Non-wrapped columns should have nowrap class", hasNowrap);
            }
        }
    }
}