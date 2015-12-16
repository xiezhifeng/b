package com.atlassian.confluence.extra.jira;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URI;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import com.atlassian.applinks.api.ApplicationId;
import com.atlassian.applinks.api.ApplicationLinkRequest;
import com.atlassian.applinks.api.ApplicationLinkRequestFactory;
import com.atlassian.applinks.api.CredentialsRequiredException;
import com.atlassian.applinks.api.ReadOnlyApplicationLink;
import com.atlassian.applinks.api.ReadOnlyApplicationLinkService;
import com.atlassian.config.util.BootstrapUtils;
import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.content.render.xhtml.DefaultConversionContext;
import com.atlassian.confluence.content.render.xhtml.Streamable;
import com.atlassian.confluence.content.render.xhtml.editor.macro.EditorMacroMarshaller;
import com.atlassian.confluence.content.render.xhtml.macro.MacroMarshallingFactory;
import com.atlassian.confluence.core.FormatSettingsManager;
import com.atlassian.confluence.extra.jira.JiraIssuesMacro.Type;
import com.atlassian.confluence.extra.jira.JiraIssuesManager.Channel;
import com.atlassian.confluence.extra.jira.api.services.AsyncJiraIssueBatchService;
import com.atlassian.confluence.extra.jira.api.services.JiraIssueBatchService;
import com.atlassian.confluence.extra.jira.helper.ImagePlaceHolderHelper;
import com.atlassian.confluence.extra.jira.helper.JiraExceptionHelper;
import com.atlassian.confluence.extra.jira.model.JiraColumnInfo;
import com.atlassian.confluence.extra.jira.services.DefaultJiraIssueBatchService;
import com.atlassian.confluence.extra.jira.util.JiraConnectorUtils;
import com.atlassian.confluence.extra.jira.util.JiraIssueUtil;
import com.atlassian.confluence.languages.LocaleManager;
import com.atlassian.confluence.macro.MacroExecutionException;
import com.atlassian.confluence.pages.Page;
import com.atlassian.confluence.plugins.jira.JiraServerBean;
import com.atlassian.confluence.renderer.PageContext;
import com.atlassian.confluence.security.Permission;
import com.atlassian.confluence.security.PermissionManager;
import com.atlassian.confluence.security.trust.TrustedTokenFactory;
import com.atlassian.confluence.setup.BootstrapManager;
import com.atlassian.confluence.setup.settings.Settings;
import com.atlassian.confluence.setup.settings.SettingsManager;
import com.atlassian.confluence.util.GeneralUtil;
import com.atlassian.confluence.util.http.HttpRequest;
import com.atlassian.confluence.util.http.HttpResponse;
import com.atlassian.confluence.util.http.HttpRetrievalService;
import com.atlassian.confluence.util.http.trust.TrustedConnectionStatus;
import com.atlassian.confluence.util.http.trust.TrustedConnectionStatusBuilder;
import com.atlassian.confluence.util.i18n.I18NBean;
import com.atlassian.confluence.util.i18n.I18NBeanFactory;
import com.atlassian.confluence.web.context.HttpContext;
import com.atlassian.confluence.xhtml.api.MacroDefinition;
import com.atlassian.plugin.webresource.WebResourceManager;
import com.atlassian.renderer.TokenType;
import com.atlassian.renderer.v2.macro.Macro;
import com.atlassian.renderer.v2.macro.MacroException;
import com.atlassian.sal.api.features.DarkFeatureManager;
import com.atlassian.sal.api.net.Request;
import com.atlassian.sal.api.net.Request.MethodType;
import com.atlassian.sal.api.net.ResponseException;
import com.atlassian.user.User;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.xpath.XPath;
import org.junit.Assert;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import junit.framework.TestCase;

import static com.atlassian.confluence.extra.jira.JiraIssuesMacro.JiraIssuesType.SINGLE;
import static com.atlassian.confluence.extra.jira.JiraIssuesMacro.JiraIssuesType.TABLE;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest(JiraConnectorUtils.class)
public class TestJiraIssuesMacro extends TestCase
{
    @Mock private I18NBeanFactory i18NBeanFactory;

    @Mock private I18NBean i18NBean;

    @Mock private JiraIssuesManager jiraIssuesManager;

    @Mock private SettingsManager settingsManager;

    private JiraIssuesColumnManager jiraIssuesColumnManager;

    private JiraIssuesUrlManager jiraIssuesUrlManager;

    @Mock private JiraIssuesSettingsManager jiraIssuesSettingsManager;

    @Mock private ReadOnlyApplicationLinkService appLinkService;

    @Mock private HttpRetrievalService httpRetrievalService;

    @Mock private ApplicationLinkResolver applicationLinkResolver;

    @Mock private HttpRequest httpRequest;

    @Mock private HttpResponse httpResponse;

    @Mock private TrustedConnectionStatusBuilder trustedConnectionStatusBuilder;

    @Mock private TrustedTokenFactory trustedTokenFactory;

    @Mock private WebResourceManager webResourceManager;

    @Mock private BootstrapManager bootstrapManager;

    @Mock private HttpServletRequest httpServletRequest;

    @Mock private HttpContext httpContext;

    @Mock private PermissionManager permissionManager;

    @Mock private ApplicationLinkRequestFactory requestFactory;

    @Mock private MacroMarshallingFactory macroMarshallingFactory;

    @Mock private Streamable streamable;

    @Mock private EditorMacroMarshaller macroMarshaller;

    @Mock private LocaleManager localeManager;

    @Mock private JiraCacheManager jiraCacheManager;

    @Mock private ReadOnlyApplicationLink appLink;

    @Mock private FormatSettingsManager formatSettingsManager;

    @Mock private JiraIssueSortingManager jiraIssueSortingManager;

    @Mock private JiraConnectorManager jiraConnectorManager;

    @Mock private JiraIssueBatchService jiraIssueBatchService;

    @Mock private DarkFeatureManager darkFeatureManager;

    private JiraIssuesMacro jiraIssuesMacro;

    private SAXBuilder saxBuilder;

    private Map<String, String> params;

    private Map<String, Object> macroVelocityContext;

    private static final String JIRA_KEY_DEFAULT_PARAM = "0";

    private static final String DEFAULT_DATE_FORMAT = "MMM dd yyyy";

    private static final String APPLICATION_ID = "8835b6b9-5676-3de4-ad59-bbe987416662";

    private VelocityEngine ve;

    private GeneralUtil generalUtil;

    private Locale defaultLocale = new Locale("EN");

    private JiraServerBean jiraServerBean;

    @Mock
    private TrustedApplicationConfig trustedApplicationConfig;

    @Mock
    private ImagePlaceHolderHelper imagePlaceHolderHelper;

    @Mock
    private JiraExceptionHelper jiraExceptionHelper;

