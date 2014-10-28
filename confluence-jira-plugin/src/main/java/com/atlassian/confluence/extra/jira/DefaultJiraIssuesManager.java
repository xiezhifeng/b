package com.atlassian.confluence.extra.jira;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.core.MediaType;

import com.atlassian.confluence.extra.jira.JiraResponseHandler.HandlerType;
import com.atlassian.confluence.extra.jira.util.JiraUtil;
import com.atlassian.confluence.plugins.jira.beans.BasicJiraIssueBean;
import com.atlassian.confluence.plugins.jira.beans.JiraIssueBean;
import com.atlassian.confluence.util.http.HttpRequest;
import com.atlassian.confluence.util.http.HttpResponse;
import com.atlassian.confluence.util.http.HttpRetrievalService;

import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.applinks.api.ApplicationLinkRequest;
import com.atlassian.applinks.api.ApplicationLinkRequestFactory;
import com.atlassian.applinks.api.CredentialsRequiredException;
import com.atlassian.applinks.api.auth.Anonymous;
import com.atlassian.sal.api.net.Request;
import com.atlassian.sal.api.net.Request.MethodType;
import com.atlassian.sal.api.net.Response;
import com.atlassian.sal.api.net.ResponseException;
import com.atlassian.sal.api.net.ResponseTransportException;
import com.atlassian.sal.api.net.ReturningResponseHandler;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.collect.Maps;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.httpclient.HttpStatus;

public class DefaultJiraIssuesManager implements JiraIssuesManager
{
    private static final String CREATE_JIRA_ISSUE_URL = "/rest/api/2/issue/";
    private static final String CREATE_JIRA_ISSUE_BATCH_URL = "/rest/api/2/issue/bulk";
    // this isn't known to be the exact build number, but it is slightly greater
    // than or equal to the actual number, and people shouldn't really be using
    // the intervening versions anyway
    // private static final int MIN_JIRA_BUILD_FOR_SORTING = 328;

    private final JiraIssuesColumnManager jiraIssuesColumnManager;

    private final JiraIssuesUrlManager jiraIssuesUrlManager;

    private final HttpRetrievalService httpRetrievalService;

    private final JiraConnectorManager jiraConnectorManager;

    private com.google.common.cache.Cache<ApplicationLink, Boolean> batchIssueCapableCache;
    // private final static String saxParserClass =
    // "org.apache.xerces.parsers.SAXParser";

    public DefaultJiraIssuesManager(
            final JiraIssuesColumnManager jiraIssuesColumnManager,
            final JiraIssuesUrlManager jiraIssuesUrlManager,
            final HttpRetrievalService httpRetrievalService,
            final JiraConnectorManager jiraConnectorManager
            )
    {
        this.jiraIssuesColumnManager = jiraIssuesColumnManager;
        this.jiraIssuesUrlManager = jiraIssuesUrlManager;
        this.httpRetrievalService = httpRetrievalService;
        this.jiraConnectorManager = jiraConnectorManager;
    }

    @Override
    public Map<String, String> getColumnMap(final String jiraIssuesUrl)
    {
        return jiraIssuesColumnManager.getColumnMap(jiraIssuesUrlManager.getRequestUrl(jiraIssuesUrl));
    }

    @Override
    public void setColumnMap(final String jiraIssuesUrl, final Map<String, String> columnMap)
    {
        jiraIssuesColumnManager.setColumnMap(jiraIssuesUrlManager.getRequestUrl(jiraIssuesUrl), columnMap);
    }

