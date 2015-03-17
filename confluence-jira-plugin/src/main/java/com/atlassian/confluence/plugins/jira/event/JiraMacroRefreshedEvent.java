package com.atlassian.confluence.plugins.jira.event;

import com.atlassian.analytics.api.annotations.EventName;

/**
 * Fired when the JIRA issues macro is refreshed.
 */
@EventName("confluence.jira.macro.refreshed")
public class JiraMacroRefreshedEvent
{
    private final long pageId;
    private final long durationMillis;

    public JiraMacroRefreshedEvent(final long pageId, final long durationMillis)
    {
        this.pageId = pageId;
        this.durationMillis = durationMillis;
    }

    public long getPageId()
    {
        return pageId;
    }

    public long getDurationMillis()
    {
        return durationMillis;
    }
}
