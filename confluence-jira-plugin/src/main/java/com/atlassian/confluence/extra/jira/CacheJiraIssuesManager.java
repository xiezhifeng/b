package com.atlassian.confluence.extra.jira;

import java.io.IOException;
import java.util.List;

import com.atlassian.applinks.api.ApplicationLinkRequestFactory;
import com.atlassian.applinks.api.CredentialsRequiredException;
import com.atlassian.applinks.api.ReadOnlyApplicationLink;
import com.atlassian.confluence.extra.jira.JiraResponseHandler.HandlerType;
import com.atlassian.confluence.extra.jira.cache.CacheKey;
import com.atlassian.confluence.extra.jira.cache.CacheLoggingUtils;
import com.atlassian.confluence.extra.jira.cache.JIMCacheProvider;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.confluence.util.http.HttpRetrievalService;
import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.sal.api.net.Request.MethodType;
import com.atlassian.sal.api.net.ResponseException;
import com.atlassian.tenancy.api.event.TenantArrivedEvent;
import com.atlassian.util.concurrent.Lazy;
import com.atlassian.util.concurrent.Supplier;
import com.atlassian.vcache.DirectExternalCache;
import com.atlassian.vcache.PutPolicy;
import com.atlassian.vcache.VCacheFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import static com.atlassian.confluence.extra.jira.util.JiraUtil.JIRA_PLUGIN_KEY;
import static com.atlassian.vcache.VCacheUtils.fold;

public class CacheJiraIssuesManager extends DefaultJiraIssuesManager implements InitializingBean, DisposableBean
{

    private static final Logger log = LoggerFactory.getLogger(CacheJiraIssuesManager.class);

    private DirectExternalCache<JiraChannelResponseHandler> responseChannelHandlerCache;
    private DirectExternalCache<JiraStringResponseHandler> responseStringHandlerCache;
    private final Supplier<String> version;
    private final ConfluenceJiraPluginSettingManager confluenceJiraPluginSettingManager;
    private final EventPublisher eventPublisher;
    private final VCacheFactory vcacheFactory;

    public CacheJiraIssuesManager(JiraIssuesColumnManager jiraIssuesColumnManager,
            JiraIssuesUrlManager jiraIssuesUrlManager, HttpRetrievalService httpRetrievalService,
            VCacheFactory vcacheFactory, PluginAccessor pluginAccessor,
            ConfluenceJiraPluginSettingManager confluenceJiraPluginSettingManager,
            EventPublisher eventPublisher)
    {
        super(jiraIssuesColumnManager, jiraIssuesUrlManager, httpRetrievalService);
        this.confluenceJiraPluginSettingManager = confluenceJiraPluginSettingManager;
        this.eventPublisher = eventPublisher;
        this.vcacheFactory = vcacheFactory;
        this.version = Lazy.supplier(() -> pluginAccessor.getPlugin(JIRA_PLUGIN_KEY).getPluginInformation().getVersion());
    }