    @Mock
    AsyncJiraIssueBatchService asyncJiraIssueBatchService;

    protected void setUp() throws Exception
    {
        super.setUp();

        MockitoAnnotations.initMocks(this);
        when(httpContext.getRequest()).thenReturn(httpServletRequest);
        when(httpServletRequest.getContextPath()).thenReturn("/contextPath");
        BootstrapUtils.setBootstrapManager(bootstrapManager);

        jiraIssuesColumnManager = new DefaultJiraIssuesColumnManager(jiraIssuesSettingsManager, localeManager, i18NBeanFactory, jiraConnectorManager);
        jiraIssuesUrlManager = new DefaultJiraIssuesUrlManager(jiraIssuesColumnManager);
        saxBuilder = new SAXBuilder("org.apache.xerces.parsers.SAXParser");
        jiraIssueSortingManager = new DefaultJiraIssueSortingManager(jiraIssuesColumnManager, jiraIssuesManager, localeManager, i18NBeanFactory);
        generalUtil= new GeneralUtil();

        when(i18NBeanFactory.getI18NBean()).thenReturn(i18NBean);
        when(i18NBeanFactory.getI18NBean(any(Locale.class))).thenReturn(i18NBean);

        when(i18NBean.getText(anyString())).thenAnswer(
                new Answer<String>()
                {
                    public String answer(InvocationOnMock invocationOnMock) throws Throwable
                    {
                        return (String) invocationOnMock.getArguments()[0];
                    }
                }
        );

        when(i18NBean.getText(anyString(), (List) anyObject())).thenAnswer(
                new Answer<String>()
                {
                    public String answer(InvocationOnMock invocationOnMock) throws Throwable
                    {
                        return (String) invocationOnMock.getArguments()[0];
                    }
                }
        );

        jiraIssuesMacro = new JiraIssuesMacro(i18NBeanFactory, jiraIssuesManager, settingsManager, jiraIssuesColumnManager, trustedApplicationConfig, permissionManager, applicationLinkResolver, macroMarshallingFactory, jiraCacheManager, imagePlaceHolderHelper, formatSettingsManager, jiraIssueSortingManager, jiraExceptionHelper, localeManager, asyncJiraIssueBatchService, darkFeatureManager);

        params = new HashMap<String, String>();
        macroVelocityContext = new HashMap<String, Object>();
        macroVelocityContext.put("generalUtil", generalUtil);

        when(appLink.getId()).thenReturn(new ApplicationId(APPLICATION_ID));
        when(appLink.getRpcUrl()).thenReturn(URI.create("http://localhost:1990/jira"));
        when(appLink.getDisplayUrl()).thenReturn(URI.create("http://displayurl/jira"));
        when(appLink.createAuthenticatedRequestFactory()).thenReturn(requestFactory);
        Settings settings = mock(Settings.class);
        when(settings.getBaseUrl()).thenReturn("http://localhost:1990/confluence");
        when(settingsManager.getGlobalSettings()).thenReturn(settings);

        when(macroMarshallingFactory.getStorageMarshaller()).thenReturn(macroMarshaller);
        when(macroMarshaller.marshal(any(MacroDefinition.class), any(ConversionContext.class))).thenReturn(streamable);

        when(applicationLinkResolver.getAppLinkForServer(any(String.class), any(String.class))).thenReturn(appLink);
        jiraServerBean = new JiraServerBean(null, null, null, true, null, 6097L);
        when(jiraConnectorManager.getJiraServer(appLink)).thenReturn(jiraServerBean);
        jiraIssueBatchService = new MockDefaultJiraIssueBatchService(jiraIssuesManager, applicationLinkResolver, jiraConnectorManager, jiraExceptionHelper);

        setupVelocityEngine();
    }

    private void setupVelocityEngine() throws Exception
    {
        ve = new VelocityEngine();
        ve.setProperty(RuntimeConstants.RESOURCE_LOADER, "file,classpath");
        ve.setProperty("classpath.resource.loader.class",
                ClasspathResourceLoader.class.getName());
        ve.init();
    }

    private ConversionContext createDefaultConversionContext(boolean isClearCache)
    {
        Page page = new Page();
        page.setId(1l);
        DefaultConversionContext conversionContext = new DefaultConversionContext(new PageContext(page));
        conversionContext.setProperty(DefaultJiraCacheManager.PARAM_CLEAR_CACHE, isClearCache);
        conversionContext.setProperty(JiraIssuesMacro.PARAM_PLACEHOLDER, Boolean.FALSE);
        return conversionContext;
    }

    public void testCallClearCacheWhenIssueTypeIsStaticTable() throws Exception
    {
        List<String> columnList=Lists.newArrayList("type","summary");
        params.put("url", "http://localhost:8080/sr/jira.issueviews:searchrequest-xml/temp/SearchRequest.xml?pid=10000&sorter/field=issuekey&sorter/order=ASC");
        params.put("columns", "type,summary");

        ReadOnlyApplicationLink appLink = mock(ReadOnlyApplicationLink.class);
        when(appLink.getRpcUrl()).thenReturn(URI.create("http://localhost:8080"));
        when(appLink.getDisplayUrl()).thenReturn(URI.create("http://displayurl.com"));

        when(permissionManager.hasPermission((User) anyObject(), (Permission) anyObject(), anyObject())).thenReturn(false);
        when(jiraIssuesManager.retrieveXMLAsChannel(params.get("url"), columnList, appLink, false, true)).thenReturn(
                new MockChannel(params.get("url")));
        when(macroMarshallingFactory.getStorageMarshaller()).thenReturn(macroMarshaller);
        when(macroMarshaller.marshal(any(MacroDefinition.class), any(ConversionContext.class))).thenReturn(streamable);
        when(localeManager.getLocale(any(User.class))).thenReturn(defaultLocale);
        when(formatSettingsManager.getDateFormat()).thenReturn(DEFAULT_DATE_FORMAT);

        Map<String, JiraColumnInfo> columns = new HashMap<String, JiraColumnInfo>();
        columns.put("type", new JiraColumnInfo("type", "Type", Boolean.TRUE));
        columns.put("summary", new JiraColumnInfo("summary", "Summary", Boolean.TRUE));

        mockRestApi(appLink);
        jiraIssuesMacro.createContextMapFromParams(params, macroVelocityContext, params.get("url"), JiraIssuesMacro.Type.URL, appLink, true, false, TABLE, createDefaultConversionContext(false));
        verify(jiraCacheManager, times(0)).clearJiraIssuesCache(anyString(), anyListOf(String.class), any(ReadOnlyApplicationLink.class), anyBoolean(), anyBoolean());

        jiraIssuesMacro.createContextMapFromParams(params, macroVelocityContext, params.get("url"), JiraIssuesMacro.Type.URL, appLink, true, false, TABLE, createDefaultConversionContext(true));
        verify(jiraCacheManager, times(1)).clearJiraIssuesCache(anyString(), anyListOf(String.class), any(ReadOnlyApplicationLink.class), anyBoolean(), anyBoolean());
    }

