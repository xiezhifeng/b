package com.atlassian.confluence.extra.jira;

import java.io.IOException;
import java.util.Collection;

import com.atlassian.applinks.api.ApplicationLink;

/**
 * The interface that {@link com.atlassian.confluence.extra.jira.JiraIssuesServlet}
 * uses to get a suitable {@link String} that it can serve to the Flexigrid widget.
 */
public interface FlexigridResponseGenerator
{
    static final String DATE_VALUE_FORMAT = "dd/MMM/yy";
    
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
     * @param appLink
     * If the source has an Application Link with Confluence. If <tt>null</tt>, description and custom fields will be escaped before returning.
     * @return
     * A {@link String} JSON string that Flexigrid can read.
     * @throws IOException
     * Implementations can throw this to indicate IO errors.
     */
    String generate(
            JiraIssuesManager.Channel channel,
            Collection<String> columnNames,
            int requestedPage,
            boolean showCount,
            ApplicationLink appLink
    ) throws IOException;
}
