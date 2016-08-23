package com.atlassian.confluence.extra.jira.util;

import com.atlassian.applinks.api.ReadOnlyApplicationLink;
import com.atlassian.confluence.extra.jira.JiraChannelResponseHandler;
import com.atlassian.confluence.extra.jira.JiraIssuesMacro;
import com.atlassian.confluence.extra.jira.JiraResponseHandler;
import com.atlassian.confluence.extra.jira.JiraResponseHandler.HandlerType;
import com.atlassian.confluence.extra.jira.JiraStringResponseHandler;
import com.atlassian.confluence.extra.jira.exception.AuthenticationException;
import com.atlassian.confluence.extra.jira.exception.JiraPermissionException;
import com.atlassian.confluence.extra.jira.exception.JiraRuntimeException;
import com.atlassian.confluence.extra.jira.exception.MalformedRequestException;
import com.atlassian.confluence.extra.jira.helper.JiraJqlHelper;
import com.atlassian.confluence.json.parser.JSONArray;
import com.atlassian.confluence.json.parser.JSONException;
import com.atlassian.confluence.json.parser.JSONObject;
import com.atlassian.confluence.plugins.jira.beans.BasicJiraIssueBean;
import com.atlassian.confluence.plugins.jira.beans.JiraIssueBean;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.jdom.Attribute;
import org.jdom.Element;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class JiraUtil
{
    private static final Logger log = Logger.getLogger(JiraUtil.class);

    public static final String JIRA_PLUGIN_KEY = "confluence.extra.jira";

    public static final int DEFAULT_NUMBER_OF_ISSUES = 20;
    public static final int MAXIMUM_ISSUES = 1000;

    public static final int PARAM_POSITION_1 = 1;
    public static final int PARAM_POSITION_2 = 2;
    public static final int PARAM_POSITION_4 = 4;
    public static final int PARAM_POSITION_5 = 5;
    public static final int PARAM_POSITION_6 = 6;
    public static final int SUMMARY_PARAM_POSITION = 7;

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
                throw new JiraPermissionException(statusMessage);
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
                throw new JiraRuntimeException(statusMessage);
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

    /**
     * Replace issue link rpc url by display url
     * @param children
     * @param appLink
     */
    public static void checkAndCorrectDisplayUrl(List<Element> children, ReadOnlyApplicationLink appLink)
    {
        if (appLink == null || appLink.getDisplayUrl() == null || appLink.getDisplayUrl().equals(appLink.getRpcUrl()))
        {
            return;
        }
        for (Element element : children)
        {
            checkAndCorrectLink(element, appLink);
            checkAndCorrectIconURL(element, appLink);
        }
    }

    /**
     * 
     * @param element @Nullable issue element
     * @param appLink @Nullable application link
     */
    @SuppressWarnings("unchecked")
    public static void checkAndCorrectIconURL(Element element, ReadOnlyApplicationLink appLink)
    {
        if (appLink == null || element == null) 
        {
            return;
        }

        correctIconURL(element, appLink.getDisplayUrl().toString(), appLink.getRpcUrl().toString());
    }

    public static void correctIconURL(Element element, String displayUrl, String rpcUrl)
    {
        if (displayUrl == null || rpcUrl == null)
        {
            return;
        }

        for (Element child : (List<Element>) element.getChildren())
        {
            Attribute iconUrl = child.getAttribute("iconUrl");
            if (iconUrl == null || StringUtils.isEmpty(iconUrl.getValue()))
            {
                continue;
            }
            if (iconUrl.getValue().startsWith(rpcUrl))
            {
                iconUrl.setValue(iconUrl.getValue().replace(rpcUrl, displayUrl));
            }
        }
    }

    /**
     * 
     * @param element @Nullable issue element
     * @param appLink @Nullable application link
     */
    private static void checkAndCorrectLink(Element element, ReadOnlyApplicationLink appLink)
    {
        if (appLink == null || element == null || element.getChild("link") == null) 
        {
            return;
        }
        Element link = element.getChild("link");
        String issueLink = link.getValue();
        if (issueLink.startsWith(appLink.getRpcUrl().toString())) 
        {
            link.setText(issueLink.replace(appLink.getRpcUrl().toString(), appLink.getDisplayUrl().toString()));
        }
    }

    public static String utf8Encode(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // will never happen in a standard java runtime environment
            throw new RuntimeException(
                    "You appear to not be running on a standard Java Runtime Environment");
        }
    }

    public static String utf8Decode(String s)
    {
        try
        {
            return URLDecoder.decode(s, "UTF-8");
        }
        catch (UnsupportedEncodingException e)
        {
            throw new RuntimeException("You appear to not be running on a standard Java Runtime Environment");
        }
    }

    /**
     * Get jira issue type
     * @param params
     * @param requestType
     * @param requestData
     * @return JiraIssuesType
     */
    public static JiraIssuesMacro.JiraIssuesType getJiraIssuesType(Map<String, String> params, JiraIssuesMacro.Type requestType, String requestData)
    {
        if(requestType == JiraIssuesMacro.Type.KEY || JiraJqlHelper.isUrlKeyType(requestData))
        {
            return JiraIssuesMacro.JiraIssuesType.SINGLE;
        }

        if ("true".equalsIgnoreCase(params.get("count")))
        {
            return JiraIssuesMacro.JiraIssuesType.COUNT;
        }
        return JiraIssuesMacro.JiraIssuesType.TABLE;
    }

    public static String getParamValue(Map<String, String> params, String paramName, int paramPosition)
    {
        String param = params.get(paramName);
        if (param == null)
        {
            param = StringUtils.defaultString(params.get(String.valueOf(paramPosition)));
        }

        return param.trim();
    }

    /**
     * Gets maximum issues.
     * @param maximumNumber
     * @return maximum number
     */
    public static int getMaximumIssues(String maximumNumber)
    {
        String maximumIssuesStr = StringUtils.defaultString(maximumNumber, String.valueOf(JiraUtil.DEFAULT_NUMBER_OF_ISSUES));
        int maximumIssues = Integer.parseInt(maximumIssuesStr);
        if (maximumIssues > JiraUtil.MAXIMUM_ISSUES)
        {
            maximumIssues = JiraUtil.MAXIMUM_ISSUES;
        }
        return maximumIssues;
    }

    public static String normalizeUrl(URI rpcUrl)
    {
        String baseUrl = rpcUrl.toString();
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    /**
     * Escapes double quote in string.
     * @param str string 
     * @return string has been escaped.
     */
    public static String escapeDoubleQuote(String str)
    {
        if (StringUtils.isNotBlank(str))
        {
            return str.replace("\"", "\\\"");
        }
        return StringUtils.EMPTY;
    }

    /**
     * This method will try to get the key value from the parameters map first.
     * If it fails to get this value, it will try to get it from the default parameter.
     * @param parameters the map of all parameters
     * @return the key of the issue or null
     */
    public static String getSingleIssueKey(Map<String, String> parameters)
    {
        if (parameters == null)
        {
            return null;
        }
        String key = parameters.get("key");
        if (key == null)
        {
            String defaultParam = parameters.get("");
            if (defaultParam != null && JiraIssuePredicates.ISSUE_KEY_PATTERN.matcher(defaultParam).matches())
            {
                return defaultParam;
            }
            // null value will be returned because the value does not match the pattern (if forced so)
            return null;
        }
        return key;
    }
}
