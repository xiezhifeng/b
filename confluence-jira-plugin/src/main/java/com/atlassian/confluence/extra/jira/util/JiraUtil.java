package com.atlassian.confluence.extra.jira.util;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.applinks.api.ApplicationLinkRequest;
import com.atlassian.applinks.api.ApplicationLinkRequestFactory;
import com.atlassian.applinks.api.CredentialsRequiredException;
import com.atlassian.applinks.api.auth.Anonymous;
import com.atlassian.confluence.extra.jira.JiraChannelResponseHandler;
import com.atlassian.confluence.extra.jira.JiraResponseHandler;
import com.atlassian.confluence.extra.jira.JiraResponseHandler.HandlerType;
import com.atlassian.confluence.extra.jira.JiraStringResponseHandler;
import com.atlassian.confluence.extra.jira.exception.AuthenticationException;
import com.atlassian.confluence.extra.jira.exception.MalformedRequestException;
import com.atlassian.confluence.json.json.JsonObject;
import com.atlassian.confluence.json.parser.JSONException;
import com.atlassian.confluence.json.parser.JSONObject;
import com.atlassian.confluence.plugins.jira.beans.BasicJiraIssueBean;
import com.atlassian.confluence.plugins.jira.beans.JiraIssueBean;
import com.atlassian.sal.api.net.Request;
import com.atlassian.sal.api.net.ResponseException;

public class JiraUtil
{
    private static final Logger log = Logger.getLogger(JiraUtil.class);
    private static final String TOTAL_ISSUE_FOLLOW_JQL = "/rest/api/2/search?jql=%s&maxResults=0";

    private JiraUtil()
    {
        // use as static mode only
    }

    public static void checkForErrors(boolean success, int status, String statusMessage) throws IOException
    {
        if (!success)
        {
            // tempMax is invalid CONFJIRA-49

            if (status == HttpServletResponse.SC_FORBIDDEN)
            {
                throw new IllegalArgumentException(statusMessage);
            } else if (status == HttpServletResponse.SC_UNAUTHORIZED)
            {
                throw new AuthenticationException(statusMessage);
            } else if (status == HttpServletResponse.SC_BAD_REQUEST)
            {
                throw new MalformedRequestException(statusMessage);
            } else
            {
                log.error("Received HTTP " + status + " from server. Error message: "
                        + StringUtils.defaultString(statusMessage, "No status message"));
                // we're not sure how to handle any other error conditions at
                // this point
                throw new RuntimeException(statusMessage);
            }
        }
    }

    public static JiraResponseHandler createResponseHandler(HandlerType handlerType, String url)
    {
        if (handlerType == HandlerType.CHANNEL_HANDLER)
        {
            return new JiraChannelResponseHandler(url);
        } else if (handlerType == HandlerType.STRING_HANDLER)
        {
            return new JiraStringResponseHandler();
        } else
        {
            throw new IllegalStateException("unable to handle " + handlerType);
        }
    }
    
    /**
     * Create JSON string for call JIRA create issue rest api
     * 
     * @param jiraIssueBean Jira issue inputted
     * @return json string
     */
    public static String createJsonStringForJiraIssueBean(JiraIssueBean jiraIssueBean)
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

    public static BasicJiraIssueBean createBasicJiraIssueBeanFromResponse(String jiraIssueResponseString)
            throws JsonParseException, JsonMappingException, IOException
    {
        ObjectMapper mapper = new ObjectMapper();
        BasicJiraIssueBean basicJiraIssueBean;

        basicJiraIssueBean = mapper.readValue(jiraIssueResponseString, BasicJiraIssueBean.class);
        return basicJiraIssueBean;
    }
    
    /**
     * Update jira issue bean fields from basic jira issue bean
     * 
     * @param jiraIssueBean
     * @param jiraIssueResultBean
     */
    public static void updateJiraIssue(JiraIssueBean jiraIssueBean, BasicJiraIssueBean basicJiraIssueBean)
    {
        jiraIssueBean.setId(basicJiraIssueBean.getId());
        jiraIssueBean.setKey(basicJiraIssueBean.getKey());
        jiraIssueBean.setSelf(basicJiraIssueBean.getSelf());
    }
    
    public static int getTotalIssue(ApplicationLink appLink, String jql) throws Exception
    {
        String result = null;
        String url = appLink.getRpcUrl() + String.format(TOTAL_ISSUE_FOLLOW_JQL, URLEncoder.encode(jql, "UTF-8") );
        try
        {
            result = requestJiraByAuthenticatedUser(appLink, url);
        }
        catch(CredentialsRequiredException e)
        {
            result = requestJiraByAnonymousUser(appLink, url);
        }
        
        return getTotalFromStringJSON(result);
    }
    
    /**
     * Request jira by login user
     * @param appLink jira server app link
     * @param url rest url
     * @return response
     * @throws CredentialsRequiredException when user is not mapping
     * @throws ResponseException when have problem request jira server
     */
    private static String requestJiraByAuthenticatedUser(ApplicationLink appLink, String url)
            throws CredentialsRequiredException, ResponseException
    {
            final ApplicationLinkRequestFactory requestFactory = appLink.createAuthenticatedRequestFactory();
            ApplicationLinkRequest request = requestFactory.createRequest(Request.MethodType.GET, url);
            return request.execute();
    }
    
    /**
     * Request jira by anonymous user
     * @param appLink jira server app link
     * @param url rest url
     * @return response
     */
    private static String requestJiraByAnonymousUser(ApplicationLink appLink, String url)
    {
        try
        {
            final ApplicationLinkRequestFactory requestFactory = appLink.createAuthenticatedRequestFactory(Anonymous.class);
            ApplicationLinkRequest request = requestFactory.createRequest(Request.MethodType.GET, url);
            return request.execute();
        }
        catch (Exception e)
        {
            log.info("Can not retrieve data from jira server by anonymous user", e);
            return null;
        }
    }
    
    private static int getTotalFromStringJSON(String str) throws Exception
    {
            JSONObject jsonObject = new JSONObject(str);
            return jsonObject.getInt("total");
    }
}
