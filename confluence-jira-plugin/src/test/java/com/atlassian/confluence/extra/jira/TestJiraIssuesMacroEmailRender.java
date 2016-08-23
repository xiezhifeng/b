package com.atlassian.confluence.extra.jira;

import java.net.URI;
import java.util.HashMap;
import java.util.List;

import com.atlassian.applinks.api.ReadOnlyApplicationLink;
import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.content.render.xhtml.macro.MacroMarshallingFactory;
import com.atlassian.confluence.core.FormatSettingsManager;
import com.atlassian.confluence.extra.jira.api.services.AsyncJiraIssueBatchService;
import com.atlassian.confluence.extra.jira.helper.ImagePlaceHolderHelper;
import com.atlassian.confluence.extra.jira.helper.JiraExceptionHelper;
import com.atlassian.confluence.languages.LocaleManager;
import com.atlassian.confluence.security.PermissionManager;
import com.atlassian.confluence.setup.settings.SettingsManager;
import com.atlassian.confluence.util.i18n.I18NBeanFactory;
import com.atlassian.renderer.RenderContextOutputType;

import com.atlassian.sal.api.features.DarkFeatureManager;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
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
    private class JiraIssuesMacroTestHarness extends JiraIssuesMacro
    {
        private JiraIssuesMacroTestHarness(I18NBeanFactory i18NBeanFactory,
                                           JiraIssuesManager jiraIssuesManager,
                                           SettingsManager settingsManager,
                                           JiraIssuesColumnManager jiraIssuesColumnManager,
                                           TrustedApplicationConfig trustedApplicationConfig,
                                           PermissionManager permissionManager,
                                           ApplicationLinkResolver applicationLinkResolver,
                                           MacroMarshallingFactory macroMarshallingFactory,
                                           JiraCacheManager jiraCacheManager,
                                           ImagePlaceHolderHelper imagePlaceHolderHelper,
                                           FormatSettingsManager formatSettingsManager,
                                           JiraIssueSortingManager jiraIssueSortingManager,
                                           JiraExceptionHelper jiraExceptionHelper,
                                           LocaleManager localeManager,
                                           AsyncJiraIssueBatchService asyncJiraIssueBatchService,
                                           DarkFeatureManager darkFeatureManager)
        {
            super(i18NBeanFactory,
                  jiraIssuesManager,
                  settingsManager,
                  jiraIssuesColumnManager,
                  trustedApplicationConfig,
                  permissionManager,
                  applicationLinkResolver,
                  macroMarshallingFactory,
                  jiraCacheManager,
                  imagePlaceHolderHelper,
                  formatSettingsManager,
                  jiraIssueSortingManager,
                  jiraExceptionHelper,
                  localeManager,
                  asyncJiraIssueBatchService,
                  darkFeatureManager);
        }
    }

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
    private MacroMarshallingFactory macroMarshallingFactory;

    @Mock (answer = Answers.RETURNS_DEEP_STUBS)
    private JiraCacheManager jiraCacheManager;

    @Mock (answer = Answers.RETURNS_DEEP_STUBS)
    private ImagePlaceHolderHelper imagePlaceHolderHelper;

    @Mock (answer = Answers.RETURNS_DEEP_STUBS)
    private FormatSettingsManager formatSettingsManager;

    @Mock (answer = Answers.RETURNS_DEEP_STUBS)
    private JiraIssueSortingManager jiraIssueSortingManager;

    @Mock (answer = Answers.RETURNS_DEEP_STUBS)
    private JiraExceptionHelper jiraExceptionHelper;

    @Mock (answer = Answers.RETURNS_DEEP_STUBS)
    private LocaleManager localeManager;

    @Mock (answer = Answers.RETURNS_DEEP_STUBS)
    private AsyncJiraIssueBatchService asyncJiraIssueBatchService;

    @Mock (answer = Answers.RETURNS_DEEP_STUBS)
    private DarkFeatureManager darkFeatureManager;

    private JiraIssuesMacroTestHarness jiraIssuesMacroTestHarness;


    @Before
    public void setUp() throws Exception
    {
        jiraIssuesMacroTestHarness
                = new JiraIssuesMacroTestHarness(i18NBeanFactory,
                                                 jiraIssuesManager,
                                                 settingsManager,
                                                 jiraIssuesColumnManager,
                                                 trustedApplicationConfig,
                                                 permissionManager,
                                                 applicationLinkResolver,
                                                 macroMarshallingFactory,
                                                 jiraCacheManager,
                                                 imagePlaceHolderHelper,
                                                 formatSettingsManager,
                                                 jiraIssueSortingManager,
                                                 jiraExceptionHelper,
                                                 localeManager,
                                                 asyncJiraIssueBatchService,
                                                 darkFeatureManager);
    }

    @Test
    public void testCreateContextMapForEmail() throws Exception
    {
        //given:
        final ConversionContext conversionContext = mock(ConversionContext.class);
        final ReadOnlyApplicationLink applicationLink = mock(ReadOnlyApplicationLink.class);
        final HashMap<String, Object> contextMap = new HashMap<String, Object>();
        when(conversionContext.getOutputDeviceType()).thenReturn(RenderContextOutputType.EMAIL);
        when(applicationLink.getRpcUrl()).thenReturn(URI.create("http://test/"));
        when(applicationLink.getDisplayUrl()).thenReturn(URI.create("http://test/"));
        final ImmutableMap<String, ImmutableSet<String>> mockI18nColumnNames;

        //when:
        jiraIssuesMacroTestHarness.createContextMapFromParams(
                new HashMap<String, String>(),
                contextMap,
                "",
                JiraIssuesMacro.Type.KEY,
                applicationLink,
                true,
                false,
                JiraIssuesMacro.JiraIssuesType.SINGLE,
                conversionContext,
                TestJiraIssuesMacro.mockGetI18nColumnNames()
        );

        //test:
        verify(jiraIssuesManager, never())
                .retrieveXMLAsChannel(anyString(),any(List.class),any(ReadOnlyApplicationLink.class),anyBoolean(),anyBoolean());
        Assert.assertTrue((Boolean) (contextMap.get(JiraIssuesMacro.IS_NO_PERMISSION_TO_VIEW)));
    }

}
