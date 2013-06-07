package com.atlassian.confluence.extra.jira;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import org.apache.log4j.Logger;

import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.applinks.api.ApplicationLinkRequest;
import com.atlassian.applinks.api.ApplicationLinkRequestFactory;
import com.atlassian.applinks.api.AuthorisationURIGenerator;
import com.atlassian.applinks.api.CredentialsRequiredException;
import com.atlassian.cache.Cache;
import com.atlassian.cache.CacheManager;
import com.atlassian.confluence.extra.jira.JiraResponseHandler.HandlerType;
import com.atlassian.confluence.extra.jira.cache.CacheKey;
import com.atlassian.confluence.security.trust.TrustedTokenFactory;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.confluence.util.http.HttpRetrievalService;
import com.atlassian.confluence.util.http.trust.TrustedConnectionStatusBuilder;
import com.atlassian.sal.api.net.ResponseException;
import com.atlassian.sal.api.net.Request.MethodType;

public class CacheJiraIssuesManager extends DefaultJiraIssuesManager
{

    private static final Logger log = Logger.getLogger(CacheJiraIssuesManager.class);

    private CacheManager cacheManager;

    public CacheJiraIssuesManager(JiraIssuesColumnManager jiraIssuesColumnManager,
            JiraIssuesUrlManager jiraIssuesUrlManager, HttpRetrievalService httpRetrievalService,
            TrustedTokenFactory trustedTokenFactory, TrustedConnectionStatusBuilder trustedConnectionStatusBuilder,
            TrustedApplicationConfig trustedAppConfig, CacheManager cacheManager)
    {
        super(jiraIssuesColumnManager, jiraIssuesUrlManager, httpRetrievalService, trustedTokenFactory,
                trustedConnectionStatusBuilder, trustedAppConfig);
        this.cacheManager = cacheManager;
    }

    @Override
    protected JiraResponseHandler retrieveXML(String url, List<String> columns, final ApplicationLink appLink,
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

        final Cache cache = cacheManager.getCache(JiraIssuesMacro.class.getName());

        boolean userIsMapped = isAnonymous == false && AuthenticatedUserThreadLocal.getUsername() != null;

        final CacheKey mappedCacheKey = new CacheKey(url, appLink.getId().toString(), columns, false, forceAnonymous,
                false, true);

        JiraResponseHandler cachedResponseHandler = (JiraResponseHandler) cache.get(mappedCacheKey);
        if (userIsMapped == false) // only care unmap cache in case user not logged it
        {
            if (cachedResponseHandler == null)
            {
                CacheKey unmappedCacheKey = new CacheKey(url, appLink.getId().toString(), columns, false,
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
            JiraResponseHandler responseHandler = super.retrieveXML(url, columns, appLink, forceAnonymous, isAnonymous,
                    handlerType, useCache);
            cache.put(cacheKey, responseHandler);
            return responseHandler;
        } else
        {
            log.debug("returning cached version");
            return cachedResponseHandler;
        }
    }

}
