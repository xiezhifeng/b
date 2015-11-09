package com.atlassian.confluence.plugins.jira;

import com.atlassian.applinks.api.ApplicationId;
import com.atlassian.applinks.api.CredentialsRequiredException;
import com.atlassian.applinks.api.ReadOnlyApplicationLink;
import com.atlassian.applinks.api.ReadOnlyApplicationLinkService;
import com.atlassian.confluence.extra.jira.JiraIssuesManager;
import com.atlassian.confluence.extra.jira.util.ResponseUtil;
import com.atlassian.confluence.languages.LocaleManager;
import com.atlassian.confluence.plugins.jira.beans.JiraIssueBean;
import com.atlassian.confluence.util.i18n.I18NBean;
import com.atlassian.confluence.util.i18n.I18NBeanFactory;
import com.atlassian.plugins.rest.common.security.AnonymousAllowed;
import com.atlassian.sal.api.net.ResponseException;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Locale;

@Path("/jira-issue")
public class CreateJiraIssueResource
{
    private static final Logger logger = LoggerFactory.getLogger(CreateJiraIssueResource.class);
    
    private ReadOnlyApplicationLinkService appLinkService;
    private JiraIssuesManager jiraIssuesManager;
    private I18NBeanFactory i18NBeanFactory;
    private LocaleManager localeManager;

    public void setAppLinkService(ReadOnlyApplicationLinkService appLinkService)
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

    @POST
    @Path("create-jira-issues/{appLinkId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @AnonymousAllowed
    public Response createJiraIssues(@PathParam("appLinkId") String appLinkId, List<JiraIssueBean> jiraIssueBeans)
    {
        try
        {
            ReadOnlyApplicationLink appLink = appLinkService.getApplicationLink(new ApplicationId(appLinkId));
            List<JiraIssueBean> resultJiraIssueBeans = jiraIssuesManager.createIssues(jiraIssueBeans, appLink);
            
            Predicate<JiraIssueBean> jiraIssueSuccess = new Predicate<JiraIssueBean>()
            {
                public boolean apply(JiraIssueBean jiraIssueBean)
                {
                    return jiraIssueBean.getErrors() == null || jiraIssueBean.getErrors().isEmpty();
                }
            };
            
            if (Collections2.filter(resultJiraIssueBeans, jiraIssueSuccess).isEmpty())
            {
                return Response.status(Response.Status.BAD_REQUEST).entity(resultJiraIssueBeans).build();
            }
            return Response.ok(resultJiraIssueBeans).build();
        }
        catch (CredentialsRequiredException e)
        {
            String authorisationURI = e.getAuthorisationURI().toString();
            return ResponseUtil.buildUnauthorizedResponse(authorisationURI);
        }
        catch (ResponseException re)
        {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(re.getMessage()).build();
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