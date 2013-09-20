package com.atlassian.confluence.plugins.jira;

import com.atlassian.confluence.event.events.content.page.PageCreateEvent;
import com.atlassian.confluence.event.events.content.page.PageUpdateEvent;
import com.atlassian.confluence.pages.AbstractPage;
import com.atlassian.confluence.plugins.createcontent.events.BlueprintPageCreateEvent;
import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import java.util.Map;
import org.springframework.beans.factory.DisposableBean;

public class ConfluenceEventListener implements DisposableBean
{
    private final EventPublisher eventPublisher;
    private final JiraRemoteLinkCreator jiraRemoteLinkCreator;

    public ConfluenceEventListener(EventPublisher eventPublisher, JiraRemoteLinkCreator jiraRemoteLinkCreator)
    {
        this.eventPublisher = eventPublisher;
        this.jiraRemoteLinkCreator = jiraRemoteLinkCreator;
        eventPublisher.register(this);
    }

    @EventListener
    public void createJiraRemoteLinks(PageCreateEvent event)
    {
        final AbstractPage page = event.getPage();
        jiraRemoteLinkCreator.createLinksForEmbeddedMacros(page);
    }

    @EventListener
    public void createJiraRemoteLinks(PageUpdateEvent event)
    {
        final AbstractPage prevPage = event.getOriginalPage();
        final AbstractPage page = event.getPage();
        jiraRemoteLinkCreator.createLinksForEmbeddedMacros(prevPage, page);
    }

    @EventListener
    public void createJiraRemoteLinks(BlueprintPageCreateEvent event)
    {
        final AbstractPage page = event.getPage();
        final Map<String, Object> context = event.getContext();

        if (context.containsKey("applinkId"))
        {
            String fallbackUrl = context.get("fallbackUrl") != null ? context.get("fallbackUrl").toString() : "";
            if (context.containsKey("issueKey"))
            {
                jiraRemoteLinkCreator.createLinkToIssue(page, context.get("applinkId").toString(), context.get("issueKey").toString(), fallbackUrl);
            }
            else if (context.containsKey("sprintId"))
            {
                jiraRemoteLinkCreator.createLinkToSprint(page, context.get("applinkId").toString(), context.get("sprintId").toString(), fallbackUrl);
            }
        }
    }

    public void destroy() throws Exception
    {
        eventPublisher.unregister(this);
    }
}
