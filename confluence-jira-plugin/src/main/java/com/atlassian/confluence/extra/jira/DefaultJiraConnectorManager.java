package com.atlassian.confluence.extra.jira;

import com.atlassian.applinks.api.*;
import com.atlassian.applinks.api.application.jira.JiraApplicationType;
import com.atlassian.cache.Cache;
import com.atlassian.cache.CacheManager;
import com.atlassian.confluence.extra.jira.util.JiraConnectorUtils;
import com.atlassian.confluence.plugins.jira.JiraServerBean;
import com.atlassian.sal.api.net.Request;
import com.atlassian.sal.api.net.ResponseStatusException;
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
    public static final long NOT_SUPPORTED_BUILD_NUMBER = -1L;

    private static final Logger LOG = LoggerFactory.getLogger(JiraConnectorManager.class);

    private ApplicationLinkService appLinkService;
    private Cache cache;

    public DefaultJiraConnectorManager(ApplicationLinkService appLinkService, CacheManager cacheManager)
    {
        this.appLinkService = appLinkService;
        this.cache = cacheManager.getCache(JiraConnectorManager.class.getName());
    }

    @Override
    public List<JiraServerBean> getJiraServers()
    {
        Iterable<ApplicationLink> appLinks = appLinkService.getApplicationLinks(JiraApplicationType.class);
        if(appLinks == null)
        {
            return Collections.EMPTY_LIST;
        }

        List<JiraServerBean> servers = new ArrayList<JiraServerBean>();
        for (ApplicationLink applicationLink : appLinks)
        {
            JiraServerBean jiraServerBean;
            Object cacheValue = cache.get(applicationLink.getId());
            if(cacheValue instanceof JiraServerBean)
            {
                //update Auth Url
                jiraServerBean = (JiraServerBean)cacheValue;
                jiraServerBean.setAuthUrl(JiraConnectorUtils.getAuthUrl(applicationLink));
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
    public JiraServerBean getJiraServer(ApplicationLink applicationLink)
    {
        return new JiraServerBean(applicationLink.getId().toString(), applicationLink.getRpcUrl().toString(),
                applicationLink.getName(), applicationLink.isPrimary(), JiraConnectorUtils.getAuthUrl(applicationLink), getServerBuildNumber(applicationLink));
    }

    private long getServerBuildNumber(ApplicationLink appLink)
    {
        try
        {
            ApplicationLinkRequest request = JiraConnectorUtils.getApplicationLinkRequest(appLink, Request.MethodType.GET, REST_URL_SERVER_INFO);
            request.addHeader("Content-Type", MediaType.APPLICATION_JSON);
            String responseString = request.execute();
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(responseString);
            return rootNode.path(JSON_PATH_BUILD_NUMBER).getLongValue();
        }
        catch (ResponseStatusException e) // We could connect to JIRA Server but the REST URL provided is version 4.x
        {
            LOG.warn(e.getMessage());
            return NOT_SUPPORTED_BUILD_NUMBER;
        }
        catch (Exception e) // In other cases we assume that it is supported version
        {
            LOG.warn(e.getMessage());
            return Long.MAX_VALUE;
        }
    }
}
