package com.atlassian.confluence.extra.jira.listeners;

import org.springframework.beans.factory.DisposableBean;

import com.atlassian.confluence.event.events.plugin.PluginFrameworkStartedEvent;
import com.atlassian.confluence.extra.jira.handlers.ConfluenceUpgradeFinishedHandler;
import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;

/**
 * Listener class which is supposed to uninstall confluence-jira-connector upon
 * {@link PluginFrameworkStartedEvent} since it's deprecated to this version of
 * plugin.
 */
public class ConfluenceUpgradeFinishedListener implements DisposableBean
{

    private EventPublisher eventPublisher;
    protected ConfluenceUpgradeFinishedHandler confluenceUpgradeFinishedHandler;

    public ConfluenceUpgradeFinishedListener(EventPublisher eventPublisher)
    {
        this.eventPublisher = eventPublisher;
        eventPublisher.register(this);
    }

    @EventListener
    public void handleUpgradeFinishedEvent(PluginFrameworkStartedEvent event)
    {
        confluenceUpgradeFinishedHandler.uninstallJiraConnectorPlugin();
    }

    public void setConfluenceUpgradeFinishedHandler(ConfluenceUpgradeFinishedHandler confluenceUpgradeFinishedHandler)
    {
        this.confluenceUpgradeFinishedHandler = confluenceUpgradeFinishedHandler;
    }

    @Override
    public void destroy() throws Exception
    {
        eventPublisher.unregister(this);
    }

}
