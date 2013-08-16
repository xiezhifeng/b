package com.atlassian.confluence.plugins.jira;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.applinks.api.ApplicationId;
import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.applinks.api.ApplicationLinkRequest;
import com.atlassian.applinks.api.ApplicationLinkRequestFactory;
import com.atlassian.applinks.api.ApplicationLinkService;
import com.atlassian.applinks.api.CredentialsRequiredException;
import com.atlassian.applinks.api.TypeNotInstalledException;
import com.atlassian.applinks.api.auth.Anonymous;
import com.atlassian.confluence.json.json.JsonObject;
import com.atlassian.confluence.languages.LocaleManager;
import com.atlassian.confluence.plugins.jira.beans.BasicJiraIssueBean;
import com.atlassian.confluence.plugins.jira.beans.JiraIssueBean;
import com.atlassian.confluence.util.i18n.I18NBean;
import com.atlassian.confluence.util.i18n.I18NBeanFactory;
import com.atlassian.plugins.rest.common.security.AnonymousAllowed;
import com.atlassian.sal.api.net.Request.MethodType;
import com.atlassian.sal.api.net.ResponseException;

@Path("/jira-issue")
public class CreateJiraIssueResource
{
    private static final Logger logger = LoggerFactory.getLogger(CreateJiraIssueResource.class);
    private static final String CREATE_ISSUE_REST_URI = "/rest/api/2/issue/";
    private final static Status BAD_REQUEST = Response.Status.BAD_REQUEST;

    private ApplicationLinkService appLinkService;
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

    @POST
    @Path("create-jira-issues/{appLinkId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @AnonymousAllowed
    public Response createJiraIssues(@PathParam("appLinkId") String appLinkId, List<JiraIssueBean> jiraIssueBeans)
    {
        ApplicationLink appLink = null;
        try
        {
            appLink = appLinkService.getApplicationLink(new ApplicationId(appLinkId));
            ApplicationLinkRequest request = createRequest(appLink);
            if (request != null)
            {
                request.addHeader("Content-Type", MediaType.APPLICATION_JSON);
                for (JiraIssueBean jiraIssueBean : jiraIssueBeans)
                {
                    createAndUpdateResultForJiraIssue(request, jiraIssueBean);
                }
            }
            else
            {
                return Response.status(BAD_REQUEST).entity(i18nBean().getText("create.jira.issue.error.request"))
                        .build();
            }
        }
        catch (TypeNotInstalledException e)
        {
            logger.error("Can not get the app link: ", e);
            return Response.status(BAD_REQUEST).entity(i18nBean().getText("create.jira.issue.error.applink")).build();
        }
        return Response.ok(jiraIssueBeans).build();
    }

    /**
     * Create request to JIRA, try create request by logged-in user first then
     * anonymous user
     * 
     * @param appLink jira server app link
     * @return applink's request, null if can not create
     */
    private ApplicationLinkRequest createRequest(ApplicationLink appLink)
    {
        ApplicationLinkRequestFactory requestFactory = null;
        ApplicationLinkRequest request = null;

        String url = appLink.getRpcUrl() + CREATE_ISSUE_REST_URI;

        requestFactory = appLink.createAuthenticatedRequestFactory();
        try
        {
            request = requestFactory.createRequest(MethodType.POST, url);
        }
        catch (CredentialsRequiredException e)
        {
            logger.error("Can not create request for logged-in user: ", e);
            requestFactory = appLink.createAuthenticatedRequestFactory(Anonymous.class);
            try
            {
                request = requestFactory.createRequest(MethodType.POST, url);
            }
            catch (CredentialsRequiredException e1)
            {
                logger.error("Can not create request for Anonymous: ", e1);
            }
        }

        return request;
    }

    /**
     * Call create JIRA issue and update it with issue was created using given
     * JIRA applink request
     * 
     * @param request
     * @param jiraIssueBean jira issue inputted
     */
    private void createAndUpdateResultForJiraIssue(ApplicationLinkRequest request, JiraIssueBean jiraIssueBean)
    {
        BasicJiraIssueBean jiraIssueResult = null;
        String jiraIssueJson = createJsonStringForJiraIssueBean(jiraIssueBean);
        request.setRequestBody(jiraIssueJson);
        String errorMessage = "";
        try
        {
            String issueResult = request.execute();
            ObjectMapper mapper = new ObjectMapper();
            try
            {
                jiraIssueResult = mapper.readValue(issueResult, BasicJiraIssueBean.class);
            }
            catch (Exception e)
            {
                logger.error("Has error when convert response to Basic Jira issue: ", e);
                errorMessage = e.getMessage();
            }
        }
        catch (ResponseException e)
        {
            logger.error("Request execute error: ", e);
            errorMessage = e.getMessage();
        }
        updateJiraIssueFromResult(jiraIssueBean, jiraIssueResult, errorMessage);
    }

    /**
     * Create JSON string for call JIRA create issue rest api
     * 
     * @param jiraIssueBean Jira issue inputted
     * @return json string
     */
    private String createJsonStringForJiraIssueBean(JiraIssueBean jiraIssueBean)
    {
        JsonObject issue = new JsonObject();
        JsonObject fields = new JsonObject();
        JsonObject project = new JsonObject();
        JsonObject issuetype = new JsonObject();

        project.setProperty("id", jiraIssueBean.getProjectId());
        issuetype.setProperty("id", jiraIssueBean.getIssueTypeId());
        fields.setProperty("project", project);
        fields.setProperty("summary", jiraIssueBean.getSummary());
        fields.setProperty("description", StringUtils.trimToEmpty(jiraIssueBean.getDescription()));
        fields.setProperty("issuetype", issuetype);
        issue.setProperty("fields", fields);
        return issue.serialize();
    }

    /**
     * When Jira issue successful created - the errorMessage is empty, update
     * issue Id, key, self value received
     * 
     * @param jiraIssueBean
     * @param jiraIssueResultBean
     * @param errorMessage
     */
    private void updateJiraIssueFromResult(JiraIssueBean jiraIssueBean, BasicJiraIssueBean jiraIssueResultBean,
            String errorMessage)
    {
        if (StringUtils.isEmpty(errorMessage))
        {
            jiraIssueBean.setId(jiraIssueResultBean.getId());
            jiraIssueBean.setKey(jiraIssueResultBean.getKey());
            jiraIssueBean.setSelf(jiraIssueResultBean.getSelf());
        }
        else
        {
            jiraIssueBean.setError(i18nBean().getText("create.jira.issue.error.create.issue",
                    Arrays.asList(errorMessage)));
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