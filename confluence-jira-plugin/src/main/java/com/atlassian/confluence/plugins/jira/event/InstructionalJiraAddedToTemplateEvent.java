package com.atlassian.confluence.plugins.jira.event;

import com.atlassian.analytics.api.annotations.EventName;

public class InstructionalJiraAddedToTemplateEvent
{
    private static final String EVENT_NAME = "confluence.template.instructional.create.jira";

    private String instances;
    
    public InstructionalJiraAddedToTemplateEvent(String instances)
    {
        this.instances = instances;
    }

    @EventName
    public String getEventName()
    {
        return EVENT_NAME;
    }

    public String getInstances()
    {
        return instances;
    }
}
