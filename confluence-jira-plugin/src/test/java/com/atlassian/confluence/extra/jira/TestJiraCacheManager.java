package com.atlassian.confluence.extra.jira;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;

import org.mockito.Mock;

import com.atlassian.applinks.api.ApplicationId;
import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.cache.Cache;
import com.atlassian.cache.CacheManager;
import com.atlassian.confluence.extra.jira.cache.CacheKey;

public class TestJiraCacheManager extends TestCase
{
    @Mock private CacheManager cacheManager;

    @Mock private Cache cache;

    @Mock private ApplicationLink appLink;

    private JiraCacheManager jiraCacheManager;

    protected void setUp() throws Exception
    {
        super.setUp();
        cacheManager = mock(CacheManager.class);
        cache = mock(Cache.class);
        appLink = mock(ApplicationLink.class);
        jiraCacheManager = new DefaultJiraCacheManager(cacheManager);
    }

    public void testClearExistingJiraIssuesCache()
    {
        String url = "http://localhost:8080/sr/jira.issueviews:searchrequest-xml/temp/SearchRequest.xml?pid=10000&sorter/field=issuekey&sorter/order=ASC";
        List<String> columns = Arrays.asList("key", "type", "summary");
        boolean forceAnonymous = false;
        boolean isAnonymous = false;
        ApplicationId appLinkId = new ApplicationId("8835b6b9-5676-3de4-ad59-bbe987416662");
        CacheKey cacheKey = new CacheKey(url, appLinkId.toString(), columns, false, forceAnonymous, false, true);

        when(cacheManager.getCache(JiraIssuesMacro.class.getName())).thenReturn(cache);
        when(appLink.getId()).thenReturn(appLinkId);
        when(cache.get(cacheKey)).thenReturn(new Object());

        jiraCacheManager.clearJiraIssuesCache(url, columns, appLink, forceAnonymous, isAnonymous);

        verify(cache).remove(cacheKey);
    }

    public void testClearJiraIssuesCacheAnonymously()
    {
        String url = "http://localhost:8080/sr/jira.issueviews:searchrequest-xml/temp/SearchRequest.xml?pid=10000&sorter/field=issuekey&sorter/order=ASC";
        List<String> columns = Arrays.asList("key", "type", "summary");
        boolean forceAnonymous = false;
        boolean isAnonymous = true;
        ApplicationId appLinkId = new ApplicationId("8835b6b9-5676-3de4-ad59-bbe987416662");
        CacheKey mappedCacheKey = new CacheKey(url, appLinkId.toString(), columns, false, forceAnonymous, false, true);
        CacheKey unmappedCacheKey = new CacheKey(url, appLinkId.toString(), columns, false, forceAnonymous, false, false);

        when(cacheManager.getCache(JiraIssuesMacro.class.getName())).thenReturn(cache);
        when(appLink.getId()).thenReturn(appLinkId);
        when(cache.get(mappedCacheKey)).thenReturn(null);

        jiraCacheManager.clearJiraIssuesCache(url, columns, appLink, forceAnonymous, isAnonymous);

        verify(cache).remove(unmappedCacheKey);
    }

}