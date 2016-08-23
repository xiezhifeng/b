package com.atlassian.confluence.extra.jira.util;

import com.atlassian.applinks.api.ReadOnlyApplicationLink;
import com.atlassian.confluence.extra.jira.JiraIssuesMacro;
import com.atlassian.confluence.extra.jira.JiraRequestData;
import com.atlassian.confluence.extra.jira.helper.JiraJqlHelper;
import com.atlassian.confluence.macro.MacroExecutionException;
import com.atlassian.confluence.user.ConfluenceUser;
import com.atlassian.confluence.util.i18n.I18NBean;
import com.atlassian.confluence.xhtml.api.MacroDefinition;
import com.google.common.collect.Sets;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.util.URIUtil;
import org.apache.commons.lang3.StringUtils;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;

public class JiraIssueUtil
{
    private static final String JIRA_URL_KEY_PARAM = "url";
    private static final String JQL_QUERY = "jqlQuery";
    private static final String POSITIVE_INTEGER_REGEX = "[0-9]+";
    private static final String ISSUE_NAVIGATOR_PATH = "secure/IssueNavigator.jspa";


    /**
     * Get jira url
     * @param requestData request data
     * @param requestType request type KEY/JQL/XML
     * @param applicationLink application link
     * @param baseUrl base url
     * @return jira url
     */
    public static String getClickableUrl(String requestData, JiraIssuesMacro.Type requestType, ReadOnlyApplicationLink applicationLink, String baseUrl)
    {
        if (requestType != JiraIssuesMacro.Type.URL && applicationLink == null)
        {
            return null;
        }
        String clickableUrl = null;
        switch (requestType)
        {
            case URL:
                clickableUrl = makeClickableUrl(requestData);
                break;
            case JQL:
                clickableUrl = JiraUtil.normalizeUrl(applicationLink.getDisplayUrl())
                        + "/" + ISSUE_NAVIGATOR_PATH + "?reset=true&jqlQuery="
                        + JiraUtil.utf8Encode(requestData);
                break;
            case KEY:
                clickableUrl = JiraUtil.normalizeUrl(applicationLink.getDisplayUrl()) + "/browse/"
                        + JiraUtil.utf8Encode(requestData);
                break;
        }

        if (StringUtils.isNotEmpty(baseUrl))
        {
            clickableUrl = rebaseUrl(clickableUrl, baseUrl.trim());
        }
        return appendSourceParam(clickableUrl);
    }

    /**
     * parse jira issue macro param to type and data
     * @param params
     * @return JiraRequestData
     * @throws MacroExecutionException
     */
    public static JiraRequestData parseRequestData(Map<String, String> params, I18NBean i18NBean) throws MacroExecutionException
    {

        if(params.containsKey(JIRA_URL_KEY_PARAM))
        {
            return createJiraRequestData(params.get(JIRA_URL_KEY_PARAM), JiraIssuesMacro.Type.URL, i18NBean);
        }

        if(params.containsKey(JQL_QUERY))
        {
            return createJiraRequestData(params.get(JQL_QUERY), JiraIssuesMacro.Type.JQL, i18NBean);
        }

        if(params.containsKey(JiraIssuesMacro.KEY))
        {
            return createJiraRequestData(params.get(JiraIssuesMacro.KEY), JiraIssuesMacro.Type.KEY, i18NBean);
        }

        String requestData = getPrimaryParam(params, i18NBean);
        if (requestData.startsWith("http"))
        {
            return createJiraRequestData(requestData, JiraIssuesMacro.Type.URL, i18NBean);
        }

        Matcher keyMatcher = JiraJqlHelper.ISSUE_KEY_PATTERN.matcher(requestData);
        if (keyMatcher.find() && keyMatcher.start() == 0)
        {
            return createJiraRequestData(requestData, JiraIssuesMacro.Type.KEY, i18NBean);
        }

        return createJiraRequestData(requestData, JiraIssuesMacro.Type.JQL, i18NBean);
    }

    /**
     * Filter param
     * @param baseUrl
     * @param filter
     * @return String
     */
    public static String filterOutParam(StringBuffer baseUrl, final String filter)
    {
        int tempMaxParamLocation = baseUrl.indexOf(filter);
        if (tempMaxParamLocation != -1)
        {
            String value;
            int nextParam = baseUrl.indexOf("&", tempMaxParamLocation);
            // finding start of next param, if there is one. can't be ? because filter is before any next param
            if (nextParam != -1)
            {
                value = baseUrl.substring(
                        tempMaxParamLocation + filter.length(), nextParam);
                baseUrl.delete(tempMaxParamLocation, nextParam + 1);
            }
            else
            {
                value = baseUrl.substring(
                        tempMaxParamLocation + filter.length(),
                        baseUrl.length());
                // tempMaxParamLocation-1 to remove ?/& since
                // it won't be used by next param in this case

                baseUrl.delete(tempMaxParamLocation - 1, baseUrl.length());
            }
            return value;
        }
        return null;
    }


