package com.atlassian.confluence.plugins.jira;

import com.atlassian.applinks.api.ApplicationId;
import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.applinks.api.ApplicationLinkService;
import com.atlassian.applinks.api.CredentialsRequiredException;
import com.atlassian.applinks.api.TypeNotInstalledException;
import com.atlassian.confluence.extra.jira.JiraIssuesManager;
import com.atlassian.confluence.extra.jira.api.services.AsyncJiraIssueBatchService;
import com.atlassian.confluence.extra.jira.model.JiraResponseData;
import com.atlassian.plugins.rest.common.security.AnonymousAllowed;
import com.atlassian.sal.api.net.ResponseException;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * This service request jira server to get JQL by save filter id
 * Rest URL: jira/rest/jiraanywhere/1.0/jira/appLink/{appLinkId}/filter/{filterId}
 */

@Path("/jira")
@Produces({MediaType.APPLICATION_JSON})
@AnonymousAllowed
public class JiraFilterService {

    private ApplicationLinkService appLinkService;

    private JiraIssuesManager jiraIssuesManager;

    private AsyncJiraIssueBatchService asyncJiraIssueBatchService;

    public JiraFilterService(ApplicationLinkService appLinkService, JiraIssuesManager jiraIssuesManager, AsyncJiraIssueBatchService asyncJiraIssueBatchService)
    {
        this.appLinkService = appLinkService;
        this.jiraIssuesManager = jiraIssuesManager;
        this.asyncJiraIssueBatchService = asyncJiraIssueBatchService;
    }

    /**
     * get rendered macro in HTML format
     * @param clientId Id for one or group of jira-issue
     * @return JiraResponseData in JSON format
     * @throws Exception
     */
    @GET
    @Path("clientId/{clientId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @AnonymousAllowed
    public Response getRenderIssueMacro(@PathParam("clientId") String clientId) throws Exception
    {
        JiraResponseData jiraResponseData = asyncJiraIssueBatchService.getAsyncJiraResults(clientId);

        if (jiraResponseData == null)
        {
            return Response.ok(String.format("Jira issues for this client %s is not available", clientId)).status(Response.Status.PRECONDITION_FAILED).build();
        }

        if (jiraResponseData.getStatus() == JiraResponseData.Status.WORKING)
        {
            return Response.ok().status(Response.Status.ACCEPTED).build();
        }
        return Response.ok(new Gson().toJson(jiraResponseData)).build();
    }

    /**
     *
     * @param appLinkId application link used to connect to jira server
     * @param filterId filter id on jira server
     * @return Response response data from jira
     * @throws TypeNotInstalledException
     */
    @GET
    @Path("appLink/{appLinkId}/filter/{filterId}")
    public Response getJiraFilterObject(@PathParam("appLinkId") String appLinkId, @PathParam("filterId") String filterId) throws TypeNotInstalledException
    {
        ApplicationLink appLink = appLinkService.getApplicationLink(new ApplicationId(appLinkId));
        if (appLink != null) {

            try {
                String jql = jiraIssuesManager.retrieveJQLFromFilter(filterId, appLink);
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("jql", jql);
                return Response.ok(jsonObject.toString()).build();
            }
            catch (ResponseException e)
            {
                if(e.getCause() instanceof CredentialsRequiredException) {
                    String authorisationURI = ((CredentialsRequiredException) e.getCause()).getAuthorisationURI().toString();
                    return buildUnauthorizedResponse(authorisationURI);
                }
                return Response.status(HttpServletResponse.SC_BAD_REQUEST).build();
            }
        }

        return Response.status(HttpServletResponse.SC_INTERNAL_SERVER_ERROR).build();
    }

    /**
     * Build response in case user not mapping
     * @param oAuthenticationUri link to authenticate
     * @return Response response with unauthorized status
     */
    private Response buildUnauthorizedResponse(String oAuthenticationUri)
    {
        return Response.status(HttpServletResponse.SC_UNAUTHORIZED)
                .header("WWW-Authenticate", "OAuth realm=\"" + oAuthenticationUri + "\"")
                .build();
    }
}
