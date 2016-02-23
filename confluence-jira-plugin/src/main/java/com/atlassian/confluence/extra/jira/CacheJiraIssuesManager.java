package com.atlassian.confluence.extra.jira;

import com.atlassian.applinks.api.ApplicationLinkRequestFactory;
import com.atlassian.applinks.api.CredentialsRequiredException;
import com.atlassian.applinks.api.ReadOnlyApplicationLink;
import com.atlassian.confluence.extra.jira.JiraResponseHandler.HandlerType;
import com.atlassian.confluence.extra.jira.cache.CacheKey;
import com.atlassian.confluence.extra.jira.cache.JIMCacheProvider;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.confluence.util.http.HttpRetrievalService;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.sal.api.net.Request.MethodType;
import com.atlassian.sal.api.net.ResponseException;
import com.atlassian.util.concurrent.Lazy;
import com.atlassian.util.concurrent.Supplier;
import com.atlassian.vcache.DirectExternalCache;
import com.atlassian.vcache.JvmCache;
import com.atlassian.vcache.PutPolicy;
import com.atlassian.vcache.VCacheFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static com.atlassian.confluence.extra.jira.util.JiraUtil.JIRA_PLUGIN_KEY;
import static com.atlassian.vcache.VCacheUtils.join;

public class CacheJiraIssuesManager extends DefaultJiraIssuesManager
{

    private static final Logger log = LoggerFactory.getLogger(CacheJiraIssuesManager.class);

    private final DirectExternalCache<JiraChannelResponseHandler> responseChannelHandlerCache;
    private final DirectExternalCache<JiraStringResponseHandler> responseStringHandlerCache;
    private final Supplier<String> version;

    public CacheJiraIssuesManager(JiraIssuesColumnManager jiraIssuesColumnManager,
            JiraIssuesUrlManager jiraIssuesUrlManager, HttpRetrievalService httpRetrievalService,
            VCacheFactory vcacheFactory, PluginAccessor pluginAccessor)
    {
        super(jiraIssuesColumnManager, jiraIssuesUrlManager, httpRetrievalService);
        this.responseChannelHandlerCache = JIMCacheProvider.getChannelResponseHandlersCache(vcacheFactory);
        this.responseStringHandlerCache = JIMCacheProvider.getStringResponseHandlersCache(vcacheFactory);
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
        return join(cache.get(mappedCacheKey.toKey())).orElseGet(() -> {
            if (!userIsMapped)
            {
                return join(cache.get(unmappedCacheKey.toKey())).orElse(null);
            }
            return null;
        });
    }

    private void populateCache(CacheKey cacheKey, JiraResponseHandler responseHandler)
    {
        if (responseHandler instanceof JiraChannelResponseHandler)
        {
            join(responseChannelHandlerCache.put(cacheKey.toKey(), (JiraChannelResponseHandler) responseHandler, PutPolicy.ADD_ONLY));
        }
        else if (responseHandler instanceof JiraStringResponseHandler)
        {
            join(responseStringHandlerCache.put(cacheKey.toKey(), (JiraStringResponseHandler) responseHandler, PutPolicy.ADD_ONLY));
        }
        else
        {
            throw new IllegalArgumentException("Cached value should be either JiraChannelResponseHandler or "
                    + "JiraStringResponseHandler. " + responseHandler.getClass().getName() + " is not supported.");
        }
    }

}
