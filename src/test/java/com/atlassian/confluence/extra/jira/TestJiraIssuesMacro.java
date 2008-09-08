package com.atlassian.confluence.extra.jira;

import com.atlassian.renderer.RenderContext;
import com.atlassian.confluence.core.ConfluenceActionSupport;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.jmock.cglib.MockObjectTestCase;
import org.jmock.Mock;

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
        initConfluenceActionSupportForI18nColumnNames();

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
        expectedContextMap.put("sortEnabled", Boolean.TRUE);

        Map contextMap =  new HashMap();
        jiraIssuesMacro.createContextMapFromParams(params,contextMap,false);
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
        jiraIssuesMacro.createContextMapFromParams(params,contextMap,false);
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

    public static class TestConfluenceActionSupport extends ConfluenceActionSupport
    {
        /* Avoid dup class def error */
    }
}