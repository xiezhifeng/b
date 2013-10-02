package com.atlassian.confluence.plugins.jira;

import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.applinks.api.ApplicationLinkService;
import com.atlassian.applinks.api.CredentialsRequiredException;
import com.atlassian.confluence.content.render.xhtml.DefaultConversionContext;
import com.atlassian.confluence.macro.Macro;
import com.atlassian.confluence.macro.MacroExecutionException;
import com.atlassian.confluence.macro.xhtml.MacroManager;
import com.atlassian.plugins.rest.common.security.AnonymousAllowed;
import com.atlassian.sal.api.net.Request.MethodType;
import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Path("refresh")
@Produces({MediaType.APPLICATION_JSON})
@AnonymousAllowed
public class JiraRefreshResource {
    private static final Logger log = LoggerFactory.getLogger(JiraRefreshResource.class);

    private MacroManager macroManager;


    @GET
    public Response getJiraHTML() throws MacroExecutionException
    {
        String text = "";
        Macro macro = macroManager.getMacroByName("jira");
        if (macro != null) {
            Map<String, String> params = Maps.newHashMap();
            params.put("server", "Your Company JIRA");
            params.put("serverId", "ec5a3b5c-fead-380a-8db5-3990a4039d3c");
            params.put("jqlQuery", "status=closed");
            text = macro.execute(params, null, new DefaultConversionContext(null));
        }
        return Response.ok(text).build();
    }

    public void setMacroManager(MacroManager macroManager)
    {
        this.macroManager = macroManager;
    }
}
