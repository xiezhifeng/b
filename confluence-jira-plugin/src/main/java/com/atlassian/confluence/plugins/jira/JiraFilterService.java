package com.atlassian.confluence.plugins.jira;

import com.atlassian.applinks.api.ApplicationId;
import com.atlassian.applinks.api.CredentialsRequiredException;
import com.atlassian.applinks.api.ReadOnlyApplicationLink;
import com.atlassian.applinks.api.ReadOnlyApplicationLinkService;
import com.atlassian.applinks.api.TypeNotInstalledException;
import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.content.render.xhtml.DefaultConversionContext;
import com.atlassian.confluence.content.render.xhtml.Renderer;
import com.atlassian.confluence.extra.jira.DefaultJiraCacheManager;
import com.atlassian.confluence.extra.jira.JiraIssuesMacro;
import com.atlassian.confluence.extra.jira.JiraIssuesManager;
import com.atlassian.confluence.extra.jira.api.services.AsyncJiraIssueBatchService;
import com.atlassian.confluence.extra.jira.model.JiraResponseData;
import com.atlassian.confluence.plugins.jira.beans.MacroTableParam;
import com.atlassian.confluence.renderer.PageContext;
import com.atlassian.plugins.rest.common.security.AnonymousAllowed;
import com.atlassian.sal.api.net.ResponseException;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.net.URLDecoder;
import java.nio.charset.Charset;

/**
 * This service request jira server to get JQL by save filter id
 * Rest URL: jira/rest/jiraanywhere/1.0/jira/appLink/{appLinkId}/filter/{filterId}
 */

@Path("/jira")
@Produces({MediaType.APPLICATION_JSON})
@AnonymousAllowed
public class JiraFilterService {

    private ReadOnlyApplicationLinkService appLinkService;
    private JiraIssuesManager jiraIssuesManager;
    private AsyncJiraIssueBatchService asyncJiraIssueBatchService;

    private Renderer viewRenderer;

    public JiraFilterService(ReadOnlyApplicationLinkService appLinkService, JiraIssuesManager jiraIssuesManager, AsyncJiraIssueBatchService asyncJiraIssueBatchService,
                             Renderer viewRenderer)
    {
        this.appLinkService = appLinkService;
        this.jiraIssuesManager = jiraIssuesManager;
        this.asyncJiraIssueBatchService = asyncJiraIssueBatchService;

        this.viewRenderer = viewRenderer;
    }

    /**
     * get rendered macro in HTML format
     * @param clientIds Ids for one or group of jira-issue
     * @return JiraResponseData in JSON format
     * @throws Exception
     */
    @POST
    @Path("clientIds")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @AnonymousAllowed
    public Response getRenderedJiraMacros(@Nonnull String clientIds) throws Exception
    {
        String[] clientIdArr = StringUtils.split(clientIds, ",");
        JsonArray clientIdJsons = new JsonArray();
        Status globalStatus = Status.OK;
        for (String clientId : clientIdArr)
        {
            JiraResponseData jiraResponseData = asyncJiraIssueBatchService.getAsyncJiraResults(clientId);
            JsonObject resultJsonObject;
            if (jiraResponseData == null)
            {
                if (asyncJiraIssueBatchService.reprocessRequest(clientId))
                {
                    resultJsonObject = createResultJsonObject(clientId, Status.ACCEPTED.getStatusCode(), "");
                    globalStatus = Status.ACCEPTED;
                }
                else
                {
                    resultJsonObject = createResultJsonObject(clientId, Status.PRECONDITION_FAILED.getStatusCode(), String.format("Jira issues for client %s is not available", clientId));
                }
            }
            else if (jiraResponseData.getStatus() == JiraResponseData.Status.WORKING)
            {
                resultJsonObject = createResultJsonObject(clientId, Status.ACCEPTED.getStatusCode(), "");
                globalStatus = Status.ACCEPTED;
            }
            else
            {
                resultJsonObject = createResultJsonObject(clientId, Status.OK.getStatusCode(), new Gson().toJson(jiraResponseData));
            }
            clientIdJsons.add(resultJsonObject);
        }
        return Response.status(globalStatus).entity(clientIdJsons.toString()).build();
    }

    private JsonObject createResultJsonObject(String clientId, int statusCode, String data)
    {
        JsonObject responseDataJson = new JsonObject();
        responseDataJson.addProperty("clientId", clientId);
        responseDataJson.addProperty("data", data);
        responseDataJson.addProperty("status", statusCode);
        return responseDataJson;
    }

    /**
     * get rendered macro HTML format
     * @param macroTableParam request parameter
     * @return html data as String
     * @throws Exception
     */
    @POST
    @Path("renderTable")
    @Consumes({ MediaType.APPLICATION_JSON})
    @Produces({ MediaType.APPLICATION_JSON})
    @AnonymousAllowed
    public Response getRenderedJiraMacroTable(MacroTableParam macroTableParam) throws Exception
    {
        ConversionContext conversionContext = new DefaultConversionContext(new PageContext());
        conversionContext.setProperty(DefaultJiraCacheManager.PARAM_CLEAR_CACHE, macroTableParam.getClearCache());
        conversionContext.setProperty("orderColumnName", macroTableParam.getColumnName());
        conversionContext.setProperty("order", macroTableParam.getOrder());
        conversionContext.setProperty(JiraIssuesMacro.PARAM_PLACEHOLDER, Boolean.FALSE);
        String htmlTableContent =  viewRenderer.render(URLDecoder.decode(macroTableParam.getWikiMarkup(), Charset.defaultCharset().name()), conversionContext);
        return Response.ok(createResultJsonObject(null, Response.Status.OK.getStatusCode(), htmlTableContent).toString()).build();
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
        ReadOnlyApplicationLink appLink = appLinkService.getApplicationLink(new ApplicationId(appLinkId));
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
