package com.atlassian.confluence.extra.jira;

import java.io.IOException;
import java.util.List;

import com.atlassian.confluence.extra.jira.JiraResponseHandler.HandlerType;
import com.atlassian.confluence.extra.jira.cache.CacheKey;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.confluence.util.http.HttpRetrievalService;

import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.applinks.api.ApplicationLinkRequestFactory;
import com.atlassian.applinks.api.CredentialsRequiredException;
import com.atlassian.cache.Cache;
import com.atlassian.cache.CacheManager;
import com.atlassian.sal.api.net.Request.MethodType;
import com.atlassian.sal.api.net.ResponseException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CacheJiraIssuesManager extends DefaultJiraIssuesManager
{

    private static final Logger log = LoggerFactory.getLogger(CacheJiraIssuesManager.class);

    private final CacheManager cacheManager;

    public CacheJiraIssuesManager(final JiraIssuesColumnManager jiraIssuesColumnManager,
 final JiraIssuesUrlManager jiraIssuesUrlManager,
            final HttpRetrievalService httpRetrievalService, final CacheManager cacheManager, final JiraConnectorManager jiraConnectorManager)
    {
        super(jiraIssuesColumnManager, jiraIssuesUrlManager, httpRetrievalService, jiraConnectorManager);
        this.cacheManager = cacheManager;
    }

    @Override
    protected JiraResponseHandler retrieveXML(final String url, final List<String> columns, final ApplicationLink appLink,
            final boolean forceAnonymous, final boolean isAnonymous, final HandlerType handlerType, final boolean useCache) throws IOException,
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

        final Cache cache = this.cacheManager.getCache(JiraIssuesMacro.class.getName());

        final boolean userIsMapped = isAnonymous == false && AuthenticatedUserThreadLocal.getUsername() != null;

        final CacheKey mappedCacheKey = new CacheKey(url, appLink.getId().toString(), columns, false, forceAnonymous,
                false, true);

        JiraResponseHandler cachedResponseHandler = (JiraResponseHandler) cache.get(mappedCacheKey);
        if (userIsMapped == false) // only care unmap cache in case user not logged it
        {
            if (cachedResponseHandler == null)
            {
                final CacheKey unmappedCacheKey = new CacheKey(url, appLink.getId().toString(), columns, false,
                        forceAnonymous, false, false);
                cachedResponseHandler = (JiraResponseHandler) cache.get(unmappedCacheKey);
            }
        }

        // Neither mapped cache nor unmapped cache available, request JIRA for data
        if (cachedResponseHandler == null)
        {
            final CacheKey cacheKey = new CacheKey(url, appLink.getId().toString(), columns, false, forceAnonymous,
                    false, userIsMapped);
            log.debug("building cache: " + cacheKey);
            final JiraResponseHandler responseHandler = super.retrieveXML(url, columns, appLink, forceAnonymous, isAnonymous,
                    handlerType, useCache);
            cache.put(cacheKey, responseHandler);
            return responseHandler;
        }
        log.debug("returning cached version");
        return cachedResponseHandler;
    }

}