    private void mockRestApi(ReadOnlyApplicationLink appLink)
            throws CredentialsRequiredException, ResponseException {
        PowerMockito.mockStatic(JiraConnectorUtils.class);
        ApplicationLinkRequest applicationLinkRequest = mock(ApplicationLinkRequest.class);
        when(JiraConnectorUtils.getApplicationLinkRequest(appLink, MethodType.GET, "/rest/api/2/field")).thenReturn(applicationLinkRequest);
        String fieldsJson = "[" + "{\"id\":\"customfield_10560\",\"name\":\"Reviewers\",\"custom\":true,\"orderable\":true,\"navigable\":true,\"searchable\":true,\"schema\":{\"type\":\"array\",\"items\":\"user\",\"custom\":\"com.atlassian.jira.plugin.system.customfieldtypes:multiuserpicker\",\"customId\":10560}}," + "{\"id\":\"summary\",\"name\":\"Summary\",\"custom\":false,\"orderable\":true,\"navigable\":true,\"searchable\":true,\"schema\":{\"type\":\"string\",\"system\":\"summary\"}}"+"]";
        when(applicationLinkRequest.execute()).thenReturn(fieldsJson);
    }

    public void testCreateContextMapForTemplate() throws Exception
    {
        List<String> columnList=Lists.newArrayList("type","summary");
        params.put("url", "http://localhost:8080/sr/jira.issueviews:searchrequest-xml/temp/SearchRequest.xml?pid=10000&sorter/field=issuekey&sorter/order=ASC");
        params.put("columns", "type,summary");
        params.put("title", "EXPLICIT VALUE");

        Map<String, Object> expectedContextMap = Maps.newHashMap();

        ReadOnlyApplicationLink appLink = mock(ReadOnlyApplicationLink.class);
        when(appLink.getRpcUrl()).thenReturn(URI.create("http://localhost:8080"));
        when(appLink.getDisplayUrl()).thenReturn(URI.create("http://displayurl.com"));

        when(permissionManager.hasPermission((User) anyObject(), (Permission) anyObject(), anyObject())).thenReturn(false);
        when(jiraIssuesManager.retrieveXMLAsChannel(params.get("url"), columnList, appLink, false, true)).thenReturn(
                new MockChannel(params.get("url")));
        when(macroMarshallingFactory.getStorageMarshaller()).thenReturn(macroMarshaller);
        when(macroMarshaller.marshal(any(MacroDefinition.class), any(ConversionContext.class))).thenReturn(streamable);
        when(localeManager.getLocale(any(User.class))).thenReturn(defaultLocale);
        when(formatSettingsManager.getDateFormat()).thenReturn(DEFAULT_DATE_FORMAT);

        mockRestApi(appLink);

        expectedContextMap.put("isSourceApplink", true);
        expectedContextMap.put("showTrustWarnings", false);
        expectedContextMap.put("showSummary", true);
        expectedContextMap.put("trustedConnectionStatus",null);
        expectedContextMap.put("width", "100%");
        List<JiraColumnInfo> cols = Lists.newArrayList(new JiraColumnInfo("type"),new JiraColumnInfo("summary"));
        expectedContextMap.put("columns", cols);
        expectedContextMap.put("trustedConnection",false);
        expectedContextMap.put("title", "EXPLICIT VALUE");
        expectedContextMap.put("entries",new MockChannel(params.get("url")).getChannelElement().getChildren("item"));
        expectedContextMap.put("xmlXformer",jiraIssuesMacro.getXmlXformer());
        expectedContextMap.put("clickableUrl", "http://localhost:8080/secure/IssueNavigator.jspa?reset=true&pid=10000&sorter/field=issuekey&sorter/order=ASC&src=confmacro");
        expectedContextMap.put("jiraIssuesColumnManager", jiraIssuesColumnManager);
        expectedContextMap.put("isAdministrator", false);
        expectedContextMap.put("channel",new MockChannel(params.get("url")).getChannelElement());
        expectedContextMap.put("userLocale", Locale.getDefault());
        expectedContextMap.put("contentId", "1");
        expectedContextMap.put("wikiMarkup", "");
        expectedContextMap.put("maxIssuesToDisplay", 20);
        expectedContextMap.put("enableRefresh", Boolean.TRUE);
        expectedContextMap.put("returnMax", "true");
        expectedContextMap.put("generalUtil", generalUtil);
        expectedContextMap.put("jiraServerUrl", "http://displayurl.com");
        expectedContextMap.put("dateFormat", new SimpleDateFormat(DEFAULT_DATE_FORMAT, defaultLocale));
        expectedContextMap.put("singleIssueTable", false);
        expectedContextMap.put(JiraIssuesMacro.PARAM_PLACEHOLDER, Boolean.FALSE);

        ConversionContext conversionContext = createDefaultConversionContext(true);
        Map<String, JiraColumnInfo> columns = new HashMap<String, JiraColumnInfo>();
        columns.put("type", new JiraColumnInfo("type", "Type", Boolean.TRUE));
        columns.put("summary", new JiraColumnInfo("summary", "Summary", Boolean.TRUE));
        jiraIssuesMacro.createContextMapFromParams(params, macroVelocityContext, params.get("url"), JiraIssuesMacro.Type.URL, appLink, true, false, TABLE, conversionContext);
        // comment back in to debug the assert equals on the two maps
        /*
        Set<String> keySet = expectedContextMap.keySet();
        for (String string : keySet)
        {
            if(expectedContextMap.get(string) != null) {
                if (!expectedContextMap.get(string).equals(macroVelocityContext.get(string)))
                    {
                        Object a = expectedContextMap.get(string);
                        Object b = macroVelocityContext.get(string);
                        int x = 0;
                    }
                } else {
                    if(macroVelocityContext.get(string) != null) {
                        int x = 0;
                    }
                }
        }
        */

        /**
         * By definition the 2 List/Elements have cannot be equals
         * -not custom equal method is implemented for the Elements-
         * So, we just measure their length ( should be 0 )
         * and then we remove them to compare the rest of
         * the map.
         *
         * Not very elegant....
         * TODO : Improve this TestCase.
         */

        cleanMaps(expectedContextMap,macroVelocityContext);

        assertEquals(expectedContextMap, macroVelocityContext);

        macroVelocityContext.clear();

        params.put("url", "http://localhost:8080/sr/jira.issueviews:searchrequest-xml/temp/SearchRequest.xml?pid=10000");
        params.put("title", "Some Random & Unlikely Issues");
        params.put("cache", "off");
        params.put("columns", "type,summary,key,reporter");
        params.put("height", "300");

        cols.add(new JiraColumnInfo("key", "key"));
        cols.add(new JiraColumnInfo("reporter", "reporter"));
        columnList.add("key");
        columnList.add("reporter");
        expectedContextMap.put("height", "300");
        expectedContextMap.put("clickableUrl", "http://localhost:8080/secure/IssueNavigator.jspa?reset=true&pid=10000&src=confmacro");
        expectedContextMap.put("title", "Some Random &amp; Unlikely Issues");
        expectedContextMap.remove("generalUtil");

        //Put back the 2 keys previously removed...
        expectedContextMap.put("entries",new MockChannel(params.get("url")).getChannelElement().getChildren("item"));
        expectedContextMap.put("channel",new MockChannel(params.get("url")).getChannelElement());

        when(jiraIssuesManager.retrieveXMLAsChannel(params.get("url"), columnList, appLink, false, false)).thenReturn(
                new MockChannel(params.get("url")));

        jiraIssuesMacro.createContextMapFromParams(params, macroVelocityContext, params.get("url"), JiraIssuesMacro.Type.URL, appLink, true, false, TABLE, conversionContext);

        cleanMaps(expectedContextMap,macroVelocityContext);

        assertEquals(expectedContextMap, macroVelocityContext);
    }