    private static JiraRequestData createJiraRequestData(String requestData, JiraIssuesMacro.Type requestType, I18NBean i18NBean) throws MacroExecutionException
    {
        if (requestType == JiraIssuesMacro.Type.KEY && requestData.indexOf(',') != -1)
        {
            String jql = "issuekey in (" + requestData + ")";
            return new JiraRequestData(jql, JiraIssuesMacro.Type.JQL);
        }

        if (requestType == JiraIssuesMacro.Type.URL)
        {
            try
            {
                new URL(requestData);
                requestData = URIUtil.decode(requestData);
                requestData = URIUtil.encodeQuery(requestData);
            }
            catch(MalformedURLException e)
            {
                throw new MacroExecutionException(i18NBean.getText("jiraissues.error.invalidurl", Arrays.asList(requestData)), e);
            }
            catch (URIException e)
            {
                throw new MacroExecutionException(e);
            }

            requestData = cleanUrlParentheses(requestData).trim().replaceFirst("/sr/jira.issueviews:searchrequest.*-rss/", "/sr/jira.issueviews:searchrequest-xml/");
        }
        return new JiraRequestData(requestData, requestType);
    }

    private static String cleanUrlParentheses(String url)
    {
        if (url.indexOf('(') > 0)
        {
            url = url.replaceAll("\\(", "%28");
        }

        if (url.indexOf(')') > 0)
        {
            url = url.replaceAll("\\)", "%29");
        }

        if (url.indexOf("&amp;") > 0)
        {
            url = url.replaceAll("&amp;", "&");
        }

        return url;
    }

    // url needs its own method because in the v2 macros params with equals
    // don't get saved into the map with numbered keys such as "0", unlike the
    // old macros
    private static String getPrimaryParam(Map<String, String> params, I18NBean i18NBean) throws MacroExecutionException
    {
        if(params.get("data") != null)
        {
            return params.get("data").trim();
        }

        Set<String> keys = params.keySet();
        for(String key : keys)
        {
            if(StringUtils.isNotBlank(key) && !JiraIssuesMacro.MACRO_PARAMS.contains(key))
            {
                return key.matches(POSITIVE_INTEGER_REGEX) ? params.get(key) : key + "=" + params.get(key);
            }
        }

        throw new MacroExecutionException(i18NBean.getText("jiraissues.error.invalidMacroFormat"));
    }

    private static String makeClickableUrl(String url)
    {
        StringBuffer link = new StringBuffer(url);
        filterOutParam(link, "view="); // was removing only view=rss but this
        // way is okay as long as there's not
        // another kind of view= that we should
        // keep
        filterOutParam(link, "decorator="); // was removing only decorator=none
        // but this way is okay as long as
        // there's not another kind of
        // decorator= that we should keep
        filterOutParam(link, "os_username=");
        filterOutParam(link, "os_password=");
        filterOutParam(link, "returnMax=");

        String linkString = link.toString();
        linkString = linkString.replaceFirst(
                "sr/jira.issueviews:searchrequest-xml/temp/SearchRequest.xml\\?",
                ISSUE_NAVIGATOR_PATH + "?reset=true&");
        linkString = linkString.replaceFirst(
                "sr/jira.issueviews:searchrequest-xml/temp/SearchRequest.xml",
                ISSUE_NAVIGATOR_PATH + "?reset=true");
        linkString = linkString.replaceFirst(
                "sr/jira.issueviews:searchrequest-xml/[0-9]+/SearchRequest-([0-9]+).xml\\?",
                ISSUE_NAVIGATOR_PATH + "?requestId=$1&");
        linkString = linkString.replaceFirst(
                "sr/jira.issueviews:searchrequest-xml/[0-9]+/SearchRequest-([0-9]+).xml",
                ISSUE_NAVIGATOR_PATH + "?requestId=$1");
        return linkString;
    }

    private static String rebaseUrl(String clickableUrl, String baseUrl)
    {
        return clickableUrl.replaceFirst("^" + // only at start of string
                        ".*?" + // minimum number of characters (the schema) followed
                        // by...
                        "://" + // literally: colon-slash-slash
                        "[^/]+", // one or more non-slash characters (the hostname)
                baseUrl);
    }

    private static String appendSourceParam(String clickableUrl)
    {
        String operator = clickableUrl.contains("?") ? "&" : "?";
        return clickableUrl + operator + "src=confmacro";
    }

    public static Set<String> getIssueKeys(List<MacroDefinition> macroDefinitions)
    {
        Set<String> issueKeys = Sets.newHashSet();
        for(MacroDefinition macroDefinition: macroDefinitions)
        {
            issueKeys.add(macroDefinition.getParameter(JiraIssuesMacro.KEY));
        }
        return issueKeys;
    }

    public static String getUserKey(ConfluenceUser user)
    {
        return user != null ? user.getKey().getStringValue() : "anonymous";
    }
}
