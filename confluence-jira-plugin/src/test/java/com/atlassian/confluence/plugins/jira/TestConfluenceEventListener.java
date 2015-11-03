package com.atlassian.confluence.plugins.jira;

import com.atlassian.confluence.extra.jira.JiraConnectorManager;
import com.atlassian.confluence.extra.jira.executor.JiraExecutorFactory;
import com.atlassian.confluence.pages.AbstractPage;
import com.atlassian.confluence.plugins.createcontent.events.BlueprintPageCreateEvent;
import com.atlassian.confluence.plugins.jira.links.JiraRemoteEpicLinkManager;
import com.atlassian.confluence.plugins.jira.links.JiraRemoteIssueLinkManager;
import com.atlassian.confluence.plugins.jira.links.JiraRemoteSprintLinkManager;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.plugin.ModuleCompleteKey;
import com.atlassian.test.concurrent.MockThreadLocalDelegateExecutorFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.HashMap;
import java.util.Map;

import static junit.framework.Assert.fail;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class TestConfluenceEventListener
{
    private ConfluenceEventListener event;
    private Map<String, Object> params = new HashMap<String, Object>();;

    private static final String APPLINK_ID = "applinkId";
    private static final String FALLBACK_URL = "fallbackUrl";
    private static final String AGILE_MODE = "agileMode";
    private static final String SPRINT_ID = "sprintId";
    private static final String ISSUE_KEY = "issueKey";
    private static final String CREATION_TOKEN = "creationToken";
    private static final String AGILE_MODE_VALUE_PLAN = "plan";

    @Mock private EventPublisher eventPublisher;
    @Mock private JiraRemoteIssueLinkManager jiraRemoteIssueLinkManager;
    @Mock private JiraRemoteSprintLinkManager jiraRemoteSprintLinkManager;
    @Mock private JiraRemoteEpicLinkManager jiraRemoteEpicLinkManager;
    @Mock private JiraConnectorManager jiraConnectorManager;
    @Mock private BlueprintPageCreateEvent blueprintPageCreateEvent;
    private ModuleCompleteKey moduleCompleteKey;

    @Before
    public void setUp()
    {
        moduleCompleteKey = new ModuleCompleteKey("foo:bar");

        initMocks(this);


        when(blueprintPageCreateEvent.getContext()).thenReturn(params);
        when(blueprintPageCreateEvent.getBlueprintKey()).thenReturn(moduleCompleteKey);
        when(blueprintPageCreateEvent.getPage()).thenReturn(null);

        when(jiraRemoteEpicLinkManager.createLinkToEpic(any(AbstractPage.class), anyString(), anyString(), anyString(), anyString())).thenReturn(true);
        when(jiraRemoteSprintLinkManager.createLinkToSprint(any(AbstractPage.class), anyString(), anyString(), anyString(), anyString())).thenReturn(true);
        JiraExecutorFactory factory = new JiraExecutorFactory(new MockThreadLocalDelegateExecutorFactory());
        event = new ConfluenceEventListener(eventPublisher,
                jiraRemoteSprintLinkManager, jiraRemoteIssueLinkManager, jiraRemoteEpicLinkManager,
                jiraConnectorManager, factory);
    }

    @Test
    public void testParamsMissingCreationToken()
    {
        params.clear();
        params.put(APPLINK_ID, "12345");
        params.put(ISSUE_KEY, "CONFDEV-12345");
        params.put(FALLBACK_URL, "http://localhost:8090/jira");

        try
        {
            event.handleBlueprintPageCreate(blueprintPageCreateEvent);
        }
        catch (NullPointerException npe)
        {
            fail("NullPointerException Encountered.");
        }

        verify(eventPublisher, times(0)).publish(any());
    }

    @Test
    public void testParamsMissingAgileMode()
    {
        params.clear();
        params.put(APPLINK_ID, "12345");
        params.put(SPRINT_ID, "CONFDEV-12345");
        params.put(FALLBACK_URL, "http://localhost:8090/jira");
        params.put(CREATION_TOKEN, "12341234");

        try
        {
            event.handleBlueprintPageCreate(blueprintPageCreateEvent);
        }
        catch (NullPointerException npe)
        {
            fail("NullPointerException Encountered.");
        }

        verify(eventPublisher, times(0)).publish(any());
    }

    @Test
    public void testNoParamsMissingCreateIssue()
    {
        params.clear();
        params.put(APPLINK_ID, "12345");
        params.put(ISSUE_KEY, "CONFDEV-12345");
        params.put(FALLBACK_URL, "http://localhost:8090/jira");
        params.put(CREATION_TOKEN, "12341234");

        try
        {
            event.handleBlueprintPageCreate(blueprintPageCreateEvent);
        }
        catch (NullPointerException npe)
        {
            fail("NullPointerException Encountered.");
        }
        verify(eventPublisher, timeout(100).times(1)).publish(any());
    }

    @Test
    public void testNoParamsMissingCreateSprint()
    {
        params.clear();
        params.put(APPLINK_ID, "12345");
        params.put(SPRINT_ID, "CONFDEV-12345");
        params.put(FALLBACK_URL, "http://localhost:8090/jira");
        params.put(CREATION_TOKEN, "12341234");
        params.put(AGILE_MODE, AGILE_MODE_VALUE_PLAN);

        try
        {
            event.handleBlueprintPageCreate(blueprintPageCreateEvent);
        }
        catch (NullPointerException npe)
        {
            fail("NullPointerException Encountered.");
        }

        verify(eventPublisher, timeout(100).times(1)).publish(any());
    }
}
