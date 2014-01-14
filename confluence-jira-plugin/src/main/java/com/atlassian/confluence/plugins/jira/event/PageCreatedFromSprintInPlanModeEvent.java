package com.atlassian.confluence.plugins.jira.event;

import com.atlassian.analytics.api.annotations.EventName;
import com.atlassian.confluence.core.ContentEntityObject;

@EventName("confluence.jira.content.created.sprint.plan")
public class PageCreatedFromSprintInPlanModeEvent extends PageCreatedFromJiraEvent
{
    public PageCreatedFromSprintInPlanModeEvent(Object src, ContentEntityObject content, String blueprintKey)
    {
        super(src, content, blueprintKey);
    }
}
