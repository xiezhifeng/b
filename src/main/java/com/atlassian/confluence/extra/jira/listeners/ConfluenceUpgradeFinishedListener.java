package com.atlassian.confluence.extra.jira.listeners;

import org.springframework.beans.factory.DisposableBean;

import com.atlassian.confluence.event.events.admin.UpgradeFinishedEvent;
import com.atlassian.confluence.extra.jira.handlers.ConfluenceUpgradeFinishedHandler;
import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;

/**
 * Listener class which is supposed to uninstall confluence-jira-connector upon
 * {@link UpgradeFinishedEvent} since it's deprecated to this version of plugin.
 */
public class ConfluenceUpgradeFinishedListener implements DisposableBean
{

    private EventPublisher eventPublisher;
    private ConfluenceUpgradeFinishedHandler confluenceUpgradeFinishedHandler;

    public ConfluenceUpgradeFinishedListener(EventPublisher eventPublisher)
    {
        this.eventPublisher = eventPublisher;
        eventPublisher.register(this);
    }

    @EventListener
    public void handleUpgradeFinishedEvent(UpgradeFinishedEvent event)
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
