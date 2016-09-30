package com.atlassian.confluence.extra.jira;

import java.util.List;

import com.atlassian.applinks.api.ReadOnlyApplicationLink;
import com.atlassian.confluence.extra.jira.cache.CacheKey;
import com.atlassian.confluence.extra.jira.cache.CacheLoggingUtils;
import com.atlassian.confluence.extra.jira.cache.CompressingStringCache;
import com.atlassian.confluence.extra.jira.cache.JIMCacheProvider;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.tenancy.api.event.TenantArrivedEvent;
import com.atlassian.util.concurrent.Lazy;
import com.atlassian.util.concurrent.Supplier;
import com.atlassian.vcache.DirectExternalCache;
import com.atlassian.vcache.VCacheFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import static com.atlassian.confluence.extra.jira.util.JiraUtil.JIRA_PLUGIN_KEY;
import static com.atlassian.vcache.VCacheUtils.fold;

public class DefaultJiraCacheManager implements JiraCacheManager, InitializingBean, DisposableBean
{

    private static final Logger log = LoggerFactory.getLogger(DefaultJiraCacheManager.class);
    public static final String PARAM_CLEAR_CACHE = "clearCache";

    private final DirectExternalCache<CompressingStringCache> responseCache;
    private DirectExternalCache<JiraChannelResponseHandler> channelResponseCache;
    private DirectExternalCache<JiraStringResponseHandler> stringResponseCache;
    private final Supplier<String> version;
    private final ConfluenceJiraPluginSettingManager confluenceJiraPluginSettingManager;
    private final EventPublisher eventPublisher;
    private final VCacheFactory vcacheFactory;

    public DefaultJiraCacheManager(VCacheFactory vcacheFactory, PluginAccessor pluginAccessor,
                ConfluenceJiraPluginSettingManager confluenceJiraPluginSettingManager,
                EventPublisher eventPublisher)
    {
        this.responseCache = JIMCacheProvider.getResponseCache(vcacheFactory);
        this.confluenceJiraPluginSettingManager = confluenceJiraPluginSettingManager;
        this.eventPublisher = eventPublisher;
        this.vcacheFactory = vcacheFactory;
        this.version = Lazy.supplier(() -> pluginAccessor.getPlugin(JIRA_PLUGIN_KEY).getPluginInformation().getVersion());
    }

    @Override
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

        if (channelResponseCache == null || stringResponseCache == null)
        {
            this.initializeCache();
        }

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
                    fold(cache.remove(mappedKey.toKey()), (result, throwable) -> {
                        CacheLoggingUtils.log(log, throwable, true);
                        return null;
                    });
                }
                else
                {
                    boolean userIsMapped = isAnonymous == false && AuthenticatedUserThreadLocal.getUsername() != null;
                    if (!userIsMapped) // only care unmap cache in case user not logged it
                    {
                        fold(cache.remove(unmappedKey.toKey()), (result, throwable) -> {
                            CacheLoggingUtils.log(log, throwable, true);
                            return null;
                        });
                    }
                }
                return true;
            }, throwable -> {
                CacheLoggingUtils.log(log, throwable, false);
                return null;
            });
    }

    @Override
    public void afterPropertiesSet() throws Exception
    {
        this.eventPublisher.register(this);
    }

    @Override
    public void destroy() throws Exception
    {
        this.eventPublisher.unregister(this);
    }

    @EventListener
    public void onTenantArrived(TenantArrivedEvent event)
    {
        this.initializeCache();
    }

    @Override
    public void initializeCache()
    {
        this.channelResponseCache = JIMCacheProvider.getChannelResponseHandlersCache(vcacheFactory,
                this.confluenceJiraPluginSettingManager.getCacheTimeoutInMinutes());
        this.stringResponseCache = JIMCacheProvider.getStringResponseHandlersCache(vcacheFactory,
                this.confluenceJiraPluginSettingManager.getCacheTimeoutInMinutes());
    }
}
