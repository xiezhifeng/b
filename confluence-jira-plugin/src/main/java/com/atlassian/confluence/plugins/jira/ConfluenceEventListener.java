package com.atlassian.confluence.plugins.jira;

import com.atlassian.applinks.api.event.ApplicationLinkDetailsChangedEvent;
import com.atlassian.applinks.api.event.ApplicationLinkMadePrimaryEvent;
import com.atlassian.confluence.event.events.content.blogpost.BlogPostCreateEvent;
import com.atlassian.confluence.event.events.content.page.PageCreateEvent;
import com.atlassian.confluence.event.events.content.page.PageUpdateEvent;
import com.atlassian.confluence.extra.jira.JiraConnectorManager;
import com.atlassian.confluence.pages.AbstractPage;
import com.atlassian.confluence.plugins.createcontent.events.BlueprintPageCreateEvent;
import com.atlassian.confluence.plugins.jira.event.PageCreatedFromEpicInPlanMode;
import com.atlassian.confluence.plugins.jira.event.PageCreatedFromSprintInPlanMode;
import com.atlassian.confluence.plugins.jira.event.PageCreatedFromSprintInReportMode;
import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.google.common.base.Function;
import com.google.common.collect.Maps;
import org.springframework.beans.factory.DisposableBean;

import java.util.Map;
import javax.annotation.Nullable;

import static com.atlassian.confluence.plugins.jira.ConfluenceEventListener.CreationContextParams.*;

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
        handlePageCreateInitiatedFromJIRAEntity(event.getBlogPost(), "", Maps.transformValues(event.getContext(), PARAM_VALUE_TO_STRING_FUNCTION));
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
        if (params.containsKey(APPLINK_ID))
        {
            if (params.containsKey(ISSUE_KEY))
            {
                jiraRemoteLinkCreator.createLinkToEpic(page, params.get(APPLINK_ID).toString(), params.get(ISSUE_KEY), params.get(FALLBACK_URL), params.get(CREATION_TOKEN).toString());
                eventPublisher.publish(new PageCreatedFromEpicInPlanMode(this, page, blueprintModuleKey));
            }
            else if (params.containsKey(SPRINT_ID))
            {
                jiraRemoteLinkCreator.createLinkToSprint(page, params.get(APPLINK_ID).toString(), params.get(SPRINT_ID), params.get(FALLBACK_URL), params.get(CREATION_TOKEN).toString());
                if (AGILE_MODE_VALUE_PLAN.equals(params.get(AGILE_MODE)))
                {
                    eventPublisher.publish(new PageCreatedFromSprintInPlanMode(this, page, blueprintModuleKey));
                }
                else if (AGILE_MODE_VALUE_REPORT.equals(params.get(AGILE_MODE)))
                {
                    eventPublisher.publish(new PageCreatedFromSprintInReportMode(this, page, blueprintModuleKey));
                }
            }
        }
    }

    public void destroy() throws Exception
    {
        eventPublisher.unregister(this);
    }

    class CreationContextParams
    {
        static final String APPLINK_ID = "applinkId";
        static final String FALLBACK_URL = "fallbackUrl";
        static final String AGILE_MODE = "agileMode";
        static final String SPRINT_ID = "sprintId";
        static final String ISSUE_KEY = "issueKey";
        static final String CREATION_TOKEN = "creationToken";
        static final String AGILE_MODE_VALUE_PLAN = "plan";
        static final String AGILE_MODE_VALUE_REPORT = "report";
    }
}
