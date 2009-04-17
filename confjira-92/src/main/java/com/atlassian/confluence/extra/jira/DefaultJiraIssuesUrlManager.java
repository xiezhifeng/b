package com.atlassian.confluence.extra.jira;

import org.apache.commons.lang.StringUtils;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DefaultJiraIssuesUrlManager implements JiraIssuesUrlManager
{
    private static final Pattern URL_WITHOUT_QUERY_STRING_PATTERN = Pattern.compile("(^.*)\\?");

    private static final Pattern TEMPMAX_REQUEST_PARAMETER_PATTERN = Pattern.compile("tempMax=\\d+");

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
     * request parameters get set to the specified results per page.
     *
     * @param urlBuilder
     * The {@link StringBuilder} containing the URL currently being built.
     * @param resultsPerPage
     * The results per page desired
     *
     * @returns
     * Returns <tt>true</tt> if the URL {@ink StringBuilder} has been modified. This happens when it has
     * <tt>tempMax</tt> request parameters that were modified. Returns <tt>false</tt> if the content of
     * the URL {@link StringBuilder} has not been modified.
     */
    private boolean setTempMaxRequestParameterToResultsPerPage(StringBuilder urlBuilder, String resultsPerPage)
    {
        String urlWeHaveSoFar = urlBuilder.toString();

        Matcher tempMaxMatchcer = TEMPMAX_REQUEST_PARAMETER_PATTERN.matcher(urlWeHaveSoFar);

        if (tempMaxMatchcer.find())
        {
            urlBuilder.setLength(0);
            urlBuilder.append(tempMaxMatchcer.replaceAll(new StringBuilder("tempMax=").append(resultsPerPage).toString()));
            return true;
        }
        else
        {
            return false;
        }
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

            if (!setTempMaxRequestParameterToResultsPerPage(jiraXmlUrlBuilder, resultsPerPage))
                jiraXmlUrlBuilder.append("&tempMax=").append(resultsPerPageInt);

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
                Map columnMapForJiraInstance = jiraIssuesColumnManager.getColumnMap(url);
                if (columnMapForJiraInstance != null && columnMapForJiraInstance.containsKey(sortField))
                    sortField = (String) columnMapForJiraInstance.get(sortField);
            }
            
            jiraXmlUrlBuilder.append("&sorter/field=").append(sortField);
        }

        if (StringUtils.isNotBlank(sortOrder))
            jiraXmlUrlBuilder.append("&sorter/order=").append(sortOrder.toUpperCase()); // seems to work without upperizing but thought best to do it

        return jiraXmlUrlBuilder.toString().replaceFirst("\\?1=1&", "?");
    }
}
