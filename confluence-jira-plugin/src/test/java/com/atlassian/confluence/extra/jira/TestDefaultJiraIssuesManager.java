package com.atlassian.confluence.extra.jira;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import junit.framework.TestCase;

import org.apache.commons.io.IOUtils;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.xpath.XPath;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.applinks.api.ApplicationLinkRequest;
import com.atlassian.applinks.api.ApplicationLinkRequestFactory;
import com.atlassian.applinks.api.ApplicationLinkService;
import com.atlassian.applinks.api.CredentialsRequiredException;
import com.atlassian.confluence.extra.jira.exception.AuthenticationException;
import com.atlassian.confluence.extra.jira.exception.MalformedRequestException;
import com.atlassian.confluence.security.trust.TrustedTokenFactory;
import com.atlassian.confluence.util.http.HttpRequest;
import com.atlassian.confluence.util.http.HttpResponse;
import com.atlassian.confluence.util.http.HttpRetrievalService;
import com.atlassian.confluence.util.http.trust.TrustedConnectionStatusBuilder;
import com.atlassian.sal.api.net.Request;
import com.atlassian.sal.api.net.ResponseException;
import com.google.common.collect.Lists;

public class TestDefaultJiraIssuesManager extends TestCase
{
    @Mock private JiraIssuesSettingsManager jiraIssuesSettingsManager;

    @Mock private JiraIssuesColumnManager jiraIssuesColumnManager;

    private JiraIssuesUrlManager jiraIssuesUrlManager;
    
    @Mock private ProjectKeyCache projectKeyCache;


    @Mock private TrustedTokenFactory trustedTokenFactory;

    @Mock private TrustedConnectionStatusBuilder trustedConnectionStatusBuilder;

    @Mock private HttpRetrievalService httpRetrievalService;
    
    @Mock private ApplicationLinkService appLinkService;

    @Mock private HttpResponse httpResponse;

    @Mock private ApplicationLink appLink;

    @Mock private ApplicationLinkRequestFactory appLinkRequestFactory; 

    @Mock private ApplicationLinkRequest appLinkRequest;

    private String url;

    private String jsonResponse;

    private URI rpcUrl;

    private String urlWithoutQueryString;

    private DefaultJiraIssuesManager defaultJiraIssuesManager;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        MockitoAnnotations.initMocks(this);

        jiraIssuesUrlManager = new DefaultJiraIssuesUrlManager(jiraIssuesColumnManager);

        url = "http://developer.atlassian.com/jira/sr/jira.issueviews:searchrequest-xml/temp/SearchRequest.xml?type=1&pid=10675&status=1&sorter/field=issuekey&sorter/order=DESC&tempMax=1000";
        urlWithoutQueryString = "http://developer.atlassian.com/jira/sr/jira.issueviews:searchrequest-xml/temp/SearchRequest.xml";

        jsonResponse = "{\"self\": \"http://www.example.com/jira/rest/api/2/filter/10000\",\"id\": \"10000\",\"name\": \"All Open Bugs\",\"description\": \"Lists all open bugs\",\"jql\": \"type = Bug and resolution is empty\"}";
        rpcUrl = new URI("http://www.example.com/jira");