    private void cleanMaps(Map<String,Object> expectedContext, Map<String, Object> velocityContext) throws Exception {
        if(velocityContext.containsKey("entries")){
            @SuppressWarnings("rawtypes")
            List velocityEntries = (List)velocityContext.get("entries");

            @SuppressWarnings("rawtypes")
            List expectedEntries = (List)expectedContext.get("entries");
            if(!(velocityEntries.size() == expectedEntries.size())){
                throw new Exception("Incorrect value for key ['entries']");
            }
        } else {
            throw new Exception("Missing key ['entries']");
        }

        if(velocityContext.containsKey("channel")){
            Element velocityChannel = (Element)velocityContext.get("channel");
            Element expectedChannel = (Element)expectedContext.get("channel");
            if(!(velocityChannel.getValue().equals(expectedChannel.getValue()))){
                throw new Exception("Incorect value for key ['channel']");
            }
        } else {
            throw new Exception("Missing key ['channel']");
        }

        expectedContext.remove("entries");
        velocityContext.remove("entries");
        expectedContext.remove("channel");
        velocityContext.remove("channel");
        velocityContext.remove("refreshId");
    }

    public void testContextMapForDynamicSingleIssues() throws Exception
    {
        ReadOnlyApplicationLink appLink = mock(ReadOnlyApplicationLink.class);
        when(appLink.getRpcUrl()).thenReturn(URI.create("http://localhost:8080"));
        when(appLink.getDisplayUrl()).thenReturn(URI.create("http://displayurl.com"));

        mockRestApi(appLink);

        params.put("key", "TEST-1");
        params.put("title", "EXPLICIT VALUE");

        Map<String, Object> expectedContextMap = new HashMap<String, Object>();
        expectedContextMap.put("clickableUrl", "http://displayurl.com/browse/TEST-1?src=confmacro");
        expectedContextMap.put("columns",
                               ImmutableList.of(new JiraColumnInfo("type"), new JiraColumnInfo("key"), new JiraColumnInfo("summary"),
                                                new JiraColumnInfo("assignee"), new JiraColumnInfo("reporter"), new JiraColumnInfo("priority"),
                                                new JiraColumnInfo("status"), new JiraColumnInfo("resolution"), new JiraColumnInfo("created"),
                                                new JiraColumnInfo("updated"), new JiraColumnInfo("due")));
        expectedContextMap.put("title", "EXPLICIT VALUE");
        expectedContextMap.put("width", "100%");
        expectedContextMap.put("showTrustWarnings", false);
        expectedContextMap.put("showSummary", true);
        expectedContextMap.put("isSourceApplink", true);
        expectedContextMap.put("isAdministrator", false);
        expectedContextMap.put("key", "TEST-1");
        expectedContextMap.put("applink", appLink);
        expectedContextMap.put("maxIssuesToDisplay", 20);
        expectedContextMap.put("returnMax", "true");
        expectedContextMap.put("generalUtil", generalUtil);

        when(permissionManager.hasPermission((User) anyObject(), (Permission) anyObject(), anyObject())).thenReturn(false);
        jiraIssuesMacro.createContextMapFromParams(params, macroVelocityContext, params.get("key"), Type.KEY, appLink, false, false, SINGLE, createDefaultConversionContext(false));

        assertEquals(expectedContextMap, macroVelocityContext);
    }

    public void testContextMapForStaticSingleIssues() throws Exception
    {
        ReadOnlyApplicationLink appLink = mock(ReadOnlyApplicationLink.class);
        when(appLink.getRpcUrl()).thenReturn(URI.create("http://localhost:8080"));
        when(appLink.getDisplayUrl()).thenReturn(URI.create("http://displayurl.com"));

        mockRestApi(appLink);

        params.put("key", "TEST-1");
        params.put("title", "EXPLICIT VALUE");

        Map<String, Object> expectedContextMap = new HashMap<String, Object>();
        expectedContextMap.put("clickableUrl", "http://displayurl.com/browse/TEST-1?src=confmacro");
        expectedContextMap.put("columns",
                ImmutableList.of(new JiraColumnInfo("type"), new JiraColumnInfo("key"), new JiraColumnInfo("summary"),
                        new JiraColumnInfo("assignee"), new JiraColumnInfo("reporter"), new JiraColumnInfo("priority"),
                        new JiraColumnInfo("status"), new JiraColumnInfo("resolution"), new JiraColumnInfo("created"),
                        new JiraColumnInfo("updated"), new JiraColumnInfo("due")));
        expectedContextMap.put("title", "EXPLICIT VALUE");
        expectedContextMap.put("width", "100%");
        expectedContextMap.put("showTrustWarnings", false);
        expectedContextMap.put("showSummary", true);
        expectedContextMap.put("isSourceApplink", true);
        expectedContextMap.put("isAdministrator", false);
        expectedContextMap.put("key", "");
        expectedContextMap.put("maxIssuesToDisplay", 20);
        expectedContextMap.put("returnMax", "true");
        expectedContextMap.put("resolved", true);
        expectedContextMap.put("summary", "");
        expectedContextMap.put("status", "");
        expectedContextMap.put("iconUrl", null);
        expectedContextMap.put("statusIcon", null);
        expectedContextMap.put("generalUtil", generalUtil);
        expectedContextMap.put("isPlaceholder", false);

        when(permissionManager.hasPermission((User) anyObject(), (Permission) anyObject(), anyObject())).thenReturn(false);

        String requestURL = "http://localhost:8080/sr/jira.issueviews:searchrequest-xml/temp/SearchRequest.xml?jqlQuery=key+in+%28TEST-1%29&returnMax=true";
        String[] columns = {"summary", "type", "resolution", "status"};
        when(jiraIssuesManager.retrieveXMLAsChannel(requestURL, Arrays.asList(columns), appLink, false, false))
                .thenReturn(new MockSingleChannel(requestURL));
        when(jiraIssuesManager.retrieveXMLAsChannel(requestURL, Arrays.asList(columns), appLink, false, true))
                .thenReturn(new MockSingleChannel(requestURL));

        //Create with staticMode = false
        jiraIssuesMacro.createContextMapFromParams(params, macroVelocityContext, params.get("key"), Type.KEY, appLink, true, false, SINGLE, createDefaultConversionContext(false));

        assertEquals(expectedContextMap, macroVelocityContext);
    }

