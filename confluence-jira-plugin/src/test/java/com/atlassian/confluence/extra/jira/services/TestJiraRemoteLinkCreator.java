package com.atlassian.confluence.extra.jira.services;

import com.atlassian.applinks.api.ReadOnlyApplicationLink;
import com.atlassian.applinks.api.ReadOnlyApplicationLinkService;
import com.atlassian.applinks.api.application.jira.JiraApplicationType;
import com.atlassian.applinks.host.spi.HostApplication;
import com.atlassian.confluence.extra.jira.api.services.JiraMacroFinderService;
import com.atlassian.confluence.extra.jira.util.ApplicationLinkHelper;
import com.atlassian.confluence.plugins.jira.JiraRemoteLinkCreator;
import com.atlassian.confluence.setup.settings.SettingsManager;
import com.atlassian.confluence.xhtml.api.MacroDefinition;
import com.atlassian.sal.api.net.RequestFactory;
import junit.framework.Assert;
import junit.framework.TestCase;

import java.util.Collections;

import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TestJiraRemoteLinkCreator  extends TestCase
{
    @Mock
    private ReadOnlyApplicationLinkService applicationLinkService;

    public void testGetAppLinkByMacroDefinition()
    {
        ReadOnlyApplicationLink fakeAppLink = mock(ReadOnlyApplicationLink.class);
        ReadOnlyApplicationLink outAppLink = new ApplicationLinkHelper().findApplicationLink(applicationLinkService, new MacroDefinition());
        Assert.assertNotNull("Must have the default value", outAppLink);
        Assert.assertEquals(fakeAppLink, outAppLink);
    }
}
