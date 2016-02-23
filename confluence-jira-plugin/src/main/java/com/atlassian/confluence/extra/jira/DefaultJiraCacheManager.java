package com.atlassian.confluence.extra.jira;

import com.atlassian.applinks.api.ReadOnlyApplicationLink;
import com.atlassian.confluence.extra.jira.cache.CacheKey;
import com.atlassian.confluence.extra.jira.cache.CompressingStringCache;
import com.atlassian.confluence.extra.jira.cache.JIMCacheProvider;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.util.concurrent.Lazy;
import com.atlassian.util.concurrent.Supplier;
import com.atlassian.vcache.DirectExternalCache;
import com.atlassian.vcache.JvmCache;
import com.atlassian.vcache.VCacheFactory;

import java.util.List;

import static com.atlassian.confluence.extra.jira.util.JiraUtil.JIRA_PLUGIN_KEY;
import static com.atlassian.vcache.VCacheUtils.join;

public class DefaultJiraCacheManager implements JiraCacheManager
{

    public static final String PARAM_CLEAR_CACHE = "clearCache";

    private final DirectExternalCache<CompressingStringCache> cache;
    private final JvmCache<CacheKey, JiraResponseHandler> instanceCache;
    private final Supplier<String> version;

    public DefaultJiraCacheManager(VCacheFactory vcacheFactory, PluginAccessor pluginAccessor)
    {
        this.cache = JIMCacheProvider.getResponseCache(vcacheFactory);
        this.instanceCache = JIMCacheProvider.getResponseHandlersCache(vcacheFactory);
        this.version = Lazy.supplier(() -> pluginAccessor.getPlugin(JIRA_PLUGIN_KEY).getPluginInformation().getVersion());
    }

    public void clearJiraIssuesCache(final String url, List<String> columns, final ReadOnlyApplicationLink appLink,
            boolean forceAnonymous, boolean isAnonymous)
    {
        if (appLink == null)
        {
            return;
        }
        final CacheKey mappedCacheKey = new CacheKey(url, appLink.getId().toString(), columns, false, forceAnonymous,
                false, true, version.get());

        if (join(cache.get(mappedCacheKey.toKey())).isPresent())
        {
            join(cache.remove(mappedCacheKey.toKey()));
            instanceCache.remove(mappedCacheKey);
        }
        else
        {
            boolean userIsMapped = isAnonymous == false && AuthenticatedUserThreadLocal.getUsername() != null;
            if (userIsMapped == false) // only care unmap cache in case user not logged it
            {
                CacheKey unmappedCacheKey = new CacheKey(url, appLink.getId().toString(), columns, false,
                        forceAnonymous, false, false, version.get());
                join(cache.remove(unmappedCacheKey.toKey()));
                instanceCache.remove(unmappedCacheKey);
            }
        }
    }

}
