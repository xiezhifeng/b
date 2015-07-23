package com.atlassian.confluence.plugins.jira;

import com.atlassian.applinks.api.*;
import com.atlassian.confluence.extra.jira.JiraIssuesManager;
import com.atlassian.confluence.extra.jira.api.services.AsyncJiraIssueBatchService;
import com.atlassian.confluence.extra.jira.model.JiraBatchResponseData;
import com.atlassian.confluence.macro.xhtml.MacroManager;
import com.atlassian.confluence.pages.PageManager;
import com.atlassian.plugins.rest.common.security.AnonymousAllowed;
import com.atlassian.sal.api.net.ResponseException;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;

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

    private MacroManager macroManager;

    private PageManager pageManager;

    private AsyncJiraIssueBatchService asyncJiraIssueBatchService;

    public JiraFilterService(ApplicationLinkService appLinkService, JiraIssuesManager jiraIssuesManager,
                             MacroManager macroManager, PageManager pageManager, AsyncJiraIssueBatchService asyncJiraIssueBatchService)
    {
        this.appLinkService = appLinkService;
        this.jiraIssuesManager = jiraIssuesManager;
        this.macroManager = macroManager;
        this.pageManager = pageManager;
        this.asyncJiraIssueBatchService = asyncJiraIssueBatchService;
    }

    @GET
    @Path("page/{pageId}/server/{serverId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @AnonymousAllowed
    public Response getRender(@PathParam("pageId") Long pageId, @PathParam("serverId") String serverId) throws Exception
    {
        JiraBatchResponseData jiraBatchResponseData = asyncJiraIssueBatchService.getAsyncBatchResults(pageId, serverId);

        JsonObject parentJsonObject = new JsonObject();
        JsonArray issueElements = new JsonArray();
        parentJsonObject.add("issues", issueElements);

        Map<String, List<String>> renderedIssues = jiraBatchResponseData.getHtmlMacro();
        for(String issueKey: renderedIssues.keySet())
        {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("issueKey", issueKey);
            if (renderedIssues.get(issueKey).size() > 1)
            {
                JsonArray issueArray = new JsonArray();
                for (String renderedHtml : renderedIssues.get(issueKey))
                {
                    issueArray.add(new JsonPrimitive(renderedHtml));
                }
                jsonObject.add("htmlPlaceHolder", issueArray);
            }
            else
            {
                jsonObject.addProperty("htmlPlaceHolder", renderedIssues.get(issueKey).get(0));
            }
            issueElements.add(jsonObject);
        }
        return Response.ok(parentJsonObject.toString()).build();
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
