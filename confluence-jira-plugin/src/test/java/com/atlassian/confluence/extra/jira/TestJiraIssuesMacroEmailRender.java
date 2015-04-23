package com.atlassian.confluence.extra.jira;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.content.render.xhtml.macro.MacroMarshallingFactory;
import com.atlassian.confluence.core.FormatSettingsManager;
import com.atlassian.confluence.extra.jira.helper.JiraExceptionHelper;
import com.atlassian.confluence.languages.LocaleManager;
import com.atlassian.confluence.plugins.jira.render.JiraIssueRender;
import com.atlassian.confluence.plugins.jira.render.single.StaticSingleJiraIssueRender;
import com.atlassian.confluence.security.PermissionManager;
import com.atlassian.confluence.setup.settings.SettingsManager;
import com.atlassian.confluence.util.i18n.I18NBean;
import com.atlassian.confluence.util.i18n.I18NBeanFactory;
import com.atlassian.renderer.RenderContextOutputType;

import com.atlassian.user.User;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import junit.framework.Assert;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith (MockitoJUnitRunner.class)
public class TestJiraIssuesMacroEmailRender
{

    @Mock
    private JiraIssuesManager jiraIssuesManager;

    @Mock (answer = Answers.RETURNS_DEEP_STUBS)
    private JiraIssuesColumnManager jiraIssuesColumnManager;

    @Mock (answer = Answers.RETURNS_DEEP_STUBS)
    private PermissionManager permissionManager;

    @Mock (answer = Answers.RETURNS_DEEP_STUBS)
    private I18NBeanFactory i18NBeanFactory;

    @Mock (answer = Answers.RETURNS_DEEP_STUBS)
    private SettingsManager settingsManager;

    @Mock (answer = Answers.RETURNS_DEEP_STUBS)
    private TrustedApplicationConfig trustedApplicationConfig;

    @Mock (answer = Answers.RETURNS_DEEP_STUBS)
    private ApplicationLinkResolver applicationLinkResolver;

    @Mock (answer = Answers.RETURNS_DEEP_STUBS)
    private JiraIssuesDateFormatter jiraIssuesDateFormatter;

    @Mock (answer = Answers.RETURNS_DEEP_STUBS)
    private MacroMarshallingFactory macroMarshallingFactory;

    @Mock (answer = Answers.RETURNS_DEEP_STUBS)
    private JiraCacheManager jiraCacheManager;

    @Mock (answer = Answers.RETURNS_DEEP_STUBS)
    private FormatSettingsManager formatSettingsManager;

    @Mock (answer = Answers.RETURNS_DEEP_STUBS)
    private JiraIssueSortingManager jiraIssueSortingManager;

    @Mock (answer = Answers.RETURNS_DEEP_STUBS)
    private JiraExceptionHelper jiraExceptionHelper;

    @Mock (answer = Answers.RETURNS_DEEP_STUBS)
    private LocaleManager localeManager;

    @Mock
    private I18NBean i18NBean;

    private JiraIssueRender jiraIssueRender;

    @Before
    public void setUp() throws Exception
    {
        when(i18NBeanFactory.getI18NBean()).thenReturn(i18NBean);
        when(i18NBeanFactory.getI18NBean(any(Locale.class))).thenReturn(i18NBean);
        when(localeManager.getLocale(any(User.class))).thenReturn(new Locale("EN"));

        jiraIssueRender = new StaticSingleJiraIssueRender();
        jiraIssueRender.setPermissionManager(permissionManager);
        jiraIssueRender.setI18NBeanFactory(i18NBeanFactory);
        jiraIssueRender.setLocaleManager(localeManager);
    }

    @Test
    public void testCreateContextMapForEmail() throws Exception
    {
        //given:
        final ConversionContext conversionContext = mock(ConversionContext.class);
        final ApplicationLink applicationLink = mock(ApplicationLink.class);
        final HashMap<String, Object> contextMap = new HashMap<String, Object>();
        when(conversionContext.getOutputDeviceType()).thenReturn(RenderContextOutputType.EMAIL);
        when(applicationLink.getRpcUrl()).thenReturn(URI.create("http://test/"));
        when(applicationLink.getDisplayUrl()).thenReturn(URI.create("http://test/"));

        //when:

        jiraIssueRender.setupCommonContextMap(
                new HashMap<String, String>(),
                contextMap,
                new JiraRequestData("", JiraIssuesMacro.Type.KEY, JiraIssuesMacro.JiraIssuesType.SINGLE),
                applicationLink,
                conversionContext
        );

        //test:
        verify(jiraIssuesManager, never())
                .retrieveXMLAsChannel(anyString(), any(List.class), any(ApplicationLink.class), anyBoolean(), anyBoolean());
        Assert.assertTrue((Boolean) (contextMap.get(JiraIssuesMacro.IS_NO_PERMISSION_TO_VIEW)));
    }

}
