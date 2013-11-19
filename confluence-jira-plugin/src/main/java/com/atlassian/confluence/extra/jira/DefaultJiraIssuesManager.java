package com.atlassian.confluence.extra.jira;

import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.applinks.api.ApplicationLinkRequest;
import com.atlassian.applinks.api.ApplicationLinkRequestFactory;
import com.atlassian.applinks.api.CredentialsRequiredException;
import com.atlassian.applinks.api.auth.Anonymous;
import com.atlassian.confluence.extra.jira.JiraResponseHandler.HandlerType;
import com.atlassian.confluence.extra.jira.util.JiraUtil;
import com.atlassian.confluence.plugins.jira.beans.BasicJiraIssueBean;
import com.atlassian.confluence.plugins.jira.beans.JiraIssueBean;
import com.atlassian.confluence.util.http.HttpRequest;
import com.atlassian.confluence.util.http.HttpResponse;
import com.atlassian.confluence.util.http.HttpRetrievalService;
import com.atlassian.sal.api.net.Request;
import com.atlassian.sal.api.net.Request.MethodType;
import com.atlassian.sal.api.net.Response;
import com.atlassian.sal.api.net.ResponseException;
import com.atlassian.sal.api.net.ReturningResponseHandler;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class DefaultJiraIssuesManager implements JiraIssuesManager
{
    private static final Logger log = LoggerFactory.getLogger(DefaultJiraIssuesManager.class);
    private static final String CREATE_JIRA_ISSUE_URL = "/rest/api/2/issue/";
    private static final String CREATE_JIRA_ISSUE_BATCH_URL = "/rest/api/2/issue/bulk";
    // this isn't known to be the exact build number, but it is slightly greater
    // than or equal to the actual number, and people shouldn't really be using
    // the intervening versions anyway
    // private static final int MIN_JIRA_BUILD_FOR_SORTING = 328;

    private JiraIssuesColumnManager jiraIssuesColumnManager;

    private JiraIssuesUrlManager jiraIssuesUrlManager;

    private HttpRetrievalService httpRetrievalService;

    private com.google.common.cache.Cache<ApplicationLink, Boolean> batchIssueCapableCache;
    // private final static String saxParserClass =
    // "org.apache.xerces.parsers.SAXParser";

    public DefaultJiraIssuesManager(
            JiraIssuesColumnManager jiraIssuesColumnManager,
            JiraIssuesUrlManager jiraIssuesUrlManager,
            HttpRetrievalService httpRetrievalService
    )
    {
        this.jiraIssuesColumnManager = jiraIssuesColumnManager;
        this.jiraIssuesUrlManager = jiraIssuesUrlManager;
        this.httpRetrievalService = httpRetrievalService;
    }

    public Map<String, String> getColumnMap(String jiraIssuesUrl)
    {
        return jiraIssuesColumnManager.getColumnMap(jiraIssuesUrlManager.getRequestUrl(jiraIssuesUrl));
    }

    public void setColumnMap(String jiraIssuesUrl, Map<String, String> columnMap)
    {
        jiraIssuesColumnManager.setColumnMap(jiraIssuesUrlManager.getRequestUrl(jiraIssuesUrl), columnMap);
    }

    @SuppressWarnings("unchecked")
    protected JiraResponseHandler retrieveXML(final String url, List<String> columns, final ApplicationLink appLink,
            boolean forceAnonymous, boolean isAnonymous, final HandlerType handlerType, boolean useCache)
            throws IOException, CredentialsRequiredException, ResponseException
    {
        final String finalUrl = getFieldRestrictedUrl(columns, url);
        if (appLink != null && !forceAnonymous)
        {
            final ApplicationLinkRequestFactory requestFactory = createRequestFactory(appLink, isAnonymous);
            ApplicationLinkRequest request = requestFactory.createRequest(MethodType.GET, finalUrl);
            try
            {
                JiraAppLinkResponseHandler jiraApplinkResponseHandler = new JiraAppLinkResponseHandler(handlerType,
                        url, requestFactory);
                request.execute(jiraApplinkResponseHandler);
                return jiraApplinkResponseHandler.getResponseHandler();
            }
            catch (ResponseException e)
            {
                // Jumping through hoops here to mimic exception handling in requests
                // that don't use applinks. 
                Throwable t = e.getCause();
                if (t != null && t instanceof IOException)
                {
                    throw (IOException)t;
                }
                else if (t != null && t instanceof CredentialsRequiredException)
                {
                    throw (CredentialsRequiredException)t;
                }
                else
                {
                    throw e;
                }
            }
        }
        else
        {
            final boolean isRelativeUrl = !finalUrl.startsWith("http");
            final boolean isValidAppLink = appLink != null;

            final String absoluteUrl = isRelativeUrl && isValidAppLink
                    ? appLink.getRpcUrl() + finalUrl
                    : finalUrl;

            final HttpRequest req = httpRetrievalService.getDefaultRequestFor(absoluteUrl);
            final HttpResponse resp = httpRetrievalService.get(req);

            JiraUtil.checkForErrors(!resp.isFailed(), resp.getStatusCode(), resp.getStatusMessage());
            final JiraResponseHandler responseHandler = JiraUtil.createResponseHandler(handlerType, url);
            responseHandler.handleJiraResponse(resp.getResponse(), null);
            return responseHandler;
        }
    }

    protected ApplicationLinkRequestFactory createRequestFactory(ApplicationLink applicationLink, boolean isAnonymous)
    {
        if (isAnonymous)
        {
            return applicationLink.createAuthenticatedRequestFactory(Anonymous.class);
        }

        return applicationLink.createAuthenticatedRequestFactory();
    }

    protected String getFieldRestrictedUrl(List<String> columns, String url)
    {
        StringBuffer urlBuffer = new StringBuffer(url);
        boolean hasCustomField = false;
        for (String columnName : columns)
        {
            String key = jiraIssuesColumnManager
                    .getCanonicalFormOfBuiltInField(columnName);
            if (key.equals("key"))
            {
                continue;
            } else if (key.equalsIgnoreCase("fixversion"))
            {
                urlBuffer.append("&field=").append("fixVersions");
                continue;
            }
            if (!jiraIssuesColumnManager.isColumnBuiltIn(key) && !hasCustomField)
            {
                urlBuffer.append("&field=allcustom");
                hasCustomField=true;
            }
            urlBuffer.append("&field=").append(JiraIssuesMacro.utf8Encode(key));
        }
        urlBuffer.append("&field=link");
        return urlBuffer.toString();
    }

    public Channel retrieveXMLAsChannel(final String url, List<String> columns, final ApplicationLink applink,
            boolean forceAnonymous, boolean useCache) throws IOException, CredentialsRequiredException,
            ResponseException
    {
            JiraChannelResponseHandler handler = (JiraChannelResponseHandler) retrieveXML(url, columns, applink,
                    forceAnonymous, false, HandlerType.CHANNEL_HANDLER, useCache);

            return handler.getResponseChannel();
        }

    public Channel retrieveXMLAsChannelByAnonymous(final String url, List<String> columns,
            final ApplicationLink applink, boolean forceAnonymous, boolean useCache) throws IOException,
            CredentialsRequiredException, ResponseException
    {
            JiraChannelResponseHandler handler = (JiraChannelResponseHandler) retrieveXML(url, columns, applink,
                    forceAnonymous, true, HandlerType.CHANNEL_HANDLER, useCache);

            return handler.getResponseChannel();
        }

    public String retrieveXMLAsString(String url, List<String> columns, ApplicationLink applink,
            boolean forceAnonymous, boolean useCache) throws IOException, CredentialsRequiredException,
            ResponseException
    {
            JiraStringResponseHandler handler = (JiraStringResponseHandler) retrieveXML(url, columns, applink,
                    forceAnonymous, false, HandlerType.STRING_HANDLER, useCache);
            return handler.getResponseBody();
    }

    @Override
    public String retrieveJQLFromFilter(String filterId, ApplicationLink appLink) throws ResponseException
    {
        JsonObject jsonObject;
        String url = appLink.getRpcUrl() + "/rest/api/2/filter/" + filterId;
        try {
            final ApplicationLinkRequestFactory requestFactory = appLink.createAuthenticatedRequestFactory();
            ApplicationLinkRequest request = requestFactory.createRequest(Request.MethodType.GET, url);
            jsonObject = (JsonObject) new JsonParser().parse(request.execute());

        }
        catch (CredentialsRequiredException e)
        {
            jsonObject = retrieveFilerByAnonymous(appLink, url);
        }
        catch (Exception e) {
            throw new ResponseException(e);
        }
        return jsonObject.get("jql").getAsString();

    }

    public String executeJqlQuery(String jqlQuery, ApplicationLink applicationLink) throws CredentialsRequiredException, ResponseException
    {
        String restUrl = "/rest/api/2/search?" + jqlQuery;
        ApplicationLinkRequestFactory applicationLinkRequestFactory = applicationLink.createAuthenticatedRequestFactory();
        ApplicationLinkRequest applicationLinkRequest = applicationLinkRequestFactory.createRequest(MethodType.GET, restUrl);
        return applicationLinkRequest.executeAndReturn(new ReturningResponseHandler<Response, String>()
        {
            @Override
            public String handle(Response response) throws ResponseException
            {
                return response.getResponseBodyAsString();
            }
        });
    }
    
    private JsonObject retrieveFilerByAnonymous(ApplicationLink appLink, String url) throws ResponseException {
        try
        {
            final ApplicationLinkRequestFactory requestFactory = appLink.createAuthenticatedRequestFactory(Anonymous.class);
            ApplicationLinkRequest request = requestFactory.createRequest(Request.MethodType.GET, url);
            return  (JsonObject) new JsonParser().parse(request.execute());
        }
        catch (Exception e)
        {
            throw new ResponseException(e);
        }
    }

    @Override
    public List<JiraIssueBean> createIssues(List<JiraIssueBean> jiraIssueBeans, ApplicationLink appLink) throws CredentialsRequiredException
    {
        if(CollectionUtils.isEmpty(jiraIssueBeans))
        {
            throw new IllegalArgumentException("List of Jira issues cannot be empty");
        }
        if (jiraIssueBeans.size() > 1 && isSupportBatchIssue(appLink))
        {
            try
            {
                return createIssuesInBatch(jiraIssueBeans, appLink);
            } catch (ResponseException responseException)
            {
                throw new RuntimeException("Unexpected error when create issue in batch!", responseException);
            }
        } else
        {
            return createIssuesInSingle(jiraIssueBeans, appLink);
        }
    }

    protected Boolean isSupportBatchIssue(ApplicationLink appLink)
    {
        return getBatchIssueCapableCache().getUnchecked(appLink);
    }

    protected List<JiraIssueBean> createIssuesInSingle(List<JiraIssueBean> jiraIssueBeans, ApplicationLink appLink) throws CredentialsRequiredException
    {
        ApplicationLinkRequest request = createRequest(appLink, MethodType.POST, CREATE_JIRA_ISSUE_URL);

        request.addHeader("Content-Type", MediaType.APPLICATION_JSON);
        for (JiraIssueBean jiraIssueBean : jiraIssueBeans)
        {
            createAndUpdateResultForJiraIssue(request, jiraIssueBean);
        }

        return jiraIssueBeans;
    }
    /**
     * Create jira issue in batch
     * @param jiraIssueBeans
     * @param appLink
     * @return list of jiraissue after creating
     * @throws CredentialsRequiredException
     * @throws ResponseException
     */
    protected List<JiraIssueBean> createIssuesInBatch(List<JiraIssueBean> jiraIssueBeans, ApplicationLink appLink) 
            throws CredentialsRequiredException, ResponseException
    {
        ApplicationLinkRequest applinkRequest = createRequest(appLink, MethodType.POST, CREATE_JIRA_ISSUE_BATCH_URL);
        applinkRequest.addHeader("Content-Type", MediaType.APPLICATION_JSON);
        
        //build json string in batch (format: https://docs.atlassian.com/jira/REST/latest/#d2e1294)
        JsonArray jsonIssues = new JsonArray();
        for(JiraIssueBean jiraIssueBean: jiraIssueBeans)
        {
            String jiraIssueJson = JiraUtil.createJsonStringForJiraIssueBean(jiraIssueBean);
            JsonObject jsonObject = new JsonParser().parse(jiraIssueJson).getAsJsonObject();
            jsonIssues.add(jsonObject);
        }
        JsonObject rootIssueJson = new JsonObject();
        rootIssueJson.add("issueUpdates", jsonIssues);
        
        //execute create jira issue
        applinkRequest.setRequestBody(rootIssueJson.toString());
        String jiraIssueResponseString = applinkRequest.executeAndReturn(new ReturningResponseHandler<Response, String>()
        {
            @Override
            public String handle(Response response) throws ResponseException
            {
                if (response.isSuccessful() || response.getStatusCode() == HttpStatus.SC_BAD_REQUEST)
                {
                    return response.getResponseBodyAsString();
                }
                throw new ResponseException(String.format("Execute applink with error! [statusCode=%s, statusText=%s]",
                        response.getStatusCode(), response.getStatusText()));
            }
        });
        
        // update info back to previous JiraIssue
        updateResultForJiraIssueInBatch(jiraIssueBeans, jiraIssueResponseString);
        return jiraIssueBeans;
    }

    /**
     * Create request to JIRA, try create request by logged-in user first then
     * anonymous user
     * 
     * @param appLink jira server app link
     * @param baseRestUrl (without host) rest endpoint url
     * @return applink's request
     * @throws CredentialsRequiredException
     */
    private ApplicationLinkRequest createRequest(ApplicationLink appLink, MethodType methodType, String baseRestUrl) throws CredentialsRequiredException 
    {
        ApplicationLinkRequestFactory requestFactory = null;
        ApplicationLinkRequest request = null;

        String url = appLink.getRpcUrl() + baseRestUrl;

        requestFactory = appLink.createAuthenticatedRequestFactory();
        try
        {
            request = requestFactory.createRequest(methodType, url);
        }
        catch (CredentialsRequiredException e)
        {
            requestFactory = appLink.createAuthenticatedRequestFactory(Anonymous.class);
            request = requestFactory.createRequest(methodType, url);
        }

        return request;
    }

    /**
     * Verify the support of creating issue by batching
     * @param appLink
     * @return boolean
     * @throws CredentialsRequiredException
     */
    protected Boolean isCreateIssueBatchUrlAvailable(ApplicationLink appLink) throws CredentialsRequiredException
    {
        ApplicationLinkRequest applinkRequest = createRequest(appLink, MethodType.GET, CREATE_JIRA_ISSUE_BATCH_URL);
        try
        {
            return applinkRequest.executeAndReturn(new ReturningResponseHandler<Response, Boolean>()
            {
                @Override
                public Boolean handle(Response response) throws ResponseException
                {
                    return response.getStatusCode() == HttpStatus.SC_METHOD_NOT_ALLOWED || response.isSuccessful();
                }
            });
        } catch (ResponseException e)
        {
            return false;
        }
    }
    
    /**
     * Update info back to old JiraIssue
     * It could come with success/error in one response
     * @param jiraIssueBeansInput
     * @param jiraIssueResponseString
     */
    private void updateResultForJiraIssueInBatch(final List<JiraIssueBean> jiraIssueBeansInput, String jiraIssueResponseString) 
    {
        JsonObject returnIssuesJson = new JsonParser().parse(jiraIssueResponseString).getAsJsonObject();
        
        //update error
        JsonArray errorsJson = returnIssuesJson.getAsJsonArray("errors");
        for(JsonElement errorElement: errorsJson)
        {
            JsonObject errorObj = errorElement.getAsJsonObject();
            int errorAt = errorObj.get("failedElementNumber").getAsInt();
            String errorMsg = errorObj.getAsJsonObject("elementErrors").get("errors").toString();
            jiraIssueBeansInput.get(errorAt).setError(errorMsg);
        }
        
        //update success
        JsonArray issuesJson = returnIssuesJson.getAsJsonArray("issues");
        int successItemIndex = 0;
        for(JiraIssueBean jiraIssueBean: jiraIssueBeansInput)
        {
            //error case has been handled before.
            if (StringUtils.isBlank(jiraIssueBean.getError()))
            {
                String jsonIssueString = issuesJson.get(successItemIndex++).toString();
                try
                {
                    BasicJiraIssueBean basicJiraIssueBeanReponse = JiraUtil.createBasicJiraIssueBeanFromResponse(jsonIssueString);
                    JiraUtil.updateJiraIssue(jiraIssueBean, basicJiraIssueBeanReponse);
                } catch (IOException e)
                {
                    //this case should not happen because the error json string has been handled before
                    throw new RuntimeException("Create BasicJiraIssueBean error! JSON string is " + jsonIssueString, e);
                }
            }
        }

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
        String jiraIssueJson = JiraUtil.createJsonStringForJiraIssueBean(jiraIssueBean);
        request.setRequestBody(jiraIssueJson);
        try
        {
            String jiraIssueResponseString = request.execute();
            BasicJiraIssueBean basicJiraIssueBeanReponse = JiraUtil
                    .createBasicJiraIssueBeanFromResponse(jiraIssueResponseString);
            JiraUtil.updateJiraIssue(jiraIssueBean, basicJiraIssueBeanReponse);
        }
        catch (Exception e)
        {
            log.error("Create issue error: ", e);
            jiraIssueBean.setError(e.getMessage());
        }
    }

    private com.google.common.cache.Cache<ApplicationLink, Boolean> getBatchIssueCapableCache()
    {
        if (batchIssueCapableCache == null)
        {
            batchIssueCapableCache = CacheBuilder.newBuilder()
                    .expireAfterWrite(1, TimeUnit.DAYS)
                    .build(new CacheLoader<ApplicationLink, Boolean>()
                    {
                        @Override
                        public Boolean load(ApplicationLink appLink)
                        {
                            try
                            {
                                return isCreateIssueBatchUrlAvailable(appLink);
                            } catch (CredentialsRequiredException e)
                            {
                                return false;
                            }
                        }
                    });
        }
        return batchIssueCapableCache;
    }
}

