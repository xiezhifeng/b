package com.atlassian.confluence.extra.jira.services;

import com.atlassian.applinks.api.ReadOnlyApplicationLink;
import com.atlassian.applinks.api.ReadOnlyApplicationLinkService;
import com.atlassian.applinks.api.application.jira.JiraApplicationType;
import com.atlassian.applinks.host.spi.HostApplication;
import com.atlassian.confluence.extra.jira.api.services.JiraMacroFinderService;
import com.atlassian.confluence.plugins.jira.links.JiraRemoteLinkManager;
import com.atlassian.confluence.setup.settings.SettingsManager;
import com.atlassian.confluence.xhtml.api.MacroDefinition;
import com.atlassian.sal.api.net.RequestFactory;
import junit.framework.Assert;
import junit.framework.TestCase;

import java.util.Collections;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestJiraRemoteLinkCreator  extends TestCase
{
    public void testGetAppLinkByMacroDefinition()
    {
        RequestFactory requestFactory = mock(RequestFactory.class);
        HostApplication hostApplication = mock(HostApplication.class);
        SettingsManager settingsManager = mock(SettingsManager.class);
        JiraMacroFinderService finderService = mock(JiraMacroFinderService.class);
        ReadOnlyApplicationLinkService applicationLinkService = mock(ReadOnlyApplicationLinkService.class);
        ReadOnlyApplicationLink fakeAppLink = mock(ReadOnlyApplicationLink.class);
        when(applicationLinkService.getApplicationLinks(JiraApplicationType.class)).thenReturn(Collections.EMPTY_LIST);
        when(applicationLinkService.getPrimaryApplicationLink(JiraApplicationType.class)).thenReturn(fakeAppLink);


        JiraRemoteLinkManagerMock objToTest = new JiraRemoteLinkManagerMock(applicationLinkService, hostApplication, settingsManager, finderService, requestFactory);
        ReadOnlyApplicationLink outAppLink = objToTest.findApplicationLink(new MacroDefinition());

        Assert.assertNotNull("Must have the default value", outAppLink);
        Assert.assertEquals(fakeAppLink, outAppLink);
    }

    private class JiraRemoteLinkManagerMock extends JiraRemoteLinkManager
    {

        public JiraRemoteLinkManagerMock(ReadOnlyApplicationLinkService applicationLinkService,
                                         HostApplication hostApplication,
                                         SettingsManager settingsManager,
                                         JiraMacroFinderService finderService,
                                         RequestFactory requestFactory)
        {
            super(applicationLinkService, hostApplication, settingsManager, finderService, requestFactory);
        }

        public ReadOnlyApplicationLink findApplicationLink(final MacroDefinition macroDefinition) {
            return super.findApplicationLink(macroDefinition);
        }
    }
}
