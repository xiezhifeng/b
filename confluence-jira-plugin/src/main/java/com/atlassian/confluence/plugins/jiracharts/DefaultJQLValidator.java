package com.atlassian.confluence.plugins.jiracharts;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.atlassian.confluence.extra.jira.DefaultJiraConnectorManager;
import com.atlassian.confluence.extra.jira.JiraConnectorManager;
import com.atlassian.confluence.extra.jira.util.JiraConnectorUtils;
import com.atlassian.confluence.plugins.jira.JiraServerBean;
import com.atlassian.confluence.util.i18n.I18NBeanFactory;
import org.apache.commons.httpclient.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.applinks.api.ApplicationLinkRequest;
import com.atlassian.applinks.api.ApplicationLinkResponseHandler;
import com.atlassian.applinks.api.ApplicationLinkService;
import com.atlassian.applinks.api.CredentialsRequiredException;
import com.atlassian.applinks.api.TypeNotInstalledException;
import com.atlassian.confluence.json.parser.JSONException;
import com.atlassian.confluence.json.parser.JSONObject;
import com.atlassian.confluence.macro.MacroExecutionException;
import com.atlassian.confluence.plugins.jiracharts.model.JQLValidationResult;
import com.atlassian.confluence.util.GeneralUtil;
import com.atlassian.confluence.web.UrlBuilder;
import com.atlassian.sal.api.net.Request;
import com.atlassian.sal.api.net.Response;
import com.atlassian.sal.api.net.ResponseException;
import com.google.gson.Gson;

class DefaultJQLValidator implements JQLValidator
{
    private static Logger log = LoggerFactory.getLogger(JiraChartMacro.class);

    private static final String JIRA_SEARCH_URL = "/rest/api/2/search";
    private static final String JIRA_FILTER_NAV_URL = "/secure/IssueNavigator.jspa?reset=true&mode=hide";
    private static final Long START_JIRA_UNSUPPORTED_BUILD_NUMBER = 6109L; //jira version 6.0.8
    private static final Long END_JIRA_UNSUPPORTED_BUILD_NUMBER = 6155L; //jira version 6.1.1

    private ApplicationLinkService applicationLinkService;
    private I18NBeanFactory i18NBeanFactory;
    private JiraConnectorManager jiraConnectorManager;

    public DefaultJQLValidator(ApplicationLinkService applicationLinkService, I18NBeanFactory i18NBeanFactory, JiraConnectorManager jiraConnectorManager)
    {
        this.applicationLinkService = applicationLinkService;
        this.jiraConnectorManager = jiraConnectorManager;
        this.i18NBeanFactory = i18NBeanFactory;
    }

    public JQLValidationResult doValidate(Map<String, String> parameters) throws MacroExecutionException
    {
        try
        {
            String jql = GeneralUtil.urlDecode(parameters.get("jql"));
            String appLinkId = parameters.get("serverId");
            ApplicationLink applicationLink = JiraConnectorUtils.getApplicationLink(applicationLinkService, appLinkId);
            validateJiraSupportedVersion(applicationLink);

            return validateJQL(applicationLink, jql);
        }
        catch (TypeNotInstalledException e)
        {
            log.debug("AppLink is not exits", e);
            throw new MacroExecutionException(i18NBeanFactory.getI18NBean().getText("jirachart.error.applicationLinkNotExist"));
        }

    }

    private JQLValidationResult validateJQL(ApplicationLink applicationLink, String jql) throws MacroExecutionException
    {
        JQLValidationResult result = new JQLValidationResult();
        try
        {
            validateInternal(applicationLink, jql, result);

            UrlBuilder builder = new UrlBuilder(applicationLink.getDisplayUrl().toString() + JIRA_FILTER_NAV_URL);
            builder.add("jqlQuery", jql);
            result.setFilterUrl(builder.toUrl());
        }
        catch (Exception e)
        {
            log.debug("Exception during make a call to JIRA via Applink", e);
            throw new MacroExecutionException(e);
        }

        return result;
    }

    private void validateJiraSupportedVersion(ApplicationLink appLinkId) throws MacroExecutionException
    {
        JiraServerBean jiraServerBean = jiraConnectorManager.getJiraServer(appLinkId);
        if(jiraServerBean != null)
        {
            Long buildNumber = jiraServerBean.getBuildNumber();
            if(buildNumber == DefaultJiraConnectorManager.NOT_SUPPORTED_BUILD_NUMBER ||
                    (buildNumber >= START_JIRA_UNSUPPORTED_BUILD_NUMBER && buildNumber < END_JIRA_UNSUPPORTED_BUILD_NUMBER))
            {
                throw new MacroExecutionException(i18NBeanFactory.getI18NBean().getText("jirachart.version.unsupported"));
            }
        }
    }
    
    /**
     * Call the Jira Rest endpoint to do validation
     * 
     * @param jql
     * @param result
     * @throws CredentialsRequiredException
     * @throws ResponseException
     */
    private void validateInternal(ApplicationLink applicationLink, String jql, JQLValidationResult result)
            throws CredentialsRequiredException, ResponseException
    {
        UrlBuilder urlBuilder = new UrlBuilder(JIRA_SEARCH_URL);
        urlBuilder.add("jql", jql).add("maxResults", 0);

        Object[] objects = JiraConnectorUtils.getApplicationLinkRequestWithOauUrl(applicationLink, Request.MethodType.GET, urlBuilder.toUrl());
        JiraResponse jiraResponse = ((ApplicationLinkRequest)objects[0]).execute(new JQLApplicationLinkResponseHandler());

        if(objects[1] != null)
        {
            result.setAuthUrl((String)objects[1]);
        }
        result.setErrorMgs(jiraResponse.getErrors());
        result.setIssueCount(jiraResponse.getIssueCount());
    }

    /**
     * Handler response from Jira Chart
     * 
     */
    private class JQLApplicationLinkResponseHandler implements ApplicationLinkResponseHandler<JiraResponse>
    {

        @Override
        public JiraResponse handle(Response response) throws ResponseException
        {
            JiraResponse returnValue = new JiraResponse();
            int responseStatus = response.getStatusCode();
            String responseBody = response.getResponseBodyAsString();

            try
            {
                JSONObject json = new JSONObject(responseBody);

                if (responseStatus >= HttpStatus.SC_BAD_REQUEST)
                {
                    String errorsStr = json.getString("errorMessages");
                    Gson gson = new Gson();
                    String[] errors = gson.fromJson(errorsStr, String[].class);
                    returnValue.setErrors(Arrays.asList(errors));
                }

                if (responseStatus == HttpStatus.SC_OK)
                {
                    returnValue.setIssueCount(json.getInt("total"));
                }

            }
            catch (JSONException ex)
            {
                throw new ResponseException("Could not parse json from JIRA", ex);
            }

            return returnValue;
        }

        @Override
        public JiraResponse credentialsRequired(Response paramResponse) throws ResponseException
        {
            return null;
        }

    }

    private class JiraResponse
    {

        private List<String> errors;

        private int issueCount;

        public List<String> getErrors()
        {
            return errors;
        }

        public void setErrors(List<String> errors)
        {
            this.errors = errors;
        }

        public int getIssueCount()
        {
            return issueCount;
        }

        public void setIssueCount(int issueCount)
        {
            this.issueCount = issueCount;
        }
    }
}
