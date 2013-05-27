package com.atlassian.confluence.plugins.jira;

import com.atlassian.applinks.api.*;
import com.atlassian.confluence.extra.jira.TrustedAppsException;
import com.atlassian.sal.api.net.Request.MethodType;
import com.atlassian.sal.api.net.ResponseException;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/jira")
@Produces({MediaType.APPLICATION_JSON})
public class JiraFilterService {

    private static final String FILTER_REST_API_URI = "/rest/api/2/filter/";

    private ApplicationLinkService appLinkService;

    public JiraFilterService(ApplicationLinkService appLinkService)
    {
        this.appLinkService = appLinkService;
    }

    @GET
    @Path("appLink/{appLinkId}/filter/{filterId}")
    public Response getJiraFilterObject(@PathParam("appLinkId") String appLinkId, @PathParam("filterId") String filterId) throws TypeNotInstalledException
    {
        ApplicationLink appLink = appLinkService.getApplicationLink(new ApplicationId(appLinkId));
        if (appLink != null) {
            final ApplicationLinkRequestFactory requestFactory = appLink.createAuthenticatedRequestFactory();
            String finalUrl = appLink.getRpcUrl() + FILTER_REST_API_URI + filterId;

            try {
                ApplicationLinkRequest request = requestFactory.createRequest(MethodType.GET, finalUrl);
                Object response = request.execute(new ApplicationLinkResponseHandler<Object>() {
                    public Object handle(com.atlassian.sal.api.net.Response resp) throws ResponseException
                    {
                        if ("ERROR".equals(resp.getHeader("X-Seraph-Trusted-App-Status"))) {
                            String taError = resp.getHeader("X-Seraph-Trusted-App-Error");
                            throw new TrustedAppsException(taError);
                        }
                        return resp.getResponseBodyAsString();
                    }

                    public Object credentialsRequired(com.atlassian.sal.api.net.Response response) throws ResponseException
                    {
                        throw new ResponseException(new CredentialsRequiredException(requestFactory, ""));
                    }
                });

                return Response.ok(response).build();
            }
            catch (CredentialsRequiredException e) {
                return buildUnauthorizedResponse(e.getAuthorisationURI().toString());
            }
            catch (ResponseException e) {
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
