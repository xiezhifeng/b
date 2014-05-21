package com.atlassian.confluence.plugins.jiracharts;

import java.util.List;
import java.util.Locale;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
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
import com.atlassian.applinks.api.TypeNotInstalledException;
import com.atlassian.confluence.languages.LocaleManager;
import com.atlassian.confluence.plugins.jiracharts.model.StatTypeModel;
import com.atlassian.confluence.util.i18n.I18NBean;
import com.atlassian.confluence.util.i18n.I18NBeanFactory;
import com.atlassian.plugins.rest.common.security.AnonymousAllowed;

@Path("/stattypes")
@Produces({ MediaType.APPLICATION_JSON })
@AnonymousAllowed
public class JiraStatTypeResource
{
    private static final Logger logger = LoggerFactory.getLogger(JiraStatTypeResource.class);
    
    private ApplicationLinkService appLinkService;
    private JiraChartStatTypeManager jiraChartStatTypeManager;
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

    public void setJiraChartStatTypeManager(JiraChartStatTypeManager jiraChartStatTypeManager)
    {
        this.jiraChartStatTypeManager = jiraChartStatTypeManager;
    }

    @GET
    @Path("{appLinkId}")
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_JSON})
    @AnonymousAllowed
    public Response getStatTypes(@PathParam("appLinkId") String appLinkId)
    {
        try {
            ApplicationLink appLink = appLinkService.getApplicationLink(new ApplicationId(appLinkId));
            List<StatTypeModel> chartStatTypes = jiraChartStatTypeManager.getStatTypes(appLink);
            return Response.ok(chartStatTypes).build();
        } catch (TypeNotInstalledException tne) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(i18nBean().getText("jira.chart.statTypes.applink")).build();
        }
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