        defaultJiraIssuesManager = new DefaultJiraIssuesManager();
    }
    
    public void testColumnsForURL()
    {
        ArrayList<String> columns = Lists.newArrayList("Summary", "Type");
        when(jiraIssuesColumnManager.getCanonicalFormOfBuiltInField("Summary")).thenReturn("summary");
        when(jiraIssuesColumnManager.getCanonicalFormOfBuiltInField("Type")).thenReturn("type");
        when(jiraIssuesColumnManager.isColumnBuiltIn("type")).thenReturn(false);
        when(jiraIssuesColumnManager.isColumnBuiltIn("summary")).thenReturn(false);

        String fieldRestrictedUrl = defaultJiraIssuesManager.getFieldRestrictedUrl(columns, "http://test.com?nomatter");
        assertTrue(fieldRestrictedUrl.contains("field=summary"));
        assertFalse(fieldRestrictedUrl.contains("field=Summary"));
        assertTrue(fieldRestrictedUrl.contains("field=type"));
        assertFalse(fieldRestrictedUrl.contains("field=Type"));
    }

    public void testGetColumnMapFromJiraIssuesColumnManager()
    {
        Map<String, String> columnMap = new HashMap<String, String>();

        when(jiraIssuesColumnManager.getColumnMap(urlWithoutQueryString)).thenReturn(columnMap);
        assertSame(columnMap, defaultJiraIssuesManager.getColumnMap(url));
    }

    public void testSetColumnMapToJiraIssuesColumnManager()
    {
        Map<String, String> columnMap = new HashMap<String, String>();


        defaultJiraIssuesManager.setColumnMap(url, columnMap);
        verify(jiraIssuesColumnManager).setColumnMap(urlWithoutQueryString, columnMap);
    }


    private Element getJiraIssuesXmlResponseChannelElement(String classpathResource) throws IOException, JDOMException
    {
        InputStream in = null;

        try
        {
            in = getClass().getClassLoader().getResourceAsStream(classpathResource);

            SAXBuilder saxBuilder = new SAXBuilder("org.apache.xerces.parsers.SAXParser");
            Document document = saxBuilder.build(in);

            return (Element) XPath.selectSingleNode(document, "/rss//channel");
        }
        finally
        {
            IOUtils.closeQuietly(in);
        }
    }

     /**
     * Tests that MalforedRequestException is thrown by {@link DefaultJiraIssuesManager#retrieveXML(String, boolean)}
     * @throws ResponseException 
     * @throws CredentialsRequiredException 
     */
    public void testMalformedRequestExceptionThrown() throws IOException, CredentialsRequiredException, ResponseException
    {
        when(httpRetrievalService.get((HttpRequest) any())).thenReturn(httpResponse);
        when(httpResponse.isFailed()).thenReturn(true);
        when(httpResponse.getStatusCode()).thenReturn(HttpServletResponse.SC_BAD_REQUEST);
        try
        {
            defaultJiraIssuesManager.retrieveXMLAsChannel(url, new ArrayList<String>(), null, true, false);
            fail("Expected a MalformedRequestException");
        }
        catch (MalformedRequestException mre)
        { // ExpectedException
        }
    }

    /**
     * Tests that Authenticationexception is thrown by {@link DefaultJiraIssuesManager#retrieveXML(String, boolean)}
     * @throws ResponseException 
     * @throws CredentialsRequiredException 
     */
    public void testAuthenticationExceptionThrown() throws IOException, CredentialsRequiredException, ResponseException
    {
        when(httpRetrievalService.get((HttpRequest) any())).thenReturn(httpResponse);
        when(httpResponse.isFailed()).thenReturn(true);
        when(httpResponse.getStatusCode()).thenReturn(HttpServletResponse.SC_UNAUTHORIZED);
        try
        {
            defaultJiraIssuesManager.retrieveXMLAsChannel(url, new ArrayList<String>(), null, true, false);
            fail("Expected an AuthenticationException");
        }
        catch (AuthenticationException mre)
        { // ExpectedException
        }
    }

    private class DefaultJiraIssuesManager extends com.atlassian.confluence.extra.jira.DefaultJiraIssuesManager
    {
        private DefaultJiraIssuesManager()
        {
            super(jiraIssuesColumnManager, jiraIssuesUrlManager, httpRetrievalService, trustedTokenFactory, trustedConnectionStatusBuilder, new DefaultTrustedApplicationConfig());
        }
    }

    public void testCheckFilterId() throws CredentialsRequiredException, ResponseException {
        String filterId = "10000";
        String restUrl = "http://www.example.com/jira/rest/api/2/filter/10000";

        when(appLink.getRpcUrl()).thenReturn(rpcUrl);
        when(appLink.createAuthenticatedRequestFactory()).thenReturn(appLinkRequestFactory);
        when(appLinkRequestFactory.createRequest(Request.MethodType.GET, restUrl)).thenReturn(appLinkRequest);
        when(appLinkRequest.execute()).thenReturn(jsonResponse);

        assertEquals(filterId, defaultJiraIssuesManager.checkFilterId(filterId, appLink));
    }

    public void testRetrieveJQLFromFilter() throws CredentialsRequiredException, ResponseException {
        String filterId = "10000";
        String restUrl = "http://www.example.com/jira/rest/api/2/filter/10000";

        when(appLink.getRpcUrl()).thenReturn(rpcUrl);
        when(appLink.createAuthenticatedRequestFactory()).thenReturn(appLinkRequestFactory);
        when(appLinkRequestFactory.createRequest(Request.MethodType.GET, restUrl)).thenReturn(appLinkRequest);
        when(appLinkRequest.execute()).thenReturn(jsonResponse);

        assertEquals("type = Bug and resolution is empty", defaultJiraIssuesManager.retrieveJQLFromFilter(filterId, appLink));
    }

}
