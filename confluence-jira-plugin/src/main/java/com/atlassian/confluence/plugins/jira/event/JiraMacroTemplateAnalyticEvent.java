package com.atlassian.confluence.plugins.jira.event;

import com.atlassian.analytics.api.annotations.EventName;

public class JiraMacroTemplateAnalyticEvent {

    private String eventName;
    private String attributes;
    
    public JiraMacroTemplateAnalyticEvent(String analyticName, String attributes)
    {
        this.eventName = analyticName;
        this.attributes = attributes;
    }

    @EventName
    public String getEventName()
    {
        return eventName;
    }

    public String getAttributes()
    {
        return attributes;
    }
}
