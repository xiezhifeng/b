package com.atlassian.confluence.extra.jira;

import com.atlassian.confluence.extra.jira.util.JiraUtil;
import org.apache.commons.lang3.StringUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DefaultJiraIssuesUrlManager implements JiraIssuesUrlManager
{
    private static final Pattern URL_WITHOUT_QUERY_STRING_PATTERN = Pattern.compile("(^.*)\\?");

    private static final Pattern TEMPMAX_REQUEST_PARAMETER_PATTERN = Pattern.compile("tempMax=\\d+");
    
    private static final Pattern JQLQUERY_REQUEST_PARAMER_PATTERN = Pattern.compile("jqlQuery=([^\\&]*)", Pattern.CASE_INSENSITIVE);
    
    private final JiraIssuesColumnManager jiraIssuesColumnManager;

    public DefaultJiraIssuesUrlManager(JiraIssuesColumnManager jiraIssuesColumnManager)
    {
        this.jiraIssuesColumnManager = jiraIssuesColumnManager;
    }

    public String getRequestUrl(String anyUrl)
    {
        Matcher urlWithoutQueryStringMatcher = URL_WITHOUT_QUERY_STRING_PATTERN.matcher(anyUrl);

        if (urlWithoutQueryStringMatcher.find())
        {
            return urlWithoutQueryStringMatcher.group(1);
        }
        else
        {
            /* The URL specified does not have a '?' in it. */
            return anyUrl;
        }
    }

    public String getJiraXmlUrlFromFlexigridRequest(String url, String resultsPerPage, String sortField, String sortOrder)
    {
        return getJiraXmlUrlFromFlexigridRequest(url, resultsPerPage, null, sortField, sortOrder);
    }

    /*
     * Modifies the URL contained in the specified {@link StringBuilder} so that all of its <tt>tempMax</tt>
     * request parameters get set to the specified results per page. If there is no <tt>tempMax</tt> parameter,
     * one will be appended.
     *
     * @param url
     * The URL built so far.
     * @param resultsPerPage
     * The results per page desired
     *
     * @returns
     * The adjusted URL.
     */
    private String getAbsoluteUrlWithTempMaxRequestParametersSetToResultsPerPage(String url, String resultsPerPage)
    {
        Matcher tempMaxMatcher = TEMPMAX_REQUEST_PARAMETER_PATTERN.matcher(url);
        StringBuilder urlBuilder = new StringBuilder();

        if (tempMaxMatcher.find())
        {
            urlBuilder.setLength(0);
            urlBuilder.append(tempMaxMatcher.replaceAll(new StringBuilder("tempMax=").append(resultsPerPage).toString()));
        }
        else
        {
            urlBuilder.append(url).append(url.indexOf('?') >= 0 ? '&' : '?').append("tempMax=").append(resultsPerPage).toString();
        }

        return urlBuilder.toString();
    }

    public String getJiraXmlUrlFromFlexigridRequest(
            String url,
            String resultsPerPage,
            String page,
            String sortField,
            String sortOrder)
    {
        StringBuilder jiraXmlUrlBuilder = new StringBuilder(url);
        
        /*
         * The reason why we append "?1=1" is to simplify the code that appends
         * requests parameters in the rest of the method. The appending code does not need
         * to know if it's the first parameter or not (to really, decide if it should prefix '&' or '?' to the parameter).
         * The append code can just assume that the parameters it appends will _never_ be the first.
         *
         * Anyways, the bogus parameter is removed at the end of the method.
         */
        jiraXmlUrlBuilder.append(url.indexOf("?") >= 0 ? "" : "?1=1");

        if (StringUtils.isNotBlank(resultsPerPage))
        {
            int resultsPerPageInt = Integer.parseInt(resultsPerPage);

            jiraXmlUrlBuilder.setLength(0);
            jiraXmlUrlBuilder.append(getAbsoluteUrlWithTempMaxRequestParametersSetToResultsPerPage(url, resultsPerPage));

            if (StringUtils.isNotBlank(page))
                jiraXmlUrlBuilder.append("&pager/start=").append(resultsPerPageInt * (Integer.parseInt(page) - 1));
        }

        if (StringUtils.isNotBlank(sortField))
        {
            
            if (sortField.equals("key"))
            {
                sortField = "issuekey";
            }
            else if (sortField.equals("type"))
            {
                sortField = "issuetype";
            }
            else
            {
                Map columnMapForJiraInstance = jiraIssuesColumnManager.getColumnMap(getRequestUrl(url));
                if (columnMapForJiraInstance != null && columnMapForJiraInstance.containsKey(sortField))
                    sortField = (String) columnMapForJiraInstance.get(sortField);
            }
            
            // see if the original url contains a jql query
            Matcher jqlMatcher = JQLQUERY_REQUEST_PARAMER_PATTERN.matcher(jiraXmlUrlBuilder);
            if (jqlMatcher.find())
            {
                // rework the original jql query to contain a new order by statement
                // this is the only way to do sorting with xml + jql
                String jqlQuery = utf8Decode(jqlMatcher.group(1));
                String lcJqlQuery = jqlQuery.toLowerCase();
                int orderByIdx = lcJqlQuery.lastIndexOf(" order by");
                if (orderByIdx != -1)
                {
                    jqlQuery = jqlQuery.substring(0, orderByIdx);
                }
                if (sortField.startsWith("customfield_"))
                {
                    sortField = "cf[" + sortField.substring("customfield_".length()) + "]";
                }
                
                jqlQuery = jqlQuery + " ORDER BY " + sortField;
                
                if (StringUtils.isNotBlank(sortOrder))
                {
                    jqlQuery = jqlQuery + ' ' + sortOrder;
                }
                jiraXmlUrlBuilder = new StringBuilder(jqlMatcher.replaceFirst("jqlQuery=" + JiraUtil.utf8Encode(jqlQuery)));
                
            }
            else
            {
                jiraXmlUrlBuilder.append("&sorter/field=").append(JiraUtil.utf8Encode(sortField));
                if (StringUtils.isNotBlank(sortOrder))
                    jiraXmlUrlBuilder.append("&sorter/order=").append(sortOrder.toUpperCase()); // seems to work without upperizing but thought best to do it
            }
            
        }
        return jiraXmlUrlBuilder.toString().replaceFirst("\\?1=1&", "?");
    }
    private String utf8Decode(String s)
    {
        try
        {
            return URLDecoder.decode(s, "UTF-8");
        }
        catch (UnsupportedEncodingException e)
        {
            // will never happen in a standard java runtime environment
            throw new RuntimeException("You appear to not be running on a standard Java Rutime Environment");
        }
    }
    
}
