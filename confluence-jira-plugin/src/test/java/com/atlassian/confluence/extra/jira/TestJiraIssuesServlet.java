package com.atlassian.confluence.extra.jira;

import com.atlassian.applinks.api.ApplicationId;
import com.atlassian.applinks.api.ReadOnlyApplicationLink;
import com.atlassian.applinks.api.ReadOnlyApplicationLinkService;
import com.atlassian.confluence.extra.jira.cache.CacheKey;
import com.atlassian.confluence.extra.jira.cache.CompressingStringCache;
import com.atlassian.confluence.extra.jira.cache.JIMCacheProvider;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.vcache.DirectExternalCache;
import com.atlassian.vcache.PutPolicy;
import com.atlassian.vcache.VCacheFactory;
import com.atlassian.vcache.internal.core.ThreadLocalRequestContextSupplier;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import static com.atlassian.confluence.extra.jira.cache.CacheKeyTestHelper.getPluginVersionExpectations;
import static com.atlassian.vcache.VCacheUtils.join;
import static com.atlassian.vcache.internal.test.utils.VCacheTestHelper.getExternalCache;
import static com.atlassian.vcache.internal.test.utils.VCacheTestHelper.getFactory;
import static org.junit.Assert.assertEquals;
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

@RunWith(MockitoJUnitRunner.class)
public class TestJiraIssuesServlet
{
    private static final String PLUGIN_VERSION = "6.0.0";
    private static final String OLD_PLUGIN_VERSION = "5.0.0";
    public static final ThreadLocalRequestContextSupplier CONTEXT_SUPPLIER = ThreadLocalRequestContextSupplier.strictSupplier();

    @Mock private JiraIssuesManager jiraIssuesManager;

    @Mock private FlexigridResponseGenerator jiraIssuesResponseGenerator;

    @Mock private JiraIssuesColumnManager jiraIssuesColumnManager;

    private JiraIssuesUrlManager jiraIssuesUrlManager;

    @Mock private HttpServletRequest httpServletRequest;

    @Mock private HttpServletResponse httpServletResponse;

    @Mock private ReadOnlyApplicationLinkService readOnlyApplicationLinkService;

    @Mock private PluginAccessor pluginAccessor;
    private VCacheFactory vcacheFactory;
    private DirectExternalCache<CompressingStringCache> cache;

    private JiraIssuesServlet jiraIssuesServlet;

    private String url;

    private String[] columnNames;

    @BeforeClass
    public static void initThread()
    {
        CONTEXT_SUPPLIER.initThread("myPartition");
    }

    @Before
    public void setUp() throws Exception
    {
        getPluginVersionExpectations(pluginAccessor, PLUGIN_VERSION);
        vcacheFactory = Mockito.spy(getFactory(CONTEXT_SUPPLIER));
        cache = getExternalCache(vcacheFactory, JIMCacheProvider.JIM_CACHE_NAME, CompressingStringCache.class);

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

        jiraIssuesUrlManager = new DefaultJiraIssuesUrlManager(jiraIssuesColumnManager);

        jiraIssuesServlet = new JiraIssuesServlet();
    }

    @Test
    public void testJsonResponseCached() throws IOException
    {
        StringWriter firstWrite = new StringWriter();
        StringWriter secondWrite = new StringWriter();

        when(jiraIssuesResponseGenerator.generate(
                (JiraIssuesManager.Channel) anyObject(),
                eq(Arrays.asList(columnNames)),
                eq(1),
                eq(false),
                eq(false)
        )).thenReturn("foobarbaz");

        when(httpServletResponse.getWriter()).thenReturn(new PrintWriter(firstWrite)).thenReturn(new PrintWriter(secondWrite));

        jiraIssuesServlet.doGet(httpServletRequest, httpServletResponse);
        jiraIssuesServlet.doGet(httpServletRequest, httpServletResponse);

        verify(jiraIssuesResponseGenerator, times(1)).generate(
                (JiraIssuesManager.Channel) anyObject(),
                isA(Collection.class),
                anyInt(),
                anyBoolean(),
                anyBoolean());

        assertEquals("foobarbaz", firstWrite.toString());
        assertEquals("foobarbaz", secondWrite.toString());
    }

    public void testContentIsSetAsAttachment() throws IOException
    {
        StringWriter firstWrite = new StringWriter();

        when(jiraIssuesResponseGenerator.generate(
                (JiraIssuesManager.Channel) anyObject(),
                eq(Arrays.asList(columnNames)),
                eq(1),
                eq(false),
                eq(false)
        )).thenReturn("foobarbaz");

        Map<String, String> headers = new HashMap<>();
        when(httpServletResponse.getWriter()).thenReturn(new PrintWriter(firstWrite));

        jiraIssuesServlet.doGet(httpServletRequest, httpServletResponse);

        verify(httpServletResponse).setHeader("Content-Disposition", "attachment");
        assertEquals("foobarbaz", firstWrite.toString());
    }

