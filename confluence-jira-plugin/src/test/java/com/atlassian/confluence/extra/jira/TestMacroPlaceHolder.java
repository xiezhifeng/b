package com.atlassian.confluence.extra.jira;

import com.atlassian.applinks.api.ApplicationId;
import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.applinks.api.ApplicationLinkService;
import com.atlassian.cache.Cache;
import com.atlassian.cache.CacheManager;
import com.atlassian.confluence.content.render.xhtml.macro.MacroMarshallingFactory;
import com.atlassian.confluence.core.FormatSettingsManager;
import com.atlassian.confluence.extra.jira.helper.ImagePlaceHolderHelper;
import com.atlassian.confluence.extra.jira.helper.JiraExceptionHelper;
import com.atlassian.confluence.languages.LocaleManager;
import com.atlassian.confluence.macro.ImagePlaceholder;
import com.atlassian.confluence.security.PermissionManager;
import com.atlassian.confluence.setup.settings.SettingsManager;
import com.atlassian.confluence.util.i18n.I18NBeanFactory;
import junit.framework.TestCase;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyCollection;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestMacroPlaceHolder extends TestCase
{

    private static final String JIRA_TABLE_DISPLAY_PLACEHOLDER_IMG_PATH = "/download/resources/confluence.extra.jira/jira-table.png";

    @Mock
    private ApplicationLinkService appLinkService;

    @Mock
    private JiraIssuesUrlManager jiraIssuesUrlManager;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private JiraIssuesManager jiraIssuesManager;

    @Mock
    private FlexigridResponseGenerator flexigridResponseGenerator;

    @Mock
    private ApplicationLinkResolver applicationLinkResolver;

    @Mock
    private LocaleManager localeManager;

    private JiraIssuesMacro jiraIssuesMacro;

    private ImagePlaceHolderHelper imagePlaceHolderHelper;

    private Map<String, String> parameters;

    @Mock
    private I18NBeanFactory i18NBeanFactory;

    @Mock
    private SettingsManager settingsManager;

    @Mock
    private JiraIssuesColumnManager jiraIssuesColumnManager;

    @Mock
    private TrustedApplicationConfig trustedApplicationConfig;

    @Mock
    private PermissionManager permissionManager;

    @Mock
    private JiraIssuesDateFormatter jiraIssuesDateFormatter;

    @Mock
    private MacroMarshallingFactory macroMarshallingFactory;

    @Mock
    private JiraCacheManager jiraCacheManager;

    @Mock
    private FormatSettingsManager formatSettingsManager;

    @Mock
    private JiraIssueSortingManager jiraIssueSortingManager;

    @Mock
    private JiraExceptionHelper jiraExceptionHelper;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();
        MockitoAnnotations.initMocks(this);
        imagePlaceHolderHelper = new ImagePlaceHolderHelper(jiraIssuesManager, localeManager, null, applicationLinkResolver, flexigridResponseGenerator);
        jiraIssuesMacro = new JiraIssuesMacro(i18NBeanFactory, jiraIssuesManager, settingsManager, jiraIssuesColumnManager, trustedApplicationConfig, permissionManager, applicationLinkResolver, jiraIssuesDateFormatter, macroMarshallingFactory, jiraCacheManager, imagePlaceHolderHelper, formatSettingsManager, jiraIssueSortingManager, jiraExceptionHelper, localeManager);
        //jiraIssuesMacro.setImagePlaceHolderHelper(imagePlaceHolderHelper);
        parameters = new HashMap<String, String>();

    }

    public void testGenerateImagePlaceholderWithCount() throws Exception
    {
        parameters.put("count", "true");
        parameters.put("serverId", "8835b6b9-5676-3de4-ad59-bbe987416662");
        parameters.put("jqlQuery", "project=demo");

        URI uri = new URI("localhost:1990/jira");
        String url = "/sr/jira.issueviews:searchrequest-xml/temp/SearchRequest.xml?jqlQuery=";

        ApplicationId applicationId = new ApplicationId("8835b6b9-5676-3de4-ad59-bbe987416662");
        ApplicationLink applicationLink = mock(ApplicationLink.class);
        when(applicationLink.getDisplayUrl()).thenReturn(uri);
        when(applicationLink.getId()).thenReturn(applicationId);
        url = applicationLink.getDisplayUrl() + url + URLEncoder.encode(parameters.get("jqlQuery"), "UTF-8") + "&tempMax=0";
        when(appLinkService.getApplicationLink(any(ApplicationId.class))).thenReturn(applicationLink);
        when(applicationLinkResolver.resolve(any(JiraIssuesMacro.Type.class), anyString(), anyMap())).thenReturn(applicationLink);
        when(jiraIssuesUrlManager.getJiraXmlUrlFromFlexigridRequest(url, "10", null, null)).thenReturn("jiraIssueXmlUrlWithoutPaginationParam");
        Cache cache = mock(Cache.class);
        when(cacheManager.getCache(anyString())).thenReturn(cache);
        JiraIssuesManager.Channel channel = mock(JiraIssuesManager.Channel.class);
        when(jiraIssuesManager.retrieveXMLAsChannel(url, new ArrayList<String>(), applicationLink, false, false)).thenReturn(channel);
        when(flexigridResponseGenerator.generate(any(JiraIssuesManager.Channel.class), anyCollection(), anyInt(), anyBoolean(), anyBoolean())).thenReturn("5");

        ImagePlaceholder defaultImagePlaceholder = jiraIssuesMacro.getImagePlaceholder(parameters, null);
        assertEquals(defaultImagePlaceholder.getUrl(), "/plugins/servlet/image-generator?totalIssues=5");
    }

    public void testGenerateImagePlaceholderWithNoCountAndNoJql()
    {
        parameters.put("key", "TP");
        jiraIssuesMacro.setResourcePath("jira-xhtml");

        when(localeManager.getSiteDefaultLocale()).thenReturn(Locale.ENGLISH);
        ImagePlaceholder defaultImagePlaceholder = jiraIssuesMacro.getImagePlaceholder(parameters, null);
        assertNotNull(defaultImagePlaceholder);
    }

    public void testGetTableImagePlaceholder()
    {
        parameters.put("jqlQuery", "status=open");
        ImagePlaceholder imagePlaceholder = jiraIssuesMacro.getImagePlaceholder(parameters, null);
        assertEquals(imagePlaceholder.getUrl(), JIRA_TABLE_DISPLAY_PLACEHOLDER_IMG_PATH);
    }

}
