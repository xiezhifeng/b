package com.atlassian.confluence.plugins.sprint.resource;

import com.atlassian.applinks.api.ApplicationId;
import com.atlassian.applinks.api.CredentialsRequiredException;
import com.atlassian.applinks.api.ReadOnlyApplicationLink;
import com.atlassian.confluence.extra.jira.ApplicationLinkResolver;
import com.atlassian.confluence.extra.jira.util.ResponseUtil;
import com.atlassian.confluence.plugins.sprint.services.JiraAgileService;
import com.atlassian.plugins.rest.common.security.AnonymousAllowed;
import com.atlassian.sal.api.message.I18nResolver;
import com.atlassian.sal.api.net.ResponseException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * The rest resource retrieves jira sprints rest url: /rest/jiraanywhere/1.0/jira/agile/
 */
@Path("/jira/agile")
@Produces({MediaType.APPLICATION_JSON})
@AnonymousAllowed
public class JiraSprintsResource
{
    private final JiraAgileService jiraAgileService;
    private final I18nResolver i18nResolver;
    private final ApplicationLinkResolver applicationLinkResolver;

    public JiraSprintsResource(JiraAgileService jiraAgileService, ApplicationLinkResolver applicationLinkResolver, I18nResolver i18nResolver)
    {
        this.jiraAgileService = jiraAgileService;
        this.i18nResolver = i18nResolver;
        this.applicationLinkResolver = applicationLinkResolver;
    }

    @GET
    @Path("/{jiraServerId}/boards")
    public Response getBoards(@PathParam("jiraServerId") String jiraServerId, @QueryParam("name") String nameFilter)
    {
        ReadOnlyApplicationLink applicationLink = applicationLinkResolver.getAppLinkForServer("", jiraServerId);
        try
        {
            return Response.ok(jiraAgileService.getBoards(applicationLink, nameFilter)).build();
        }
        catch (CredentialsRequiredException e)
        {
            String authorisationURI = e.getAuthorisationURI().toString();
            return ResponseUtil.buildUnauthorizedResponse(authorisationURI);
        }
        catch (ResponseException re)
        {
            return Response.serverError().entity(re.getMessage()).build();
        }
    }

    @GET
    @Path("/{jiraServerId}/boards/{boardId}/sprints")
    public Response getSprints(@PathParam("jiraServerId") ApplicationId jiraServerId, @PathParam("boardId") String boardId)
    {
        ReadOnlyApplicationLink applicationLink = applicationLinkResolver.getAppLinkForServer("", jiraServerId.get());
        try
        {
            return Response.ok(jiraAgileService.getSprints(applicationLink, boardId)).build();
        }
        catch (CredentialsRequiredException e)
        {
            String authorisationURI = e.getAuthorisationURI().toString();
            return ResponseUtil.buildUnauthorizedResponse(authorisationURI);
        }
        catch (ResponseException re)
        {
            return Response.serverError().entity(re.getMessage()).build();
        }
    }
}
