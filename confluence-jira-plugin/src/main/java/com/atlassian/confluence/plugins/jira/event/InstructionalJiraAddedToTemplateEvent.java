package com.atlassian.confluence.plugins.jira.event;

import com.atlassian.analytics.api.annotations.AnalyticsEnabled;
import com.atlassian.analytics.api.annotations.EventName;

@AnalyticsEnabled
@EventName("confluence.template.instructional.create.jira")
public class InstructionalJiraAddedToTemplateEvent
{

    @AnalyticsEnabled
    private String instances;
    
    public InstructionalJiraAddedToTemplateEvent(String instances)
    {
        this.instances = instances;
    }

    public String getInstances()
    {
        return instances;
    }
}
