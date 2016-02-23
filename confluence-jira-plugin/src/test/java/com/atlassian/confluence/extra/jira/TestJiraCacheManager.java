package com.atlassian.confluence.extra.jira;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import com.atlassian.applinks.api.ApplicationId;
import com.atlassian.applinks.api.ReadOnlyApplicationLink;
import com.atlassian.confluence.extra.jira.cache.CacheKey;
import com.atlassian.confluence.extra.jira.cache.CompressingStringCache;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.vcache.DirectExternalCache;
import com.atlassian.vcache.JvmCache;
import com.atlassian.vcache.PutPolicy;
import com.atlassian.vcache.VCacheFactory;

import org.mockito.Mock;

import junit.framework.TestCase;

import static com.atlassian.confluence.extra.jira.cache.CacheKeyTestHelper.getPluginVersionExpectations;
import static com.atlassian.confluence.extra.jira.cache.VCacheTestHelper.getExternalCacheOnCall;
import static com.atlassian.confluence.extra.jira.cache.VCacheTestHelper.getJvmCacheOnCall;
import static com.atlassian.confluence.extra.jira.cache.VCacheTestHelper.mockVCacheFactory;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TestJiraCacheManager extends TestCase
{
    private static final String PLUGIN_VERSION = "6.0.0";

    @Mock private ReadOnlyApplicationLink appLink;
    private PluginAccessor pluginAccessor;
    private VCacheFactory cacheFactory;
    private DirectExternalCache<CompressingStringCache> cache;
    private JvmCache<CacheKey, JiraResponseHandler> instanceCache;

    private JiraCacheManager jiraCacheManager;

    protected void setUp() throws Exception
    {
        super.setUp();
        pluginAccessor = mock(PluginAccessor.class);
        getPluginVersionExpectations(pluginAccessor, PLUGIN_VERSION);
        cacheFactory = mockVCacheFactory();
        cache = getExternalCacheOnCall(cacheFactory);
        instanceCache = getJvmCacheOnCall(cacheFactory);
        appLink = mock(ReadOnlyApplicationLink.class);
        jiraCacheManager = new DefaultJiraCacheManager(cacheFactory, pluginAccessor);
    }

    public void testClearExistingJiraIssuesCache()
    {
        String url = "http://localhost:8080/sr/jira.issueviews:searchrequest-xml/temp/SearchRequest.xml?pid=10000&sorter/field=issuekey&sorter/order=ASC";
        List<String> columns = Arrays.asList("key", "type", "summary");
        boolean forceAnonymous = false;
        boolean isAnonymous = false;
        ApplicationId appLinkId = new ApplicationId("8835b6b9-5676-3de4-ad59-bbe987416662");
        CacheKey cacheKey = new CacheKey(url, appLinkId.toString(), columns, false, forceAnonymous, false, true, PLUGIN_VERSION);

        when(appLink.getId()).thenReturn(appLinkId);
        cache.put(cacheKey.toKey(), new CompressingStringCache(new ConcurrentHashMap()), PutPolicy.PUT_ALWAYS);

        jiraCacheManager.clearJiraIssuesCache(url, columns, appLink, forceAnonymous, isAnonymous);

        verify(cache).remove(cacheKey.toKey());
        verify(instanceCache).remove(cacheKey);
    }

    public void testClearJiraIssuesCacheAnonymously()
    {
        String url = "http://localhost:8080/sr/jira.issueviews:searchrequest-xml/temp/SearchRequest.xml?pid=10000&sorter/field=issuekey&sorter/order=ASC";
        List<String> columns = Arrays.asList("key", "type", "summary");
        boolean forceAnonymous = false;
        boolean isAnonymous = true;
        ApplicationId appLinkId = new ApplicationId("8835b6b9-5676-3de4-ad59-bbe987416662");
        CacheKey unmappedCacheKey = new CacheKey(url, appLinkId.toString(), columns, false, forceAnonymous, false,
                false, PLUGIN_VERSION);

        when(appLink.getId()).thenReturn(appLinkId);
        cache.put(unmappedCacheKey.toKey(), new CompressingStringCache(new ConcurrentHashMap()), PutPolicy.PUT_ALWAYS);

        jiraCacheManager.clearJiraIssuesCache(url, columns, appLink, forceAnonymous, isAnonymous);

        verify(cache).remove(unmappedCacheKey.toKey());
        verify(instanceCache).remove(unmappedCacheKey);
    }
}