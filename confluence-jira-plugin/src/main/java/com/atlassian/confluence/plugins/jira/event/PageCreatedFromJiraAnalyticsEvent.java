package com.atlassian.confluence.plugins.jira.event;

import com.atlassian.analytics.api.annotations.AnalyticsEnabled;
import com.atlassian.analytics.api.annotations.EventName;
import com.atlassian.confluence.event.events.ConfluenceEvent;

@AnalyticsEnabled
public class PageCreatedFromJiraAnalyticsEvent extends ConfluenceEvent
{
    protected final String eventName;
    @AnalyticsEnabled
    protected final String blueprintModuleKey;

    public PageCreatedFromJiraAnalyticsEvent(Object src, EventType eventType, String blueprintModuleKey)
    {
        super(src);
        this.eventName = eventType.getAnalyticsEventName();
        this.blueprintModuleKey = blueprintModuleKey;
    }

    @EventName
    public String calculateEventName()
    {
        return eventName;
    }

    public String getBlueprintModuleKey()
    {
        return blueprintModuleKey;
    }

    public enum EventType
    {
        EPIC_FROM_PLAN_MODE("confluence.jira.content.created.epic.plan"),
        SPRINT_FROM_PLAN_MODE("confluence.jira.content.created.sprint.plan"),
        SPRINT_FROM_REPORT_MODE("confluence.jira.content.created.sprint.report");

        private final String analyticsEventName;

        private EventType(String analyticsEventName)
        {
            this.analyticsEventName = analyticsEventName;
        }

        public String getAnalyticsEventName()
        {
            return analyticsEventName;
        }
    }
}
