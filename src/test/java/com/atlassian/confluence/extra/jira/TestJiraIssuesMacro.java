package com.atlassian.confluence.extra.jira;

import com.atlassian.confluence.extra.jira.JiraIssuesMacro.ColumnInfo;
import com.atlassian.confluence.util.http.HttpRequest;
import com.atlassian.confluence.util.http.HttpResponse;
import com.atlassian.confluence.util.http.HttpRetrievalService;
import com.atlassian.confluence.util.http.httpclient.TrustedTokenAuthenticator;
import com.atlassian.confluence.util.http.trust.TrustedConnectionStatusBuilder;
import com.atlassian.confluence.util.i18n.I18NBean;
import com.atlassian.confluence.util.i18n.I18NBeanFactory;
import com.atlassian.renderer.v2.macro.MacroException;
import junit.framework.TestCase;
import org.mockito.Mock;
import static org.mockito.Mockito.anyObject;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TestJiraIssuesMacro extends TestCase
{
    @Mock private I18NBeanFactory i18NBeanFactory;

    @Mock private I18NBean i18NBean;

    @Mock private JiraIssuesManager jiraIssuesManager;

    private JiraIssuesColumnManager jiraIssuesColumnManager;

    private JiraIssuesUrlManager jiraIssuesUrlManager;

    @Mock private JiraIssuesSettingsManager jiraIssuesSettingsManager;

    @Mock private HttpRetrievalService httpRetrievalService;

    @Mock private HttpRequest httpRequest;

    @Mock private HttpResponse httpResponse;

    @Mock private TrustedConnectionStatusBuilder trustedConnectionStatusBuilder;
    
    private JiraIssuesMacro jiraIssuesMacro;

    private Map<String, String> params;

    private Map<String, Object> macroVelocityContext;

    protected void setUp() throws Exception
    {
        super.setUp();

        MockitoAnnotations.initMocks(this);

        jiraIssuesColumnManager = new DefaultJiraIssuesColumnManager(jiraIssuesSettingsManager);
        jiraIssuesUrlManager = new DefaultJiraIssuesUrlManager(jiraIssuesColumnManager);

        when(i18NBeanFactory.getI18NBean()).thenReturn(i18NBean);

        when(i18NBean.getText(anyString())).thenAnswer(
                new Answer<String>()
                {
                    public String answer(InvocationOnMock invocationOnMock) throws Throwable
                    {
                        return (String) invocationOnMock.getArguments()[0];
                    }
                }
        );

        when(i18NBean.getText(anyString(), (List) anyObject())).thenAnswer(
                new Answer<String>()
                {
                    public String answer(InvocationOnMock invocationOnMock) throws Throwable
                    {
                        return (String) invocationOnMock.getArguments()[0];
                    }
                }
        );

        jiraIssuesMacro = new JiraIssuesMacro();

        params = new HashMap<String, String>();
        macroVelocityContext = new HashMap<String, Object>();
    }

    public void testCreateContextMapForTemplate() throws Exception
    {
        params.put("url", "http://localhost:8080/sr/jira.issueviews:searchrequest-xml/temp/SearchRequest.xml?pid=10000&sorter/field=issuekey&sorter/order=ASC");
        params.put("columns", "type,summary");

        Map<String, Object> expectedContextMap = new HashMap<String, Object>();
        expectedContextMap.put("useTrustedConnection", true); /* We use trusted connections by default, according to http://confluence.atlassian.com/display/DOC/JIRA+Issues+Macro */
        expectedContextMap.put("showTrustWarnings", true);
        expectedContextMap.put("startOn", 0);
        expectedContextMap.put("clickableUrl", "http://localhost:8080/secure/IssueNavigator.jspa?reset=true&pid=10000&sorter/field=issuekey&sorter/order=ASC");
        expectedContextMap.put("resultsPerPage", 500);
        expectedContextMap.put("retrieverUrlHtml", "/plugins/servlet/issue-retriever?url=http%3A%2F%2Flocalhost%3A8080%2Fsr%2Fjira.issueviews%3Asearchrequest-xml%2Ftemp%2FSearchRequest.xml%3Fpid%3D10000&columns=type&columns=summary&useTrustedConnection=true");
        expectedContextMap.put("sortOrder", "asc");
        expectedContextMap.put("sortField", "issuekey");
        List<ColumnInfo> cols = new ArrayList<ColumnInfo>();
        cols.add(new ColumnInfo("type"));
        cols.add(new ColumnInfo("summary"));
        expectedContextMap.put("columns", cols);
        expectedContextMap.put("useCache", true);
        expectedContextMap.put("title", "jiraissues.title");
        expectedContextMap.put("width", "100%");
        expectedContextMap.put("sortEnabled", true);

        when(jiraIssuesSettingsManager.getSort(anyString())).thenReturn(JiraIssuesSettingsManager.Sort.SORT_ENABLED);


        jiraIssuesManager = new DefaultJiraIssuesManager(
                jiraIssuesSettingsManager, jiraIssuesColumnManager, jiraIssuesUrlManager, null, null, trustedConnectionStatusBuilder, httpRetrievalService, "org.apache.xerces.parsers.SAXParser"
        );
        (jiraIssuesMacro = new JiraIssuesMacro()).createContextMapFromParams(params, macroVelocityContext, false, false);
        assertEquals(expectedContextMap, macroVelocityContext);

        macroVelocityContext.clear();

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
                               "/plugins/servlet/issue-retriever?url=http%3A%2F%2Flocalhost%3A8080%2Fsr%2Fjira.issueviews%3Asearchrequest-xml%2Ftemp%2FSearchRequest.xml%3Fpid%3D10000&columns=type&columns=summary&columns=key&columns=reporter&useTrustedConnection=true");
        expectedContextMap.put("height", "300");
        jiraIssuesMacro.createContextMapFromParams(params ,macroVelocityContext, false, false);
        assertEquals(expectedContextMap, macroVelocityContext);
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

    /** <a href="http://developer.atlassian.com/jira/browse/CONFJIRA-124">CONFJIRA-124</a> */
    public void testDescriptionColumnWrapped()
    {
        assertTrue(new ColumnInfo("description").shouldWrap());
    }

    /**
     * <a href="http://developer.atlassian.com/jira/browse/CONFJIRA-133">CONFJIRA_133</a>
     */
    public void testBuildInfoRequestedWithCredentialsAndFilterUrls() throws IOException, MacroException
    {
        params.put("anonymous", "true");
        params.put("url", "http://localhost:1990/jira/sr/jira.issueviews:searchrequest-xml/10000/SearchRequest-10000.xml?tempMax=1000&os_username=admin&os_password=admin");

        jiraIssuesManager = new DefaultJiraIssuesManager(
                jiraIssuesSettingsManager, jiraIssuesColumnManager, jiraIssuesUrlManager, null, null, trustedConnectionStatusBuilder, httpRetrievalService, "org.apache.xerces.parsers.SAXParser"
        );

        jiraIssuesMacro = new JiraIssuesMacro()
        {
            @Override
            protected void createContextMapFromParams(Map<String, String> params, Map<String, Object> contextMap, boolean renderInHtml, boolean showCount) throws MacroException
            {
                super.createContextMapFromParams(params, contextMap, renderInHtml, showCount);

                assertTrue((Boolean) contextMap.get("sortEnabled"));
            }
        };

        when(jiraIssuesSettingsManager.getSort(anyString())).thenReturn(JiraIssuesSettingsManager.Sort.SORT_ENABLED);
        when(httpRetrievalService.getDefaultRequestFor("http://localhost:1990/jira/sr/jira.issueviews:searchrequest-xml/10000/SearchRequest-10000.xml?os_username=admin&os_password=admin&tempMax=0")).thenReturn(httpRequest);
        when(httpRetrievalService.get(httpRequest)).thenReturn(httpResponse);
        when(httpResponse.getResponse()).thenReturn(
                new ByteArrayInputStream(
                        (
                                "<rss version=\"0.92\" >\n" +
                                        "<channel>\n" +
                                        "    <title>Your Company JIRA</title>\n" +
                                        "    <link>http://localhost:1990/jira/secure/IssueNavigator.jspa?reset=true&amp;pid=10000&amp;sorter/field=issuekey&amp;sorter/order=DESC</link>\n" +
                                        "    <description>An XML representation of a search request</description>\n" +
                                        "    <language>en-us</language>     <issue start=\"0\" end=\"1\" total=\"1\" />     <build-info>\n" +
                                        "        <version>3.13.2</version>\n" +
                                        "        <build-number>335</build-number>\n" +
                                        "        <build-date>05-12-2008</build-date>\n" +
                                        "        <edition>Enterprise</edition>\n" +
                                        "    </build-info>\n" +
                                        "</channel>\n" +
                                        "</rss>"
                        ).getBytes("UTF-8")
                )
        );

        jiraIssuesMacro.createContextMapFromParams(params, macroVelocityContext, false, false);
    }

    /**
     * <a href="http://developer.atlassian.com/jira/browse/CONFJIRA-133">CONFJIRA_133</a>
     */
    public void testBuildInfoRequestedOverTrustedConnectionAndFilterUrls() throws IOException, MacroException
    {
        params.put("anonymous", "false");
        params.put("url", "http://localhost:1990/jira/sr/jira.issueviews:searchrequest-xml/10000/SearchRequest-10000.xml?tempMax=1000");

        jiraIssuesManager = new DefaultJiraIssuesManager(
                jiraIssuesSettingsManager, jiraIssuesColumnManager, jiraIssuesUrlManager, null, null, trustedConnectionStatusBuilder, httpRetrievalService, "org.apache.xerces.parsers.SAXParser"
        );

        jiraIssuesMacro = new JiraIssuesMacro()
        {
            @Override
            protected void createContextMapFromParams(Map<String, String> params, Map<String, Object> contextMap, boolean renderInHtml, boolean showCount) throws MacroException
            {
                super.createContextMapFromParams(params, contextMap, renderInHtml, showCount);

                assertTrue((Boolean) contextMap.get("sortEnabled"));
            }
        };

        when(jiraIssuesSettingsManager.getSort(anyString())).thenReturn(JiraIssuesSettingsManager.Sort.SORT_UNKNOWN);
        when(httpRetrievalService.getDefaultRequestFor("http://localhost:1990/jira/sr/jira.issueviews:searchrequest-xml/10000/SearchRequest-10000.xml?tempMax=0")).thenReturn(httpRequest);
        when(httpRetrievalService.get(httpRequest)).thenReturn(httpResponse);
        when(httpResponse.getResponse()).thenReturn(
                new ByteArrayInputStream(
                        (
                                "<rss version=\"0.92\" >\n" +
                                        "<channel>\n" +
                                        "    <title>Your Company JIRA</title>\n" +
                                        "    <link>http://localhost:1990/jira/secure/IssueNavigator.jspa?reset=true&amp;pid=10000&amp;sorter/field=issuekey&amp;sorter/order=DESC</link>\n" +
                                        "    <description>An XML representation of a search request</description>\n" +
                                        "    <language>en-us</language>     <issue start=\"0\" end=\"1\" total=\"1\" />     <build-info>\n" +
                                        "        <version>3.13.2</version>\n" +
                                        "        <build-number>335</build-number>\n" +
                                        "        <build-date>05-12-2008</build-date>\n" +
                                        "        <edition>Enterprise</edition>\n" +
                                        "    </build-info>\n" +
                                        "</channel>\n" +
                                        "</rss>"
                        ).getBytes("UTF-8")
                )
        );

        jiraIssuesMacro.createContextMapFromParams(params, macroVelocityContext, false, false);

        verify(httpRequest).setAuthenticator(isA(TrustedTokenAuthenticator.class));
    }

    private class JiraIssuesMacro extends com.atlassian.confluence.extra.jira.JiraIssuesMacro
    {
        private JiraIssuesMacro()
        {
            setI18NBeanFactory(i18NBeanFactory);
            setJiraIssuesColumnManager(jiraIssuesColumnManager);
            setJiraIssuesManager(jiraIssuesManager);
            setTrustedApplicationConfig(new DefaultTrustedApplicationConfig());
        }
    }
}