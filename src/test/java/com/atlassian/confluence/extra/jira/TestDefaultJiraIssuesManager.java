package com.atlassian.confluence.extra.jira;

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

import javax.servlet.http.HttpServletResponse;

public class TestDefaultJiraIssuesManager extends TestCase
{
    @Mock private JiraIssuesSettingsManager jiraIssuesSettingsManager;

    @Mock private JiraIssuesColumnManager jiraIssuesColumnManager;

    private JiraIssuesUrlManager jiraIssuesUrlManager;

    @Mock private JiraIssuesIconMappingManager jiraIssuesIconMappingManager;

    @Mock private TrustedTokenFactory trustedTokenFactory;

    @Mock private TrustedConnectionStatusBuilder trustedConnectionStatusBuilder;

    @Mock private HttpRetrievalService httpRetrievalService;

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

    public void testSortDisabledIfSettingsSaySo() throws IOException
    {
        when(jiraIssuesSettingsManager.getSort(urlWithoutQueryString)).thenReturn(JiraIssuesSettingsManager.Sort.SORT_DISABLED);
        assertFalse(defaultJiraIssuesManager.isSortEnabled(url, false));
    }

    public void testSortEnabledIfSettingsSaySo() throws IOException
    {
        when(jiraIssuesSettingsManager.getSort(urlWithoutQueryString)).thenReturn(JiraIssuesSettingsManager.Sort.SORT_ENABLED);
        assertTrue(defaultJiraIssuesManager.isSortEnabled(url, false));
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

    public void testSortStatusFiguredOutAutomaticallyAndPersistedIfSettingsIsClueless() throws IOException, JDOMException
    {
        when(jiraIssuesSettingsManager.getSort(urlWithoutQueryString)).thenReturn(JiraIssuesSettingsManager.Sort.SORT_UNKNOWN);
        assertTrue(
                new DefaultJiraIssuesManager()
                {
                    @Override
                    public Channel retrieveXML(String url, boolean useTrustedConnection) throws IOException
                    {
                        try
                        {
                            return new Channel(
                                    url,
                                    getJiraIssuesXmlResponseChannelElement("jiraResponseWithoutIssues.xml"),
                                    null);
                        }
                        catch (JDOMException de)
                        {
                            fail("Test data corrupted. See jiraResponseWithoutIssues.xml");
                            throw new IOException("Just to get out of this method. Blame me.");
                        }
                    }
                }.isSortEnabled(url, false)
        );

        verify(jiraIssuesSettingsManager).setSort(urlWithoutQueryString, JiraIssuesSettingsManager.Sort.SORT_ENABLED);
    }

    public void testSortNotEnabledForVeryOldJiraInstances() throws IOException, JDOMException
    {
        when(jiraIssuesSettingsManager.getSort(urlWithoutQueryString)).thenReturn(JiraIssuesSettingsManager.Sort.SORT_UNKNOWN).thenReturn(
                JiraIssuesSettingsManager.Sort.SORT_DISABLED
        );
        
        assertFalse(
                new DefaultJiraIssuesManager()
                {
                    @Override
                    public Channel retrieveXML(String url, boolean useTrustedConnection) throws IOException
                    {
                        try
                        {
                            return new Channel(
                                    url,
                                    getJiraIssuesXmlResponseChannelElement("oldJiraResponseWithoutIssues.xml"),
                                    null);
                        }
                        catch (JDOMException de)
                        {
                            fail("Test data corrupted. See jiraResponseWithoutIssues.xml");
                            throw new IOException("Just to get out of this method. Blame me.");
                        }
                    }
                }.isSortEnabled(url, false)
        );

        verify(jiraIssuesSettingsManager).setSort(urlWithoutQueryString, JiraIssuesSettingsManager.Sort.SORT_DISABLED);
    }

    public void testIconMapFromJiraIssuesIconManager()
    {
        Map<String, String> iconMap = new HashMap<String, String>();
        Element itemElem = mock(Element.class);
        Element linkElem = mock(Element.class);
        String link = "http://developer.atlassian.com/jira/browse/CONFJIRA-92";

        when(itemElem.getChild("link")).thenReturn(linkElem);
        when(linkElem.getValue()).thenReturn(link);
        when(jiraIssuesIconMappingManager.getFullIconMapping(link)).thenReturn(iconMap);

        assertSame(iconMap, defaultJiraIssuesManager.getIconMap(itemElem));
    }

    /**
     * Tests that the correct exceptions are thrown by {@link DefaultJiraIssuesManager#retrieveXML(String, boolean)}
     * @see #testCorrectExceptionsThrown(int, String)
     */
    public void testCorrectExceptionsThrown() throws IOException
    {
        when(httpRetrievalService.get((HttpRequest) any())).thenReturn(httpResponse);
        when(httpResponse.isFailed()).thenReturn(true);

        testCorrectExceptionsThrown(HttpServletResponse.SC_BAD_REQUEST,"Expected a MalformedRequestException");
        testCorrectExceptionsThrown(HttpServletResponse.SC_UNAUTHORIZED,"Expected an AuthenticationException");
    }

    private void testCorrectExceptionsThrown(int statusCode, String errorMessage)
    {
        when(httpResponse.getStatusCode()).thenReturn(statusCode);
        try
        {
            defaultJiraIssuesManager.retrieveXML("foo", false);
        }
        catch (IOException e)
        {
            // find out if the correct exceptions is thrown
            if(!isMatchingExceptionType(e,statusCode))
                fail(errorMessage);
        }
    }

    /**
     * Finds out if IOException has a matching Http error code.
     *
     * @param e Exception thrown by JIRA
     * @param statusCode Http status code
     * @return true if the IOException matches with the Http error code
     */
    private boolean isMatchingExceptionType(IOException e, int statusCode)
    {
        boolean matched=false;

        // this block finds out if IOException has a matching Http error code
        switch(statusCode)
        {
            case HttpServletResponse.SC_BAD_REQUEST:  // expects a http error 400
                if (e instanceof MalformedRequestException)
                    matched = true;
                break;
            case HttpServletResponse.SC_UNAUTHORIZED: // expects a 401
                if (e instanceof AuthenticationException)
                    matched=true;
                break;
        }
        return matched;
    }

    private class DefaultJiraIssuesManager extends com.atlassian.confluence.extra.jira.DefaultJiraIssuesManager
    {
        private DefaultJiraIssuesManager()
        {
            super(jiraIssuesSettingsManager, jiraIssuesColumnManager, jiraIssuesUrlManager, jiraIssuesIconMappingManager, trustedTokenFactory, trustedConnectionStatusBuilder, httpRetrievalService, "org.apache.xerces.parsers.SAXParser");
        }
    }

}
