package com.atlassian.confluence.plugins.conluenceview.rest;

import javax.ws.rs.*;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import com.atlassian.confluence.plugins.conluenceview.query.ConfluencePagesQuery;
import com.atlassian.confluence.plugins.conluenceview.rest.dto.ConfluencePagesDto;
import com.atlassian.confluence.plugins.conluenceview.rest.dto.LinkedSpacesDto;
import com.atlassian.confluence.plugins.conluenceview.rest.params.PagesSearchParam;
import com.atlassian.confluence.plugins.conluenceview.services.ConfluenceJiraLinksService;
import com.atlassian.confluence.plugins.conluenceview.services.ConfluencePagesService;
import com.atlassian.plugins.rest.common.security.AnonymousAllowed;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

/**
 * The rest resource retrieves jira sprints rest url: /rest/jiraanywhere/1.0/jira/agile/
 */
@Path ("/confluence-view-in-jira")
@Consumes (APPLICATION_JSON)
@Produces (APPLICATION_JSON)
@AnonymousAllowed
public class ConfluenceInJiraViewResource
{
    private final ConfluencePagesService confluencePagesService;
    private final ConfluenceJiraLinksService confluenceJiraLinksService;


    public ConfluenceInJiraViewResource(ConfluencePagesService confluencePagesService, ConfluenceJiraLinksService confluenceJiraLinksService)
    {
        this.confluencePagesService = confluencePagesService;
        this.confluenceJiraLinksService = confluenceJiraLinksService;
    }

    @POST
    @Path ("/pages/search")
    public Response getPagesByIds(PagesSearchParam param)
    {
        ConfluencePagesDto result = confluencePagesService.getPagesByIds(ConfluencePagesQuery.newBuilder()
                .withCacheToken(param.getCacheToken()).withPageIds(param.getPageIds())
                .withSearchString(param.getSearchString())
                .withLimit(param.getLimit()).withStart(param.getStart()).build());

        return Response.ok(result).build();
    }

    @GET
    @Path ("/{spaceKey}/pages")
    public Response getPagesInSpace(@PathParam("spaceKey") String spaceKey,
                                    @QueryParam("start") int start, @QueryParam("limit") int limit)
    {
        ConfluencePagesDto result = confluencePagesService.getPagesInSpace(ConfluencePagesQuery.newBuilder()
                .withSpaceKey(spaceKey)
                .withLimit(limit).withStart(start).build());

        return Response.ok(result).build();
    }

    @GET
    @Path("/od-application-link-id")
    public Response getODApplicationId()
    {
        return Response.ok(confluenceJiraLinksService.getODApplicationLinkId()).build();
    }

    @GET
    @Path ("/linked-spaces")
    public Response getLinkedSpace(@QueryParam ("jiraUrl") String jiraUrl, @QueryParam ("projectKey") String projectKey)
    {
        return Response.ok(LinkedSpacesDto.newBuilder().withSpaces(confluenceJiraLinksService.getLinkedSpaces(jiraUrl, projectKey)).build()).build();
    }
}
