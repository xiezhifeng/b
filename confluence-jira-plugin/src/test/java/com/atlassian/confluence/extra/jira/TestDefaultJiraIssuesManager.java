package com.atlassian.confluence.extra.jira;

import com.atlassian.applinks.api.ApplicationLinkService;
import com.atlassian.applinks.api.CredentialsRequiredException;
import com.atlassian.confluence.extra.jira.exception.AuthenticationException;
import com.atlassian.confluence.extra.jira.exception.MalformedRequestException;
import com.atlassian.confluence.util.http.HttpRequest;
import com.atlassian.confluence.util.http.HttpResponse;
import junit.framework.TestCase;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.io.IOUtils;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.*;
import org.mockito.MockitoAnnotations;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Document;
import org.jdom.xpath.XPath;
import org.jdom.input.SAXBuilder;
import org.w3c.dom.DOMException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.io.IOException;
import java.io.InputStream;

import com.atlassian.confluence.security.trust.TrustedTokenFactory;
import com.atlassian.confluence.util.http.trust.TrustedConnectionStatusBuilder;
import com.atlassian.confluence.util.http.HttpRetrievalService;
import com.atlassian.sal.api.net.ResponseException;
import com.google.common.collect.Lists;

import javax.servlet.http.HttpServletResponse;

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

    private String url;

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

}
