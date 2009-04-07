package com.atlassian.confluence.extra.jira;

public interface JiraIssuesUrlManager
{
    /**
     * Must work exactly the same as {@link javax.servlet.http.HttpServletRequest#getRequestURL()}
     * @param anyUrl
     * Any URL, really.
     * @return
     * See the return value description of {@link javax.servlet.http.HttpServletRequest#getRequestURL()}
     */
    String getRequestUrl(String anyUrl);

    String getJiraXmlUrlFromFlexigridRequest(
            String url,
            String resultsPerPage,
            String sortField,
            String sortOrder);

    String getJiraXmlUrlFromFlexigridRequest(
            String url,
            String resultsPerPage,
            String page,
            String sortField,
            String sortOrder);
}
