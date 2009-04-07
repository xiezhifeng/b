package com.atlassian.confluence.extra.jira;

import java.io.IOException;
import java.util.Collection;

/**
 * The interface that {@link com.atlassian.confluence.extra.jira.JiraIssuesServlet}
 * uses to get a suitable {@link String} that it can serve to the Flexigrid widget.
 */
public interface JiraIssuesResponseGenerator
{
    /**
     * Generate a {@link String} that the Flexigrid widget can use.
     *
     * @param channel
     * The JIRA response.
     * @param columnNames
     * The columns requested by the user.
     * @param requestedPage
     * The page number.
     * @param showCount
     * If this is <tt>true</tt>, implementations must generate a count of the JIRA response. If <tt>false</tt>
     * implementations must generate details.
     * @return
     * A {@link String} (presumable JSON) that Flexigrid can read.
     * @throws IOException
     * Implementations can throw this to indicate IO errors.
     */
    String generate(
            JiraIssuesManager.Channel channel,
            Collection<String> columnNames,
            int requestedPage,
            boolean showCount
    ) throws IOException;
}
