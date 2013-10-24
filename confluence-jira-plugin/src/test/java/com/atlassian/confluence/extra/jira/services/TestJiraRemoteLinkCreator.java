package com.atlassian.confluence.extra.jira.services;

import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.applinks.api.ApplicationLinkService;
import com.atlassian.applinks.api.application.jira.JiraApplicationType;
import com.atlassian.applinks.host.spi.HostApplication;
import com.atlassian.confluence.extra.jira.api.services.JiraMacroFinderService;
import com.atlassian.confluence.plugins.jira.JiraRemoteLinkCreator;
import com.atlassian.confluence.setup.settings.SettingsManager;
import com.atlassian.confluence.xhtml.api.MacroDefinition;
import com.atlassian.confluence.xhtml.api.XhtmlContent;
import junit.framework.Assert;
import junit.framework.TestCase;

import java.util.Collections;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestJiraRemoteLinkCreator  extends TestCase
{
    public void testGetAppLinkByMacroDefinnition()
    {
        XhtmlContent xhtmlContent = mock(XhtmlContent.class);
        HostApplication hostApplication = mock(HostApplication.class);
        SettingsManager settingsManager = mock(SettingsManager.class);
        JiraMacroFinderService finderService = mock(JiraMacroFinderService.class);
        ApplicationLinkService applicationLinkService = mock(ApplicationLinkService.class);
        ApplicationLink fakeAppLink = mock(ApplicationLink.class);
        when(applicationLinkService.getApplicationLinks(JiraApplicationType.class)).thenReturn(Collections.EMPTY_LIST);
        when(applicationLinkService.getPrimaryApplicationLink(JiraApplicationType.class)).thenReturn(fakeAppLink);

        JiraRemoteLinkCreatorMock objToTest = new JiraRemoteLinkCreatorMock(xhtmlContent,
                applicationLinkService, hostApplication, settingsManager, finderService);
        ApplicationLink outAppLink = objToTest.findApplicationLink(new MacroDefinition());
        Assert.assertNotNull("Must have the default value", outAppLink);
        Assert.assertEquals(fakeAppLink, outAppLink);
    }

    private class JiraRemoteLinkCreatorMock extends JiraRemoteLinkCreator
    {

        public JiraRemoteLinkCreatorMock(XhtmlContent xhtmlContent,
                                         ApplicationLinkService applicationLinkService,
                                         HostApplication hostApplication,
                                         SettingsManager settingsManager,
                                         JiraMacroFinderService finderService)
        {
            super(xhtmlContent, applicationLinkService, hostApplication, settingsManager, finderService);
        }

        public ApplicationLink findApplicationLink(final MacroDefinition macroDefinition) {
            return super.findApplicationLink(macroDefinition);
        }
    }
}
