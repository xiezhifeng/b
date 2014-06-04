package com.atlassian.confluence.plugins.jira;

import com.atlassian.confluence.extra.jira.JiraConnectorManager;
import com.atlassian.plugins.rest.common.security.AnonymousAllowed;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

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
