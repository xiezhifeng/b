package com.atlassian.confluence.plugins.jira.event;

import org.springframework.beans.factory.DisposableBean;

import com.atlassian.applinks.api.event.ApplicationLinkDetailsChangedEvent;
import com.atlassian.confluence.extra.jira.JiraConnectorManager;
import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;

public class ApplicationLinkDetailsChangedEventListener implements DisposableBean
{

    private final EventPublisher eventPublisher;
    public ApplicationLinkDetailsChangedEventListener(final EventPublisher eventPublisher, final JiraConnectorManager jiraConnectorManager)
    {
        this.eventPublisher = eventPublisher;
        eventPublisher.register(this);
    }

    @EventListener
    public void publishAnalyticTemplateEvent(final ApplicationLinkDetailsChangedEvent applicationLinkDetailsChangedEvent)
    {
        System.out.println("ApplicationLinkDetailsChangedEventListener.publishAnalyticTemplateEvent() **** rebuilding cache");
        // this.jiraConnectorManager.rebuildJiraServerCache();
    }

    @Override
    public void destroy() throws Exception
    {
        this.eventPublisher.unregister(this);
    }

}
