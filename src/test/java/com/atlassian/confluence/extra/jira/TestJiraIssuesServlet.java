package com.atlassian.confluence.extra.jira;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collection;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.cache.Cache;
import com.atlassian.cache.CacheManager;
import com.atlassian.cache.memory.MemoryCache;
import com.atlassian.confluence.extra.jira.cache.CacheKey;

import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import junit.framework.TestCase;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyObject;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TestJiraIssuesServlet extends TestCase
{
    @Mock private CacheManager cacheManager;

    @Mock private JiraIssuesManager jiraIssuesManager;

    @Mock private FlexigridResponseGenerator jiraIssuesResponseGenerator;

    @Mock private JiraIssuesColumnManager jiraIssuesColumnManager;

    private JiraIssuesUrlManager jiraIssuesUrlManager;

    @Mock private HttpServletRequest httpServletRequest;

    @Mock private HttpServletResponse httpServletResponse;

    private JiraIssuesServlet jiraIssuesServlet;

    private String url;

    private String[] columnNames;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();

        MockitoAnnotations.initMocks(this);

        url = "http://developer.atlassian.com/jira/sr/jira.issueviews:searchrequest-xml/temp/SearchRequest.xml?type=1&pid=10675&status=1&sorter/field=issuekey&sorter/order=DESC&tempMax=1000";

        columnNames = new String[] { "key" };

        when(httpServletRequest.getParameterValues("columns")).thenReturn(columnNames);
        when(httpServletRequest.getParameter("showCount")).thenReturn("false");
        when(httpServletRequest.getParameter("forceAnonymous")).thenReturn("false");
        when(httpServletRequest.getParameter("useCache")).thenReturn("true");
        when(httpServletRequest.getParameter("url")).thenReturn(url);
        when(httpServletRequest.getParameter("rp")).thenReturn("10");
        when(httpServletRequest.getParameter("page")).thenReturn("1");
        when(httpServletRequest.getParameter("flexigrid")).thenReturn("true");

        when(cacheManager.getCache(JiraIssuesMacro.class.getName())).thenReturn(new MemoryCache(com.atlassian.confluence.extra.jira.JiraIssuesServlet.class.getName()));

        jiraIssuesUrlManager = new DefaultJiraIssuesUrlManager(jiraIssuesColumnManager);

        jiraIssuesServlet = new JiraIssuesServlet();
    }
    
    public void testJsonResponseCached() throws IOException
    {
        StringWriter firstWrite = new StringWriter();
        StringWriter secondWrite = new StringWriter();

        when(jiraIssuesResponseGenerator.generate(
                (JiraIssuesManager.Channel) anyObject(),
                eq(Arrays.asList(columnNames)),
                eq(1),
                eq(false),
                any(ApplicationLink.class)
        )).thenReturn("foobarbaz");

        when(httpServletResponse.getWriter()).thenReturn(new PrintWriter(firstWrite)).thenReturn(new PrintWriter(secondWrite));

        jiraIssuesServlet.doGet(httpServletRequest, httpServletResponse);
        jiraIssuesServlet.doGet(httpServletRequest, httpServletResponse);

        verify(jiraIssuesResponseGenerator, times(1)).generate(
                (JiraIssuesManager.Channel) anyObject(),
                isA(Collection.class),
                anyInt(),
                anyBoolean(),
                any(ApplicationLink.class));

        assertEquals("foobarbaz", firstWrite.toString());
        assertEquals("foobarbaz", secondWrite.toString());
    }

    public void testJsonResultsCachedByPage() throws IOException
    {
        StringWriter firstWrite = new StringWriter();
        StringWriter secondWrite = new StringWriter();

        when(jiraIssuesResponseGenerator.generate(
                (JiraIssuesManager.Channel) anyObject(),
                eq(Arrays.asList(columnNames)),
                eq(1),
                eq(false),
                any(ApplicationLink.class)
                )).thenReturn("foobarbaz");
        when(jiraIssuesResponseGenerator.generate(
                (JiraIssuesManager.Channel) anyObject(),
                eq(Arrays.asList(columnNames)),
                eq(2),
                eq(false),
                any(ApplicationLink.class)
        )).thenReturn("foobarbaz2");

        when(httpServletResponse.getWriter()).thenReturn(new PrintWriter(firstWrite)).thenReturn(new PrintWriter(secondWrite));
        when(httpServletRequest.getParameter("page")).thenReturn("1").thenReturn("2");

        jiraIssuesServlet.doGet(httpServletRequest, httpServletResponse);


        jiraIssuesServlet.doGet(httpServletRequest, httpServletResponse);

        verify(jiraIssuesResponseGenerator, times(2)).generate(
                (JiraIssuesManager.Channel) anyObject(),
                isA(Collection.class),
                anyInt(),
                anyBoolean(),
                any(ApplicationLink.class));

        assertEquals("foobarbaz", firstWrite.toString());
        assertEquals("foobarbaz2", secondWrite.toString());
    }

    public void testJsonResultsCacheNotUsedWhenUseCacheIsFalse() throws IOException
    {
        StringWriter firstWrite = new StringWriter();
        StringWriter secondWrite = new StringWriter();

        when(jiraIssuesResponseGenerator.generate(
                (JiraIssuesManager.Channel) anyObject(),
                eq(Arrays.asList(columnNames)),
                eq(1),
                eq(false),
                any(ApplicationLink.class)
        )).thenReturn("foobarbaz");

        when(httpServletResponse.getWriter()).thenReturn(new PrintWriter(firstWrite)).thenReturn(new PrintWriter(secondWrite));
        when(httpServletRequest.getParameter("useCache")).thenReturn("false");

        jiraIssuesServlet.doGet(httpServletRequest, httpServletResponse);


        jiraIssuesServlet.doGet(httpServletRequest, httpServletResponse);

        verify(jiraIssuesResponseGenerator, times(2)).generate(
                (JiraIssuesManager.Channel) anyObject(),
                isA(Collection.class),
                anyInt(),
                anyBoolean(),
                any(ApplicationLink.class));

        assertEquals("foobarbaz", firstWrite.toString());
        assertEquals("foobarbaz", secondWrite.toString());
    }

    public void testCachedResponseDiscardedIfItWasStoredByAnOlderVersionOfThePlugin() throws IOException
    {
        StringWriter firstWriter = new StringWriter();
        Cache cache = mock(Cache.class);

        when(jiraIssuesResponseGenerator.generate(
                (JiraIssuesManager.Channel) anyObject(),
                eq(Arrays.asList(columnNames)),
                eq(1),
                eq(false),
                any(ApplicationLink.class)
        )).thenReturn("foobarbaz");

        when(httpServletResponse.getWriter()).thenReturn(new PrintWriter(firstWriter));
        when(cacheManager.getCache(JiraIssuesMacro.class.getName())).thenReturn(cache);
        when(cache.get(anyObject())).thenReturn("Not a CacheKey object to generate a ClassCastException");

        jiraIssuesServlet.doGet(httpServletRequest, httpServletResponse);

        verify(jiraIssuesResponseGenerator).generate(
                (JiraIssuesManager.Channel) anyObject(),
                isA(Collection.class),
                anyInt(),
                anyBoolean(),
                any(ApplicationLink.class));

        ArgumentCaptor<CacheKey> cacheKey = ArgumentCaptor.forClass(CacheKey.class);
        verify(cache).remove(cacheKey.capture());

        assertEquals("http://developer.atlassian.com/jira/sr/jira.issueviews:searchrequest-xml/temp/SearchRequest.xml?type=1&pid=10675&status=1&sorter/field=issuekey&sorter/order=DESC&tempMax=10", cacheKey.getValue().getPartialUrl());
        assertEquals("foobarbaz", firstWriter.toString());
    }

    private class JiraIssuesServlet extends com.atlassian.confluence.extra.jira.JiraIssuesServlet
    {
        private JiraIssuesServlet()
        {
            setCacheManager(cacheManager);
            setJiraIssuesManager(jiraIssuesManager);
            setJiraIssuesResponseGenerator(jiraIssuesResponseGenerator);
            setJiraIssuesUrlManager(jiraIssuesUrlManager);
        }
    }
}