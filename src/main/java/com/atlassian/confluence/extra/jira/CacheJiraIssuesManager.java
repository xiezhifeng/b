package com.atlassian.confluence.extra.jira;

import java.io.IOException;
import java.util.List;

import org.apache.log4j.Logger;

import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.applinks.api.CredentialsRequiredException;
import com.atlassian.cache.Cache;
import com.atlassian.cache.CacheManager;
import com.atlassian.confluence.extra.jira.JiraResponseHandler.HandlerType;
import com.atlassian.confluence.extra.jira.cache.CacheKey;
import com.atlassian.confluence.security.trust.TrustedTokenFactory;
import com.atlassian.confluence.util.http.HttpRetrievalService;
import com.atlassian.confluence.util.http.trust.TrustedConnectionStatusBuilder;
import com.atlassian.sal.api.net.ResponseException;

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

        final CacheKey cacheKey = new CacheKey(url, appLink.getId().toString(), columns, false, forceAnonymous, false);

        final Cache cache = cacheManager.getCache(JiraIssuesMacro.class.getName());
        final JiraResponseHandler cachedResponseHandler = (JiraResponseHandler) cache.get(cacheKey);
        if (cachedResponseHandler == null)
        {
            log.debug("building cache: " + cacheKey);
            JiraResponseHandler responseHandler = super.retrieveXML(url, columns, appLink, forceAnonymous, isAnonymous,
                    handlerType, useCache);
            cache.put(cacheKey, responseHandler);
            return responseHandler;
        } else
        {
            log.debug("returning cached version, key: " + cacheKey);
            return cachedResponseHandler;
        }
    }

}
