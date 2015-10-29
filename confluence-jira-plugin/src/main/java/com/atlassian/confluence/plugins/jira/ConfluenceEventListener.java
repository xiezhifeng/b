package com.atlassian.confluence.plugins.jira;

import com.atlassian.applinks.api.event.ApplicationLinkDetailsChangedEvent;
import com.atlassian.applinks.api.event.ApplicationLinkMadePrimaryEvent;
import com.atlassian.confluence.event.events.content.blogpost.BlogPostCreateEvent;
import com.atlassian.confluence.event.events.content.blogpost.BlogPostUpdateEvent;
import com.atlassian.confluence.event.events.content.page.PageCreateEvent;
import com.atlassian.confluence.event.events.content.page.PageRemoveEvent;
import com.atlassian.confluence.event.events.content.page.PageUpdateEvent;
import com.atlassian.confluence.extra.jira.JiraConnectorManager;
import com.atlassian.confluence.pages.AbstractPage;
import com.atlassian.confluence.plugins.createcontent.events.BlueprintPageCreateEvent;
import com.atlassian.confluence.plugins.jira.event.PageCreatedFromJiraAnalyticsEvent;
import com.atlassian.confluence.plugins.jira.links.JiraRemoteEpicLinkManager;
import com.atlassian.confluence.plugins.jira.links.JiraRemoteIssueLinkManager;
import com.atlassian.confluence.plugins.jira.links.JiraRemoteSprintLinkManager;
import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.sal.api.executor.ThreadLocalDelegateExecutorFactory;
import com.google.common.base.Function;
import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ConfluenceEventListener implements DisposableBean
{
    private final static Logger LOGGER = LoggerFactory.getLogger(ConfluenceEventListener.class);

    private final EventPublisher eventPublisher;

    private final JiraRemoteSprintLinkManager jiraRemoteSprintLinkManager;
    private final JiraRemoteEpicLinkManager jiraRemoteEpicLinkManager;
    private final JiraRemoteIssueLinkManager jiraRemoteIssueLinkManager;

    private final JiraConnectorManager jiraConnectorManager;

    private final ThreadLocalDelegateExecutorFactory threadLocalDelegateExecutorFactory;
    private final ExecutorService jiraLinkExecutorService = Executors.newCachedThreadPool();
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

    public ConfluenceEventListener(
            EventPublisher eventPublisher,
            JiraRemoteSprintLinkManager jiraRemoteSprintLinkManager,
            JiraRemoteIssueLinkManager jiraRemoteIssueLinkManager,
            JiraRemoteEpicLinkManager jiraRemoteEpicLinkManager,
            JiraConnectorManager jiraConnectorManager,
            ThreadLocalDelegateExecutorFactory threadLocalDelegateExecutorFactory)
    {
        this.eventPublisher = eventPublisher;
        this.jiraRemoteSprintLinkManager = jiraRemoteSprintLinkManager;
        this.jiraRemoteEpicLinkManager = jiraRemoteEpicLinkManager;
        this.jiraRemoteIssueLinkManager = jiraRemoteIssueLinkManager;
        this.jiraConnectorManager = jiraConnectorManager;
        this.threadLocalDelegateExecutorFactory = threadLocalDelegateExecutorFactory;
        eventPublisher.register(this);
    }

    @EventListener
    public void createJiraRemoteLinks(PageCreateEvent event)
    {
        createJiraRemoteLinksForNewPage(event.getPage(), event.getContext());
    }

    @EventListener
    public void createJiraRemoteLinks(BlogPostCreateEvent event)
    {
        createJiraRemoteLinksForNewPage(event.getBlogPost(), event.getContext());
    }

    @EventListener
    public void updateJiraRemoteLinks(BlogPostUpdateEvent event)
    {
        updateJiraRemoteLinks(event.getBlogPost(), event.getOriginalBlogPost());
    }

    @EventListener
    public void updateJiraRemoteLinks(PageUpdateEvent event)
    {
        updateJiraRemoteLinks(event.getPage(), event.getOriginalPage());
    }

    @EventListener
    public void handleBlueprintPageCreate(BlueprintPageCreateEvent event)
    {
        handleBlueprintPageCreate(event.getPage(), event.getBlueprintKey().getCompleteKey(), event.getContext());
    }

    @EventListener
    public void deleteJiraRemoteLinks(PageRemoveEvent event)
    {
        deleteJiraRemoteLinks(event.getPage());
    }

    private void createJiraRemoteLinksForNewPage(final AbstractPage newPage, final Map<String, ?> context)
    {
        executeJiraLinkCallable(new Callable<Void>()
        {
            @Override
            public Void call() throws Exception
            {
                jiraRemoteIssueLinkManager.createIssueLinksForEmbeddedMacros(newPage);
                handlePageCreateInitiatedFromJIRAEntity(newPage, "", Maps.transformValues(context, PARAM_VALUE_TO_STRING_FUNCTION));
                return null;
            }
        });
    }

    private void updateJiraRemoteLinks(final AbstractPage originalPage, final AbstractPage currentPage)
    {
        executeJiraLinkCallable(new Callable<Void>()
        {
            @Override
            public Void call() throws Exception
            {
                jiraRemoteIssueLinkManager.updateIssueLinksForEmbeddedMacros(originalPage, currentPage);
                return null;
            }
        });
    }

    private void deleteJiraRemoteLinks(final AbstractPage page)
    {
        executeJiraLinkCallable(new Callable<Void>()
        {
            @Override
            public Void call() throws Exception {
                jiraRemoteIssueLinkManager.deleteIssueLinksForEmbeddedMacros(page);
                return null;
            }
        });
    }

    private void handleBlueprintPageCreate(final AbstractPage page, final String blueprintKey, final Map<String, ?> context)
    {
        executeJiraLinkCallable(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                handlePageCreateInitiatedFromJIRAEntity(page, blueprintKey, Maps.transformValues(context, PARAM_VALUE_TO_STRING_FUNCTION));
                return null;
            }
        });
    }

    private void executeJiraLinkCallable(Callable<Void> callable)
    {
        jiraLinkExecutorService.submit(threadLocalDelegateExecutorFactory.createCallable(callable));
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
                boolean successfulLink = jiraRemoteEpicLinkManager.createLinkToEpic(page, params.get(APPLINK_ID),
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
                boolean successfulLink = jiraRemoteSprintLinkManager.createLinkToSprint(page, params.get(APPLINK_ID),
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
    private boolean containsValue(String key, Map<String, String> params, boolean logIfNotPresent)
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
        if (!containsValue && logIfNotPresent)
        {
            LOGGER.warn("Link could not be created for a page created from JIRA, as no value was provided for '{}'",
                    key);
        }
        return containsValue;
    }

    public void destroy() throws Exception
    {
        eventPublisher.unregister(this);
    }
}
