package com.atlassian.confluence.extra.jira;

import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.atlassian.applinks.api.ApplicationLinkService;
import com.atlassian.confluence.content.render.xhtml.DefaultConversionContext;
import com.atlassian.confluence.extra.jira.JiraIssuesMacro.ColumnInfo;
import com.atlassian.confluence.extra.jira.JiraIssuesMacro.Type;
import com.atlassian.confluence.macro.MacroExecutionException;
import com.atlassian.confluence.security.trust.TrustedTokenFactory;
import com.atlassian.confluence.util.http.HttpRequest;
import com.atlassian.confluence.util.http.HttpResponse;
import com.atlassian.confluence.util.http.HttpRetrievalService;
import com.atlassian.confluence.util.http.trust.TrustedConnectionStatusBuilder;
import com.atlassian.confluence.util.i18n.I18NBean;
import com.atlassian.confluence.util.i18n.I18NBeanFactory;
import com.atlassian.plugin.webresource.WebResourceManager;
import com.atlassian.renderer.v2.macro.Macro;
import com.atlassian.renderer.v2.macro.MacroException;

public class TestJiraIssuesMacro extends TestCase
{
    @Mock private I18NBeanFactory i18NBeanFactory;

    @Mock private I18NBean i18NBean;

    @Mock private JiraIssuesManager jiraIssuesManager;

    private JiraIssuesColumnManager jiraIssuesColumnManager;

    private JiraIssuesUrlManager jiraIssuesUrlManager;

    @Mock private JiraIssuesSettingsManager jiraIssuesSettingsManager;

    @Mock private ApplicationLinkService appLinkService;
    
    @Mock private HttpRetrievalService httpRetrievalService;

    @Mock private HttpRequest httpRequest;

    @Mock private HttpResponse httpResponse;

    @Mock private TrustedConnectionStatusBuilder trustedConnectionStatusBuilder;
    
    @Mock private TrustedTokenFactory trustedTokenFactory;
    
    @Mock private WebResourceManager webResourceManager;
    
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
        expectedContextMap.put("startOn", 0);
        expectedContextMap.put("clickableUrl", "http://localhost:8080/secure/IssueNavigator.jspa?reset=true&pid=10000&sorter/field=issuekey&sorter/order=ASC");
        expectedContextMap.put("resultsPerPage", 10);
        expectedContextMap.put("retrieverUrlHtml", "/plugins/servlet/issue-retriever?url=http%3A%2F%2Flocalhost%3A8080%2Fsr%2Fjira.issueviews%3Asearchrequest-xml%2Ftemp%2FSearchRequest.xml%3Fpid%3D10000&columns=type&columns=summary&forceAnonymous=false&flexigrid=true");
        expectedContextMap.put("sortOrder", "asc");
        expectedContextMap.put("sortField", "issuekey");
        List<ColumnInfo> cols = new ArrayList<ColumnInfo>();
        cols.add(new ColumnInfo("type"));
        cols.add(new ColumnInfo("summary"));
        expectedContextMap.put("columns", cols);
        expectedContextMap.put("useCache", true);
        expectedContextMap.put("title", "jiraissues.title");
        expectedContextMap.put("width", "100%");
        expectedContextMap.put("showTrustWarnings", false);

