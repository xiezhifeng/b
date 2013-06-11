package com.atlassian.confluence.plugins.jira;

import com.atlassian.applinks.api.*;
import com.atlassian.confluence.extra.jira.JiraIssuesManager;
import com.atlassian.confluence.extra.jira.TrustedAppsException;
import com.atlassian.plugins.rest.common.security.AnonymousAllowed;
import com.atlassian.sal.api.net.Request.MethodType;
import com.atlassian.sal.api.net.ResponseException;
import com.google.gson.JsonObject;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/jira")
@Produces({MediaType.APPLICATION_JSON})
@AnonymousAllowed
public class JiraFilterService {

    private ApplicationLinkService appLinkService;

    private JiraIssuesManager jiraIssuesManager;

    public void setAppLinkService(ApplicationLinkService appLinkService)
    {
        this.appLinkService = appLinkService;
    }

    public void setJiraIssuesManager(JiraIssuesManager jiraIssuesManager)
    {
        this.jiraIssuesManager = jiraIssuesManager;
    }

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

    private Response buildUnauthorizedResponse(String oAuthenticationUri)
    {
        return Response.status(HttpServletResponse.SC_UNAUTHORIZED)
                .header("WWW-Authenticate", "OAuth realm=\"" + oAuthenticationUri + "\"")
                .build();
    }


}
