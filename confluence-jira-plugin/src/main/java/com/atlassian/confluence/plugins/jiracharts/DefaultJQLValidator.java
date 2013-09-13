package com.atlassian.confluence.plugins.jiracharts;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.applinks.api.ApplicationId;
import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.applinks.api.ApplicationLinkRequest;
import com.atlassian.applinks.api.ApplicationLinkRequestFactory;
import com.atlassian.applinks.api.ApplicationLinkResponseHandler;
import com.atlassian.applinks.api.ApplicationLinkService;
import com.atlassian.applinks.api.CredentialsRequiredException;
import com.atlassian.applinks.api.TypeNotInstalledException;
import com.atlassian.confluence.json.parser.JSONArray;
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

    private ApplicationLinkService applicationLinkService;

    public DefaultJQLValidator(ApplicationLinkService applicationLinkService)
    {
        this.applicationLinkService = applicationLinkService;
    }
    
    

    public JQLValidationResult doValidate(Map<String, String> parameters) throws MacroExecutionException
    {
        String jql = GeneralUtil.urlDecode(parameters.get("jql"));
        String appLinkId = parameters.get("serverId");

        JQLValidationResult result = new JQLValidationResult();
        try
        {
            ApplicationLinkRequestFactory requestFactory = getApplicationLinkRequestFactory(appLinkId);
            if (requestFactory == null)
            {
                return null;
            }

            validateInternal(requestFactory, jql, appLinkId, result);
            
            UrlBuilder builder = new UrlBuilder(getDisplayUrl(appLinkId) + JIRA_FILTER_NAV_URL);
            builder.add("jqlQuery", jql);
            result.setFilterUrl(builder.toUrl());
        }
        catch (CredentialsRequiredException e)
        {
            // we need use to input credential
            result.setAuthUrl(e.getAuthorisationURI().toString());
        }
        catch (ResponseException e)
        {
            log.error("Exception during make a call to JIRA via Applink", e);
            throw new MacroExecutionException(e);
        }
        catch (TypeNotInstalledException e)
        {
            log.error("AppLink is not exits", e);
            throw new MacroExecutionException("Applink is not exits", e);
        }

        return result;
    }
    
    private String getDisplayUrl(String appLinkId) throws TypeNotInstalledException{
        ApplicationLink appLink = applicationLinkService.getApplicationLink(new ApplicationId(appLinkId));
        return appLink.getDisplayUrl().toString();
    }

    private ApplicationLinkRequestFactory getApplicationLinkRequestFactory(String appLinkId)
            throws TypeNotInstalledException
    {
        ApplicationLink appLink = applicationLinkService.getApplicationLink(new ApplicationId(appLinkId));
        ApplicationLinkRequestFactory requestFactory = appLink.createAuthenticatedRequestFactory();

        return requestFactory;
    }

    /**
     * Call the Jira Rest endpoint to do validation
     * 
     * @param requestFactory
     * @param jql
     * @param appLinkId
     * @param result
     * @throws TypeNotInstalledException
     * @throws CredentialsRequiredException
     * @throws ResponseException
     */
    private void validateInternal(ApplicationLinkRequestFactory requestFactory, String jql, String appLinkId,
            JQLValidationResult result) throws TypeNotInstalledException, CredentialsRequiredException,
            ResponseException
    {
        UrlBuilder urlBuilder = new UrlBuilder(JIRA_SEARCH_URL);
        urlBuilder.add("jql", jql).add("maxResults", 0);
        String url = urlBuilder.toUrl();

        ApplicationLinkRequest request = requestFactory.createRequest(Request.MethodType.GET, url);
        JiraResponse jiraResponse = request.execute(new JQLApplicationLinkResponseHandler());

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

            int totalIssue = 0;
            try
            {
                JSONObject json = new JSONObject(responseBody);

                if (responseStatus >= 400)
                {
                    String errorsStr = json.getString("errorMessages");
                    Gson gson = new Gson();
                    String[] errors = gson.fromJson(errorsStr, String[].class);
                    returnValue.setErrors(Arrays.asList(errors));
                }

                if (responseStatus == 200)
                {
                    // get total count
                    totalIssue = json.getInt("total");
                    returnValue.setIssueCount(totalIssue);
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
