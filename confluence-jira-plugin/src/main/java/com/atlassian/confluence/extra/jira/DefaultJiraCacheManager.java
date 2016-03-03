package com.atlassian.confluence.extra.jira;

import java.util.List;

import com.atlassian.applinks.api.ReadOnlyApplicationLink;
import com.atlassian.confluence.extra.jira.cache.CacheKey;
import com.atlassian.confluence.extra.jira.cache.CompressingStringCache;
import com.atlassian.confluence.extra.jira.cache.JIMCacheProvider;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.util.concurrent.Lazy;
import com.atlassian.util.concurrent.Supplier;
import com.atlassian.vcache.DirectExternalCache;
import com.atlassian.vcache.VCacheFactory;

import static com.atlassian.confluence.extra.jira.util.JiraUtil.JIRA_PLUGIN_KEY;
import static com.atlassian.vcache.VCacheUtils.fold;

public class DefaultJiraCacheManager implements JiraCacheManager
{

    public static final String PARAM_CLEAR_CACHE = "clearCache";

    private final DirectExternalCache<CompressingStringCache> responseCache;
    private final DirectExternalCache<JiraChannelResponseHandler> channelResponseCache;
    private final DirectExternalCache<JiraStringResponseHandler> stringResponseCache;
    private final Supplier<String> version;

    public DefaultJiraCacheManager(VCacheFactory vcacheFactory, PluginAccessor pluginAccessor)
    {
        this.responseCache = JIMCacheProvider.getResponseCache(vcacheFactory);
        this.channelResponseCache = JIMCacheProvider.getChannelResponseHandlersCache(vcacheFactory);
        this.stringResponseCache = JIMCacheProvider.getStringResponseHandlersCache(vcacheFactory);
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
        final CacheKey unmappedCacheKey = new CacheKey(url, appLink.getId().toString(), columns, false,
                forceAnonymous, false, false, version.get());

        clean(mappedCacheKey, unmappedCacheKey, isAnonymous, responseCache);
        clean(mappedCacheKey, unmappedCacheKey, isAnonymous, channelResponseCache);
        clean(mappedCacheKey, unmappedCacheKey, isAnonymous, stringResponseCache);
    }

    private static <T> void clean(CacheKey mappedKey, CacheKey unmappedKey, boolean isAnonymous,
            DirectExternalCache<T> cache)
    {
        fold(cache.get(mappedKey.toKey()), t ->
            {
                if (t.isPresent())
                {
                    fold(cache.remove(mappedKey.toKey()), (bool, throwable) -> true);
                }
                else
                {
                    boolean userIsMapped = isAnonymous == false && AuthenticatedUserThreadLocal.getUsername() != null;
                    if (!userIsMapped) // only care unmap cache in case user not logged it
                    {
                        fold(cache.remove(unmappedKey.toKey()), (bool, throwable) -> true);
                    }
                }
                return true;
            }, throwable -> null);
    }
}
