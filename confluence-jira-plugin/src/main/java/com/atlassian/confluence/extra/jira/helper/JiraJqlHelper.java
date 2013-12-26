package com.atlassian.confluence.extra.jira.helper;

import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.confluence.extra.jira.JiraIssuesManager;
import com.atlassian.confluence.macro.MacroExecutionException;
import com.atlassian.confluence.util.i18n.I18NBean;
import com.atlassian.sal.api.net.ResponseException;
import org.apache.log4j.Logger;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JiraJqlHelper
{
    private static final Logger LOGGER = Logger.getLogger(JiraJqlHelper.class);

    public static final String ISSUE_KEY_REGEX = "(^|[^a-zA-Z]|\n)(([A-Z][A-Z]+)-[0-9]+)";
    public static final String XML_KEY_REGEX = ".+/([A-Za-z]+-[0-9]+)/.+";
    public static final String URL_KEY_REGEX = ".+/(i#)?browse/([A-Za-z]+-[0-9]+)";
    public static final String URL_JQL_REGEX = ".+(jqlQuery|jql)=([^&]+)";
    public static final String FILTER_URL_REGEX = ".+(requestId|filter)=([^&]+)";
    public static final String FILTER_XML_REGEX = ".+searchrequest-xml/([0-9]+)/SearchRequest.+";
    public static final String SORTING_REGEX = "(Order\\s*BY) (.?)";
    public static final String XML_SORT_REGEX = ".+(jqlQuery|jql)=([^&]+).+tempMax=([0-9]+)";

    public static final Pattern ISSUE_KEY_PATTERN = Pattern.compile(ISSUE_KEY_REGEX);
    public static final Pattern XML_KEY_PATTERN = Pattern.compile(XML_KEY_REGEX);
    public static final Pattern URL_KEY_PATTERN = Pattern.compile(URL_KEY_REGEX);
    public static final Pattern URL_JQL_PATTERN = Pattern.compile(URL_JQL_REGEX);
    public static final Pattern FILTER_URL_PATTERN = Pattern.compile(FILTER_URL_REGEX);
    public static final Pattern FILTER_XML_PATTERN = Pattern.compile(FILTER_XML_REGEX);
    public static final Pattern SORTING_PATTERN = Pattern.compile(SORTING_REGEX, Pattern.CASE_INSENSITIVE);
    public static final Pattern XML_SORTING_PATTERN = Pattern.compile(XML_SORT_REGEX, Pattern.CASE_INSENSITIVE);

    /**
     * Get JQL from URL
     * @param requestData
     * @return jql
     */
    public static String getJQLFromJQLURL(String requestData)
    {
        String jql = getValueByRegEx(requestData, URL_JQL_PATTERN, 2);
        if (jql != null)
        {
            try
            {
                // make sure we won't encode it twice
                jql = URLDecoder.decode(jql, "UTF-8");
            } catch (UnsupportedEncodingException e)
            {
                LOGGER.info("unable to decode jql: " + jql);
            }
        }
        return jql;
    }

    /**
     * Get issue key from URL
     * @param url
     * @return key
     */
    public static String getKeyFromURL(String url)
    {
        String key = getValueByRegEx(url, XML_KEY_PATTERN, 1);
        if (key != null)
        {
            return key;
        }

        key = getValueByRegEx(url, URL_KEY_PATTERN, 2);
        return key != null ? key : url;
    }

    /**
     * Get filter id from url
     * @param url
     * @return filter Id
     */
    public static String getFilterIdFromURL(String url)
    {
        String filterId = getValueByRegEx(url, FILTER_URL_PATTERN, 2);
        if (filterId != null)
        {
            return filterId;
        }

        filterId = getValueByRegEx(url, FILTER_XML_PATTERN, 1);
        return filterId != null ? filterId : url;
    }

    /**
     * Get JQL by filter Id
     * @param appLink
     * @param url
     * @param jiraIssuesManager
     * @param i18NBean
     * @return jql
     * @throws MacroExecutionException
     */
    public static String getJQLFromFilter(ApplicationLink appLink, String url, JiraIssuesManager jiraIssuesManager, I18NBean i18NBean) throws MacroExecutionException
    {
        String filterId = JiraJqlHelper.getFilterIdFromURL(url);
        try
        {
            return jiraIssuesManager.retrieveJQLFromFilter(filterId, appLink);
        } catch (ResponseException e)
        {
            throw new MacroExecutionException(i18NBean.getText("insert.jira.issue.message.nofilter"), e);
        }
    }

    public static String getValueByRegEx(String data, Pattern pattern, int group)
    {
        Matcher matcher = pattern.matcher(data);
        if (matcher.find())
        {
            return matcher.group(group);
        }

        return null;
    }

    /**
     * Check url is a key type or not
     * @param url
     * @return boolean type
     */
    public static boolean isKeyType(String url)
    {
        return url.matches(URL_KEY_REGEX) || url.matches(XML_KEY_REGEX);
    }

    /**
     * Check url is a filter type or not
     * @param url
     * @return boolean type
     */
    public static boolean isFilterType(String url)
    {
        return url.matches(FILTER_URL_REGEX) || url.matches(FILTER_XML_REGEX);
    }
}