    public void testFilterOutParam()
    {
        String expectedUrl = "http://localhost:8080/sr/jira.issueviews:searchrequest-xml/temp/SearchRequest.xml?pid=10000&sorter/field=issuekey&sorter/order=DESC";
        String filter = "tempMax=";
        String value;

        StringBuffer urlWithParamAtEnd = new StringBuffer("http://localhost:8080/sr/jira.issueviews:searchrequest-xml/temp/SearchRequest.xml?pid=10000&sorter/field=issuekey&sorter/order=DESC&tempMax=259");
        value = JiraIssueUtil.filterOutParam(urlWithParamAtEnd, filter);
        assertEquals("259", value);
        assertEquals(expectedUrl, urlWithParamAtEnd.toString());

        StringBuffer urlWithParamAtBeginning = new StringBuffer("http://localhost:8080/sr/jira.issueviews:searchrequest-xml/temp/SearchRequest.xml?tempMax=1&pid=10000&sorter/field=issuekey&sorter/order=DESC");
        value = JiraIssueUtil.filterOutParam(urlWithParamAtBeginning, filter);
        assertEquals("1", value);
        assertEquals(expectedUrl, urlWithParamAtBeginning.toString());

        StringBuffer urlWithParamInMiddle = new StringBuffer("http://localhost:8080/sr/jira.issueviews:searchrequest-xml/temp/SearchRequest.xml?pid=10000&sorter/field=issuekey&tempMax=30&sorter/order=DESC");
        value = JiraIssueUtil.filterOutParam(urlWithParamInMiddle, filter);
        assertEquals("30", value);
        assertEquals(expectedUrl, urlWithParamInMiddle.toString());

    }

    // testing transformation of urls from xml to issue navigator styles
    public void testMakeClickableUrl()
    {
        assertEquals("http://jira.atlassian.com/secure/IssueNavigator.jspa?reset=true&pid=11011&pid=11772&src=confmacro",
                JiraIssueUtil.getClickableUrl("http://jira.atlassian.com/sr/jira.issueviews:searchrequest-xml/temp/SearchRequest.xml?pid=11011&pid=11772", Type.URL, null, null));

        assertEquals("http://jira.atlassian.com/secure/IssueNavigator.jspa?reset=true&src=confmacro",
                JiraIssueUtil.getClickableUrl("http://jira.atlassian.com/sr/jira.issueviews:searchrequest-xml/temp/SearchRequest.xml", Type.URL, null, null));

        assertEquals("http://jira.atlassian.com/secure/IssueNavigator.jspa?requestId=15701&tempMax=200&src=confmacro",
                JiraIssueUtil.getClickableUrl("http://jira.atlassian.com/sr/jira.issueviews:searchrequest-xml/15701/SearchRequest-15701.xml?tempMax=200", Type.URL, null, null));

        assertEquals("http://jira.atlassian.com/secure/IssueNavigator.jspa?requestId=15701&src=confmacro",
                JiraIssueUtil.getClickableUrl("http://jira.atlassian.com/sr/jira.issueviews:searchrequest-xml/15701/SearchRequest-15701.xml", Type.URL, null, null));
    }

    public void testPrepareDisplayColumns()
    {
        List<JiraColumnInfo> defaultColumns = new ArrayList<JiraColumnInfo>();
        defaultColumns.add(new JiraColumnInfo("type"));
        defaultColumns.add(new JiraColumnInfo("key"));
        defaultColumns.add(new JiraColumnInfo("summary"));
        defaultColumns.add(new JiraColumnInfo("assignee"));
        defaultColumns.add(new JiraColumnInfo("reporter"));
        defaultColumns.add(new JiraColumnInfo("priority"));
        defaultColumns.add(new JiraColumnInfo("status"));
        defaultColumns.add(new JiraColumnInfo("resolution"));
        defaultColumns.add(new JiraColumnInfo("created"));
        defaultColumns.add(new JiraColumnInfo("updated"));
        defaultColumns.add(new JiraColumnInfo("due"));

        List<JiraColumnInfo> threeColumns = new ArrayList<JiraColumnInfo>();
        threeColumns.add(new JiraColumnInfo("key"));
        threeColumns.add(new JiraColumnInfo("summary"));
        threeColumns.add(new JiraColumnInfo("assignee"));

        // make sure get default columns when have empty column list
        Map<String, JiraColumnInfo> columns = new HashMap<String, JiraColumnInfo>();
        Map<String, String> columnParams = new HashMap<String, String>();
        columnParams.put("columns", "type,key,summary,assignee,reporter,priority, status,resolution,created,updated,due");
        ReadOnlyApplicationLink appLink = mock(ReadOnlyApplicationLink.class);
        assertEquals(defaultColumns.size(), jiraIssuesColumnManager.getColumnInfo(columnParams, columns, appLink).size());

        // make sure get columns properly
        columnParams.clear();
        columnParams.put("columns", "key,summary,assignee");
        assertEquals(threeColumns, jiraIssuesColumnManager.getColumnInfo(columnParams, columns, appLink));
        columnParams.clear();
        columnParams.put("columns", "key;summary;assignee");
        assertEquals(threeColumns, jiraIssuesColumnManager.getColumnInfo(columnParams, columns, appLink));

        // make sure empty columns are removed
        columnParams.clear();
        columnParams.put("columns", ";key;summary;;assignee");
        assertEquals(threeColumns, jiraIssuesColumnManager.getColumnInfo(columnParams, columns, appLink));
        columnParams.clear();
        columnParams.put("columns", "key;summary;assignee;");
        assertEquals(threeColumns, jiraIssuesColumnManager.getColumnInfo(columnParams, columns, appLink));

        // make sure if all empty columns are removed, get default columns
        columnParams.clear();
        columnParams.put("columns", ";");
        assertEquals(defaultColumns, jiraIssuesColumnManager.getColumnInfo(columnParams, columns, appLink));
    }

