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


    /**
     * This is should behave exactly the same as {@link #getJiraXmlUrlFromFlexigridRequest(String, String, String, String, String)},
     * except it will have one less parameter (&quot;page&quot;).
     * @param url
     * The value of the &quot;url&quot; parameter passed to the {@link com.atlassian.confluence.extra.jira.JiraIssuesServlet}.
     * @param resultsPerPage
     * The value of the &quot;rp&quot; parameter passed to the {@link com.atlassian.confluence.extra.jira.JiraIssuesServlet}.
     * @param sortField
     * The value of the &quot;sortname&quot; parameter passed to the {@link com.atlassian.confluence.extra.jira.JiraIssuesServlet}.
     * @param sortOrder
     * The value of the &quot;sortorder&quot; parameter passed to the {@link com.atlassian.confluence.extra.jira.JiraIssuesServlet}.
     * @return
     * A URL built based on the parameters.
     */
    String getJiraXmlUrlFromFlexigridRequest(
            String url,
            String resultsPerPage,
            String sortField,
            String sortOrder);

    /**
     * Builds a JIRA issues URL (the one represented by the &quot;XML&quot; link in the Issue Navigator) from
     * the specified parameters.
     * @param url
     * The value of the &quot;url&quot; parameter passed to the {@link com.atlassian.confluence.extra.jira.JiraIssuesServlet}.
     * @param resultsPerPage
     * The value of the &quot;rp&quot; parameter passed to the {@link com.atlassian.confluence.extra.jira.JiraIssuesServlet}.
     * @param page
     * The value of the &quot;page&quot; parameter passed to the {@link com.atlassian.confluence.extra.jira.JiraIssuesServlet}.
     * @param sortField
     * The value of the &quot;sortname&quot; parameter passed to the {@link com.atlassian.confluence.extra.jira.JiraIssuesServlet}.
     * @param sortOrder
     * The value of the &quot;sortorder&quot; parameter passed to the {@link com.atlassian.confluence.extra.jira.JiraIssuesServlet}.
     * @return
     * A URL built based on the parameters.
     */
    String getJiraXmlUrlFromFlexigridRequest(
            String url,
            String resultsPerPage,
            String page,
            String sortField,
            String sortOrder);
}
