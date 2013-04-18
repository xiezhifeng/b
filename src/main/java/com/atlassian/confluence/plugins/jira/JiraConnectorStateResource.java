package com.atlassian.confluence.plugins.jira;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.applinks.api.ApplicationLinkService;
import com.atlassian.applinks.api.CredentialsRequiredException;
import com.atlassian.confluence.security.Permission;
import com.atlassian.confluence.security.PermissionManager;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.sal.api.net.Request.MethodType;

@Path("servers")
@Produces({MediaType.APPLICATION_JSON})
public class JiraConnectorStateResource
{
    private ApplicationLinkService appLinkService;

    private PermissionManager permissionManager;

    public JiraConnectorStateResource(ApplicationLinkService appLinkService, PermissionManager permissionManager)
    {
        this.appLinkService = appLinkService;
        this.permissionManager = permissionManager;
    }
  
    @GET
    public Response getJiraServers()
    {
        //check user is admin
        boolean isAdministrator = permissionManager.hasPermission(AuthenticatedUserThreadLocal.getUser(),
                Permission.ADMINISTER, PermissionManager.TARGET_APPLICATION);

        List<JiraServerBean> servers = new ArrayList<JiraServerBean>();

        Iterable<ApplicationLink> appLinks = appLinkService.getApplicationLinks(com.atlassian.applinks.api.application.jira.JiraApplicationType.class);
        if (appLinks != null)
        {
            for (ApplicationLink link : appLinks)
            {
                String authUrl = null;
                try
                {
                    link.createAuthenticatedRequestFactory().createRequest(MethodType.GET, "");
                }
                catch(CredentialsRequiredException e)
                {
                    // if an exception is thrown, we need to prompt for oauth                
                    authUrl = e.getAuthorisationURI().toString();
                }
                servers.add(new JiraServerBean(link.getId().toString(), link.getRpcUrl().toString(),link.getName(), link.isPrimary(), authUrl, isAdministrator));
            }
            if (!servers.isEmpty())
            {
                return Response.ok(servers).build();
            }
        }
        
        servers.add(new JiraServerBean(isAdministrator));
        
        return Response.ok(servers).build();
    }
    
    
}