    public void testColumnWrapping()
    {
        final String NOWRAP = "nowrap";
        final List<String> NO_WRAPPED_TEXT_FIELDS = Arrays.asList("key", "type", "priority", "status", "created", "updated", "due" );

        List<JiraColumnInfo> columnInfo = jiraIssuesColumnManager.getColumnInfo(new HashMap<String, String>(), new HashMap<String, JiraColumnInfo>(), appLink);

        for (JiraColumnInfo colInfo : columnInfo)
        {
            boolean hasNowrap = colInfo.getHtmlClassName().contains(NOWRAP);
            if(NO_WRAPPED_TEXT_FIELDS.contains(colInfo.getKey()))
            {
                assertTrue("Non-wrapped columns should have nowrap class", hasNowrap);
            }
            else
            {
                assertFalse("Wrapped columns should not have nowrap class (" + colInfo.getKey() + ", " + colInfo.getHtmlClassName() +")", hasNowrap);
            }
        }
    }

    /** <a href="http://developer.atlassian.com/jira/browse/CONFJIRA-124">CONFJIRA-124</a> */
    public void testDescriptionColumnWrapped()
    {
        assertTrue(new JiraColumnInfo("description").shouldWrap());
    }

    /**
     * <a href="http://developer.atlassian.com/jira/browse/CONFJIRA-133">CONFJIRA_133</a>
     */
    public void testBuildInfoRequestedWithCredentialsAndFilterUrls() throws Exception
    {
        params.put("anonymous", "true");
        params.put("url", "http://localhost:1990/jira/sr/jira.issueviews:searchrequest-xml/10000/SearchRequest-10000.xml?tempMax=1000&os_username=admin&os_password=admin");

        ReadOnlyApplicationLink appLink = mock(ReadOnlyApplicationLink.class);
        ApplicationLinkRequest request =  mock(ApplicationLinkRequest.class);

        List<String> columnList = Lists.newArrayList("type", "key", "summary", "assignee", "reporter", "priority", "status", "resolution", "created", "updated", "due");

        when(appLink.getId()).thenReturn(new ApplicationId("8835b6b9-5676-3de4-ad59-bbe987416662"));
        when(appLink.getRpcUrl()).thenReturn(URI.create("http://localhost:1990/jira"));
        when(appLink.createAuthenticatedRequestFactory()).thenReturn(requestFactory);
        String requestUrl = appLink.getRpcUrl() + "/rest/api/2/filter/10000";
        when(requestFactory.createRequest(Request.MethodType.GET, requestUrl)).thenReturn(request);
        when(request.execute()).thenReturn("{\"jql\":\"status=open\"}");
        when(jiraIssuesManager.retrieveXMLAsChannel(params.get("url"), columnList, null, true, false)).thenReturn(
                new MockChannel(params.get("url")));
        when(jiraIssuesManager.retrieveJQLFromFilter("10000", appLink)).thenReturn("status=open");
        Settings settings = new Settings();
        settings.setBaseUrl("http://localhost:1990/confluence");
        when(settingsManager.getGlobalSettings()).thenReturn(settings);

        when(httpRetrievalService.getDefaultRequestFor("http://localhost:1990/jira/sr/jira.issueviews:searchrequest-xml/10000/SearchRequest-10000.xml?os_username=admin&os_password=admin&tempMax=0")).thenReturn(httpRequest);
        when(httpRetrievalService.get(httpRequest)).thenReturn(httpResponse);
        when(httpResponse.getResponse()).thenReturn(
                new ByteArrayInputStream(
                        (
                                "<rss version=\"0.92\" >\n" +
                                        "<channel>\n" +
                                        "    <title>Your Company JIRA</title>\n" +
                                        "    <link>http://localhost:1990/jira/secure/IssueNavigator.jspa?reset=true&amp;pid=10000&amp;sorter/field=issuekey&amp;sorter/order=DESC</link>\n" +
                                        "    <description>An XML representation of a search request</description>\n" +
                                        "    <language>en-us</language>     <issue start=\"0\" end=\"1\" total=\"1\" />     <build-info>\n" +
                                        "        <version>3.13.2</version>\n" +
                                        "        <build-number>335</build-number>\n" +
                                        "        <build-date>05-12-2008</build-date>\n" +
                                        "        <edition>Enterprise</edition>\n" +
                                        "    </build-info>\n" +
                                        "</channel>\n" +
                                        "</rss>"
                        ).getBytes("UTF-8")
                )
        );
        when(macroMarshallingFactory.getStorageMarshaller()).thenReturn(macroMarshaller);
        when(macroMarshaller.marshal(any(MacroDefinition.class), any(ConversionContext.class))).thenReturn(streamable);
        mockRestApi(appLink);

        jiraIssuesMacro.createContextMapFromParams(params, macroVelocityContext, params.get("url"), JiraIssuesMacro.Type.URL, appLink, false, false, TABLE, createDefaultConversionContext(false));
    }

