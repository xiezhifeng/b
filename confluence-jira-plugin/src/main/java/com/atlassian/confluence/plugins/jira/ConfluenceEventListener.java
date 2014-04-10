package com.atlassian.confluence.plugins.jira;

import com.atlassian.applinks.api.event.ApplicationLinkDetailsChangedEvent;
import com.atlassian.applinks.api.event.ApplicationLinkMadePrimaryEvent;
import com.atlassian.confluence.event.events.content.blogpost.BlogPostCreateEvent;
import com.atlassian.confluence.event.events.content.blogpost.BlogPostUpdateEvent;
import com.atlassian.confluence.event.events.content.page.PageCreateEvent;
import com.atlassian.confluence.event.events.content.page.PageUpdateEvent;
import com.atlassian.confluence.extra.jira.JiraConnectorManager;
import com.atlassian.confluence.pages.AbstractPage;
import com.atlassian.confluence.plugins.createcontent.events.BlueprintPageCreateEvent;
import com.atlassian.confluence.plugins.jira.event.PageCreatedFromJiraAnalyticsEvent;
import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.google.common.base.Function;
import com.google.common.collect.Maps;
import java.util.Map;
import javax.annotation.Nullable;
import org.springframework.beans.factory.DisposableBean;

public class ConfluenceEventListener implements DisposableBean
{
    private final EventPublisher eventPublisher;
    private final JiraRemoteLinkCreator jiraRemoteLinkCreator;
    private final JiraConnectorManager jiraConnectorManager;

    private static final Function<Object, String> PARAM_VALUE_TO_STRING_FUNCTION = new Function<Object, String>()
    {
        @Override
        public String apply(@Nullable Object input)
        {
            return input != null ? input.toString() : "";
        }
    };

    /* Context parameters for page creation triggered from JIRA Agile */
    private static final String APPLINK_ID = "applinkId";
    private static final String FALLBACK_URL = "fallbackUrl";
    private static final String AGILE_MODE = "agileMode";
    private static final String SPRINT_ID = "sprintId";
    private static final String ISSUE_KEY = "issueKey";
    private static final String CREATION_TOKEN = "creationToken";
    private static final String AGILE_MODE_VALUE_PLAN = "plan";
    private static final String AGILE_MODE_VALUE_REPORT = "report";

    public ConfluenceEventListener(EventPublisher eventPublisher, JiraRemoteLinkCreator jiraRemoteLinkCreator, JiraConnectorManager jiraConnectorManager)
    {
        this.eventPublisher = eventPublisher;
        this.jiraRemoteLinkCreator = jiraRemoteLinkCreator;
        this.jiraConnectorManager = jiraConnectorManager;
        eventPublisher.register(this);
    }

    @EventListener
    public void createJiraRemoteLinks(PageCreateEvent event)
    {
        final AbstractPage page = event.getPage();
        jiraRemoteLinkCreator.createLinksForEmbeddedMacros(page);
        handlePageCreateInitiatedFromJIRAEntity(page, "", Maps.transformValues(event.getContext(), PARAM_VALUE_TO_STRING_FUNCTION));
    }

    @EventListener
    public void createJiraRemoteLinks(BlogPostCreateEvent event)
    {
        final AbstractPage blog = event.getBlogPost();
        jiraRemoteLinkCreator.createLinksForEmbeddedMacros(blog);
        handlePageCreateInitiatedFromJIRAEntity(blog, "", Maps.transformValues(event.getContext(), PARAM_VALUE_TO_STRING_FUNCTION));
    }

    @EventListener
    public void updateJiraRemoteLinks(BlogPostUpdateEvent event)
    {
        final AbstractPage originalBlogPost = event.getOriginalBlogPost();
        final AbstractPage blogPost = event.getBlogPost();
        jiraRemoteLinkCreator.createLinksForEmbeddedMacros(originalBlogPost, blogPost);
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
        handlePageCreateInitiatedFromJIRAEntity(event.getPage(), event.getBlueprintKey().getModuleKey(), Maps.transformValues(event.getContext(), PARAM_VALUE_TO_STRING_FUNCTION));
    }

    @EventListener
    public void updatePrimaryApplink(ApplicationLinkMadePrimaryEvent event)
    {
        jiraConnectorManager.updatePrimaryServer(event.getApplicationLink());
    }

    @EventListener
    public void updateDetailJiraServerInfor(ApplicationLinkDetailsChangedEvent event)
    {
        jiraConnectorManager.updateDetailJiraServerInfor(event.getApplicationLink());
    }

    //If content was created from JIRA with the proper parameters, we call specific endpoints that allow us to link the content back from JIRA
    //even if the user is not authorised
    private void handlePageCreateInitiatedFromJIRAEntity(AbstractPage page, String blueprintModuleKey, Map<String, String> params)
    {
        if (containsValue(APPLINK_ID, params, false))
        {
            if (containsValue(ISSUE_KEY, params, false) &&
                    containsValue(FALLBACK_URL, params, true) && containsValue(CREATION_TOKEN, params, true))
            {
                boolean successfulLink = jiraRemoteLinkCreator.createLinkToEpic(page, params.get(APPLINK_ID),
                        params.get(ISSUE_KEY), params.get(FALLBACK_URL), params.get(CREATION_TOKEN));
                if (successfulLink)
                {
                    eventPublisher.publish(new PageCreatedFromJiraAnalyticsEvent(this,
                            PageCreatedFromJiraAnalyticsEvent.EventType.EPIC_FROM_PLAN_MODE, blueprintModuleKey));
                }
            }
            else if (containsValue(SPRINT_ID, params, false) && containsValue(FALLBACK_URL, params, true) &&
                    containsValue(CREATION_TOKEN, params, true) && containsValue(AGILE_MODE, params, true))
            {
                boolean successfulLink = jiraRemoteLinkCreator.createLinkToSprint(page, params.get(APPLINK_ID),
                        params.get(SPRINT_ID), params.get(FALLBACK_URL), params.get(CREATION_TOKEN));
                if (successfulLink && AGILE_MODE_VALUE_PLAN.equals(params.get(AGILE_MODE)))
                {
                    eventPublisher.publish(new PageCreatedFromJiraAnalyticsEvent(this,
                            PageCreatedFromJiraAnalyticsEvent.EventType.SPRINT_FROM_PLAN_MODE, blueprintModuleKey));
                }
                else if (successfulLink && AGILE_MODE_VALUE_REPORT.equals(params.get(AGILE_MODE)))
                {
                    eventPublisher.publish(new PageCreatedFromJiraAnalyticsEvent(this,
                            PageCreatedFromJiraAnalyticsEvent.EventType.SPRINT_FROM_REPORT_MODE, blueprintModuleKey));
                }
            }
        }
    }

    // Helper function to correctly log for missing values, and check for null values and empty strings.
    private boolean containsValue(String key, Map<String, String> params, boolean expectValue)
    {
        boolean containsValue = false;
        if (params.containsKey(key))
        {
            String value = params.get(key);
            if (value != null && !value.isEmpty())
            {
                containsValue = true;
            }
        }
        if (!containsValue && expectValue)
        {
            // TODO: Implement logging.
        }
        return containsValue;
    }

    public void destroy() throws Exception
    {
        eventPublisher.unregister(this);
    }
}
