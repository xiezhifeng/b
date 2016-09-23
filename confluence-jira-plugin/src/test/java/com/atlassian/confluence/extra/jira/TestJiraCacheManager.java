package com.atlassian.confluence.extra.jira;

import com.atlassian.applinks.api.ApplicationId;
import com.atlassian.applinks.api.ReadOnlyApplicationLink;
import com.atlassian.confluence.extra.jira.cache.CacheKey;
import com.atlassian.confluence.extra.jira.cache.CompressingStringCache;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.vcache.DirectExternalCache;
import com.atlassian.vcache.PutPolicy;
import com.atlassian.vcache.VCacheFactory;
import com.atlassian.vcache.internal.core.ThreadLocalRequestContextSupplier;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static com.atlassian.confluence.extra.jira.cache.CacheKeyTestHelper.getPluginVersionExpectations;
import static com.atlassian.vcache.internal.test.utils.VCacheTestHelper.getExternalCache;
import static com.atlassian.vcache.internal.test.utils.VCacheTestHelper.getFactory;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TestJiraCacheManager
{
    private static final String PLUGIN_VERSION = "6.0.0";
    public static final ThreadLocalRequestContextSupplier CONTEXT_SUPPLIER = ThreadLocalRequestContextSupplier.strictSupplier();

    @Mock private ReadOnlyApplicationLink appLink;
    @Mock private EventPublisher eventPublisher;
    @Mock private PluginAccessor pluginAccessor;
    private VCacheFactory cacheFactory;
    private DirectExternalCache<CompressingStringCache> responseCache;
    private DirectExternalCache<JiraChannelResponseHandler> responseChannelCache;
    private DirectExternalCache<JiraStringResponseHandler> responseStringCache;
    @Mock private ConfluenceJiraPluginSettingManager confluenceJiraPluginSettingManager;

    private JiraCacheManager jiraCacheManager;

    @BeforeClass
    public static void initThread()
    {
        CONTEXT_SUPPLIER.initThread("myPartition");
    }

    @Before
    public void setUp() throws Exception
    {
        getPluginVersionExpectations(pluginAccessor, PLUGIN_VERSION);
        cacheFactory = Mockito.spy(getFactory(CONTEXT_SUPPLIER));
        responseCache = getExternalCache(cacheFactory, "com.atlassian.confluence.extra.jira.JiraIssuesMacro", CompressingStringCache.class);
        responseChannelCache = getExternalCache(cacheFactory,
                "com.atlassian.confluence.extra.jira.JiraIssuesMacro.channel", JiraChannelResponseHandler.class);
        responseStringCache = getExternalCache(cacheFactory,
                "com.atlassian.confluence.extra.jira.JiraIssuesMacro.string", JiraStringResponseHandler.class);
        appLink = mock(ReadOnlyApplicationLink.class);
        jiraCacheManager = new DefaultJiraCacheManager(cacheFactory, pluginAccessor, confluenceJiraPluginSettingManager, eventPublisher);
        jiraCacheManager.initializeCache();
    }

    @Test
    public void testClearExistingJiraIssuesCache()
    {
        String url = "http://localhost:8080/sr/jira.issueviews:searchrequest-xml/temp/SearchRequest.xml?pid=10000&sorter/field=issuekey&sorter/order=ASC";
        List<String> columns = Arrays.asList("key", "type", "summary");
        boolean forceAnonymous = false;
        boolean isAnonymous = false;
        ApplicationId appLinkId = new ApplicationId("8835b6b9-5676-3de4-ad59-bbe987416662");
        CacheKey cacheKey = new CacheKey(url, appLinkId.toString(), columns, false, forceAnonymous, false, true, PLUGIN_VERSION);

        when(appLink.getId()).thenReturn(appLinkId);
        responseCache.put(cacheKey.toKey(), new CompressingStringCache(new ConcurrentHashMap()), PutPolicy.PUT_ALWAYS);

        jiraCacheManager.clearJiraIssuesCache(url, columns, appLink, forceAnonymous, isAnonymous);

        verify(responseCache).remove(cacheKey.toKey());
    }

    @Test
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
        responseCache.put(unmappedCacheKey.toKey(), new CompressingStringCache(new ConcurrentHashMap()), PutPolicy.PUT_ALWAYS);

        jiraCacheManager.clearJiraIssuesCache(url, columns, appLink, forceAnonymous, isAnonymous);

        verify(responseCache).remove(unmappedCacheKey.toKey());
    }
}