    @Override
    protected JiraResponseHandler retrieveXML(String url, List<String> columns, final ReadOnlyApplicationLink appLink,
            boolean forceAnonymous, boolean isAnonymous, HandlerType handlerType, boolean useCache) throws IOException,
            CredentialsRequiredException, ResponseException
    {
        if (!useCache || appLink == null)
        {
            return super.retrieveXML(url, columns, appLink, forceAnonymous, isAnonymous, handlerType, useCache);
        }

        // This will
        // check for access token of current login user against provided
        // appLink. If isAnonymous == true and user is logged in Confluence,
        // CredentialsRequiredException will be thrown
        final ApplicationLinkRequestFactory requestFactory = createRequestFactory(appLink, isAnonymous);
        requestFactory.createRequest(MethodType.GET, url);

        boolean userIsMapped = isAnonymous == false && AuthenticatedUserThreadLocal.getUsername() != null;

        final CacheKey mappedCacheKey = new CacheKey(url, appLink.getId().toString(), columns, false, forceAnonymous,
                false, true, version.get());
        final CacheKey unmappedCacheKey = new CacheKey(url, appLink.getId().toString(), columns, false,
                forceAnonymous, false, false, version.get());

        JiraResponseHandler cachedResponseHandler = tryToFindResponseHandlerInAllCaches(mappedCacheKey,
                unmappedCacheKey, userIsMapped);

        // Neither mapped cache nor unmapped cache available, request JIRA for data
        if (cachedResponseHandler == null)
        {
            final CacheKey cacheKey = new CacheKey(url, appLink.getId().toString(), columns, false, forceAnonymous,
                    false, userIsMapped, version.get());
            log.debug("building cache: " + cacheKey);
            JiraResponseHandler responseHandler = super.retrieveXML(url, columns, appLink, forceAnonymous, isAnonymous,
                    handlerType, useCache);
            populateCache(cacheKey, responseHandler);
            return responseHandler;
        }
        log.debug("returning cached version");
        return cachedResponseHandler;
    }

    private JiraResponseHandler tryToFindResponseHandlerInAllCaches(CacheKey mappedCacheKey, CacheKey
            unmappedCacheKey, boolean userIsMapped)
    {
        if(responseChannelHandlerCache == null || responseStringHandlerCache == null)
        {
            this.initializeCache();
        }

        JiraResponseHandler responseHandler = tryCache(mappedCacheKey, unmappedCacheKey, userIsMapped,
                responseChannelHandlerCache);
        if(responseHandler == null)
        {
            responseHandler = tryCache(mappedCacheKey, unmappedCacheKey, userIsMapped, responseStringHandlerCache);
        }

        return responseHandler;
    }

    private static <T extends JiraResponseHandler> T tryCache(CacheKey mappedCacheKey,
        CacheKey unmappedCacheKey, boolean userIsMapped, DirectExternalCache<T> cache)
    {
        return fold(cache.get(mappedCacheKey.toKey()), t -> t.orElseGet(() -> {
            if (!userIsMapped)
            {
                return fold(cache.get(unmappedCacheKey.toKey()), r -> r.orElse(null), throwable -> {
                    CacheLoggingUtils.log(log, throwable, false);
                    return null;
                });
            }
            return null;
        }), throwable -> {
            CacheLoggingUtils.log(log, throwable, false);
            return null;
        });
    }

    private void populateCache(CacheKey cacheKey, JiraResponseHandler responseHandler)
    {
        if(responseChannelHandlerCache == null || responseStringHandlerCache == null)
        {
            this.initializeCache();
        }

        if (responseHandler instanceof JiraChannelResponseHandler)
        {
            fold(responseChannelHandlerCache.put(cacheKey.toKey(), (JiraChannelResponseHandler) responseHandler,
                            PutPolicy.ADD_ONLY), (result, error) -> {
                CacheLoggingUtils.log(log, error, false);
                return result;
            });
        }
        else if (responseHandler instanceof JiraStringResponseHandler)
        {
            fold(responseStringHandlerCache.put(cacheKey.toKey(), (JiraStringResponseHandler) responseHandler,
                    PutPolicy.ADD_ONLY), (result, error) -> {
                CacheLoggingUtils.log(log, error, false);
                return result;
            });
        }
        else
        {
            throw new IllegalArgumentException("Cached value should be either JiraChannelResponseHandler or "
                    + "JiraStringResponseHandler. " + responseHandler.getClass().getName() + " is not supported.");
        }
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

    public void initializeCache()
    {
        this.responseChannelHandlerCache = JIMCacheProvider.getChannelResponseHandlersCache(this.vcacheFactory,
                this.confluenceJiraPluginSettingManager.getCacheTimeoutInMinutes());
        this.responseStringHandlerCache = JIMCacheProvider.getStringResponseHandlersCache(this.vcacheFactory,
                this.confluenceJiraPluginSettingManager.getCacheTimeoutInMinutes());
    }
}
