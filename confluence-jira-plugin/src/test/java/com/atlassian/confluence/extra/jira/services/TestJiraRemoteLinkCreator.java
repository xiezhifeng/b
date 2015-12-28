package com.atlassian.confluence.extra.jira.services;

import java.util.UUID;

import com.atlassian.applinks.api.ApplicationId;
import com.atlassian.applinks.api.ApplicationLinkRequestFactory;
import com.atlassian.applinks.api.CredentialsRequiredException;
import com.atlassian.applinks.api.ReadOnlyApplicationLink;
import com.atlassian.applinks.api.ReadOnlyApplicationLinkService;
import com.atlassian.applinks.host.spi.HostApplication;
import com.atlassian.applinks.test.mock.MockApplicationLinkRequest;
import com.atlassian.confluence.extra.jira.api.services.JiraMacroFinderService;
import com.atlassian.confluence.pages.Page;
import com.atlassian.confluence.plugins.jira.JiraRemoteLinkCreator;
import com.atlassian.confluence.setup.settings.Settings;
import com.atlassian.confluence.setup.settings.SettingsManager;
import com.atlassian.confluence.xhtml.api.MacroDefinition;
import com.atlassian.sal.api.net.Request;
import com.atlassian.sal.api.net.RequestFactory;
import com.atlassian.spring.container.ContainerContext;
import com.atlassian.spring.container.ContainerManager;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import junit.framework.Assert;
import junit.framework.TestCase;

import static com.atlassian.confluence.extra.jira.util.JiraConnectorUtils.findApplicationLink;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TestJiraRemoteLinkCreator  extends TestCase
{
    @Mock
    private ReadOnlyApplicationLinkService applicationLinkService;

    @Mock
    private HostApplication hostApplication;

    @Mock
    private SettingsManager settingsManager;

    @Mock
    private JiraMacroFinderService macroFinderService;

    @Mock
    private RequestFactory requestFactory;

    @Mock
    private ReadOnlyApplicationLink fakeAppLink;

    @Mock
    private ApplicationLinkRequestFactory applicationLinkRequestFactory;

    @Mock
    private ContainerContext containerContext;

    @Mock
    private Settings settings;

    @InjectMocks
    JiraRemoteLinkCreator jiraRemoteLinkCreator;


    public void testGetAppLinkByMacroDefinition()
    {
        ReadOnlyApplicationLink outAppLink = findApplicationLink(applicationLinkService, new MacroDefinition());
        Assert.assertNotNull("Must have the default value", outAppLink);
        Assert.assertEquals(fakeAppLink, outAppLink);
    }

    @Test
    public void testCreateRemoteIssueLink() throws CredentialsRequiredException, JSONException
    {
        when(applicationLinkService.getApplicationLink(any(ApplicationId.class))).thenReturn(fakeAppLink);
        String requestUrl = "rest/jc/1.0/issue/linkConfluencePage";
        MockApplicationLinkRequest request = new MockApplicationLinkRequest(Request.MethodType.POST, requestUrl);
        when(applicationLinkRequestFactory.createRequest(eq(Request.MethodType.POST), eq(requestUrl))).thenReturn(request);
        when(fakeAppLink.createAuthenticatedRequestFactory()).thenReturn(applicationLinkRequestFactory);
        ContainerManager.getInstance().setContainerContext(containerContext);
        when(containerContext.isSetup()).thenReturn(true);
        when(containerContext.getComponent(eq("settingsManager"))).thenReturn(settingsManager);
        when(settingsManager.getGlobalSettings()).thenReturn(settings);
        when(settings.getBaseUrl()).thenReturn("baseUrl");

        jiraRemoteLinkCreator.createRemoteIssueLink(new Page(), String.valueOf(UUID.randomUUID()), "issueKey", "fallbackUrl");

        assertEquals("[application/json]", request.getHeader("Content-Type").toString());
        JSONObject json = new JSONObject(request.getRequestBody());
        assertEquals("issueKey" ,json.get("issueKey"));
        assertEquals("baseUrl/pages/viewpage.action?pageId=0" ,json.get("pageUrl"));
    }

    @Test
    public void testCreateLinkToSprint() throws CredentialsRequiredException, JSONException
    {
        when(applicationLinkService.getApplicationLink(any(ApplicationId.class))).thenReturn(fakeAppLink);
        String requestUrl = "null/rest/greenhopper/1.0/api/sprints/springId/remotelinkchecked";
        MockApplicationLinkRequest request = new MockApplicationLinkRequest(Request.MethodType.PUT, requestUrl);
        when(requestFactory.createRequest(eq(Request.MethodType.PUT), eq(requestUrl))).thenReturn(request);

        ContainerManager.getInstance().setContainerContext(containerContext);
        when(containerContext.isSetup()).thenReturn(true);
        when(containerContext.getComponent(eq("settingsManager"))).thenReturn(settingsManager);
        when(settingsManager.getGlobalSettings()).thenReturn(settings);
        when(settings.getBaseUrl()).thenReturn("baseUrl");
        when(settings.getSiteTitle()).thenReturn("some title");

        when(hostApplication.getId()).thenReturn(new ApplicationId(String.valueOf(UUID.randomUUID())));

        jiraRemoteLinkCreator.createLinkToSprint(new Page(), String.valueOf(UUID.randomUUID()), "springId", "fallbackUrl", "createToken");
        assertEquals("[application/json]", request.getHeader("Content-Type").toString());
        JSONObject json = new JSONObject(request.getRequestBody());
        assertNotNull(json.get("globalId"));
        assertNotNull(json.get("application"));
    }

    @Test
    public void testCreateLinkToEpic() throws CredentialsRequiredException, JSONException
    {
        when(applicationLinkService.getApplicationLink(any(ApplicationId.class))).thenReturn(fakeAppLink);
        String requestUrl = "null/rest/greenhopper/1.0/api/epics/issueKey/remotelinkchecked";
        MockApplicationLinkRequest request = new MockApplicationLinkRequest(Request.MethodType.PUT, requestUrl);
        when(requestFactory.createRequest(eq(Request.MethodType.PUT), eq(requestUrl))).thenReturn(request);

        ContainerManager.getInstance().setContainerContext(containerContext);
        when(containerContext.isSetup()).thenReturn(true);
        when(containerContext.getComponent(eq("settingsManager"))).thenReturn(settingsManager);
        when(settingsManager.getGlobalSettings()).thenReturn(settings);
        when(settings.getBaseUrl()).thenReturn("baseUrl");
        when(settings.getSiteTitle()).thenReturn("some title");

        when(hostApplication.getId()).thenReturn(new ApplicationId(String.valueOf(UUID.randomUUID())));

        jiraRemoteLinkCreator.createLinkToEpic(new Page(), String.valueOf(UUID.randomUUID()), "issueKey", "fallbackUrl", "createToken");
        assertEquals("[application/json]", request.getHeader("Content-Type").toString());
        JSONObject json = new JSONObject(request.getRequestBody());
        assertNotNull(json.get("globalId"));
        assertNotNull(json.get("application"));

    }
}
