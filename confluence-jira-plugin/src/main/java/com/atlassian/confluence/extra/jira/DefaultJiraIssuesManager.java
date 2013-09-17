package com.atlassian.confluence.extra.jira;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MediaType;
import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.applinks.api.ApplicationLinkRequest;
import com.atlassian.applinks.api.ApplicationLinkRequestFactory;
import com.atlassian.applinks.api.CredentialsRequiredException;
import com.atlassian.applinks.api.auth.Anonymous;
import com.atlassian.confluence.extra.jira.JiraResponseHandler.HandlerType;
import com.atlassian.confluence.extra.jira.util.JiraUtil;
import com.atlassian.confluence.plugins.jira.beans.BasicJiraIssueBean;
import com.atlassian.confluence.plugins.jira.beans.JiraIssueBean;
import com.atlassian.confluence.security.trust.TrustedTokenFactory;
import com.atlassian.confluence.util.http.HttpRequest;
import com.atlassian.confluence.util.http.HttpResponse;
import com.atlassian.confluence.util.http.HttpRetrievalService;
import com.atlassian.confluence.util.http.httpclient.TrustedTokenAuthenticator;
import com.atlassian.confluence.util.http.trust.TrustedConnectionStatus;
import com.atlassian.confluence.util.http.trust.TrustedConnectionStatusBuilder;
import com.atlassian.sal.api.net.Request;
import com.atlassian.sal.api.net.Request.MethodType;
import com.atlassian.sal.api.net.ResponseException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.log4j.Logger;

public class DefaultJiraIssuesManager implements JiraIssuesManager
{
    private static final Logger log = Logger.getLogger(DefaultJiraIssuesManager.class);

    // this isn't known to be the exact build number, but it is slightly greater
    // than or equal to the actual number, and people shouldn't really be using
    // the intervening versions anyway
    // private static final int MIN_JIRA_BUILD_FOR_SORTING = 328;

    private JiraIssuesColumnManager jiraIssuesColumnManager;

    private JiraIssuesUrlManager jiraIssuesUrlManager;

    private HttpRetrievalService httpRetrievalService;

    private TrustedTokenFactory trustedTokenFactory;

    private TrustedConnectionStatusBuilder trustedConnectionStatusBuilder;

    private TrustedApplicationConfig trustedAppConfig;

    // private final static String saxParserClass =
    // "org.apache.xerces.parsers.SAXParser";

    public DefaultJiraIssuesManager(
            JiraIssuesColumnManager jiraIssuesColumnManager,
            JiraIssuesUrlManager jiraIssuesUrlManager,
            HttpRetrievalService httpRetrievalService,
            TrustedTokenFactory trustedTokenFactory,
            TrustedConnectionStatusBuilder trustedConnectionStatusBuilder,
            TrustedApplicationConfig trustedAppConfig)
    {
        this.jiraIssuesColumnManager = jiraIssuesColumnManager;
        this.jiraIssuesUrlManager = jiraIssuesUrlManager;
        this.httpRetrievalService = httpRetrievalService;
        this.trustedTokenFactory = trustedTokenFactory;
        this.trustedConnectionStatusBuilder = trustedConnectionStatusBuilder;
        this.trustedAppConfig = trustedAppConfig;
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
        String finalUrl = getFieldRestrictedUrl(columns, url);
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
            String absoluteUrl = finalUrl;
            boolean useTrustedConnection = false;
            if (!finalUrl.startsWith("http"))
            {
                absoluteUrl = appLink != null ? appLink.getRpcUrl() + finalUrl : finalUrl;
            }
            else
            {
                //this for backwards compatibility
                useTrustedConnection = !forceAnonymous && trustedAppConfig.isUseTrustTokens();
            }

            HttpRequest req = httpRetrievalService.getDefaultRequestFor(absoluteUrl);
            if (useTrustedConnection)
            {
                req.setAuthenticator(new TrustedTokenAuthenticator(trustedTokenFactory));
            }
            HttpResponse resp = httpRetrievalService.get(req);

            TrustedConnectionStatus trustedConnectionStatus = null;
            if (useTrustedConnection)
            {
                trustedConnectionStatus = trustedConnectionStatusBuilder.getTrustedConnectionStatus(resp);
            }

            JiraUtil.checkForErrors(!resp.isFailed(), resp.getStatusCode(), resp.getStatusMessage());
            JiraResponseHandler responseHandler = JiraUtil.createResponseHandler(handlerType, url);
            responseHandler.handleJiraResponse(resp.getResponse(), trustedConnectionStatus);
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
        String url = appLink.getRpcUrl() + "/rest/api/2/filter/" + filterId;
        JsonObject jsonObject = executeREST(url, appLink);
        return jsonObject.get("jql").getAsString();

    }

    public String checkFilterId(final String filterId, ApplicationLink appLink) throws ResponseException
    {
        String url = appLink.getRpcUrl() + "/rest/api/2/filter/" + filterId;
        JsonObject jsonObject = executeREST(url, appLink);
        return jsonObject.get("id").getAsString();
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
        ApplicationLinkRequest request = createRequest(appLink);

        request.addHeader("Content-Type", MediaType.APPLICATION_JSON);
        for (JiraIssueBean jiraIssueBean : jiraIssueBeans)
        {
            createAndUpdateResultForJiraIssue(request, jiraIssueBean);
        }

        return jiraIssueBeans;
    }

    /**
     * Create request to JIRA, try create request by logged-in user first then
     * anonymous user
     * 
     * @param appLink jira server app link
     * @return applink's request
     * @throws CredentialsRequiredException
     */
    private ApplicationLinkRequest createRequest(ApplicationLink appLink) throws CredentialsRequiredException 
    {
        ApplicationLinkRequestFactory requestFactory = null;
        ApplicationLinkRequest request = null;

        String url = appLink.getRpcUrl() + "/rest/api/2/issue/";

        requestFactory = appLink.createAuthenticatedRequestFactory();
        try
        {
            request = requestFactory.createRequest(MethodType.POST, url);
        }
        catch (CredentialsRequiredException e)
        {
            requestFactory = appLink.createAuthenticatedRequestFactory(Anonymous.class);
            request = requestFactory.createRequest(MethodType.POST, url);
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

    private JsonObject executeREST(String url, ApplicationLink appLink) throws ResponseException
    {
        try
        {
            final ApplicationLinkRequestFactory requestFactory = appLink.createAuthenticatedRequestFactory();
            ApplicationLinkRequest request = requestFactory.createRequest(Request.MethodType.GET, url);
            return (JsonObject) new JsonParser().parse(request.execute());
        }
        catch (CredentialsRequiredException e)
        {
            return retrieveFilerByAnonymous(appLink, url);
        }
        catch (Exception e) {
            throw new ResponseException(e);
        }
    }

}
