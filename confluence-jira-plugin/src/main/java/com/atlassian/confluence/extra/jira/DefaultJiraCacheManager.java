package com.atlassian.confluence.extra.jira;

import java.util.List;

import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.cache.Cache;
import com.atlassian.cache.CacheManager;
import com.atlassian.confluence.extra.jira.cache.CacheKey;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;

public class DefaultJiraCacheManager implements JiraCacheManager
{

    public static final String PARAM_CLEAR_CACHE = "clearCache";

    private CacheManager cacheManager;

    public DefaultJiraCacheManager(CacheManager cacheManager)
    {
        this.cacheManager = cacheManager;
    }

    public void setCacheManager(CacheManager cacheManager)
    {
        this.cacheManager = cacheManager;
    }

    public void clearJiraIssuesCache(final String url, List<String> columns, final ApplicationLink appLink,
            boolean forceAnonymous, boolean isAnonymous)
    {
        final Cache cache = cacheManager.getCache(JiraIssuesMacro.class.getName());
        final CacheKey mappedCacheKey = new CacheKey(url, appLink.getId().toString(), columns, false, forceAnonymous,
                false, true);

        if (cache.get(mappedCacheKey) != null)
        {
            cache.remove(mappedCacheKey);
        }
        else
        {
            boolean userIsMapped = isAnonymous == false && AuthenticatedUserThreadLocal.getUsername() != null;
            if (userIsMapped == false) // only care unmap cache in case user not logged it
            {
                CacheKey unmappedCacheKey = new CacheKey(url, appLink.getId().toString(), columns, false,
                        forceAnonymous, false, false);
                cache.remove(unmappedCacheKey); // remove cache if there is
            }
        }
    }

}