    @Test
    public void testJsonResultsCachedByPage() throws IOException
    {
        StringWriter firstWrite = new StringWriter();
        StringWriter secondWrite = new StringWriter();

        when(jiraIssuesResponseGenerator.generate(
                (JiraIssuesManager.Channel) anyObject(),
                eq(Arrays.asList(columnNames)),
                eq(1),
                eq(false),
                eq(false)
        )).thenReturn("foobarbaz");
        when(jiraIssuesResponseGenerator.generate(
                (JiraIssuesManager.Channel) anyObject(),
                eq(Arrays.asList(columnNames)),
                eq(2),
                eq(false),
                eq(false)
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
                anyBoolean());

        assertEquals("foobarbaz", firstWrite.toString());
        assertEquals("foobarbaz2", secondWrite.toString());
    }

    @Test
    public void testJsonResultsCacheNotUsedWhenUseCacheIsFalse() throws IOException
    {
        StringWriter firstWrite = new StringWriter();
        StringWriter secondWrite = new StringWriter();

        when(jiraIssuesResponseGenerator.generate(
                (JiraIssuesManager.Channel) anyObject(),
                eq(Arrays.asList(columnNames)),
                eq(1),
                eq(false),
                eq(false)
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
                anyBoolean());

        assertEquals("foobarbaz", firstWrite.toString());
        assertEquals("foobarbaz", secondWrite.toString());
    }

    @Test
    public void testCachedResponseDiscardedIfItWasStoredByAnOlderVersionOfThePlugin() throws IOException
    {
        StringWriter firstWriter = new StringWriter();

        when(jiraIssuesResponseGenerator.generate(
                (JiraIssuesManager.Channel) anyObject(),
                eq(Arrays.asList(columnNames)),
                eq(1),
                eq(false),
                eq(false)
        )).thenReturn("foobarbaz");

        when(httpServletResponse.getWriter()).thenReturn(new PrintWriter(firstWriter));
        String trimmedUrl = "http://developer.atlassian.com/jira/sr/jira"
                + ".issueviews:searchrequest-xml/temp/SearchRequest.xml?type=1&pid=10675&status=1&sorter/field=issuekey&sorter/order=DESC&tempMax=10";
        CacheKey oldKey = new CacheKey(trimmedUrl, null, Arrays.asList(columnNames), false, false, true, true,
                OLD_PLUGIN_VERSION);
        CacheKey newKey = new CacheKey(trimmedUrl, null, Arrays.asList(columnNames), false, false, true, true,
                PLUGIN_VERSION);
        join(cache.put(oldKey.toKey(), new CompressingStringCache(new ConcurrentHashMap()), PutPolicy.PUT_ALWAYS));

        jiraIssuesServlet.doGet(httpServletRequest, httpServletResponse);

        verify(jiraIssuesResponseGenerator).generate(
                (JiraIssuesManager.Channel) anyObject(),
                isA(Collection.class),
                anyInt(),
                anyBoolean(),
                anyBoolean());

        ArgumentCaptor<String> cacheKey = ArgumentCaptor.forClass(String.class);
        verify(cache).get(cacheKey.capture(), any(Supplier.class));

        assertEquals(newKey.toKey(), cacheKey.getValue());
        assertEquals("foobarbaz", firstWriter.toString());
    }

    // If applink rebase url to displayURL.
    @Test
    public void testRebaseLinksToDisplayURLIfAppLink() throws Exception
    {
        String rpcUrl = "http://localhost:8080/jira";
        String displayUrl = "http://publicurl/jira";

        StringWriter firstWriter = new StringWriter();
        when(httpServletResponse.getWriter()).thenReturn(new PrintWriter(firstWriter));

        when(httpServletRequest.getParameter("appId")).thenReturn(UUID.randomUUID().toString());
        ReadOnlyApplicationLink applicationLink = mock(ReadOnlyApplicationLink.class);
        when(applicationLink.getRpcUrl()).thenReturn(URI.create(rpcUrl));
        when(applicationLink.getDisplayUrl()).thenReturn(URI.create(displayUrl));
        when(readOnlyApplicationLinkService.getApplicationLink(any(ApplicationId.class))).thenReturn(applicationLink);
        when(httpServletRequest.getParameter("useCache")).thenReturn("false");

        when(jiraIssuesResponseGenerator.generate(
                (JiraIssuesManager.Channel) anyObject(),
                eq(Arrays.asList(columnNames)),
                eq(1),
                eq(false),
                eq(true)
        )).thenReturn(rpcUrl);

        jiraIssuesServlet.doGet(httpServletRequest, httpServletResponse);

        assertEquals(displayUrl, firstWriter.toString());
    }

    private class JiraIssuesServlet extends com.atlassian.confluence.extra.jira.JiraIssuesServlet
    {
        private JiraIssuesServlet()
        {
            setPluginAccessor(pluginAccessor);
            setVcacheFactory(vcacheFactory);
            setJiraIssuesManager(jiraIssuesManager);
            setJiraIssuesResponseGenerator(jiraIssuesResponseGenerator);
            setJiraIssuesUrlManager(jiraIssuesUrlManager);
            setApplicationLinkService(readOnlyApplicationLinkService);
        }
    }
}