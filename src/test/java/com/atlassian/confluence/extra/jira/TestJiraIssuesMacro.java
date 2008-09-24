package com.atlassian.confluence.extra.jira;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jmock.Mock;
import org.jmock.cglib.MockObjectTestCase;

import com.atlassian.confluence.core.ConfluenceActionSupport;
import com.atlassian.confluence.extra.jira.JiraIssuesMacro.ColumnInfo;

public class TestJiraIssuesMacro extends MockObjectTestCase
{
    private JiraIssuesMacro jiraIssuesMacro;

    private Mock mockConfluenceActionSupport;

    private ConfluenceActionSupport confluenceActionSupport;

    protected void setUp() throws Exception
    {
        super.setUp();
        mockConfluenceActionSupport = mock(TestConfluenceActionSupport.class);
        confluenceActionSupport = (ConfluenceActionSupport) mockConfluenceActionSupport.proxy();

        jiraIssuesMacro = new JiraIssuesMacro()
        {
            protected ConfluenceActionSupport getConfluenceActionSupport()
            {
                return confluenceActionSupport;
            }
        };

    }

    private void initConfluenceActionSupportForI18nColumnNames()
    {
        mockConfluenceActionSupport.expects(atLeastOnce()).method("getText").with(eq("jiraissues.column.type")).will(returnValue("Type"));
        mockConfluenceActionSupport.expects(atLeastOnce()).method("getText").with(eq("jiraissues.column.key")).will(returnValue("Key"));
        mockConfluenceActionSupport.expects(atLeastOnce()).method("getText").with(eq("jiraissues.column.summary")).will(returnValue("Summary"));
        mockConfluenceActionSupport.expects(atLeastOnce()).method("getText").with(eq("jiraissues.column.assignee")).will(returnValue("Assignee"));
        mockConfluenceActionSupport.expects(atLeastOnce()).method("getText").with(eq("jiraissues.column.reporter")).will(returnValue("Reporter"));
        mockConfluenceActionSupport.expects(atLeastOnce()).method("getText").with(eq("jiraissues.column.priority")).will(returnValue("Priority"));
        mockConfluenceActionSupport.expects(atLeastOnce()).method("getText").with(eq("jiraissues.column.status")).will(returnValue("Status"));
        mockConfluenceActionSupport.expects(atLeastOnce()).method("getText").with(eq("jiraissues.column.resolution")).will(returnValue("Resolution"));
        mockConfluenceActionSupport.expects(atLeastOnce()).method("getText").with(eq("jiraissues.column.created")).will(returnValue("Created"));
        mockConfluenceActionSupport.expects(atLeastOnce()).method("getText").with(eq("jiraissues.column.updated")).will(returnValue("Updated"));
        mockConfluenceActionSupport.expects(atLeastOnce()).method("getText").with(eq("jiraissues.column.due")).will(returnValue("Due"));
    }

    public void testCreateContextMapForTemplate() throws Exception
    {
        mockConfluenceActionSupport.expects(atLeastOnce()).method("getText").with(eq("jiraissues.column.type")).will(returnValue("Type"));
        mockConfluenceActionSupport.expects(atLeastOnce()).method("getText").with(eq("jiraissues.column.key")).will(returnValue("Key"));
        mockConfluenceActionSupport.expects(atLeastOnce()).method("getText").with(eq("jiraissues.column.summary")).will(returnValue("Summary"));
        mockConfluenceActionSupport.expects(atLeastOnce()).method("getText").with(eq("jiraissues.column.reporter")).will(returnValue("Reporter"));
        
        Map<String, Object> params = new HashMap<String, Object>();
        params.put("url", "http://localhost:8080/sr/jira.issueviews:searchrequest-xml/temp/SearchRequest.xml?pid=10000&sorter/field=issuekey&sorter/order=ASC");
        params.put("columns", "type,summary");

        Map<String, Object> expectedContextMap = new HashMap<String, Object>();
        expectedContextMap.put("useTrustedConnection", Boolean.FALSE);
        expectedContextMap.put("showTrustWarnings", Boolean.FALSE);
        expectedContextMap.put("startOn", new Integer(0));
        expectedContextMap.put("clickableUrl", "http://localhost:8080/secure/IssueNavigator.jspa?reset=true&pid=10000&sorter/field=issuekey&sorter/order=ASC");
        expectedContextMap.put("resultsPerPage", new Integer(500));
        expectedContextMap.put("retrieverUrlHtml", "/plugins/servlet/issue-retriever?url=http%3A%2F%2Flocalhost%3A8080%2Fsr%2Fjira.issueviews%3Asearchrequest-xml%2Ftemp%2FSearchRequest.xml%3Fpid%3D10000&columns=type&columns=summary&useTrustedConnection=false");
        expectedContextMap.put("sortOrder", "asc");
        expectedContextMap.put("sortField", "issuekey");
        List<ColumnInfo> cols = new ArrayList<ColumnInfo>();
        cols.add(new ColumnInfo("type"));
        cols.add(new ColumnInfo("summary"));
        expectedContextMap.put("columns", cols);
        expectedContextMap.put("useCache", Boolean.TRUE);
        expectedContextMap.put("height", new Integer(480));
        expectedContextMap.put("sortEnabled", Boolean.TRUE);

        Map<String, Object> contextMap =  new HashMap<String, Object>();
        jiraIssuesMacro.createContextMapFromParams(params,contextMap,false,false);
        assertEquals(expectedContextMap, contextMap);

        contextMap =  new HashMap<String, Object>();
        params.put("url", "http://localhost:8080/sr/jira.issueviews:searchrequest-xml/temp/SearchRequest.xml?pid=10000");
        params.put("cache", "off");
        params.put("columns", "type,summary,key,reporter");
        params.put("height", "300");
        cols.add(new ColumnInfo("key", "key"));
        cols.add(new ColumnInfo("reporter", "reporter"));
        expectedContextMap.put("clickableUrl", "http://localhost:8080/secure/IssueNavigator.jspa?reset=true&pid=10000");
        expectedContextMap.put("sortOrder", "desc");
        expectedContextMap.put("sortField", null);
        expectedContextMap.put("useCache", Boolean.FALSE);
        expectedContextMap.put("retrieverUrlHtml",
                               "/plugins/servlet/issue-retriever?url=http%3A%2F%2Flocalhost%3A8080%2Fsr%2Fjira.issueviews%3Asearchrequest-xml%2Ftemp%2FSearchRequest.xml%3Fpid%3D10000&columns=type&columns=summary&columns=key&columns=reporter&useTrustedConnection=false");
        expectedContextMap.put("height", new Integer(300));
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
        initConfluenceActionSupportForI18nColumnNames();

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
        Set<String> wrappedColumns = new HashSet<String>( Arrays.asList(new String[] { "summary" } ) );
        
        initConfluenceActionSupportForI18nColumnNames();
        List<ColumnInfo> columnInfo = jiraIssuesMacro.getColumnInfo(null);
        
        for (Iterator columnIter = columnInfo.iterator(); columnIter.hasNext();)
        {
            ColumnInfo colInfo = (ColumnInfo) columnIter.next();
            
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
    
    
    
    public static class TestConfluenceActionSupport extends ConfluenceActionSupport
    {
        /* Avoid dup class def error */
    }
}