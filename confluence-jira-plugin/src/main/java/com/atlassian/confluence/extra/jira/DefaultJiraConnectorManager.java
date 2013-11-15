package com.atlassian.confluence.extra.jira;

import com.atlassian.applinks.api.*;
import com.atlassian.applinks.api.application.jira.JiraApplicationType;
import com.atlassian.applinks.api.auth.Anonymous;
import com.atlassian.cache.Cache;
import com.atlassian.cache.CacheManager;
import com.atlassian.confluence.plugins.jira.JiraServerBean;
import com.atlassian.sal.api.net.Request;
import com.atlassian.sal.api.net.ResponseException;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DefaultJiraConnectorManager implements JiraConnectorManager
{

    private static final String JSON_PATH_BUILD_NUMBER = "buildNumber";
    private static final String REST_URL_SERVER_INFO = "/rest/api/2/serverInfo";
    private static final Long NOT_SUPPORTED_BUILD_NUMBER = -1L;

    private static final Logger LOG = LoggerFactory.getLogger(JiraConnectorManager.class);

    private ApplicationLinkService appLinkService;
    private CacheManager cacheManager;

    public DefaultJiraConnectorManager(ApplicationLinkService appLinkService, CacheManager cacheManager)
    {
        this.appLinkService = appLinkService;
        this.cacheManager = cacheManager;
    }

    @Override
    public List<JiraServerBean> getJiraServers()
    {
        Iterable<ApplicationLink> appLinks = appLinkService.getApplicationLinks(JiraApplicationType.class);
        if(appLinks == null)
        {
            return Collections.EMPTY_LIST;
        }

        Cache cache = cacheManager.getCache(JiraConnectorManager.class.getName());

        List<JiraServerBean> servers = new ArrayList<JiraServerBean>();
        for (ApplicationLink applicationLink : appLinks)
        {
            JiraServerBean jiraServerBean;
            Object cacheValue = cache.get(applicationLink.getId());
            if(cacheValue != null && cacheValue instanceof JiraServerBean)
            {
                //update Auth Url
                jiraServerBean = (JiraServerBean)cacheValue;
                jiraServerBean.setAuthUrl(getAuthUrl(applicationLink));
            }
            else
            {
                jiraServerBean = getJiraServer(applicationLink);
                cache.put(applicationLink.getId(), jiraServerBean);

            }
            servers.add(jiraServerBean);
        }
        return servers;
    }

    @Override
    public JiraServerBean getJiraServer(String appId)
    {
        try
        {
            ApplicationLink applicationLink = appLinkService.getApplicationLink(new ApplicationId(appId));
            return getJiraServer(applicationLink);
        }
        catch (TypeNotInstalledException e)
        {
            return null;
        }
    }

    private JiraServerBean getJiraServer(ApplicationLink applicationLink)
    {
        return new JiraServerBean(applicationLink.getId().toString(), applicationLink.getRpcUrl().toString(),
                applicationLink.getName(), applicationLink.isPrimary(), getAuthUrl(applicationLink), getServerBuildNumber(applicationLink));
    }

    private ApplicationLinkRequest createRequest(ApplicationLink appLink, Request.MethodType methodType, String baseRestUrl) throws CredentialsRequiredException
    {
        String url = appLink.getRpcUrl() + baseRestUrl;
        ApplicationLinkRequestFactory requestFactory = appLink.createAuthenticatedRequestFactory();
        ApplicationLinkRequest request;
        try
        {
            request = requestFactory.createRequest(methodType, url);
        }
        catch (CredentialsRequiredException e)
        {
            requestFactory = appLink.createAuthenticatedRequestFactory(Anonymous.class);
            request = requestFactory.createRequest(methodType, url);
        }

        return request;
    }

    private Long getServerBuildNumber(ApplicationLink appLink)
    {
        Long buildNumber = Long.MAX_VALUE;
        try
        {
            ApplicationLinkRequest request = createRequest(appLink, Request.MethodType.GET, REST_URL_SERVER_INFO);
            request.addHeader("Content-Type", MediaType.APPLICATION_JSON);
            String responseString = request.execute();
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(responseString);
            return rootNode.path(JSON_PATH_BUILD_NUMBER).getLongValue();
        }
        catch (ResponseException e) // We could connect to JIRA Server but the REST URL provided is version 4.x
        {
            buildNumber = NOT_SUPPORTED_BUILD_NUMBER;
            LOG.warn(e.getMessage());
        }
        catch (Exception e) // In other cases we assume that it is supported version
        {
            LOG.warn(e.getMessage());
        }

        return buildNumber;
    }

    private String getAuthUrl(ApplicationLink applicationLink)
    {
        try
        {
            applicationLink.createAuthenticatedRequestFactory().createRequest(Request.MethodType.GET, "");
            return null;
        }
        catch (CredentialsRequiredException e)
        {
            // if an exception is thrown, we need to prompt for oauth
            return e.getAuthorisationURI().toString();
        }
    }
}