    /**
     * <a href="http://developer.atlassian.com/jira/browse/CONFJIRA-133">CONFJIRA_133</a>
     */
    public void testBuildInfoRequestedOverTrustedConnectionAndFilterUrls() throws Exception
    {
        params.put("anonymous", "false");
        params.put("url", "http://localhost:1990/jira/sr/jira.issueviews:searchrequest-xml/10000/SearchRequest-10000.xml?tempMax=1000");

//        jiraIssuesManager = new DefaultJiraIssuesManager(jiraIssuesColumnManager, jiraIssuesUrlManager, httpRetrievalService, trustedTokenFactory, trustedConnectionStatusBuilder, new DefaultTrustedApplicationConfig());

        ReadOnlyApplicationLink appLink = mock(ReadOnlyApplicationLink.class);
        ApplicationLinkRequest request =  mock(ApplicationLinkRequest.class);

        List<String> columnList = Lists.newArrayList("type", "key", "summary", "assignee", "reporter", "priority", "status", "resolution", "created", "updated", "due");

        when(appLink.getId()).thenReturn(new ApplicationId("8835b6b9-5676-3de4-ad59-bbe987416662"));
        when(appLink.getRpcUrl()).thenReturn(URI.create("http://localhost:1990/jira"));
        when(appLink.createAuthenticatedRequestFactory()).thenReturn(requestFactory);
        String requestUrl = appLink.getRpcUrl() + "/rest/api/2/filter/10000";
        when(requestFactory.createRequest(Request.MethodType.GET, requestUrl)).thenReturn(request);
        when(request.execute()).thenReturn("{\"jql\":\"status=open\"}");
        when(jiraIssuesManager.retrieveXMLAsChannel(params.get("url"), columnList, null, false, false)).thenReturn(
                new MockChannel(params.get("url")));
        when(jiraIssuesManager.retrieveJQLFromFilter("10000", appLink)).thenReturn("status=open");
        Settings settings = new Settings();
        settings.setBaseUrl("http://localhost:1990/confluence");
        when(settingsManager.getGlobalSettings()).thenReturn(settings);
        when(httpRetrievalService.getDefaultRequestFor("http://localhost:1990/jira/sr/jira.issueviews:searchrequest-xml/10000/SearchRequest-10000.xml?tempMax=0")).thenReturn(httpRequest);
        when(httpRetrievalService.get(httpRequest)).thenReturn(httpResponse);
        when(httpResponse.getResponse()).thenReturn(
                new ByteArrayInputStream(
                        (
                                "<rss version=\"0.92\" >\n" +
                                        "<channel>\n" +
                                        "    <title>Your Company JIRA</title>\n" +
                                        "    <link>http://localhost:1990/jira/secure/IssueNavigator.jspa?reset=true&amp;pid=10000&amp;sorter/field=issuekey&amp;sorter/order=DESC</link>\n" +
                                        "    <description>An XML representation of a search request</description>\n" +
                                        "    <language>en-us</language>     <issue start=\"0\" end=\"1\" total=\"1\" />     <build-info>\n" +
                                        "        <version>3.13.2</version>\n" +
                                        "        <build-number>335</build-number>\n" +
                                        "        <build-date>05-12-2008</build-date>\n" +
                                        "        <edition>Enterprise</edition>\n" +
                                        "    </build-info>\n" +
                                        "</channel>\n" +
                                        "</rss>"
                        ).getBytes("UTF-8")
                )
        );
        when(macroMarshallingFactory.getStorageMarshaller()).thenReturn(macroMarshaller);
        when(macroMarshaller.marshal(any(MacroDefinition.class), any(ConversionContext.class))).thenReturn(streamable);

        mockRestApi(appLink);

        jiraIssuesMacro.createContextMapFromParams(params, macroVelocityContext, params.get("url"), JiraIssuesMacro.Type.URL, appLink, false, false, TABLE, createDefaultConversionContext(false));

        //verify(httpRequest).setAuthenticator(isA(TrustedTokenAuthenticator.class));
    }
    private void parseTest(String paramKey, String paramValue, String expectedValue, Type expectedType) throws MacroException, MacroExecutionException
    {
        Map<String, String> params = new HashMap<String, String>();
        params.put(paramKey, paramValue);

        JiraRequestData requestData = JiraIssueUtil.parseRequestData(params, null);

        assertEquals(expectedType, requestData.getRequestType());
        assertEquals(expectedValue == null ? paramValue : expectedValue, requestData.getRequestData());
    }

    public void testJqlRequestParsing() throws MacroException, MacroExecutionException
    {
        parseTest("project", "TST", "project=TST", JiraIssuesMacro.Type.JQL);
    }

    public void testJqlRequestParsingExplicit() throws MacroException, MacroExecutionException
    {
        parseTest("jqlQuery", "project = TST", null, JiraIssuesMacro.Type.JQL);
    }

    public void testSingleKeyRequestParsing() throws MacroException, MacroExecutionException
    {
        parseTest(JIRA_KEY_DEFAULT_PARAM, "TST-2", null, Type.KEY);
    }

    public void testSingleKeyParameterParsing() throws Exception
    {
        Map<String, String> params = Maps.newLinkedHashMap();
        params.put("", "TEST-2");
        params.put(null, "TEST-3");
        params.put("0", "TEST-1");
        JiraRequestData requestData = JiraIssueUtil.parseRequestData(params, null);
        assertEquals("TEST-1", requestData.getRequestData());
    }

    public void testSingleKeyRequestParsingExplicit() throws MacroException, MacroExecutionException
    {
        parseTest("key", "CONF-1234", null, Type.KEY);
    }

    public void testMultiKeyRequestParsing() throws MacroException, MacroExecutionException
    {
        String keys = "TST-1, CONF-1234, TST-5";
        parseTest(JIRA_KEY_DEFAULT_PARAM, keys, "issuekey in (" + keys + ")", Type.JQL);
    }

    public void testMultKeyRequestParsingExplicit() throws MacroException, MacroExecutionException
    {
        String keys = "TST-1, CONF-1234, TST-5";
        parseTest("key", keys, "issuekey in (" + keys + ")", Type.JQL);
    }

    public void testUrlRequestParsing() throws MacroException, MacroExecutionException
    {
        parseTest(JIRA_KEY_DEFAULT_PARAM, "http://jira.atlassian.com/sr/search.xml", null, Type.URL);
    }

    public void testUrlRequestParsingExplicit() throws MacroException, MacroExecutionException
    {
        parseTest("url", "http://jira.atlassian.com/sr/search.xml", null, Type.URL);
    }

    public void testErrorRenderedIfUrlNotSpecified() throws MacroException
    {
        params.clear();
        params.put(Macro.RAW_PARAMS_KEY, "");

        try
        {
            jiraIssuesMacro.execute(params, (String) null, (DefaultConversionContext) null);
            fail();
        }
        catch (MacroExecutionException e)
        {
            assertEquals("jiraissues.error.invalidMacroFormat", e.getCause().getMessage());
        }
    }

    /**
     * <a href="https://studio.plugins.atlassian.com/browse/CONFJIRA-211">CONFJIRA-211</a>
     */
    public void testErrorRenderedIfUrlNotValid() throws MacroException
    {
    	params.clear();
    	params.put("url", "{jiraissues:url=javascript:alert('gotcha!' + document.cookie)}");

    	try
        {
            jiraIssuesMacro.execute(params, (String) null, (DefaultConversionContext) null);
            fail();
        }
        catch (MacroExecutionException e)
        {
            assertEquals("jiraissues.error.invalidurl", e.getCause().getMessage());
        }
    }

    public void testGetTokenTypeFromString ()
    {
        TokenType result;
        TokenType testVals[] = TokenType.values();

        for(TokenType val : testVals) {
            params.clear();
            params.put(": = | TOKEN_TYPE | = :", val.toString());
            result = jiraIssuesMacro.getTokenType(params, null, null);
            assertEquals(result, val);
        }

        params.clear();
        params.put(": = | TOKEN_TYPE | = :", "Whoops");
        result = jiraIssuesMacro.getTokenType(params, null, null);
        assertEquals(result, TokenType.INLINE_BLOCK);

        params.clear();
        params.put(": = | TOKEN_TYPE | = :", null);
        result = jiraIssuesMacro.getTokenType(params, null, null);
        assertEquals(result, TokenType.INLINE_BLOCK);
    }

