package com.atlassian.confluence.extra.jira;

/**
 * An extension to {@link com.atlassian.confluence.extra.jira.JiraIssuesResponseGenerator}
 * that is suitable for use for delegation.
 */
public interface DelegatableJiraIssuesResponseGenerator extends JiraIssuesResponseGenerator
{
    /**
     * Implementations should tell the caller if it can handle a particular JIRA response.
     * @param channel
     * The JIRA responpse.
     * @return
     * Implementations should return <tt>true</tt> to indicate that it is capcable of handling
     * the JIRA response. If it can't the implementation should return <tt>false</tt>.
     */
    boolean handles(JiraIssuesManager.Channel channel);
}
