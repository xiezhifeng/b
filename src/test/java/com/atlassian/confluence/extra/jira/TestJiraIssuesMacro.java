package com.atlassian.confluence.extra.jira;

import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import junit.framework.TestCase;

import org.jdom.Element;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.atlassian.applinks.api.ApplicationId;
import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.applinks.api.ApplicationLinkRequest;
import com.atlassian.applinks.api.ApplicationLinkRequestFactory;
import com.atlassian.applinks.api.ApplicationLinkService;
import com.atlassian.config.util.BootstrapUtils;
import com.atlassian.confluence.content.render.xhtml.DefaultConversionContext;
import com.atlassian.confluence.extra.jira.JiraIssuesMacro.ColumnInfo;
import com.atlassian.confluence.extra.jira.JiraIssuesMacro.Type;
import com.atlassian.confluence.extra.jira.JiraIssuesManager.Channel;
import com.atlassian.confluence.macro.MacroExecutionException;
import com.atlassian.confluence.security.Permission;
import com.atlassian.confluence.security.PermissionManager;
import com.atlassian.confluence.security.trust.TrustedTokenFactory;
import com.atlassian.confluence.setup.BootstrapManager;
import com.atlassian.confluence.setup.settings.Settings;
import com.atlassian.confluence.setup.settings.SettingsManager;
import com.atlassian.confluence.util.http.HttpRequest;
import com.atlassian.confluence.util.http.HttpResponse;
import com.atlassian.confluence.util.http.HttpRetrievalService;
import com.atlassian.confluence.util.http.trust.TrustedConnectionStatus;
import com.atlassian.confluence.util.http.trust.TrustedConnectionStatusBuilder;
import com.atlassian.confluence.util.i18n.I18NBean;
import com.atlassian.confluence.util.i18n.I18NBeanFactory;
import com.atlassian.confluence.web.context.HttpContext;
import com.atlassian.plugin.webresource.WebResourceManager;
import com.atlassian.renderer.TokenType;
import com.atlassian.renderer.v2.macro.Macro;
import com.atlassian.renderer.v2.macro.MacroException;
import com.atlassian.sal.api.net.Request;
import com.atlassian.user.User;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class TestJiraIssuesMacro extends TestCase
{
    @Mock private I18NBeanFactory i18NBeanFactory;

    @Mock private I18NBean i18NBean;

    @Mock private JiraIssuesManager jiraIssuesManager;

    @Mock private SettingsManager settingsManager;

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

    @Mock private BootstrapManager bootstrapManager;

    @Mock private HttpServletRequest httpServletRequest;

    @Mock private HttpContext httpContext;

    @Mock private PermissionManager permissionManager;

    @Mock private ApplicationLinkRequestFactory requestFactory;

    private JiraIssuesMacro jiraIssuesMacro;

    private Map<String, String> params;

    private Map<String, Object> macroVelocityContext;

    private static final String JIRA_KEY_DEFAULT_PARAM = "0";


    protected void setUp() throws Exception
    {
        super.setUp();

        MockitoAnnotations.initMocks(this);
        when(httpContext.getRequest()).thenReturn(httpServletRequest);
        when(httpServletRequest.getContextPath()).thenReturn("/contextPath");
        BootstrapUtils.setBootstrapManager(bootstrapManager);       

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
    	List<String> columnList=Lists.newArrayList("type","summary");
        params.put("url", "http://localhost:8080/sr/jira.issueviews:searchrequest-xml/temp/SearchRequest.xml?pid=10000&sorter/field=issuekey&sorter/order=ASC");
        params.put("columns", "type,summary");
        params.put("title", "EXPLICIT VALUE");

        Map<String, Object> expectedContextMap = Maps.newHashMap();

        jiraIssuesMacro = new JiraIssuesMacro();
        jiraIssuesMacro.setPermissionManager(permissionManager);
        
        when(permissionManager.hasPermission((User) anyObject(), (Permission) anyObject(), anyObject())).thenReturn(false);
        when(jiraIssuesManager.retrieveXMLAsChannel(params.get("url"), columnList, null, true, true)).thenReturn(
                new MockChannel(params.get("url")));
        
        expectedContextMap.put("isSourceApplink", false);
        expectedContextMap.put("showTrustWarnings", false);
        expectedContextMap.put("trustedConnectionStatus",null);
        expectedContextMap.put("width", "100%");
        List<ColumnInfo> cols = Lists.newArrayList(new ColumnInfo("type"),new ColumnInfo("summary"));
        expectedContextMap.put("columns", cols);
        expectedContextMap.put("trustedConnection",false);
        expectedContextMap.put("title", "EXPLICIT VALUE");
        expectedContextMap.put("jiraIssuesManager",jiraIssuesManager);
        expectedContextMap.put("entries",new MockChannel(params.get("url")).getChannelElement().getChildren("item"));
        expectedContextMap.put("xmlXformer",jiraIssuesMacro.getXmlXformer());
        expectedContextMap.put("clickableUrl", "http://localhost:8080/secure/IssueNavigator.jspa?reset=true&pid=10000&sorter/field=issuekey&sorter/order=ASC");
        expectedContextMap.put("jiraIssuesColumnManager", jiraIssuesColumnManager);
        expectedContextMap.put("isAdministrator", false);
        expectedContextMap.put("channel",new MockChannel(params.get("url")).getChannelElement());
        expectedContextMap.put("jiraIssuesDateFormatter",null);
        expectedContextMap.put("userLocale", Locale.getDefault());
        
        jiraIssuesMacro.createContextMapFromParams(params, macroVelocityContext, params.get("url"), JiraIssuesMacro.Type.URL, null, true, false, null);
        // comment back in to debug the assert equals on the two maps
        Set<String> keySet = expectedContextMap.keySet();
        for (String string : keySet)
        {
            if(expectedContextMap.get(string) != null) {
                if (!expectedContextMap.get(string).equals(macroVelocityContext.get(string)))
                    {
                        Object a = expectedContextMap.get(string);
                        Object b = macroVelocityContext.get(string);
                        int x = 0;
                    }
                } else {
                    if(macroVelocityContext.get(string) != null) {
                        int x = 0;
                    }
                }
        }

        /**
         * By definition the 2 List/Elements have cannot be equals
         * -not custom equal method is implemented for the Elements-
         * So, we just measure their length ( should be 0 )
         * and then we remove them to compare the rest of
         * the map.
         * 
         * Not very elegant....
         * TODO : Improve this TestCase.
         */

        cleanMaps(expectedContextMap,macroVelocityContext);

        assertEquals(expectedContextMap, macroVelocityContext);

        macroVelocityContext.clear();

        params.put("url", "http://localhost:8080/sr/jira.issueviews:searchrequest-xml/temp/SearchRequest.xml?pid=10000");
        params.put("title", "Some Random & Unlikely Issues");
        params.put("cache", "off");
        params.put("columns", "type,summary,key,reporter");
        params.put("height", "300");

        cols.add(new ColumnInfo("key", "key"));
        cols.add(new ColumnInfo("reporter", "reporter"));
        columnList.add("key");
        columnList.add("reporter");
        expectedContextMap.put("height", "300");
        expectedContextMap.put("clickableUrl", "http://localhost:8080/secure/IssueNavigator.jspa?reset=true&pid=10000");
        expectedContextMap.put("title", "Some Random &amp; Unlikely Issues");
        
        //Put back the 2 keys previously removed...
        expectedContextMap.put("entries",new MockChannel(params.get("url")).getChannelElement().getChildren("item"));
        expectedContextMap.put("channel",new MockChannel(params.get("url")).getChannelElement());

        when(jiraIssuesManager.retrieveXMLAsChannel(params.get("url"), columnList, null, true, false)).thenReturn(
                new MockChannel(params.get("url")));
        
        jiraIssuesMacro.createContextMapFromParams(params, macroVelocityContext, params.get("url"), JiraIssuesMacro.Type.URL, null, true, false, null);

        cleanMaps(expectedContextMap,macroVelocityContext);

        assertEquals(expectedContextMap, macroVelocityContext);
    }

    private void cleanMaps(Map<String,Object> expectedContext, Map<String, Object> velocityContext) throws Exception {
        if(velocityContext.containsKey("entries")){
            @SuppressWarnings("rawtypes")
            List velocityEntries = (List)velocityContext.get("entries");

            @SuppressWarnings("rawtypes")
            List expectedEntries = (List)expectedContext.get("entries");
            if(!(velocityEntries.size() == expectedEntries.size())){
                throw new Exception("Incorrect value for key ['entries']");
            }
        } else {
            throw new Exception("Missing key ['entries']");
        }
        
        if(velocityContext.containsKey("channel")){
            Element velocityChannel = (Element)velocityContext.get("channel");
            Element expectedChannel = (Element)expectedContext.get("channel");
            if(!(velocityChannel.getValue().equals(expectedChannel.getValue()))){
                throw new Exception("Incorect value for key ['channel']");
            }
        } else {
            throw new Exception("Missing key ['channel']");
        }
        
        expectedContext.remove("entries");
        velocityContext.remove("entries");
        expectedContext.remove("channel");
        velocityContext.remove("channel");
    }

    public void testCreateContextMapFromParamsUsesDisplayUrl() throws Exception
    {
        ApplicationLink appLink = mock(ApplicationLink.class);
        when(appLink.getRpcUrl()).thenReturn(URI.create("http://localhost:8080"));
        when(appLink.getDisplayUrl()).thenReturn(URI.create("http://displayurl.com"));

        params.put("key", "TEST-1");
        params.put("title", "EXPLICIT VALUE");

        Map<String, Object> expectedContextMap = new HashMap<String, Object>();
        expectedContextMap.put("clickableUrl", "http://displayurl.com/browse/TEST-1");
        expectedContextMap.put("columns",
                               ImmutableList.of(new ColumnInfo("type"), new ColumnInfo("key"), new ColumnInfo("summary"),
                                                new ColumnInfo("assignee"), new ColumnInfo("reporter"), new ColumnInfo("priority"),
                                                new ColumnInfo("status"), new ColumnInfo("resolution"), new ColumnInfo("created"),
                                                new ColumnInfo("updated"), new ColumnInfo("due")));
        expectedContextMap.put("title", "EXPLICIT VALUE");
        expectedContextMap.put("width", "100%");
        expectedContextMap.put("showTrustWarnings", false);
        expectedContextMap.put("isSourceApplink", true);
        expectedContextMap.put("isAdministrator", false);
        expectedContextMap.put("key", "TEST-1");
        expectedContextMap.put("applink", appLink);

        jiraIssuesManager = new DefaultJiraIssuesManager(jiraIssuesColumnManager, jiraIssuesUrlManager,httpRetrievalService, trustedTokenFactory, trustedConnectionStatusBuilder, new DefaultTrustedApplicationConfig());
        jiraIssuesMacro = new JiraIssuesMacro();
        jiraIssuesMacro.setPermissionManager(permissionManager);
        when(permissionManager.hasPermission((User) anyObject(), (Permission) anyObject(), anyObject())).thenReturn(false);
        jiraIssuesMacro.createContextMapFromParams(params, macroVelocityContext, params.get("key"), Type.KEY, appLink, false, false, null);

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
        final List<String> NO_WRAPPED_TEXT_FIELDS = Arrays.asList("key", "type", "priority", "status", "created", "updated", "due" );

        List<ColumnInfo> columnInfo = jiraIssuesMacro.getColumnInfo(jiraIssuesMacro.getColumnNames(null));
        
        for (ColumnInfo colInfo : columnInfo)
        {   
            boolean hasNowrap = colInfo.getHtmlClassName().contains(NOWRAP);
            if(NO_WRAPPED_TEXT_FIELDS.contains(colInfo.getKey()))
            {
                assertTrue("Non-wrapped columns should have nowrap class", hasNowrap);
            }
            else 
            {
                assertFalse("Wrapped columns should not have nowrap class (" + colInfo.getKey() + ", " + colInfo.getHtmlClassName() +")", hasNowrap);
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
    public void testBuildInfoRequestedWithCredentialsAndFilterUrls() throws Exception
    {
        params.put("anonymous", "true");
        params.put("url", "http://localhost:1990/jira/sr/jira.issueviews:searchrequest-xml/10000/SearchRequest-10000.xml?tempMax=1000&os_username=admin&os_password=admin");

        ApplicationLink appLink = mock(ApplicationLink.class);
        ApplicationLinkRequest request =  mock(ApplicationLinkRequest.class);

//        jiraIssuesManager = new DefaultJiraIssuesManager(jiraIssuesColumnManager, jiraIssuesUrlManager, httpRetrievalService, trustedTokenFactory, trustedConnectionStatusBuilder, new DefaultTrustedApplicationConfig());

        jiraIssuesMacro = new JiraIssuesMacro();
        jiraIssuesMacro.setPermissionManager(permissionManager);

        List<String> columnList = Lists.newArrayList("type", "key", "summary", "assignee", "reporter", "priority", "status", "resolution", "created", "updated", "due");

        when(appLink.getId()).thenReturn(new ApplicationId("8835b6b9-5676-3de4-ad59-bbe987416662"));
        when(appLink.getRpcUrl()).thenReturn(URI.create("http://localhost:1990/jira"));
        when(appLink.createAuthenticatedRequestFactory()).thenReturn(requestFactory);
        String requestUrl = appLink.getRpcUrl() + "/rest/api/2/filter/10000";
        when(requestFactory.createRequest(Request.MethodType.GET, requestUrl)).thenReturn(request);
        when(request.execute()).thenReturn("{\"jql\":\"status=open\"}");
        when(jiraIssuesManager.retrieveXMLAsChannel(params.get("url"), columnList, null, true, false)).thenReturn(
                new MockChannel(params.get("url")));
        when(jiraIssuesManager.retrieveJQLFromFilter("10000", appLink)).thenReturn("status=open");
        Settings settings = new Settings();
        settings.setBaseUrl("http://localhost:1990/confluence");
        when(settingsManager.getGlobalSettings()).thenReturn(settings);

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

        jiraIssuesMacro.createContextMapFromParams(params, macroVelocityContext, params.get("url"), JiraIssuesMacro.Type.URL, appLink, false, false, null);
    }

    /**
     * <a href="http://developer.atlassian.com/jira/browse/CONFJIRA-133">CONFJIRA_133</a>
     */
    public void testBuildInfoRequestedOverTrustedConnectionAndFilterUrls() throws Exception
    {
        params.put("anonymous", "false");
        params.put("url", "http://localhost:1990/jira/sr/jira.issueviews:searchrequest-xml/10000/SearchRequest-10000.xml?tempMax=1000");

//        jiraIssuesManager = new DefaultJiraIssuesManager(jiraIssuesColumnManager, jiraIssuesUrlManager, httpRetrievalService, trustedTokenFactory, trustedConnectionStatusBuilder, new DefaultTrustedApplicationConfig());

        ApplicationLink appLink = mock(ApplicationLink.class);
        ApplicationLinkRequest request =  mock(ApplicationLinkRequest.class);

        jiraIssuesMacro = new JiraIssuesMacro();
        jiraIssuesMacro.setPermissionManager(permissionManager);

        List<String> columnList = Lists.newArrayList("type", "key", "summary", "assignee", "reporter", "priority", "status", "resolution", "created", "updated", "due");

        when(appLink.getId()).thenReturn(new ApplicationId("8835b6b9-5676-3de4-ad59-bbe987416662"));
        when(appLink.getRpcUrl()).thenReturn(URI.create("http://localhost:1990/jira"));
        when(appLink.createAuthenticatedRequestFactory()).thenReturn(requestFactory);
        String requestUrl = appLink.getRpcUrl() + "/rest/api/2/filter/10000";
        when(requestFactory.createRequest(Request.MethodType.GET, requestUrl)).thenReturn(request);
        when(request.execute()).thenReturn("{\"jql\":\"status=open\"}");
        when(jiraIssuesManager.retrieveXMLAsChannel(params.get("url"), columnList, null, false, false)).thenReturn(
                new MockChannel(params.get("url")));
        when(jiraIssuesManager.retrieveJQLFromFilter("10000", appLink)).thenReturn("status=open");
        Settings settings = new Settings();
        settings.setBaseUrl("http://localhost:1990/confluence");
        when(settingsManager.getGlobalSettings()).thenReturn(settings);
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

        jiraIssuesMacro.createContextMapFromParams(params, macroVelocityContext, params.get("url"), JiraIssuesMacro.Type.URL, appLink, false, false, null);

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
        parseTest("project", "TST", "project=TST", JiraIssuesMacro.Type.JQL);
    }
    
    public void testJqlRequestParsingExplicit() throws MacroException, MacroExecutionException
    {
        parseTest("jqlQuery", "project = TST", null, JiraIssuesMacro.Type.JQL);
    }
    
    public void testSingleKeyRequestParsing() throws MacroException, MacroExecutionException
    {
        parseTest(JIRA_KEY_DEFAULT_PARAM, "TST-2", null, Type.KEY);
    }
    
    public void testSingleKeyRequestParsingExplicit() throws MacroException, MacroExecutionException
    {
        parseTest("key", "CONF-1234", null, Type.KEY);
    }
    
    public void testMultiKeyRequestParsing() throws MacroException, MacroExecutionException
    {
        String keys = "TST-1, CONF-1234, TST-5";
        parseTest(JIRA_KEY_DEFAULT_PARAM, keys, "issuekey in (" + keys + ")", Type.JQL);
    }
    
    public void testMultKeyRequestParsingExplicit() throws MacroException, MacroExecutionException
    {
        String keys = "TST-1, CONF-1234, TST-5";
        parseTest("key", keys, "issuekey in (" + keys + ")", Type.JQL);
    }
    
    public void testUrlRequestParsing() throws MacroException, MacroExecutionException
    {
        parseTest(JIRA_KEY_DEFAULT_PARAM, "http://jira.atlassian.com/sr/search.xml", null, Type.URL);
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
            assertEquals("jiraissues.error.invalidMacroFormat", e.getMessage());
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

    public void testGetTokenTypeFromString () {
        TokenType result;
        TokenType testVals[] = TokenType.values();

        for(TokenType val : testVals) {
            params.clear();
            params.put(": = | TOKEN_TYPE | = :", val.toString());
            result = jiraIssuesMacro.getTokenType(params, null, null);
            assertEquals(result, val);
        }

        params.clear();
        params.put(": = | TOKEN_TYPE | = :", "Whoops");
        result = jiraIssuesMacro.getTokenType(params, null, null);
        assertEquals(result, TokenType.INLINE_BLOCK);

        params.clear();
        params.put(": = | TOKEN_TYPE | = :", null);
        result = jiraIssuesMacro.getTokenType(params, null, null);
        assertEquals(result, TokenType.INLINE_BLOCK);
    }

    private class JiraIssuesMacro extends com.atlassian.confluence.extra.jira.JiraIssuesMacro
    {
        private JiraIssuesMacro()
        {
            setI18NBeanFactory(i18NBeanFactory);
            setJiraIssuesColumnManager(jiraIssuesColumnManager);
            setJiraIssuesManager(jiraIssuesManager);
            setWebResourceManager(webResourceManager);
            setSettingsManager(settingsManager);
        }
    }
    
    private class MockChannel extends Channel {
    	protected MockChannel(String sourceURL) {
    		super(sourceURL,null,null);
    	}

        @Override
        public String getSourceUrl() {
            return super.getSourceUrl();
        }

        @Override
            public Element getChannelElement() {
            Element root = new Element("root");
            root.addContent(new Element("issue"));
            root.addContent(new Element("item"));
            return root;
        }
        
        @Override
        public TrustedConnectionStatus getTrustedConnectionStatus() {
            return super.getTrustedConnectionStatus();
        }
        
        @Override
        public boolean isTrustedConnection() {
            return super.isTrustedConnection();
        }
    }
}