    @SuppressWarnings("unchecked")
    public void testParsingResolutionDate() throws Exception
    {
        params.clear();
        params.put("server", "jac");

        List<String> columnList = Lists.newArrayList("type", "key", "summary", "assignee", "reporter", "priority", "status", "resolution", "created", "updated", "due", "resolutiondate");
        params.put("columns", Joiner.on(",").join(columnList));
        params.put("url", "http://localhost:1990/jira/sr/jira.issueviews:searchrequest-xml/temp/SearchRequest.xml?tempMax=20&returnMax=true&jqlQuery=status+%3D+open");

        Element mockElement = getMockChannelElement("jiraResponseColumns.xml");
        MockChannel mockChannel = new MockChannel("");
        mockChannel.setChannelElement(mockElement);
        when(jiraIssuesManager.retrieveXMLAsChannel(params.get("url"), columnList, appLink, false, true)).thenReturn(mockChannel);

        when(jiraIssuesManager.retrieveJQLFromFilter(any(String.class), any(ReadOnlyApplicationLink.class))).thenReturn("status = open");

        when(applicationLinkResolver.resolve(any(JiraIssuesMacro.Type.class), any(String.class), any(Map.class))).thenReturn(appLink);

        when(localeManager.getLocale(any(User.class))).thenReturn(defaultLocale);

        when(formatSettingsManager.getDateFormat()).thenReturn(DEFAULT_DATE_FORMAT);

        PowerMockito.mockStatic(JiraConnectorUtils.class);
        ApplicationLinkRequest applicationLinkRequest = mock(ApplicationLinkRequest.class);
        when(JiraConnectorUtils.getApplicationLinkRequest(appLink, MethodType.GET, "/rest/api/2/field")).thenReturn(applicationLinkRequest);
        String fieldsJson = "[" + "{\"id\":\"customfield_10560\",\"name\":\"Reviewers\",\"custom\":true,\"orderable\":true,\"navigable\":true,\"searchable\":true,\"schema\":{\"type\":\"array\",\"items\":\"user\",\"custom\":\"com.atlassian.jira.plugin.system.customfieldtypes:multiuserpicker\",\"customId\":10560}}," + "{\"id\":\"summary\",\"name\":\"Summary\",\"custom\":false,\"orderable\":true,\"navigable\":true,\"searchable\":true,\"schema\":{\"type\":\"string\",\"system\":\"summary\"}}"+"]";
        when(applicationLinkRequest.execute()).thenReturn(fieldsJson);

        jiraIssuesMacro.createContextMapFromParams(params, macroVelocityContext, params.get("url"), JiraIssuesMacro.Type.URL, appLink, true, false, TABLE, createDefaultConversionContext(false));
        Element element = ((Collection<Element>) macroVelocityContext.get("entries")).iterator().next();
        Assert.assertTrue(element.getChildText("resolved").contains("3 Dec 2015"));

        String renderedContent = merge("templates/extra/jira/staticJiraIssues.vm", macroVelocityContext);
        Assert.assertTrue(renderedContent.contains("Dec 03 2015"));
    }

    public void testGetBatchResults() throws Exception
    {
        Set keys = new HashSet();
        Map<String, Object> map = jiraIssueBatchService.getBatchResults(APPLICATION_ID, keys, createDefaultConversionContext(true));
        assertEquals(map.entrySet().size(), 2); // always contains 2 entries
        Map<String, Element> elementMap = (Map<String, Element>) map.get(JiraIssueBatchService.ELEMENT_MAP);
        assertEquals(elementMap.size(), 5);
        Element issue = elementMap.get("TSTT-1");
        assertNotNull(issue);
        String jiraServerUrl = (String) map.get(JiraIssueBatchService.JIRA_DISPLAY_URL);
        assertEquals(jiraServerUrl, "http://displayurl/jira/browse/");
    }

    private String merge(String templateName, Map context) throws Exception
    {
        Template template = ve.getTemplate(templateName);
        StringWriter sw = new StringWriter();
        template.merge(new VelocityContext(context), sw);
        return sw.toString();
    }

    private Element getMockChannelElement(String channelFileName)
    {
        InputStream stream;
        try
        {
            stream = getResourceAsStream(channelFileName);
            Document document = saxBuilder.build(stream);
            return (Element) XPath.selectSingleNode(document, "/rss//channel");
        } catch (Exception e)
        {
            Assert.assertTrue("Unable to parse mock channel : " + e, false);
            return null;
        }
    }

    private InputStream getResourceAsStream(String name) throws IOException
    {
        URL url = getClass().getClassLoader().getResource(name);
        return url.openStream();
    }

    private class MockDefaultJiraIssueBatchService extends DefaultJiraIssueBatchService
    {
        public MockDefaultJiraIssueBatchService(JiraIssuesManager jiraIssuesManager, ApplicationLinkResolver applicationLinkResolver, JiraConnectorManager jiraConnectorManager, JiraExceptionHelper jiraExceptionHelper)
        {
            super(jiraIssuesManager, applicationLinkResolver, jiraConnectorManager, jiraExceptionHelper);
        }

        protected JiraIssuesManager.Channel retrieveChannel(JiraRequestData jiraRequestData, ConversionContext conversionContext, ReadOnlyApplicationLink applicationLink) throws MacroExecutionException
        {
            Element mockElement = getMockChannelElement("jiraBatchResponse.xml");
            MockChannel mockChannel = new MockChannel("");
            mockChannel.setChannelElement(mockElement);
            return mockChannel;
        }
    }

    private class MockChannel extends Channel
    {

        protected Element root = null;

        protected MockChannel(String sourceURL)
        {
            super(sourceURL, (Element)null, null);
        }

        @Override
        public String getSourceUrl()
        {
            return super.getSourceUrl();
        }

        @Override
        public Element getChannelElement()
        {
            if (this.root != null) {
                return this.root;
            }
            root = new Element("root");
            root.addContent(new Element("issue"));
            root.addContent(new Element("item"));

            return root;
        }

        public void setChannelElement(Element root)
        {
            this.root = root;
        }

    }

    private class MockSingleChannel extends MockChannel
    {

        protected MockSingleChannel(String sourceURL)
        {
            super(sourceURL);
        }

        @Override
        public TrustedConnectionStatus getTrustedConnectionStatus()
        {
            return super.getTrustedConnectionStatus();
        }

        @Override
        public Element getChannelElement()
        {
            Element root = new Element("root");
            Element issue = new Element("item");
            issue.addContent(new Element("type"));
            issue.addContent(new Element("key"));
            issue.addContent(new Element("summary"));
            issue.addContent(new Element("link"));

            Element resolution = new Element("resolution");
            issue.addContent(resolution);
            Element status = new Element("status");
            issue.addContent(status);

            root.addContent(issue);
            return root;
        }

        public boolean isTrustedConnection()
        {
            return super.isTrustedConnection();
        }

    }

}