        jiraIssuesManager = new DefaultJiraIssuesManager(jiraIssuesColumnManager, jiraIssuesUrlManager,httpRetrievalService, trustedTokenFactory, trustedConnectionStatusBuilder, new DefaultTrustedApplicationConfig());
        (jiraIssuesMacro = new JiraIssuesMacro()).createContextMapFromParams(params, macroVelocityContext, params.get("url"), JiraIssuesMacro.Type.URL, null, false, false);
        Set<String> keySet = expectedContextMap.keySet();
        // comment back in to debug the assert equals on the two maps
//        for (String string : keySet)
//        {
//            if (!expectedContextMap.get(string).equals(macroVelocityContext.get(string)))
//            {
//                int x = 0;
//            }
//        }
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
                               "/plugins/servlet/issue-retriever?url=http%3A%2F%2Flocalhost%3A8080%2Fsr%2Fjira.issueviews%3Asearchrequest-xml%2Ftemp%2FSearchRequest.xml%3Fpid%3D10000&columns=type&columns=summary&columns=key&columns=reporter&forceAnonymous=false&flexigrid=true");
        expectedContextMap.put("height", "300");
        jiraIssuesMacro.createContextMapFromParams(params ,macroVelocityContext, params.get("url"), JiraIssuesMacro.Type.URL, null, false, false);
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
        assertEquals(defaultColumns,jiraIssuesMacro.getColumnInfo(jiraIssuesMacro.getColumnNames("")));

        // make sure get columns properly
        assertEquals(threeColumns,jiraIssuesMacro.getColumnInfo(jiraIssuesMacro.getColumnNames("key,summary,assignee")));
        assertEquals(threeColumns,jiraIssuesMacro.getColumnInfo(jiraIssuesMacro.getColumnNames("key;summary;assignee")));

        // make sure empty columns are removed
        assertEquals(threeColumns,jiraIssuesMacro.getColumnInfo(jiraIssuesMacro.getColumnNames(";key;summary;;assignee")));
        assertEquals(threeColumns,jiraIssuesMacro.getColumnInfo(jiraIssuesMacro.getColumnNames("key;summary;assignee;")));

        // make sure if all empty columns are removed, get default columns
        assertEquals(defaultColumns,jiraIssuesMacro.getColumnInfo(jiraIssuesMacro.getColumnNames(";")));
    }

    public void testColumnWrapping() 
    {
        final String NOWRAP = "nowrap";
        Set<String> wrappedColumns = new HashSet<String>( Arrays.asList( "summary" ) );

        List<ColumnInfo> columnInfo = jiraIssuesMacro.getColumnInfo(jiraIssuesMacro.getColumnNames(null));
        
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
    public void testBuildInfoRequestedWithCredentialsAndFilterUrls() throws IOException, MacroException, MacroExecutionException
    {
        params.put("anonymous", "true");
        params.put("url", "http://localhost:1990/jira/sr/jira.issueviews:searchrequest-xml/10000/SearchRequest-10000.xml?tempMax=1000&os_username=admin&os_password=admin");

        jiraIssuesManager = new DefaultJiraIssuesManager(jiraIssuesColumnManager, jiraIssuesUrlManager, httpRetrievalService, trustedTokenFactory, trustedConnectionStatusBuilder, new DefaultTrustedApplicationConfig());

        jiraIssuesMacro = new JiraIssuesMacro();

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

        jiraIssuesMacro.createContextMapFromParams(params, macroVelocityContext, params.get("url"), JiraIssuesMacro.Type.URL, null, false, false);
    }

    /**
     * <a href="http://developer.atlassian.com/jira/browse/CONFJIRA-133">CONFJIRA_133</a>
     */
    public void testBuildInfoRequestedOverTrustedConnectionAndFilterUrls() throws IOException, MacroException, MacroExecutionException
    {
        params.put("anonymous", "false");
        params.put("url", "http://localhost:1990/jira/sr/jira.issueviews:searchrequest-xml/10000/SearchRequest-10000.xml?tempMax=1000");

        jiraIssuesManager = new DefaultJiraIssuesManager(jiraIssuesColumnManager, jiraIssuesUrlManager, httpRetrievalService, trustedTokenFactory, trustedConnectionStatusBuilder, new DefaultTrustedApplicationConfig());

        jiraIssuesMacro = new JiraIssuesMacro();

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

        jiraIssuesMacro.createContextMapFromParams(params, macroVelocityContext, params.get("url"), JiraIssuesMacro.Type.URL, null, false, false);

        //verify(httpRequest).setAuthenticator(isA(TrustedTokenAuthenticator.class));
    }
    private void parseTest(String paramKey, String paramValue, String expectedValue, Type expectedType) throws MacroException, MacroExecutionException
    {
        jiraIssuesMacro = new JiraIssuesMacro();
        Map<String, String> params = new HashMap<String, String>();
        params.put(paramKey, paramValue);
        
        JiraRequestData requestData = jiraIssuesMacro.parseRequestData(params);
        
        assertEquals(expectedType, requestData.getRequestType());
        assertEquals(expectedValue == null ? paramValue : expectedValue, requestData.getRequestData());
    }
    public void testJqlRequestParsing() throws MacroException, MacroExecutionException
    {
        parseTest(Macro.RAW_PARAMS_KEY, "project = TST", null, JiraIssuesMacro.Type.JQL);
    }
    
    public void testJqlRequestParsingExplicit() throws MacroException, MacroExecutionException
    {
        parseTest("jqlQuery", "project = TST", null, JiraIssuesMacro.Type.JQL);
    }
    
    public void testSingleKeyRequestParsing() throws MacroException, MacroExecutionException
    {
        parseTest(Macro.RAW_PARAMS_KEY, "TST-2", null, Type.KEY);
    }
    
    public void testSingleKeyRequestParsingExplicit() throws MacroException, MacroExecutionException
    {
        parseTest("key", "CONF-1234", null, Type.KEY);
    }
    
    public void testMultiKeyRequestParsing() throws MacroException, MacroExecutionException
    {
        String keys = "TST-1, CONF-1234, TST-5";
        parseTest(Macro.RAW_PARAMS_KEY, keys, "issuekey in (" + keys + ")", Type.JQL);
    }
    
    public void testMultKeyRequestParsingExplicit() throws MacroException, MacroExecutionException
    {
        String keys = "TST-1, CONF-1234, TST-5";
        parseTest("key", keys, "issuekey in (" + keys + ")", Type.JQL);
    }
    
    public void testUrlRequestParsing() throws MacroException, MacroExecutionException
    {
        parseTest(Macro.RAW_PARAMS_KEY, "http://jira.atlassian.com/sr/search.xml", null, Type.URL);
    }
    
    public void testUrlRequestParsingExplicit() throws MacroException, MacroExecutionException
    {
        parseTest("url", "http://jira.atlassian.com/sr/search.xml", null, Type.URL);
    }
    
    public void testErrorRenderedIfUrlNotSpecified() throws MacroException
    {
        params.clear();
        params.put(Macro.RAW_PARAMS_KEY, "");
        
        try
        {
            jiraIssuesMacro.execute(params, (String) null, (DefaultConversionContext) null);
            fail();
        }
        catch (MacroExecutionException e) 
        {
        	assertEquals("jiraissues.error.urlnotspecified", e.getMessage());
		}
    }
    
    /**
     * <a href="https://studio.plugins.atlassian.com/browse/CONFJIRA-211">CONFJIRA-211</a>
     */
    public void testErrorRenderedIfUrlNotValid() throws MacroException
    {
    	params.clear();
    	params.put("url", "{jiraissues:url=javascript:alert('gotcha!' + document.cookie)}");
    	
    	try
        {
            jiraIssuesMacro.execute(params, (String) null, (DefaultConversionContext) null);
            fail();
        }
        catch (MacroExecutionException e) 
        {
        	assertEquals("jiraissues.error.invalidurl", e.getMessage());
		}
    }

    private class JiraIssuesMacro extends com.atlassian.confluence.extra.jira.JiraIssuesMacro
    {
        private JiraIssuesMacro()
        {
            setI18NBeanFactory(i18NBeanFactory);
            setJiraIssuesColumnManager(jiraIssuesColumnManager);
            setJiraIssuesManager(jiraIssuesManager);
            setWebResourceManager(webResourceManager);
            setApplicationLinkService(appLinkService);
            
        }
    }
}