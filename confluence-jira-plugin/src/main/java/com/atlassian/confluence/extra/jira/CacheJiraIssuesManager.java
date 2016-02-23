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
import com.atlassian.vcache.JvmCache;
import com.atlassian.vcache.VCacheFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

import static com.atlassian.confluence.extra.jira.util.JiraUtil.JIRA_PLUGIN_KEY;

public class CacheJiraIssuesManager extends DefaultJiraIssuesManager
{

    private static final Logger log = LoggerFactory.getLogger(CacheJiraIssuesManager.class);

    private final JvmCache<CacheKey, JiraResponseHandler> responseCache;
    private final Supplier<String> version;

    public CacheJiraIssuesManager(JiraIssuesColumnManager jiraIssuesColumnManager,
            JiraIssuesUrlManager jiraIssuesUrlManager, HttpRetrievalService httpRetrievalService,
            VCacheFactory vcacheFactory, PluginAccessor pluginAccessor)
    {
        super(jiraIssuesColumnManager, jiraIssuesUrlManager, httpRetrievalService);
        this.responseCache = JIMCacheProvider.getResponseHandlersCache(vcacheFactory);
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

        JiraResponseHandler cachedResponseHandler = responseCache.get(mappedCacheKey).orElseGet(() -> {
            if (userIsMapped == false)
            {
                CacheKey unmappedCacheKey = new CacheKey(url, appLink.getId().toString(), columns, false,
                        forceAnonymous, false, false, version.get());
                return responseCache.get(unmappedCacheKey).orElse(null);
            }
            return null;
        });

        // Neither mapped cache nor unmapped cache available, request JIRA for data
        if (cachedResponseHandler == null)
        {
            final CacheKey cacheKey = new CacheKey(url, appLink.getId().toString(), columns, false, forceAnonymous,
                    false, userIsMapped, version.get());
            log.debug("building cache: " + cacheKey);
            JiraResponseHandler responseHandler = super.retrieveXML(url, columns, appLink, forceAnonymous, isAnonymous,
                    handlerType, useCache);
            responseCache.put(cacheKey, responseHandler);
            return responseHandler;
        }
        log.debug("returning cached version");
        return cachedResponseHandler;
    }

}
