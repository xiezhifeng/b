package com.atlassian.confluence.extra.jira;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.core.MediaType;

import com.atlassian.confluence.extra.jira.util.JiraConnectorUtils;
import com.atlassian.confluence.plugins.jira.JiraServerBean;

import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.applinks.api.ApplicationLinkRequest;
import com.atlassian.applinks.api.ApplicationLinkService;
import com.atlassian.applinks.api.application.jira.JiraApplicationType;
import com.atlassian.applinks.spi.auth.AuthenticationConfigurationManager;
import com.atlassian.sal.api.net.Request;
import com.atlassian.sal.api.net.ResponseStatusException;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

public class DefaultJiraConnectorManager implements JiraConnectorManager
{
    private static final String JSON_PATH_BUILD_NUMBER = "buildNumber";
    private static final String REST_URL_SERVER_INFO = "/rest/api/2/serverInfo";
    public static final long NOT_SUPPORTED_BUILD_NUMBER = -1L;

    private final ApplicationLinkService appLinkService;
    private final AuthenticationConfigurationManager authenticationConfigurationManager;
    private Cache<ApplicationLink, JiraServerBean> jiraServersCache;
    private Cache<ApplicationLink, Boolean> jiraAvailabilityCache;

    public DefaultJiraConnectorManager(final ApplicationLinkService appLinkService, final AuthenticationConfigurationManager authenticationConfigurationManager)
    {
        this.appLinkService = appLinkService;
        this.authenticationConfigurationManager = authenticationConfigurationManager;
    }

    @Override
    public List<JiraServerBean> getJiraServers()
    {
        final Iterable<ApplicationLink> appLinks = appLinkService.getApplicationLinks(JiraApplicationType.class);
        if (appLinks == null)
        {
            return Collections.emptyList();
        }

        final List<JiraServerBean> servers = new ArrayList<JiraServerBean>();
        for (final ApplicationLink applicationLink : appLinks)
        {
            servers.add(getInternalJiraServer(applicationLink));
        }
        return servers;
    }

    @Override
    public JiraServerBean getJiraServer(final ApplicationLink applicationLink)
    {
        return getInternalJiraServer(applicationLink);
    }

    @Override
    public void updateDetailJiraServerInfor(final ApplicationLink applicationLink)
    {
        final JiraServerBean jiraServerBean = getInternalJiraServer(applicationLink);
        // jiraServerBean might be null, we must check its existence first
        if (jiraServerBean != null)
        {
            jiraServerBean.setName(applicationLink.getName());
            jiraServerBean.setUrl(applicationLink.getDisplayUrl().toString());
        }
    }

    @Override
    public void updatePrimaryServer(final ApplicationLink applicationLink)
    {
        final List<JiraServerBean> jiraServerBeans = getJiraServers();
        for(final JiraServerBean jiraServerBean : jiraServerBeans)
        {
            jiraServerBean.setSelected(applicationLink.getId().toString().equals(jiraServerBean.getId()));
        }
    }

    static JiraServerBean createJiraServerBean(final ApplicationLink applicationLink)
    {
        return new JiraServerBean(applicationLink.getId().toString(), applicationLink.getDisplayUrl().toString(),
                applicationLink.getName(), applicationLink.isPrimary(), null, getServerBuildNumber(applicationLink));
    }

    private static long getServerBuildNumber(final ApplicationLink appLink)
    {
        try
        {
            final ApplicationLinkRequest request = JiraConnectorUtils.getApplicationLinkRequest(appLink, Request.MethodType.GET, REST_URL_SERVER_INFO);
            request.addHeader("Content-Type", MediaType.APPLICATION_JSON);
            final String responseString = request.execute();
            final ObjectMapper mapper = new ObjectMapper();
            final JsonNode rootNode = mapper.readTree(responseString);
            return rootNode.path(JSON_PATH_BUILD_NUMBER).getLongValue();
        }
        catch (final ResponseStatusException e) // We could connect to JIRA Server but the REST URL provided is version 4.x
        {
            return NOT_SUPPORTED_BUILD_NUMBER;
        }
        catch (final Exception e) // In other cases we assume that it is supported version
        {
            return Long.MAX_VALUE;
        }
    }

    private JiraServerBean getInternalJiraServer(final ApplicationLink applicationLink)
    {
        // applicationLink can be null, it should be checked first before getting the JiraServerBean instance from the Cache instance
        if (null != applicationLink)
        {
            final JiraServerBean jiraServerBean = getJiraServersCache().getUnchecked(applicationLink);
            jiraServerBean.setAuthUrl(JiraConnectorUtils.getAuthUrl(authenticationConfigurationManager, applicationLink));
            return jiraServerBean;
        }
        return null; // return null if applicationLink is null
    }

    private Cache<ApplicationLink, JiraServerBean> getJiraServersCache()
    {
        if (jiraServersCache == null)
        {
            jiraServersCache = CacheBuilder.newBuilder()
                    .expireAfterWrite(4, TimeUnit.HOURS)
                    .build(new CacheLoader<ApplicationLink, JiraServerBean>()
                            {
                        @Override
                        public JiraServerBean load(final ApplicationLink applicationLink) throws Exception
                        {
                            return createJiraServerBean(applicationLink);
                        }
                            });
        }
        return jiraServersCache;
    }

    public Cache<ApplicationLink, Boolean> getJiraAvailabilityCache()
    {
        if (jiraAvailabilityCache == null)
        {
            jiraAvailabilityCache = CacheBuilder.newBuilder()
                    .expireAfterWrite(Integer.getInteger(JiraConnectorManager.RETRY_TIME_OUT_KEY, JiraConnectorManager.DEFAULT_RETRY_TIME_OUT_IN_MINUTE),
                            TimeUnit.MINUTES)
                    .build(new CacheLoader<ApplicationLink, Boolean>()
                            {
                        @Override
                        public Boolean load(final ApplicationLink applicationLink) throws Exception
                        {
                            return Boolean.TRUE;
                        }
                            });
        }
        return jiraAvailabilityCache;
    }

    @Override
    public boolean isJiraServerUp(final ApplicationLink applicationLink)
    {
        try
        {
            return getJiraAvailabilityCache().get(applicationLink);
        }
        catch (final ExecutionException e)
        {
            return true;
        }
    }

    @Override
    public void reportServerDown(final ApplicationLink applicationLink)
    {
        this.getJiraAvailabilityCache().asMap().put(applicationLink, false);
    }
}
