package com.atlassian.confluence.extra.jira;

import com.atlassian.applinks.api.ApplicationLinkRequest;
import com.atlassian.applinks.api.ReadOnlyApplicationLink;
import com.atlassian.applinks.api.ReadOnlyApplicationLinkService;
import com.atlassian.applinks.api.application.jira.JiraApplicationType;
import com.atlassian.applinks.spi.auth.AuthenticationConfigurationManager;
import com.atlassian.confluence.extra.jira.util.JiraConnectorUtils;
import com.atlassian.confluence.plugins.jira.JiraServerBean;
import com.atlassian.sal.api.net.ResponseStatusException;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class DefaultJiraConnectorManager implements JiraConnectorManager
{
    private static final String JSON_PATH_BUILD_NUMBER = "buildNumber";
    private static final String REST_URL_SERVER_INFO = "/rest/api/2/serverInfo";
    public static final long NOT_SUPPORTED_BUILD_NUMBER = -1L;

    private ReadOnlyApplicationLinkService appLinkService;
    private AuthenticationConfigurationManager authenticationConfigurationManager;
    private LoadingCache<ReadOnlyApplicationLink, JiraServerBean> jiraServersCache;

    public DefaultJiraConnectorManager(ReadOnlyApplicationLinkService appLinkService, AuthenticationConfigurationManager authenticationConfigurationManager)
    {
        this.appLinkService = appLinkService;
        this.authenticationConfigurationManager = authenticationConfigurationManager;
    }

    @Override
    public List<JiraServerBean> getJiraServers()
    {
        Iterable<ReadOnlyApplicationLink> appLinks = appLinkService.getApplicationLinks(JiraApplicationType.class);
        if (appLinks == null)
        {
            return Collections.emptyList();
        }

        List<JiraServerBean> servers = new ArrayList<JiraServerBean>();
        for (ReadOnlyApplicationLink applicationLink : appLinks)
        {
            servers.add(getInternalJiraServer(applicationLink));
        }
        return servers;
    }

    @Override
    public JiraServerBean getJiraServer(ReadOnlyApplicationLink applicationLink)
    {
        return getInternalJiraServer(applicationLink);
    }

    @Override
    public void updateDetailJiraServerInfor(ReadOnlyApplicationLink applicationLink)
    {
        JiraServerBean jiraServerBean = getInternalJiraServer(applicationLink);
        // jiraServerBean might be null, we must check its existence first
        if (jiraServerBean != null)
        {
            jiraServerBean.setName(applicationLink.getName());
            jiraServerBean.setUrl(applicationLink.getDisplayUrl().toString());
        }
    }

    @Override
    public void updatePrimaryServer(ReadOnlyApplicationLink applicationLink)
    {
        List<JiraServerBean> jiraServerBeans = getJiraServers();
        for(JiraServerBean jiraServerBean : jiraServerBeans)
        {
            jiraServerBean.setSelected(applicationLink.getId().toString().equals(jiraServerBean.getId()));
        }
    }

    private JiraServerBean createJiraServerBean(ReadOnlyApplicationLink applicationLink)
    {
        return new JiraServerBean(applicationLink.getId().toString(), applicationLink.getDisplayUrl().toString(),
                applicationLink.getName(), applicationLink.isPrimary(), null, getServerBuildNumber(applicationLink));
    }

    private long getServerBuildNumber(ReadOnlyApplicationLink appLink)
    {
        try
        {
            ApplicationLinkRequest request = JiraConnectorUtils.getApplicationLinkRequest(appLink, com.atlassian.sal.api.net.Request.MethodType.GET, REST_URL_SERVER_INFO);
            request.addHeader("Content-Type", MediaType.APPLICATION_JSON);
            String responseString = request.execute();
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(responseString);
            return rootNode.path(JSON_PATH_BUILD_NUMBER).getLongValue();
        }
        catch (ResponseStatusException e) // We could connect to JIRA Server but the REST URL provided is version 4.x
        {
            return NOT_SUPPORTED_BUILD_NUMBER;
        }
        catch (Exception e) // In other cases we assume that it is supported version
        {
            return Long.MAX_VALUE;
        }
    }

    private JiraServerBean getInternalJiraServer(ReadOnlyApplicationLink applicationLink)
    {
        // applicationLink can be null, it should be checked first before getting the JiraServerBean instance from the Cache instance
        if (null != applicationLink)
        {
            JiraServerBean jiraServerBean = getJiraServersCache().getUnchecked(applicationLink);
            jiraServerBean.setAuthUrl(JiraConnectorUtils.getAuthUrl(authenticationConfigurationManager, applicationLink));
            return jiraServerBean;
        }
        return null; // return null if applicationLink is null
    }

    private LoadingCache<ReadOnlyApplicationLink, JiraServerBean> getJiraServersCache()
    {
        if (jiraServersCache == null)
        {
            jiraServersCache = CacheBuilder.newBuilder()
                    .expireAfterWrite(4, TimeUnit.HOURS)
                    .build(new CacheLoader<ReadOnlyApplicationLink, JiraServerBean>()
                    {
                        @Override
                        public JiraServerBean load(ReadOnlyApplicationLink applicationLink) throws Exception
                        {
                            return createJiraServerBean(applicationLink);
                        }
                    });
        }
        return jiraServersCache;
    }
}
