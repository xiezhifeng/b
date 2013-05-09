package com.atlassian.confluence.extra.jira;

import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.applinks.api.ApplicationLinkRequest;
import com.atlassian.applinks.api.ApplicationLinkRequestFactory;
import com.atlassian.applinks.api.ApplicationLinkResponseHandler;
import com.atlassian.applinks.api.ApplicationLinkService;
import com.atlassian.applinks.api.CredentialsRequiredException;
import com.atlassian.applinks.api.auth.Anonymous;
import com.atlassian.confluence.extra.jira.JiraIssuesMacro.ColumnInfo;
import com.atlassian.confluence.extra.jira.exception.AuthenticationException;
import com.atlassian.confluence.extra.jira.exception.MalformedRequestException;
import com.atlassian.confluence.util.http.trust.TrustedConnectionStatus;
import com.atlassian.confluence.util.http.trust.TrustedConnectionStatusBuilder;
import com.atlassian.confluence.util.http.HttpRetrievalService;
import com.atlassian.confluence.util.http.HttpRequest;
import com.atlassian.confluence.util.http.HttpResponse;
import com.atlassian.confluence.util.http.httpclient.TrustedTokenAuthenticator;
import com.atlassian.confluence.security.trust.TrustedTokenFactory;
import com.atlassian.sal.api.net.Request;
import com.atlassian.sal.api.net.Response;
import com.atlassian.sal.api.net.ResponseException;
import com.atlassian.sal.api.net.ResponseHandler;
import com.atlassian.sal.api.net.ReturningResponseHandler;
import com.atlassian.sal.api.net.Request.MethodType;

