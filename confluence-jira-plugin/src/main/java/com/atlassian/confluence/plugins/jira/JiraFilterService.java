package com.atlassian.confluence.plugins.jira;

import com.atlassian.applinks.api.*;
import com.atlassian.confluence.content.render.xhtml.DefaultConversionContext;
import com.atlassian.confluence.extra.jira.JiraIssuesManager;
import com.atlassian.confluence.macro.Macro;
import com.atlassian.confluence.macro.MacroExecutionException;
import com.atlassian.confluence.macro.xhtml.MacroManager;
import com.atlassian.confluence.pages.AbstractPage;
import com.atlassian.confluence.pages.PageManager;
import com.atlassian.plugins.rest.common.security.AnonymousAllowed;
import com.atlassian.sal.api.net.ResponseException;
import com.google.common.collect.Maps;
import com.google.gson.JsonObject;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
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

    public JiraFilterService(ApplicationLinkService appLinkService, JiraIssuesManager jiraIssuesManager,
                             MacroManager macroManager, PageManager pageManager)
    {
        this.appLinkService = appLinkService;
        this.jiraIssuesManager = jiraIssuesManager;
        this.macroManager = macroManager;
        this.pageManager = pageManager;
    }

    @GET
    @Path("page/{pageId}/issue/{jiraissuekey}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @AnonymousAllowed
    public Response getRender(@PathParam("pageId") Long pageId, @PathParam("jiraissuekey") String jiraIssueKey) throws MacroExecutionException {
//        JsonObject epicResult = new JsonObject();
//        epicResult.addProperty("issueKey", "CONFDEV-123");
//        epicResult.addProperty("htmlPlaceHolder", "cai gi ma khong dc");
//        return Response.ok().entity(epicResult.toString()).build();

        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        Macro macro = macroManager.getMacroByName("jira");

        Map<String, String> params = Maps.newHashMap();
        String serverId = "4d18a5a4-281b-37bb-b34f-44787a6f1b89";
        params.put("key", jiraIssueKey);
        params.put("showSummary", Boolean.TRUE.toString());
        params.put("serverId", serverId);

        AbstractPage abstractPage = pageManager.getAbstractPage(pageId);

        String htmlPlaceHolder = macro.execute(params, null, new DefaultConversionContext(abstractPage.toPageContext()) );
        JsonObject epicResult = new JsonObject();
        epicResult.addProperty("epicKey", jiraIssueKey);
        epicResult.addProperty("htmlPlaceHolder", htmlPlaceHolder);

        return Response.ok(epicResult.toString()).build();
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
