package com.atlassian.confluence.plugins.jira;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.applinks.api.ApplicationLinkService;
import com.atlassian.applinks.api.CredentialsRequiredException;
import com.atlassian.plugins.rest.common.security.AnonymousAllowed;
import com.atlassian.sal.api.net.Request.MethodType;

@Path("servers")
@Produces({ MediaType.APPLICATION_JSON })
@AnonymousAllowed
public class JiraConnectorStateResource
{
    private static final Logger log = LoggerFactory.getLogger(JiraConnectorStateResource.class);
    private static final String TOTAL_ISSUE_FOLLOW_JQL = "/rest/api/2/search?jql=%s&maxResults=0";

    private ApplicationLinkService appLinkService;

    public JiraConnectorStateResource(ApplicationLinkService appLinkService)
    {
        this.appLinkService = appLinkService;
    }

    @GET
    public Response getJiraServers()
    {
        Iterable<ApplicationLink> appLinks = appLinkService
                .getApplicationLinks(com.atlassian.applinks.api.application.jira.JiraApplicationType.class);
        if (appLinks != null)
        {
            List<JiraServerBean> servers = new ArrayList<JiraServerBean>();
            for (ApplicationLink link : appLinks)
            {
                String authUrl = null;
                try
                {
                    link.createAuthenticatedRequestFactory().createRequest(MethodType.GET, "");
                }
                catch (CredentialsRequiredException e)
                {
                    // if an exception is thrown, we need to prompt for oauth
                    authUrl = e.getAuthorisationURI().toString();
                }
                servers.add(new JiraServerBean(link.getId().toString(), link.getRpcUrl().toString(), link.getName(),
                        link.isPrimary(), authUrl));
            }
            if (!servers.isEmpty())
            {
                return Response.ok(servers).build();
            }
        }
        return Response.ok(Collections.EMPTY_LIST).build();
    }
}
