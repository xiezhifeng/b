package com.atlassian.confluence.plugins.jira;

import com.atlassian.confluence.event.events.content.blogpost.BlogPostCreateEvent;
import com.atlassian.confluence.event.events.content.page.PageCreateEvent;
import com.atlassian.confluence.event.events.content.page.PageUpdateEvent;
import com.atlassian.confluence.pages.AbstractPage;
import com.atlassian.confluence.plugins.createcontent.events.BlueprintPageCreateEvent;
import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.google.common.base.Function;
import com.google.common.collect.Maps;
import org.springframework.beans.factory.DisposableBean;

import java.util.Map;
import javax.annotation.Nullable;

public class ConfluenceEventListener implements DisposableBean
{
    private final EventPublisher eventPublisher;
    private final JiraRemoteLinkCreator jiraRemoteLinkCreator;

    private static final Function<Object, String> PARAM_VALUE_TO_STRING_FUNCTION = new Function<Object, String>()
    {
        @Override
        public String apply(@Nullable Object input)
        {
            return input != null ? input.toString() : "";
        }
    };

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
        handlePageCreateInitiatedFromJIRAEntity(page, Maps.transformValues(event.getContext(), PARAM_VALUE_TO_STRING_FUNCTION));
    }

    @EventListener
    public void createJiraRemoteLinks(BlogPostCreateEvent event)
    {
        handlePageCreateInitiatedFromJIRAEntity(event.getBlogPost(), Maps.transformValues(event.getContext(), PARAM_VALUE_TO_STRING_FUNCTION));
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
        // A PageCreateEvent was also triggered (and handled) but only the BlueprintPageCreateEvent's context
        // contains the parameters we're checking for (when the page being created is a blueprint)
        handlePageCreateInitiatedFromJIRAEntity(event.getPage(), Maps.transformValues(event.getContext(), PARAM_VALUE_TO_STRING_FUNCTION));
    }

    private void handlePageCreateInitiatedFromJIRAEntity(AbstractPage page, Map<String, String> params)
    {
        if (params.containsKey("applinkId"))
        {
            if (params.containsKey("issueKey"))
            {
                jiraRemoteLinkCreator.createLinkToEpic(page, params.get("applinkId").toString(), params.get("issueKey"), params.get("fallbackUrl"), params.get("creationToken").toString());
            }
            else if (params.containsKey("sprintId"))
            {
                jiraRemoteLinkCreator.createLinkToSprint(page, params.get("applinkId").toString(), params.get("sprintId"), params.get("fallbackUrl"), params.get("creationToken").toString());
            }
        }
    }

    public void destroy() throws Exception
    {
        eventPublisher.unregister(this);
    }
}
