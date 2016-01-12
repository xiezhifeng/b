package com.atlassian.confluence.plugins.conluenceview.rest;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import com.atlassian.applinks.host.spi.HostApplication;
import com.atlassian.confluence.plugins.conluenceview.query.ConfluencePagesQuery;
import com.atlassian.confluence.plugins.conluenceview.rest.dto.ConfluencePagesDto;
import com.atlassian.confluence.plugins.conluenceview.rest.params.PagesSearchParam;
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
    private final HostApplication hostApplication;

    public ConfluenceInJiraViewResource(ConfluencePagesService confluencePagesService, HostApplication hostApplication)
    {
        this.confluencePagesService = confluencePagesService;
        this.hostApplication = hostApplication;
    }

    @POST
    @Path ("/pages/search")
    public Response getPages(PagesSearchParam param)
    {
        ConfluencePagesDto result = confluencePagesService.search(ConfluencePagesQuery.newBuilder()
                .withCacheToken(param.getCacheToken()).withPageIds(param.getPageIds())
                .withLimit(param.getLimit()).withStart(param.getStart()).build());

        return Response.ok(result).build();
    }

    @GET
    @Path("/od-application-link-id")
    public Response getODApplicationId()
    {
        return Response.ok(hostApplication.getId().get()).build();
    }
}
