package com.atlassian.confluence.plugins.jira;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.atlassian.confluence.extra.jira.JiraConnectorManager;
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
    private JiraConnectorManager jiraConnectorManager;

    public JiraConnectorStateResource(JiraConnectorManager jiraConnectorManager)
    {
        this.jiraConnectorManager = jiraConnectorManager;
    }

    @GET
    public Response getJiraServers()
    {
        return Response.ok(jiraConnectorManager.getJiraServers()).build();
    }
}
