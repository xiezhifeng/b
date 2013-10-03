package com.atlassian.confluence.plugins.jira;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.applinks.api.ApplicationId;
import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.applinks.api.ApplicationLinkService;
import com.atlassian.applinks.api.CredentialsRequiredException;
import com.atlassian.applinks.api.TypeNotInstalledException;
import com.atlassian.confluence.content.render.xhtml.DefaultConversionContext;
import com.atlassian.confluence.extra.jira.JiraIssuesCacheManager;
import com.atlassian.confluence.extra.jira.JiraIssuesManager;
import com.atlassian.confluence.extra.jira.util.ResponseUtil;
import com.atlassian.confluence.languages.LocaleManager;
import com.atlassian.confluence.macro.Macro;
import com.atlassian.confluence.macro.MacroExecutionException;
import com.atlassian.confluence.macro.xhtml.MacroManager;
import com.atlassian.confluence.plugins.jira.beans.JiraIssueBean;
import com.atlassian.confluence.util.i18n.I18NBean;
import com.atlassian.confluence.util.i18n.I18NBeanFactory;
import com.atlassian.plugins.rest.common.security.AnonymousAllowed;
import com.google.common.collect.Maps;

@Path("/jira-issue")
public class JiraIssuesResource
{
    private static final Logger logger = LoggerFactory.getLogger(JiraIssuesResource.class);
    
    private ApplicationLinkService appLinkService;
    private JiraIssuesManager jiraIssuesManager;
    private JiraIssuesCacheManager jiraIssuesCacheManager; 
    private MacroManager macroManager;
    private I18NBeanFactory i18NBeanFactory;
    private LocaleManager localeManager;

    public void setAppLinkService(ApplicationLinkService appLinkService)
    {
        this.appLinkService = appLinkService;
    }

    public void setI18NBeanFactory(I18NBeanFactory i18nBeanFactory)
    {
        i18NBeanFactory = i18nBeanFactory;
    }

    public void setLocaleManager(LocaleManager localeManager)
    {
        this.localeManager = localeManager;
    }

    public void setJiraIssuesManager(JiraIssuesManager jiraIssuesManager)
    {
        this.jiraIssuesManager = jiraIssuesManager;
    }

    public void setJiraIssuesCacheManager(JiraIssuesCacheManager jiraIssuesCacheManager)
    {
        this.jiraIssuesCacheManager = jiraIssuesCacheManager;
    }

    public void setMacroManager(MacroManager macroManager)
    {
        this.macroManager = macroManager;
    }

    @POST
    @Path("create-jira-issues/{appLinkId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @AnonymousAllowed
    public Response createJiraIssues(@PathParam("appLinkId") String appLinkId, List<JiraIssueBean> jiraIssueBeans)
    {
        try
        {
            ApplicationLink appLink = appLinkService.getApplicationLink(new ApplicationId(appLinkId));
            List<JiraIssueBean> resultJiraIssueBeans = jiraIssuesManager.createIssues(jiraIssueBeans, appLink);
            return Response.ok(resultJiraIssueBeans).build();
        }
        catch (TypeNotInstalledException e)
        {
            logger.error("Can not get the app link: ", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(i18nBean().getText("create.jira.issue.error.applink")).build();
        }
        catch (CredentialsRequiredException e)
        {
            String authorisationURI = ((CredentialsRequiredException) e.getCause()).getAuthorisationURI().toString();
            return ResponseUtil.buildUnauthorizedResponse(authorisationURI);
        }
    }

    @POST
    @Path("refresh-jira-issues/{appLinkId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @AnonymousAllowed
    public Response refreshJiraIssues(@PathParam("appLinkId") String appLinkId, List<JiraIssueBean> jiraIssueBeans)
    {
        String text = "";
        Macro macro = macroManager.getMacroByName("jira");
        if (macro != null) {
            Map<String, String> params = Maps.newHashMap();
            params.put("server", "Your Company JIRA");
            params.put("serverId", "ec5a3b5c-fead-380a-8db5-3990a4039d3c");
            params.put("jqlQuery", "status=closed");
            try
            {
                text = macro.execute(params, null, new DefaultConversionContext(null));
            }
            catch (MacroExecutionException e)
            {
                logger.error("Can not execute JIRA Issues Macro: ", e);
            }
        }
        return Response.ok(text).build();
    }

    private Locale getLocale()
    {
        return localeManager.getSiteDefaultLocale();
    }

    private I18NBean i18nBean()
    {
        return i18NBeanFactory.getI18NBean(getLocale());
    }
}