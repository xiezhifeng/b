package com.atlassian.confluence.extra.jira.util;

import com.atlassian.confluence.extra.jira.JiraChannelResponseHandler;
import com.atlassian.confluence.extra.jira.JiraResponseHandler;
import com.atlassian.confluence.extra.jira.JiraResponseHandler.HandlerType;
import com.atlassian.confluence.extra.jira.JiraStringResponseHandler;
import com.atlassian.confluence.extra.jira.exception.AuthenticationException;
import com.atlassian.confluence.extra.jira.exception.MalformedRequestException;
import com.atlassian.confluence.json.parser.JSONArray;
import com.atlassian.confluence.json.parser.JSONException;
import com.atlassian.confluence.json.parser.JSONObject;
import com.atlassian.confluence.plugins.jira.beans.BasicJiraIssueBean;
import com.atlassian.confluence.plugins.jira.beans.JiraIssueBean;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map.Entry;

public class JiraUtil
{
    private static final Logger log = Logger.getLogger(JiraUtil.class);

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
                log.error("Received HTTP " + status + " from server. Error message: " +
                        StringUtils.defaultString(statusMessage, "No status message"));
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
        }
        else if (handlerType == HandlerType.STRING_HANDLER)
        {
            return new JiraStringResponseHandler();
        }
        else
        {
            throw new IllegalStateException("unable to handle " + handlerType);
        }
    }

    /**
     * Create JSON string for call JIRA create issue rest api
     * 
     * @param jiraIssueBean
     *            Jira issue inputted
     * @return json string
     */
    public static String createJsonStringForJiraIssueBean(JiraIssueBean jiraIssueBean)
    {
        JSONObject issue = new JSONObject();
        JSONObject fields = new JSONObject();
        JSONObject project = new JSONObject();
        JSONObject issuetype = new JSONObject();

        try
        {
            for (Entry<String, String> entry : jiraIssueBean.getFields().entrySet())
            {
                final String value = entry.getValue().trim();
                Object jsonVal;
                if (value.startsWith("[") && value.endsWith("]"))
                {
                    jsonVal = new JSONArray(value);
                }
                else if (value.startsWith("{") && value.endsWith("}"))
                {
                    jsonVal = new JSONObject(value);
                }
                else
                {
                    jsonVal = value;
                }
                fields.put(entry.getKey(), jsonVal);
            }

            if (jiraIssueBean.getProjectId() != null)
            {
                project.put("id", jiraIssueBean.getProjectId());
                fields.put("project", project);
            }

            if (jiraIssueBean.getIssueTypeId() != null)
            {
                issuetype.put("id", jiraIssueBean.getIssueTypeId());
                fields.put("issuetype", issuetype);
            }

            if (jiraIssueBean.getSummary() != null)
            {
                fields.put("summary", jiraIssueBean.getSummary());
            }

            if (jiraIssueBean.getDescription() != null)
            {
                fields.put("description", StringUtils.trimToEmpty(jiraIssueBean.getDescription()));
            }

            issue.put("fields", fields);
            return issue.toString();
        }
        catch (JSONException ex)
        {
            throw new IllegalArgumentException(ex);
        }
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
     * @param basicJiraIssueBean
     */
    public static void updateJiraIssue(JiraIssueBean jiraIssueBean, BasicJiraIssueBean basicJiraIssueBean)
    {
        jiraIssueBean.setId(basicJiraIssueBean.getId());
        jiraIssueBean.setKey(basicJiraIssueBean.getKey());
        jiraIssueBean.setSelf(basicJiraIssueBean.getSelf());
    }
}