import org.jdom.Element;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.xpath.XPath;
import org.jdom.input.SAXBuilder;
import org.apache.log4j.Logger;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class DefaultJiraIssuesManager implements JiraIssuesManager
{
    private static final Logger log = Logger.getLogger(JiraIssuesManager.class);

    private static final int MIN_JIRA_BUILD_FOR_SORTING = 328; // this isn't known to be the exact build number, but it is slightly greater than or equal to the actual number, and people shouldn't really be using the intervening versions anyway


    private JiraIssuesColumnManager jiraIssuesColumnManager;

    private JiraIssuesUrlManager jiraIssuesUrlManager;

    private HttpRetrievalService httpRetrievalService;

    private TrustedTokenFactory trustedTokenFactory;

    private TrustedConnectionStatusBuilder trustedConnectionStatusBuilder;

    private TrustedApplicationConfig trustedAppConfig;

    private final static String saxParserClass = "org.apache.xerces.parsers.SAXParser";

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
    private void retrieveXML(String url, List<String> columns, final ApplicationLink appLink, boolean forceAnonymous,
                             final JiraResponseHandler responseHandler, boolean isAnonymous)
            throws IOException, CredentialsRequiredException, ResponseException
    {
        String finalUrl = getFieldRestrictedUrl(columns, url);
        if (appLink != null && !forceAnonymous)
        {
            final ApplicationLinkRequestFactory requestFactory = createRequestFactory(appLink, isAnonymous);
            ApplicationLinkRequest request = requestFactory.createRequest(MethodType.GET, finalUrl);
            try
            {
                request.execute(new ApplicationLinkResponseHandler<Object>()
                {

                    public Object handle(Response resp) throws ResponseException
                    {
                        try
                        {
                            if("ERROR".equals(resp.getHeader("X-Seraph-Trusted-App-Status")))
                            {
                                String taError = resp.getHeader("X-Seraph-Trusted-App-Error");
                                throw new TrustedAppsException(taError);
                            }
                            checkForErrors(resp.isSuccessful(), resp.getStatusCode(), resp.getStatusText());
                            responseHandler.handleJiraResponse(resp.getResponseBodyAsStream(), null);
                        }
                        catch (IOException e)
                        {
                            throw new ResponseException(e);
                        }
                        return null;
                    }

                    public Object credentialsRequired(Response response)
                            throws ResponseException
                    {
                        throw new ResponseException(new CredentialsRequiredException(requestFactory, ""));
                       // return null;
                    }
                });
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

            checkForErrors(!resp.isFailed(), resp.getStatusCode(), resp.getStatusMessage());
            responseHandler.handleJiraResponse(resp.getResponse(), trustedConnectionStatus);
        }
    }

    private ApplicationLinkRequestFactory createRequestFactory(ApplicationLink applicationLink, boolean isAnonymous)
    {
        if (isAnonymous)
        {
            return applicationLink.createAuthenticatedRequestFactory(Anonymous.class);
        }

        return applicationLink.createAuthenticatedRequestFactory();
    }

    private String getFieldRestrictedUrl(List<String> columns, String url)
    {
        StringBuffer urlBuffer = new StringBuffer(url);
        boolean hasCustomField = false;
        for (String name : columns)
        {
            if (name.equals("key")) continue;
            //special case, think this is a bug in jira. Has to plural and it needs an uppercase V
            else if (name.equalsIgnoreCase("fixversion"))
            {
                urlBuffer.append("&field=").append("fixVersions");
                continue;
            }
            if (!jiraIssuesColumnManager.isColumnBuiltIn(name) && !hasCustomField)
            {
                urlBuffer.append("&field=allcustom");
                hasCustomField=true;
            }
            urlBuffer.append("&field=").append(JiraIssuesMacro.utf8Encode(name));
        }
        urlBuffer.append("&field=link");
        return urlBuffer.toString();
    }


    private void checkForErrors(boolean success, int status, String statusMessage) throws IOException
    {
        if (!success)
        {
            // tempMax is invalid CONFJIRA-49

            if (status == HttpServletResponse.SC_FORBIDDEN)
            {
                throw new IllegalArgumentException(statusMessage);
            }
            else if (status == HttpServletResponse.SC_UNAUTHORIZED)
            {
                throw new AuthenticationException(statusMessage);
            }
            else if (status == HttpServletResponse.SC_BAD_REQUEST)
            {
                throw new MalformedRequestException(statusMessage);
            }
            else
            {
                log.error("Received HTTP " + status + " from server. Error message: " + StringUtils.defaultString(statusMessage, "No status message"));
                // we're not sure how to handle any other error conditions at this point
                throw new RuntimeException(statusMessage);
            }
        }
    }

    public Channel retrieveXMLAsChannel(final String url, List<String> columns, final ApplicationLink applink, boolean forceAnonymous) throws IOException, CredentialsRequiredException, ResponseException
    {
        InputStream responseStream = null;
        try
        {
            JiraChannelResponseHandler handler = new JiraChannelResponseHandler(url);
            retrieveXML(url, columns, applink, forceAnonymous, handler, false);

            return handler.getResponseChannel();
        }
        finally
        {
            IOUtils.closeQuietly(responseStream);
        }
    }

    public Channel retrieveXMLAsChannelByAnonymous(final String url, List<String> columns, final ApplicationLink applink, boolean forceAnonymous) throws IOException, CredentialsRequiredException, ResponseException
    {
        InputStream responseStream = null;
        try
        {
            JiraChannelResponseHandler handler = new JiraChannelResponseHandler(url);
            retrieveXML(url, columns, applink, forceAnonymous, handler, true);

            return handler.getResponseChannel();
        }
        finally
        {
            IOUtils.closeQuietly(responseStream);
        }
    }

    public String retrieveXMLAsString(String url, List<String> columns, ApplicationLink applink, boolean forceAnonymous) throws IOException, CredentialsRequiredException, ResponseException
    {
        InputStream responseStream = null;
        try
        {
            JiraStringResponseHandler handler = new JiraStringResponseHandler();
            retrieveXML(url, columns, applink, forceAnonymous, handler, false);
            return handler.getResponseBody();
        }
        finally
        {
            IOUtils.closeQuietly(responseStream);
        }
    }

    private static interface JiraResponseHandler
    {
        public void handleJiraResponse(InputStream in, TrustedConnectionStatus trustedConnectionStatus) throws IOException;
    }

    private static class JiraStringResponseHandler implements JiraResponseHandler
    {
        String responseBody;

        public String getResponseBody()
        {
            return responseBody;
        }

        public void handleJiraResponse(InputStream in, TrustedConnectionStatus trustedConnectionStatus) throws IOException
        {
            responseBody = IOUtils.toString(in);
        }
    }

    private static class JiraChannelResponseHandler implements JiraResponseHandler
    {
        Channel responseChannel;
        private String url;

        public JiraChannelResponseHandler(String url)
        {
            this.url = url;
        }

        public Channel getResponseChannel()
        {
            return responseChannel;
        }

        public void handleJiraResponse(InputStream in, TrustedConnectionStatus trustedConnectionStatus) throws IOException
        {
            responseChannel = new Channel(url,getChannelElement(in), trustedConnectionStatus);
        }
        Element getChannelElement(InputStream responseStream) throws IOException
        {
            try
            {
                SAXBuilder saxBuilder = SAXBuilderFactory.createSAXBuilder();
                Document document = saxBuilder.build(responseStream);
                Element root = document.getRootElement();
                if (root != null)
                {
                    return root.getChild("channel");
                }
                return null;
            }
            catch (JDOMException e)
            {
                log.error("Error while trying to assemble the issues returned in XML format: " + e.getMessage());
                throw new IOException(e.getMessage());
            }
        }
    }
}