    @SuppressWarnings("unchecked")
    protected JiraResponseHandler retrieveXML(final String url, final List<String> columns, final ApplicationLink appLink,
            final boolean forceAnonymous, final boolean isAnonymous, final HandlerType handlerType, final boolean useCache)
                    throws IOException, CredentialsRequiredException, ResponseException
    {
        final String finalUrl = getFieldRestrictedUrl(columns, url);
        if (appLink != null && !forceAnonymous)
        {
            final ApplicationLinkRequestFactory requestFactory = createRequestFactory(appLink, isAnonymous);
            final ApplicationLinkRequest request = requestFactory.createRequest(MethodType.GET, finalUrl);
            try
            {
                final JiraAppLinkResponseHandler jiraApplinkResponseHandler = new JiraAppLinkResponseHandler(handlerType,
                        url, requestFactory);
                request.execute(jiraApplinkResponseHandler);
                return jiraApplinkResponseHandler.getResponseHandler();
            }
            catch (final ResponseException e)
            {
                if (e instanceof ResponseTransportException)
                {
                    jiraConnectorManager.reportServerDown(appLink);
                }
                // Jumping through hoops here to mimic exception handling in requests
                // that don't use applinks.
                final Throwable t = e.getCause();
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
        final boolean isRelativeUrl = !finalUrl.startsWith("http");
        final boolean isValidAppLink = appLink != null;

        final String absoluteUrl = isRelativeUrl && isValidAppLink ? appLink.getRpcUrl() + finalUrl : finalUrl;

        final HttpRequest req = httpRetrievalService.getDefaultRequestFor(absoluteUrl);
        final HttpResponse resp = httpRetrievalService.get(req);

        JiraUtil.checkForErrors(!resp.isFailed(), resp.getStatusCode(), resp.getStatusMessage());
        final JiraResponseHandler responseHandler = JiraUtil.createResponseHandler(handlerType, url);
        responseHandler.handleJiraResponse(resp.getResponse(), null);
        return responseHandler;
    }

    protected static ApplicationLinkRequestFactory createRequestFactory(final ApplicationLink applicationLink, final boolean isAnonymous)
    {
        if (isAnonymous)
        {
            return applicationLink.createAuthenticatedRequestFactory(Anonymous.class);
        }

        return applicationLink.createAuthenticatedRequestFactory();
    }

    protected String getFieldRestrictedUrl(final List<String> columns, final String url)
    {
        final StringBuffer urlBuffer = new StringBuffer(url);
        boolean hasCustomField = false;
        for (final String columnName : columns)
        {
            final String key = jiraIssuesColumnManager
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
            urlBuffer.append("&field=").append(JiraUtil.utf8Encode(key));
        }
        urlBuffer.append("&field=link");
        return urlBuffer.toString();
    }

    @Override
    public Channel retrieveXMLAsChannel(final String url, final List<String> columns, final ApplicationLink applink,
            final boolean forceAnonymous, final boolean useCache) throws IOException, CredentialsRequiredException,
            ResponseException
    {
        final JiraChannelResponseHandler handler = (JiraChannelResponseHandler) retrieveXML(url, columns, applink,
                forceAnonymous, false, HandlerType.CHANNEL_HANDLER, useCache);

        return handler.getResponseChannel();
    }

    @Override
    public Channel retrieveXMLAsChannelByAnonymous(final String url, final List<String> columns,
            final ApplicationLink applink, final boolean forceAnonymous, final boolean useCache) throws IOException,
            CredentialsRequiredException, ResponseException
    {
        final JiraChannelResponseHandler handler = (JiraChannelResponseHandler) retrieveXML(url, columns, applink,
                forceAnonymous, true, HandlerType.CHANNEL_HANDLER, useCache);

        return handler.getResponseChannel();
    }

    @Override
    public String retrieveXMLAsString(final String url, final List<String> columns, final ApplicationLink applink,
            final boolean forceAnonymous, final boolean useCache) throws IOException, CredentialsRequiredException,
            ResponseException
    {
        final JiraStringResponseHandler handler = (JiraStringResponseHandler) retrieveXML(url, columns, applink,
                forceAnonymous, false, HandlerType.STRING_HANDLER, useCache);
        return handler.getResponseBody();
    }

    @Override
    public String retrieveJQLFromFilter(final String filterId, final ApplicationLink appLink) throws ResponseException
    {
        JsonObject jsonObject;
        final String url = appLink.getRpcUrl() + "/rest/api/2/filter/" + filterId;
        try {
            final ApplicationLinkRequestFactory requestFactory = appLink.createAuthenticatedRequestFactory();
            final ApplicationLinkRequest request = requestFactory.createRequest(Request.MethodType.GET, url);
            jsonObject = (JsonObject) new JsonParser().parse(request.execute());

        }
        catch (final CredentialsRequiredException e)
        {
            jsonObject = retrieveFilerByAnonymous(appLink, url);
        }
        catch (final Exception e) {
            throw new ResponseException(e);
        }
        return jsonObject.get("jql").getAsString();

    }

    @Override
    public String executeJqlQuery(final String jqlQuery, final ApplicationLink applicationLink) throws CredentialsRequiredException, ResponseException
    {
        final String restUrl = "/rest/api/2/search?" + jqlQuery;
        final ApplicationLinkRequestFactory applicationLinkRequestFactory = applicationLink.createAuthenticatedRequestFactory();
        final ApplicationLinkRequest applicationLinkRequest = applicationLinkRequestFactory.createRequest(MethodType.GET, restUrl);
        return applicationLinkRequest.executeAndReturn(new ReturningResponseHandler<Response, String>()
                {
            @Override
            public String handle(final Response response) throws ResponseException
            {
                return response.getResponseBodyAsString();
            }
                });
    }

    private JsonObject retrieveFilerByAnonymous(final ApplicationLink appLink, final String url) throws ResponseException {
        try
        {
            final ApplicationLinkRequestFactory requestFactory = appLink.createAuthenticatedRequestFactory(Anonymous.class);
            final ApplicationLinkRequest request = requestFactory.createRequest(Request.MethodType.GET, url);
            return  (JsonObject) new JsonParser().parse(request.execute());
        }
        catch (final Exception e)
        {
            throw new ResponseException(e);
        }
    }

    @Override
    public List<JiraIssueBean> createIssues(final List<JiraIssueBean> jiraIssueBeans, final ApplicationLink appLink) throws CredentialsRequiredException, ResponseException
    {
        if(CollectionUtils.isEmpty(jiraIssueBeans))
        {
            throw new IllegalArgumentException("List of Jira issues cannot be empty");
        }
        if (jiraIssueBeans.size() > 1 && isSupportBatchIssue(appLink))
        {
            return createIssuesInBatch(jiraIssueBeans, appLink);
        }
        else
        {
            return createIssuesInSingle(jiraIssueBeans, appLink);
        }
    }

    protected List<JiraIssueBean> createIssuesInSingle(final List<JiraIssueBean> jiraIssueBeans, final ApplicationLink appLink) throws CredentialsRequiredException, ResponseException
    {
        final ApplicationLinkRequest request = createRequest(appLink, MethodType.POST, CREATE_JIRA_ISSUE_URL);

        request.addHeader("Content-Type", MediaType.APPLICATION_JSON);
        for (final JiraIssueBean jiraIssueBean : jiraIssueBeans)
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
    protected List<JiraIssueBean> createIssuesInBatch(final List<JiraIssueBean> jiraIssueBeans, final ApplicationLink appLink)
            throws CredentialsRequiredException, ResponseException
            {
        final ApplicationLinkRequest applinkRequest = createRequest(appLink, MethodType.POST, CREATE_JIRA_ISSUE_BATCH_URL);
        applinkRequest.addHeader("Content-Type", MediaType.APPLICATION_JSON);

        //build json string in batch (format: https://docs.atlassian.com/jira/REST/latest/#d2e1294)
        final JsonArray jsonIssues = new JsonArray();
        for(final JiraIssueBean jiraIssueBean: jiraIssueBeans)
        {
            final String jiraIssueJson = JiraUtil.createJsonStringForJiraIssueBean(jiraIssueBean);
            final JsonObject jsonObject = new JsonParser().parse(jiraIssueJson).getAsJsonObject();
            jsonIssues.add(jsonObject);
        }
        final JsonObject rootIssueJson = new JsonObject();
        rootIssueJson.add("issueUpdates", jsonIssues);

        //execute create jira issue
        applinkRequest.setRequestBody(rootIssueJson.toString());
        final String jiraIssueResponseString = executeApplinkRequest(applinkRequest);

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
    private ApplicationLinkRequest createRequest(final ApplicationLink appLink, final MethodType methodType, final String baseRestUrl) throws CredentialsRequiredException
    {
        ApplicationLinkRequestFactory requestFactory = null;
        ApplicationLinkRequest request = null;

        final String url = appLink.getRpcUrl() + baseRestUrl;

        requestFactory = appLink.createAuthenticatedRequestFactory();
        try
        {
            request = requestFactory.createRequest(methodType, url);
        }
        catch (final CredentialsRequiredException e)
        {
            requestFactory = appLink.createAuthenticatedRequestFactory(Anonymous.class);
            request = requestFactory.createRequest(methodType, url);
        }

        return request;
    }

    private String executeApplinkRequest(final ApplicationLinkRequest appLinkRequest) throws ResponseException
    {
        final String jiraIssueResponseString = appLinkRequest.executeAndReturn(new ReturningResponseHandler<Response, String>()
                {
            @Override
            public String handle(final Response response) throws ResponseException
            {
                if (response.isSuccessful() || response.getStatusCode() == HttpStatus.SC_BAD_REQUEST)
                {
                    return response.getResponseBodyAsString();
                }
                throw new ResponseException(String.format("Execute applink with error! [statusCode=%s, statusText=%s]", response.getStatusCode(), response.getStatusText()));
            }
                });
        return jiraIssueResponseString;
    }

    /**
     * Verify the support of creating issue by batching
     * @param appLink
     * @return boolean
     * @throws CredentialsRequiredException
     */
    protected Boolean isCreateIssueBatchUrlAvailable(final ApplicationLink appLink) throws CredentialsRequiredException
    {
        final ApplicationLinkRequest applinkRequest = createRequest(appLink, MethodType.GET, CREATE_JIRA_ISSUE_BATCH_URL);
        try
        {
            return applinkRequest.executeAndReturn(new ReturningResponseHandler<Response, Boolean>()
                    {
                @Override
                public Boolean handle(final Response response) throws ResponseException
                {
                    return response.getStatusCode() == HttpStatus.SC_METHOD_NOT_ALLOWED || response.isSuccessful();
                }
                    });
        } catch (final ResponseException e)
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
    private void updateResultForJiraIssueInBatch(final List<JiraIssueBean> jiraIssueBeansInput, final String jiraIssueResponseString) throws ResponseException
    {
        final JsonObject returnIssuesJson = new JsonParser().parse(jiraIssueResponseString).getAsJsonObject();

        //update error
        final JsonArray errorsJson = returnIssuesJson.getAsJsonArray("errors");
        for(final JsonElement errorElement: errorsJson)
        {
            final JsonObject errorObj = errorElement.getAsJsonObject();
            final int errorAt = errorObj.get("failedElementNumber").getAsInt();
            final Map<String, String> errorMessages = parseErrorMessages(errorObj.getAsJsonObject("elementErrors").getAsJsonObject("errors"));
            jiraIssueBeansInput.get(errorAt).setErrors(errorMessages);
        }

        //update success
        final JsonArray issuesJson = returnIssuesJson.getAsJsonArray("issues");
        int successItemIndex = 0;
        for(final JiraIssueBean jiraIssueBean: jiraIssueBeansInput)
        {
            //error case has been handled before.
            if (jiraIssueBean.getErrors() == null || jiraIssueBean.getErrors().isEmpty())
            {
                final String jsonIssueString = issuesJson.get(successItemIndex++).toString();
                try
                {
                    final BasicJiraIssueBean basicJiraIssueBeanReponse = JiraUtil.createBasicJiraIssueBeanFromResponse(jsonIssueString);
                    JiraUtil.updateJiraIssue(jiraIssueBean, basicJiraIssueBeanReponse);
                } catch (final IOException e)
                {
                    throw new ResponseException("There is a problem processing the response from JIRA: unrecognisable response:" + jsonIssueString, e);
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
    private void createAndUpdateResultForJiraIssue(final ApplicationLinkRequest applinkRequest, final JiraIssueBean jiraIssueBean) throws ResponseException
    {
        final String jiraIssueJson = JiraUtil.createJsonStringForJiraIssueBean(jiraIssueBean);
        applinkRequest.setRequestBody(jiraIssueJson);

        final String jiraIssueResponseString = executeApplinkRequest(applinkRequest);
        final JsonObject returnIssueJson = new JsonParser().parse(jiraIssueResponseString).getAsJsonObject();
        if (returnIssueJson.has("errors"))
        {
            jiraIssueBean.setErrors(parseErrorMessages(returnIssueJson.getAsJsonObject("errors")));
        }
        else
        {
            try
            {
                final BasicJiraIssueBean basicJiraIssueBeanReponse = JiraUtil.createBasicJiraIssueBeanFromResponse(jiraIssueResponseString);
                JiraUtil.updateJiraIssue(jiraIssueBean, basicJiraIssueBeanReponse);
            }
            catch (final IOException e)
            {
                throw new ResponseException("There is a problem processing the response from JIRA: unrecognisable response:" + returnIssueJson, e);
            }
        }
    }

    private Map<String, String> parseErrorMessages(final JsonObject jsonError)
    {
        final Map<String, String> errors = Maps.newHashMap();
        for (final Map.Entry<String, JsonElement> errorEntry : jsonError.entrySet())
        {
            final String field = errorEntry.getKey();
            final String errorMessage = errorEntry.getValue().getAsString();
            errors.put(field, errorMessage);
        }
        return errors;
    }

    protected Boolean isSupportBatchIssue(final ApplicationLink appLink)
    {
        return getBatchIssueCapableCache().getUnchecked(appLink);
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
                        public Boolean load(final ApplicationLink appLink)
                        {
                            try
                            {
                                return isCreateIssueBatchUrlAvailable(appLink);
                            } catch (final CredentialsRequiredException e)
                            {
                                return false;
                            }
                        }
                            });
        }
        return batchIssueCapableCache;
    }
}

