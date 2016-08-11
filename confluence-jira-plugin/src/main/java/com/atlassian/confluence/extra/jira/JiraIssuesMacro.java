package com.atlassian.confluence.extra.jira;


import com.atlassian.applinks.api.ApplicationLinkRequest;
import com.atlassian.applinks.api.CredentialsRequiredException;
import com.atlassian.applinks.api.ReadOnlyApplicationLink;
import com.atlassian.applinks.api.TypeNotInstalledException;
import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.content.render.xhtml.ConversionContextOutputDeviceType;
import com.atlassian.confluence.content.render.xhtml.DefaultConversionContext;
import com.atlassian.confluence.content.render.xhtml.Streamable;
import com.atlassian.confluence.content.render.xhtml.XhtmlException;
import com.atlassian.confluence.content.render.xhtml.definition.RichTextMacroBody;
import com.atlassian.confluence.content.render.xhtml.macro.MacroMarshallingFactory;
import com.atlassian.confluence.core.ContentEntityObject;
import com.atlassian.confluence.core.FormatSettingsManager;
import com.atlassian.confluence.extra.jira.api.services.AsyncJiraIssueBatchService;
import com.atlassian.confluence.extra.jira.exception.JiraIssueDataException;
import com.atlassian.confluence.extra.jira.exception.JiraIssueMacroException;
import com.atlassian.confluence.extra.jira.exception.MalformedRequestException;
import com.atlassian.confluence.extra.jira.helper.Epic;
import com.atlassian.confluence.extra.jira.helper.ImagePlaceHolderHelper;
import com.atlassian.confluence.extra.jira.helper.JiraExceptionHelper;
import com.atlassian.confluence.extra.jira.helper.JiraIssueSortableHelper;
import com.atlassian.confluence.extra.jira.helper.JiraJqlHelper;
import com.atlassian.confluence.extra.jira.model.ClientId;
import com.atlassian.confluence.extra.jira.model.JiraColumnInfo;
import com.atlassian.confluence.extra.jira.util.JiraConnectorUtils;
import com.atlassian.confluence.extra.jira.util.JiraIssuePdfExportUtil;
import com.atlassian.confluence.extra.jira.util.JiraIssueUtil;
import com.atlassian.confluence.extra.jira.util.JiraUtil;
import com.atlassian.confluence.languages.LocaleManager;
import com.atlassian.confluence.macro.EditorImagePlaceholder;
import com.atlassian.confluence.macro.ImagePlaceholder;
import com.atlassian.confluence.macro.Macro;
import com.atlassian.confluence.macro.MacroExecutionException;
import com.atlassian.confluence.macro.ResourceAware;
import com.atlassian.confluence.renderer.radeox.macros.MacroUtils;
import com.atlassian.confluence.search.service.ContentTypeEnum;
import com.atlassian.confluence.security.Permission;
import com.atlassian.confluence.security.PermissionManager;
import com.atlassian.confluence.setup.settings.SettingsManager;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.confluence.util.GeneralUtil;
import com.atlassian.confluence.util.i18n.I18NBean;
import com.atlassian.confluence.util.i18n.I18NBeanFactory;
import com.atlassian.confluence.util.velocity.VelocityUtils;
import com.atlassian.confluence.xhtml.api.MacroDefinition;
import com.atlassian.renderer.RenderContext;
import com.atlassian.renderer.RenderContextOutputType;
import com.atlassian.renderer.TokenType;
import com.atlassian.renderer.v2.RenderMode;
import com.atlassian.renderer.v2.macro.BaseMacro;
import com.atlassian.renderer.v2.macro.MacroException;
import com.atlassian.sal.api.features.DarkFeatureManager;
import com.atlassian.sal.api.net.Request;
import com.atlassian.sal.api.net.ResponseException;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonParser;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.jdom.DataConversionException;
import org.jdom.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import static com.atlassian.confluence.extra.jira.DefaultJiraIssuesColumnManager.COLUMN_EPIC_COLOUR;
import static com.atlassian.confluence.extra.jira.DefaultJiraIssuesColumnManager.COLUMN_EPIC_LINK;
import static com.atlassian.confluence.extra.jira.DefaultJiraIssuesColumnManager.COLUMN_EPIC_NAME;
import static com.atlassian.confluence.extra.jira.DefaultJiraIssuesColumnManager.COLUMN_EPIC_STATUS;
import static com.atlassian.confluence.extra.jira.DefaultJiraIssuesColumnManager.matchColumnNameFromList;
import static com.atlassian.confluence.extra.jira.DefaultJiraIssuesColumnManager.matchColumnNameFromString;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A macro to import/fetch JIRA issues...
 */
public class JiraIssuesMacro extends BaseMacro implements Macro, EditorImagePlaceholder, ResourceAware
{
    private static final Logger LOGGER = LoggerFactory.getLogger(JiraIssuesMacro.class);
    private static final Gson gson = new Gson();
    private static final JsonParser parser = new JsonParser();
    private static final Random RANDOM = new Random();

    /**
     * Default constructor to get all necessary beans injected
     * @param i18NBeanFactory          see {@link com.atlassian.confluence.util.i18n.I18NBeanFactory}
     * @param jiraIssuesManager        see {@link com.atlassian.confluence.extra.jira.JiraIssuesManager}
     * @param settingsManager          see {@link com.atlassian.confluence.setup.settings.SettingsManager}
     * @param jiraIssuesColumnManager  see {@link com.atlassian.confluence.extra.jira.JiraIssuesColumnManager}
     * @param trustedApplicationConfig see {@link com.atlassian.confluence.extra.jira.TrustedApplicationConfig}
     * @param permissionManager        see {@link com.atlassian.confluence.security.PermissionManager}
     * @param applicationLinkResolver  see {@link com.atlassian.confluence.extra.jira.ApplicationLinkResolver}
     * @param macroMarshallingFactory  see {@link com.atlassian.confluence.content.render.xhtml.macro.MacroMarshallingFactory}
     * @param jiraCacheManager         see {@link com.atlassian.confluence.extra.jira.JiraCacheManager}
     * @param imagePlaceHolderHelper   see {@link com.atlassian.confluence.extra.jira.helper.ImagePlaceHolderHelper}
     * @param formatSettingsManager    see {@link com.atlassian.confluence.core.FormatSettingsManager}
     * @param jiraIssueSortingManager  see {@link com.atlassian.confluence.extra.jira.JiraIssueSortingManager}
     * @param jiraExceptionHelper      see {@link com.atlassian.confluence.extra.jira.helper.JiraExceptionHelper}
     * @param localeManager            see {@link com.atlassian.confluence.languages.LocaleManager}
     * @param darkFeatureManager       see {@link com.atlassian.sal.api.features.DarkFeatureManager}
     */
    public JiraIssuesMacro(I18NBeanFactory i18NBeanFactory, JiraIssuesManager jiraIssuesManager, SettingsManager settingsManager, JiraIssuesColumnManager jiraIssuesColumnManager, TrustedApplicationConfig trustedApplicationConfig, PermissionManager permissionManager, ApplicationLinkResolver applicationLinkResolver, MacroMarshallingFactory macroMarshallingFactory, JiraCacheManager jiraCacheManager, ImagePlaceHolderHelper imagePlaceHolderHelper, FormatSettingsManager formatSettingsManager, JiraIssueSortingManager jiraIssueSortingManager, JiraExceptionHelper jiraExceptionHelper, LocaleManager localeManager,
                           AsyncJiraIssueBatchService asyncJiraIssueBatchService,
                           DarkFeatureManager darkFeatureManager)
    {
        this.i18NBeanFactory = checkNotNull(i18NBeanFactory);
        this.jiraIssuesManager = jiraIssuesManager;
        this.settingsManager = settingsManager;
        this.jiraIssuesColumnManager = jiraIssuesColumnManager;
        this.trustedApplicationConfig = trustedApplicationConfig;
        this.permissionManager = permissionManager;
        this.applicationLinkResolver = applicationLinkResolver;
        this.macroMarshallingFactory = macroMarshallingFactory;
        this.jiraCacheManager = jiraCacheManager;
        this.imagePlaceHolderHelper = imagePlaceHolderHelper;
        this.formatSettingsManager = formatSettingsManager;
        this.jiraIssueSortingManager = jiraIssueSortingManager;
        this.jiraExceptionHelper = jiraExceptionHelper;
        this.localeManager = checkNotNull(localeManager);
        this.asyncJiraIssueBatchService = asyncJiraIssueBatchService;
        this.darkFeatureManager = darkFeatureManager;
    }

    // This parameter to distinguish the placeholder & real data mode for jira table
    public static final String PARAM_PLACEHOLDER = "placeholder";

    public static enum Type {KEY, JQL, URL}
    public static enum JiraIssuesType {SINGLE, COUNT, TABLE}
    public static final List<String> DEFAULT_COLUMNS_FOR_SINGLE_ISSUE = Arrays.asList(
            "summary", "type", "resolution", "status");

    // All context map's keys and parameters should be defined here to avoid unexpected typos and make the code clearer and easier for maintenance
    public static final String KEY = "key";
    public static final String JIRA = "jira";
    public static final String JIRAISSUES = "jiraissues";
    public static final String SHOW_SUMMARY = "showSummary";
    public static final String ITEM ="item";
    public static final String SERVER_ID = "serverId";
    public static final String CLIENT_ID = "clientId";
    public static final String CLICKABLE_URL = "clickableUrl";
    public static final String JIRA_SERVER_URL = "jiraServerUrl";

    // URL fragment appended to the display URL to create links to issues
    public static final String JIRA_BROWSE_URL = "/browse/";

    public static final String TEMPLATE_PATH = "templates/extra/jira";
    public static final String MOBILE = "mobile";
    public static final String SERVER = "server";
    public static final String ISSUE_TYPE = "issueType";
    public static final String COLUMNS = "columns";

    private static final String TOKEN_TYPE_PARAM = ": = | TOKEN_TYPE | = :";
    private static final String RENDER_MODE_PARAM = "renderMode";
    private static final String DYNAMIC_RENDER_MODE = "dynamic";
    private static final String DEFAULT_DATA_WIDTH = "100%";
    private static final String CACHE = "cache";
    private static final String ENABLE_REFRESH = "enableRefresh";
    private static final String TOTAL_ISSUES = "totalIssues";
    private static final String TITLE = "title";
    private static final String ANONYMOUS = "anonymous";
    private static final String WIDTH = "width";
    private static final String HEIGHT = "height";

    @VisibleForTesting
    static final String IS_NO_PERMISSION_TO_VIEW = "isNoPermissionToView";
    private static final String COUNT = "count";
    private static final String ICON_URL = "iconUrl";
    private static final String IS_ADMINISTRATOR = "isAdministrator";
    private static final String IS_SOURCE_APP_LINK = "isSourceApplink";
    private static final String MAX_ISSUES_TO_DISPLAY = "maxIssuesToDisplay";
    private static final String BASE_URL = "baseurl";
    private static final String MAXIMUM_ISSUES = "maximumIssues";

    private static final String TEMPLATE_MOBILE_PATH = "templates/mobile/extra/jira";
    private static final String DEFAULT_JIRA_ISSUES_COUNT = "0";

    private static final String EMAIL_RENDER = "email";
    private static final String PDF_EXPORT = "pdfExport";
    public static final List<String> MACRO_PARAMS = Arrays.asList(
            COUNT, COLUMNS, TITLE, RENDER_MODE_PARAM, CACHE, WIDTH,
            HEIGHT, SERVER, SERVER_ID, ANONYMOUS, BASE_URL, SHOW_SUMMARY, com.atlassian.renderer.v2.macro.Macro.RAW_PARAMS_KEY, MAXIMUM_ISSUES, TOKEN_TYPE_PARAM);

    private final JiraIssuesXmlTransformer xmlXformer = new JiraIssuesXmlTransformer();

    private I18NBeanFactory i18NBeanFactory;

    private JiraIssuesManager jiraIssuesManager;

    private SettingsManager settingsManager;

    private JiraIssuesColumnManager jiraIssuesColumnManager;

    private TrustedApplicationConfig trustedApplicationConfig;

    private String resourcePath;

    private PermissionManager permissionManager;

    protected ApplicationLinkResolver applicationLinkResolver;

    private LocaleManager localeManager;

    private MacroMarshallingFactory macroMarshallingFactory;

    private JiraCacheManager jiraCacheManager;

    private ImagePlaceHolderHelper imagePlaceHolderHelper;

    private FormatSettingsManager formatSettingsManager;

    private JiraIssueSortingManager jiraIssueSortingManager;

    private final AsyncJiraIssueBatchService asyncJiraIssueBatchService;

    private final DarkFeatureManager darkFeatureManager;

    protected final JiraExceptionHelper jiraExceptionHelper;

    protected I18NBean getI18NBean()
    {
        if (null != AuthenticatedUserThreadLocal.get())
        {
            return i18NBeanFactory.getI18NBean(localeManager.getLocale(AuthenticatedUserThreadLocal.get()));
        }
        return i18NBeanFactory.getI18NBean();
    }

    String getText(String i18n)
    {
        return getI18NBean().getText(i18n);
    }

    @Override
    public TokenType getTokenType(Map parameters, String body,
            RenderContext context)
    {
        String tokenTypeString = (String) parameters.get(TOKEN_TYPE_PARAM);
        if (StringUtils.isBlank(tokenTypeString))
        {
            return TokenType.INLINE_BLOCK;
        }
        for (TokenType value : TokenType.values())
        {
            if (value.toString().equals(tokenTypeString))
            {
                return TokenType.valueOf(tokenTypeString);
            }
        }
        return TokenType.INLINE_BLOCK;
    }

    public ImagePlaceholder getImagePlaceholder(Map<String, String> parameters, ConversionContext conversionContext)
    {
        try
        {
            JiraRequestData jiraRequestData = JiraIssueUtil.parseRequestData(parameters, getI18NBean());
            return imagePlaceHolderHelper.getJiraMacroImagePlaceholder(jiraRequestData, parameters, resourcePath);
        }
        catch (MacroExecutionException e)
        {
            LOGGER.error("Error generate macro placeholder", e);
        }
        //return default placeholder
        return null;
    }

    public boolean hasBody()
    {
        return false;
    }

    public RenderMode getBodyRenderMode()
    {
        return RenderMode.NO_RENDER;
    }

    private boolean isTrustWarningsEnabled()
    {
        return null != trustedApplicationConfig && trustedApplicationConfig.isTrustWarningsEnabled();
    }

    public String execute(Map params, String body, RenderContext renderContext) throws MacroException
    {
        try
        {
            return execute(params, body, new DefaultConversionContext(renderContext));
        }
        catch (MacroExecutionException e)
        {
            throw new MacroException(e);
        }
    }

    protected void createContextMapFromParams(Map<String, String> params, Map<String, Object> contextMap,
                    String requestData, Type requestType, ReadOnlyApplicationLink applink,
                    boolean staticMode, boolean isMobile, JiraIssuesType issuesType, ConversionContext conversionContext,
                    ImmutableMap<String, ImmutableSet<String>> i18nColumnNames) throws MacroExecutionException
    {
        // Prepare the maxIssuesToDisplay for velocity template
        int maximumIssues = staticMode ? JiraUtil.getMaximumIssues(params.get(MAXIMUM_ISSUES)) : JiraUtil.DEFAULT_NUMBER_OF_ISSUES;
        if (issuesType == JiraIssuesType.COUNT)
        {
            maximumIssues = 0;
        }
        contextMap.put(MAX_ISSUES_TO_DISPLAY, maximumIssues);

        String clickableUrl = JiraIssueUtil.getClickableUrl(requestData, requestType, applink, params.get(BASE_URL));
        contextMap.put(CLICKABLE_URL, clickableUrl);
        Map<String, JiraColumnInfo> jiraColumns = jiraIssuesColumnManager.getColumnsInfoFromJira(applink);
        if(issuesType == JiraIssuesType.TABLE)
        {
            requestData = jiraIssueSortingManager.getRequestDataForSorting(params, requestData, requestType, jiraColumns, conversionContext, applink);
        }

        String url = null;
        if (applink != null)
        {
            url = getXmlUrl(maximumIssues, requestData, requestType, applink);
        } else if (requestType == Type.URL)
        {
            url = requestData;
        }

        // support querying with 'no applink' ONLY IF we have base url
        if (url == null && applink == null)
        {
            throw new MacroExecutionException(getText("jiraissues.error.noapplinks"));
        }

        if (issuesType == JiraIssuesType.SINGLE)
        {
            contextMap.put(KEY, getKeyFromRequest(requestData, requestType));
        }

        params.put(TOKEN_TYPE_PARAM, issuesType == JiraIssuesType.COUNT || requestType == Type.KEY ? TokenType.INLINE.name() : TokenType.BLOCK.name());

        List<String> columnNames = JiraIssueSortableHelper.getColumnNames(JiraUtil.getParamValue(params, COLUMNS, JiraUtil.PARAM_POSITION_1), i18nColumnNames);
        List<JiraColumnInfo> columns = jiraIssuesColumnManager.getColumnInfo(params, jiraColumns, applink);
        contextMap.put(COLUMNS, columns);
        String cacheParameter = JiraUtil.getParamValue(params, CACHE, JiraUtil.PARAM_POSITION_2);
        // added parameters for pdf export
        if (RenderContext.PDF.equals(conversionContext.getOutputType()))
        {
            contextMap.put(PDF_EXPORT, Boolean.TRUE);
            JiraIssuePdfExportUtil.addedHelperDataForPdfExport(contextMap, columnNames != null ? columnNames.size() : 0);
        }
        //Only define the Title param if explicitly defined.
        if (params.containsKey(TITLE))
        {
            contextMap.put(TITLE, GeneralUtil.htmlEncode(params.get(TITLE)));
        }

        if (RenderContext.EMAIL.equals(conversionContext.getOutputType()))
        {
            contextMap.put(EMAIL_RENDER, Boolean.TRUE);
        }
        // maybe this should change to position 3 now that the former 3 param
        // got deleted, but that could break
        // backward compatibility of macros currently in use
        String anonymousStr = JiraUtil.getParamValue(params, ANONYMOUS, JiraUtil.PARAM_POSITION_4);
        if ("".equals(anonymousStr))
        {
            anonymousStr = "false";
        }

        // and maybe this should change to position 4 -- see comment for
        // anonymousStr above
        String forceTrustWarningsStr = JiraUtil.getParamValue(params, "forceTrustWarnings",
                JiraUtil.PARAM_POSITION_5);
        if ("".equals(forceTrustWarningsStr))
        {
            forceTrustWarningsStr = "false";
        }

        String width = params.get(WIDTH);
        if (width == null)
        {
            width = DEFAULT_DATA_WIDTH;
        }
        else if(!width.contains("%") && !width.contains("px"))
        {
            width += "px";
        }
        contextMap.put(WIDTH, width);

        String heightStr = JiraUtil.getParamValue(params, HEIGHT, JiraUtil.PARAM_POSITION_6);
        if (!StringUtils.isEmpty(heightStr) && StringUtils.isNumeric(heightStr))
        {
            contextMap.put(HEIGHT, heightStr);
        }

        String showSummaryParam = JiraUtil.getParamValue(params, SHOW_SUMMARY, JiraUtil.SUMMARY_PARAM_POSITION);
        if (StringUtils.isEmpty(showSummaryParam))
        {
            contextMap.put(SHOW_SUMMARY, true);
        } else
        {
            contextMap.put(SHOW_SUMMARY, Boolean.parseBoolean(showSummaryParam));
        }

        boolean forceAnonymous = Boolean.valueOf(anonymousStr)
                || (requestType == Type.URL && SeraphUtils.isUserNamePasswordProvided(requestData));

        // support rendering macros which were created without applink by legacy macro
        if (applink == null)
        {
            forceAnonymous = true;
        }

        boolean showTrustWarnings = Boolean.valueOf(forceTrustWarningsStr)
                || isTrustWarningsEnabled();
        contextMap.put("showTrustWarnings", showTrustWarnings);

        // The template needs to know whether it should escape HTML fields and
        // display a warning
        boolean isAdministrator = permissionManager.hasPermission(
                AuthenticatedUserThreadLocal.getUser(), Permission.ADMINISTER,
                PermissionManager.TARGET_APPLICATION);
        contextMap.put(IS_ADMINISTRATOR, isAdministrator);
        contextMap.put(IS_SOURCE_APP_LINK, applink != null);
        //add returnMax parameter to retrieve the limitation of jira issues returned
        contextMap.put("returnMax", "true");

        boolean userAuthenticated = AuthenticatedUserThreadLocal.get() != null;
        boolean useCache;
        if (JiraIssuesType.TABLE.equals(issuesType) && !JiraJqlHelper.isJqlKeyType(requestData))
        {
            useCache = StringUtils.isBlank(cacheParameter)
            || cacheParameter.equals("on")
            || Boolean.valueOf(cacheParameter);
        }
        else
        {
            useCache = userAuthenticated ? forceAnonymous : true; // always cache single issue and count if user is not authenticated
        }

        if (staticMode || isMobile)
        {
            switch (issuesType)
            {
                case SINGLE:
                    if (RenderContext.EMAIL.equals(conversionContext.getOutputDeviceType())
                            || RenderContext.EMAIL.equals(conversionContext.getOutputType()))
                    {
                        contextMap.put(IS_NO_PERMISSION_TO_VIEW, true);
                    }
                    else
                    {
                        populateContextMapForStaticSingleIssue(contextMap, url, applink, forceAnonymous, useCache, conversionContext);
                    }
                    break;

                case COUNT:
                    populateContextMapForStaticCountIssues(params, contextMap, columnNames, url, applink, forceAnonymous, useCache, conversionContext);
                    break;

                case TABLE:
                    contextMap.put("singleIssueTable", JiraJqlHelper.isJqlKeyType(requestData));
                    populateContextMapForStaticTable(params, contextMap, columnNames, url, applink, forceAnonymous, useCache, conversionContext, i18nColumnNames);
                    break;
            }
        }
        else
        {
            if (applink != null) {
                contextMap.put("applink", applink);
            }

            if (issuesType != JiraIssuesType.SINGLE)
            {
                populateContextMapForDynamicTable(params, contextMap, columns, useCache, url, applink, forceAnonymous);
            }
        }

        if (issuesType == JiraIssuesType.TABLE)
        {
            registerTableRefreshContext(params, contextMap, conversionContext);
        }
    }

    public void registerTableRefreshContext(Map<String, String> macroParams, Map<String, Object> contextMap, ConversionContext conversionContext) throws MacroExecutionException
    {
        int refreshId = getNextRefreshId();

        contextMap.put("refreshId", refreshId);
        MacroDefinition macroDefinition = new MacroDefinition("jira", new RichTextMacroBody(""), null, macroParams);
        try
        {
            Streamable out = macroMarshallingFactory.getStorageMarshaller().marshal(macroDefinition, conversionContext);
            StringWriter writer = new StringWriter();
            out.writeTo(writer);
            contextMap.put("wikiMarkup", writer.toString());
        }
        catch (XhtmlException e)
        {
            throw new MacroExecutionException("Unable to constract macro definition.", e);
        }
        catch (IOException e)
        {
            throw new MacroExecutionException("Unable to constract macro definition.", e);
        }
        // Fix issue/CONF-31836: Jira Issues macro displays java.lang.NullPointerException when included on Welcome Message
        // The reason is that the renderContext used in the Welcome Page is not an instance of PageContext
        // Therefore, conversionContext.getEntity() always returns a null value. to fix this, we need to check if this entity is null or not
        String contentId = conversionContext.getEntity() != null ? conversionContext.getEntity().getIdAsString() : "-1";
        contextMap.put("contentId", contentId);
    }

    private String getKeyFromRequest(String requestData, Type requestType)
    {
        String key = requestData;
        if(requestType == Type.URL)
        {
            key = JiraJqlHelper.getKeyFromURL(requestData);
        }
        return key;
    }

    private String getRenderedTemplateMobile(final Map<String, Object> contextMap, final JiraIssuesType issuesType)
            throws MacroExecutionException
    {
        switch (issuesType)
        {
            case SINGLE:
                return VelocityUtils.getRenderedTemplate(TEMPLATE_MOBILE_PATH + "/mobileSingleJiraIssue.vm", contextMap);
            case COUNT:
                return VelocityUtils.getRenderedTemplate(TEMPLATE_MOBILE_PATH + "/mobileShowCountJiraissues.vm", contextMap);
            default:
                return VelocityUtils.getRenderedTemplate(TEMPLATE_MOBILE_PATH + "/mobileJiraIssues.vm", contextMap);
        }
    }

    public String getRenderedTemplate(final Map<String, Object> contextMap, final boolean staticMode, final JiraIssuesType issuesType)
            throws MacroExecutionException
    {
        if(staticMode)
        {
            return renderStaticTemplate(contextMap, issuesType);
        }

        return renderDynamicTemplate(contextMap, issuesType);
    }

    private String renderStaticTemplate(final Map<String, Object> contextMap, final JiraIssuesType issuesType)
    {
        switch (issuesType)
        {
            case SINGLE:
                return VelocityUtils.getRenderedTemplate(TEMPLATE_PATH + "/staticsinglejiraissue.vm", contextMap);
            case COUNT:
                return VelocityUtils.getRenderedTemplate(TEMPLATE_PATH + "/staticShowCountJiraissues.vm", contextMap);
            default:
                return VelocityUtils.getRenderedTemplate(TEMPLATE_PATH + "/staticJiraIssues.vm", contextMap);
        }
    }

    private String renderDynamicTemplate(final Map<String, Object> contextMap, final JiraIssuesType issuesType)
    {
        switch (issuesType)
        {
            case SINGLE:
                return VelocityUtils.getRenderedTemplate(TEMPLATE_PATH + "/singlejiraissue.vm", contextMap);
            case COUNT:
                return VelocityUtils.getRenderedTemplate(TEMPLATE_PATH + "/showCountJiraissues.vm", contextMap);
            default:
                return VelocityUtils.getRenderedTemplate(TEMPLATE_PATH + "/dynamicJiraIssues.vm", contextMap);
        }
    }

    private void populateContextMapForStaticSingleIssue(
            Map<String, Object> contextMap, String url,
            ReadOnlyApplicationLink applicationLink, boolean forceAnonymous, boolean useCache, ConversionContext conversionContext)
            throws MacroExecutionException
    {
        JiraIssuesManager.Channel channel;
        try
        {
            channel = jiraIssuesManager.retrieveXMLAsChannel(url, DEFAULT_COLUMNS_FOR_SINGLE_ISSUE, applicationLink,
                    forceAnonymous, useCache);
            setupContextMapForStaticSingleIssue(contextMap, channel.getChannelElement().getChild(ITEM), applicationLink);
        }
        catch (CredentialsRequiredException credentialsRequiredException)
        {
            try
            {
                populateContextMapForStaticSingleIssueAnonymous(contextMap, url, applicationLink, forceAnonymous, useCache, conversionContext);
            }
            catch (MacroExecutionException e)
            {
                contextMap.put("oAuthUrl", credentialsRequiredException.getAuthorisationURI().toString());
            }
        }
        catch (Exception e)
        {
            jiraExceptionHelper.throwMacroExecutionException(e, conversionContext);
        }
    }

    private void populateContextMapForStaticSingleIssueAnonymous(
            Map<String, Object> contextMap, String url,
            ReadOnlyApplicationLink applink, boolean forceAnonymous, boolean useCache, ConversionContext conversionContext)
            throws MacroExecutionException
    {
        JiraIssuesManager.Channel channel;
        try
        {
            channel = jiraIssuesManager.retrieveXMLAsChannelByAnonymous(
                    url, DEFAULT_COLUMNS_FOR_SINGLE_ISSUE, applink, forceAnonymous, useCache);
            setupContextMapForStaticSingleIssue(contextMap, channel.getChannelElement().getChild(ITEM), applink);
        }
        catch (Exception e)
        {
            jiraExceptionHelper.throwMacroExecutionException(e, conversionContext);
        }
    }

    private void setupContextMapForStaticSingleIssue(Map<String, Object> contextMap, Element issue, ReadOnlyApplicationLink applicationLink) throws MalformedRequestException
    {
        //In Jira 6.3, when anonymous make a request to jira without permission, the result will return a empty channel
        if (issue == null) {
            if (AuthenticatedUserThreadLocal.isAnonymousUser()) {
                throw new MalformedRequestException();
            } else {
                throw new JiraIssueDataException();
            }
        }

        Element resolution = issue.getChild("resolution");
        Element status = issue.getChild("status");

        JiraUtil.checkAndCorrectIconURL(issue, applicationLink);

        contextMap.put("resolved", resolution != null && !"-1".equals(resolution.getAttributeValue("id")));
        contextMap.put(ICON_URL, issue.getChild("type").getAttributeValue(ICON_URL));
        String key = issue.getChild(KEY).getValue();
        contextMap.put(KEY, key);
        contextMap.put("summary", issue.getChild("summary").getValue());
        contextMap.put("status", status.getValue());
        contextMap.put("statusIcon", status.getAttributeValue(ICON_URL));

        Element isPlaceholder = issue.getChild("isPlaceholder");
        contextMap.put("isPlaceholder", isPlaceholder != null);

        Element clientIdElement = issue.getChild(CLIENT_ID);
        if (clientIdElement != null)
        {
            contextMap.put(CLIENT_ID, clientIdElement.getValue());
        }

        Element statusCategory = issue.getChild("statusCategory");
        if (null != statusCategory)
        {
            String colorName = statusCategory.getAttribute("colorName").getValue();
            String keyName = statusCategory.getAttribute("key").getValue();
            if (StringUtils.isNotBlank(colorName) && StringUtils.isNotBlank(keyName))
            {
                contextMap.put("statusColor", colorName);
                contextMap.put("keyName", keyName);
            }
        }
    }

    private String getXmlUrl(int maximumIssues, String requestData, Type requestType,
                             ReadOnlyApplicationLink applicationLink) throws MacroExecutionException {
        StringBuilder stringBuilder = new StringBuilder(JiraUtil.normalizeUrl(applicationLink.getRpcUrl()));
        stringBuilder.append(JiraJqlHelper.XML_SEARCH_REQUEST_URI).append("?tempMax=")
                .append(maximumIssues).append("&returnMax=true&jqlQuery=");

        switch (requestType)
        {
        case URL:
            if (JiraJqlHelper.isUrlFilterType(requestData))
            {
                String jql = JiraJqlHelper.getJQLFromFilter(applicationLink, requestData, jiraIssuesManager, getI18NBean());
                stringBuilder.append(JiraUtil.utf8Encode(jql));
                return stringBuilder.toString();
            }
            else if (requestData.contains("searchrequest-xml"))
            {
                return requestData.trim();
            }
            else
            {
                // this is not an expected XML link, try to extract jqlQuery or
                // jql parameter and return a proper xml link
                String jql = JiraJqlHelper.getJQLFromJQLURL(requestData);
                if (jql != null)
                {
                    stringBuilder.append(JiraUtil.utf8Encode(jql));
                    return stringBuilder.toString();
                }
                else if(JiraJqlHelper.isUrlKeyType(requestData))
                {
                    String key = JiraJqlHelper.getKeyFromURL(requestData);
                    return buildKeyJiraUrl(key, applicationLink);
                }
            }
        case JQL:
            stringBuilder.append(JiraUtil.utf8Encode(requestData));
            return stringBuilder.toString();
        case KEY:
            return buildKeyJiraUrl(requestData, applicationLink);

        }
        throw new MacroExecutionException("Invalid url");
    }

    private String buildKeyJiraUrl(String key, ReadOnlyApplicationLink applicationLink)
    {
        String encodedQuery = JiraUtil.utf8Encode("key in (" + key + ")");
        return JiraUtil.normalizeUrl(applicationLink.getRpcUrl())
                + "/sr/jira.issueviews:searchrequest-xml/temp/SearchRequest.xml?jqlQuery="
                + encodedQuery + "&returnMax=true";
    }

    private String executeRest(String restUrl, ReadOnlyApplicationLink appLink)
    {
        String json = "";
        try {
            ApplicationLinkRequest fieldRequest = JiraConnectorUtils.getApplicationLinkRequest(appLink,
                    Request.MethodType.GET, restUrl);
            fieldRequest.addHeader("Content-Type", MediaType.APPLICATION_JSON);
            json = fieldRequest.execute();
        } catch (CredentialsRequiredException e) {
            LOGGER.error("CredentialsRequiredException", e);
        } catch (ResponseException e) {
            LOGGER.error("ResponseExceptionException", e);
        }
        return json;
    }

    private void populateTableEpicData(Map<String, Object> contextMap, ReadOnlyApplicationLink appLink, JiraIssuesManager.Channel channel,
                                       List<String> columnNames, ImmutableMap<String, ImmutableSet<String>> i18nColumnNames) {
        boolean needEpicName = matchColumnNameFromList(COLUMN_EPIC_LINK, columnNames, i18nColumnNames);
        boolean needEpicColour = matchColumnNameFromList(COLUMN_EPIC_LINK, columnNames, i18nColumnNames) ||
                matchColumnNameFromList(COLUMN_EPIC_COLOUR, columnNames, i18nColumnNames);
        boolean needEpicStatus = matchColumnNameFromList(COLUMN_EPIC_STATUS, columnNames, i18nColumnNames);
        if(!needEpicName && !needEpicColour && !needEpicStatus){
            return;
        }

        String epicLinkCustomFieldId = "";
        String epicNameCustomFieldId = "";
        String epicStatusCustomFieldId = "";
        String epicColourCustomFieldId = "";

        // Custom field number for Epic Links are not standard across instances
        Map<String, JiraColumnInfo> columns = jiraIssuesColumnManager.getColumnsInfoFromJira(appLink);
        for(String column : columns.keySet()){
            JiraColumnInfo columnInfo = columns.get(column);
            if(matchColumnNameFromString(COLUMN_EPIC_LINK, columnInfo.getTitle(), i18nColumnNames)) {
                epicLinkCustomFieldId = column;
            } else if(matchColumnNameFromString(COLUMN_EPIC_NAME, columnInfo.getTitle(), i18nColumnNames)) {
                epicNameCustomFieldId = column;
            } else if (matchColumnNameFromString(COLUMN_EPIC_COLOUR, columnInfo.getTitle(), i18nColumnNames)) {
                epicColourCustomFieldId = column;
            } else if (matchColumnNameFromString(COLUMN_EPIC_STATUS, columnInfo.getTitle(), i18nColumnNames)){
                epicStatusCustomFieldId = column;
            }

            if(!epicLinkCustomFieldId.isEmpty() && (!needEpicName || !epicNameCustomFieldId.isEmpty()) && (!needEpicStatus || !epicStatusCustomFieldId.isEmpty())
                    && (!needEpicColour || !epicColourCustomFieldId.isEmpty())){
                break;
            }
        }

        // Instance may not have configured their task types the same way as is done
        // in Jira Software. We can't handle this situation.
        if (epicLinkCustomFieldId.isEmpty() || (needEpicName && epicNameCustomFieldId.isEmpty()) || (needEpicStatus && epicStatusCustomFieldId.isEmpty())
                || (needEpicColour && epicColourCustomFieldId.isEmpty())) {
            contextMap.put("epics", new HashMap<String, Epic>());
            return;
        }

        // Get the epic information for each of the issues in the table
        Map<String, Epic> epics = getEpicInformation(channel, appLink, epicLinkCustomFieldId, epicNameCustomFieldId,
                epicColourCustomFieldId, epicStatusCustomFieldId, i18nColumnNames);

        contextMap.put("epics", epics);
    }

    private Map<String, Epic> getEpicInformation(JiraIssuesManager.Channel channel, ReadOnlyApplicationLink appLink, String epicLinkCustomFieldId,
                                 String epicNameCustomFieldId, String epicColourCustomFieldId, String epicStatusCustomFieldId, ImmutableMap<String,
                                 ImmutableSet<String>> i18nColumnNames) {
        String json;
        String epicName = "";
        String epicColour = "";
        String epicStatus = "";
        Map<String, Epic> epics = new HashMap<>();
        Map<String, Epic> foundEpicKeys = new HashMap<>();

        for (Element issue : ((List<Element>)channel.getChannelElement().getChildren("item"))) {
            // Get the Epic Link (i.e. Issue Key of the Epic)
            String epicKey = "";

            if (foundEpicKeys.keySet().contains(issue.getChild("key").getValue())) {
                continue;
            }

            if(issue.getChild("type") == null || (issue.getChild("type") != null && !issue.getChild("type").getValue().equals("Epic"))){
                for (Element element: (List<Element>) issue.getChild("customfields").getChildren()) {
                    // This has to check that element.getValue contains "epic link", rahter than the other way around
                    // (which is how it normally works) as the element.getValue is in the form " Epic     Link       :     PROJ-123".
                    // This is not safe to do always though, as the element value might be issue type, for example, and the i18n
                    // might be type. There are no columns named Epic or Link though, so this is not a problem here.
                    if(matchColumnNameFromString(COLUMN_EPIC_LINK, element.getValue(), i18nColumnNames, true)){
                        epicKey = extractFieldValue(element.getValue());
                        break;
                    }
                }

                // From the issue key of epic, get the name of the epic
                if (!epicKey.isEmpty() && !foundEpicKeys.keySet().contains(epicKey)) {
                    json = executeRest("/rest/api/2/issue/" + epicKey, appLink);
                    epicName = parseCustomField(json, epicNameCustomFieldId);
                    epicColour = parseCustomField(json, epicColourCustomFieldId);
                    epicStatus = parseStatusField(json, epicStatusCustomFieldId);
                }
            }

            if (epicKey.isEmpty()){
                epicKey = issue.getChild("key").getValue();

                for(Element element : ((List<Element>) issue.getChild("customfields").getChildren())){
                    if(matchColumnNameFromString(COLUMN_EPIC_NAME, element.getValue(), i18nColumnNames, true)){
                        epicName = extractFieldValue(element.getValue());
                    } else if(matchColumnNameFromString(COLUMN_EPIC_COLOUR, element.getValue(), i18nColumnNames, true)){
                        epicColour = extractFieldValue(element.getValue());
                    } else if(matchColumnNameFromString(COLUMN_EPIC_STATUS, element.getValue(), i18nColumnNames, true)){
                        epicStatus = extractFieldValue(element.getValue());
                    }

                    if (!epicName.isEmpty() && !epicColour.isEmpty() && !epicStatus.isEmpty()) {
                        break;
                    }
                }
            }
            if(!epicName.isEmpty() || !epicColour.isEmpty() || !epicStatus.isEmpty()) {
                Epic epic = new Epic(epicKey, epicName, epicColour, epicStatus);
                foundEpicKeys.put(epicKey, epic);
                epics.put(issue.getChild("key").getValue(), foundEpicKeys.get(epicKey));
            }
        }
        return epics;
    }

    /**
     * Takes the string returned from the JSON parsing, with the name of the field and extra whitespace, and returns
     * just the value string.
     * Ex. " \n   Epic      Link   \n      \n      PROJ-1     " returns "PROJ-1"
     * @param field
     * @return
     */
    private String extractFieldValue(String field) {
        return field.trim().replaceAll(".*\n.*\n *", "");
    }

    private String parseStatusField(String json, String customFieldId){
        JsonElement jsonEpicField = verifyJSON(json).getAsJsonObject().get(customFieldId);
        if(jsonEpicField != null && jsonEpicField.isJsonPrimitive()){
            return jsonEpicField.getAsJsonPrimitive().getAsString();
        } else if (jsonEpicField == null || !jsonEpicField.isJsonObject()) {
            return "";
        }
        JsonElement jsonEpicStatus = jsonEpicField.getAsJsonObject().get("value");
        if(jsonEpicStatus == null || !jsonEpicStatus.isJsonPrimitive()) {
            return "";
        }
        return jsonEpicStatus.getAsJsonPrimitive().getAsString().trim();

    }

    private String parseCustomField(String json, String customFieldId){
        JsonElement jsonEpicField = verifyJSON(json).getAsJsonObject().get(customFieldId);
        if(jsonEpicField == null || !jsonEpicField.isJsonPrimitive()){
            return "";
        }
        return jsonEpicField.getAsJsonPrimitive().getAsString().trim();
    }

    private JsonElement verifyJSON(String json) {
        if(json == null){
            return new JsonNull();
        }
        JsonElement jsonElement = parser.parse(json);

        if(jsonElement == null || !jsonElement.isJsonObject()){
            return new JsonNull();
        }
        JsonElement fields = jsonElement.getAsJsonObject().get("fields");

        if(fields == null || !fields.isJsonObject()) {
            return new JsonNull();
        }
        return fields;
    }

    /**
     * Create context map for rendering issues in HTML.
     *
     * @param contextMap
     *            Map containing contexts for rendering issues in HTML
     * @param url
     *            JIRA issues XML url
     * @param appLink
     *            not null if using trusted connection
     * @param useCache true if cache is used
     * @throws MacroExecutionException
     *             thrown if Confluence failed to retrieve JIRA Issues
     */
    private void populateContextMapForStaticTable(Map<String, String> macroParams, Map<String, Object> contextMap, List<String> columnNames, String url,
              ReadOnlyApplicationLink appLink, boolean forceAnonymous, boolean useCache, ConversionContext conversionContext, ImmutableMap<String,
              ImmutableSet<String>> i18nColumnNames) throws MacroExecutionException
    {
        boolean clearCache = getBooleanProperty(conversionContext.getProperty(DefaultJiraCacheManager.PARAM_CLEAR_CACHE));
        try
        {
            boolean isViewingOrPreviewing =
                    RenderContext.DISPLAY.equals(conversionContext.getOutputType()) ||
                    RenderContext.PREVIEW.equals(conversionContext.getOutputType());

            contextMap.put(ENABLE_REFRESH, isViewingOrPreviewing);

            if (StringUtils.isNotBlank((String) conversionContext.getProperty("orderColumnName")) && StringUtils.isNotBlank((String) conversionContext.getProperty("order")))
            {
                contextMap.put("orderColumnName", conversionContext.getProperty("orderColumnName"));
                contextMap.put("order", conversionContext.getProperty("order"));
            }
            if (clearCache)
            {
                jiraCacheManager.clearJiraIssuesCache(url, columnNames, appLink, forceAnonymous, false);
            }

            // only do lazy loading for table in this 2 output types & in desktop env
            boolean placeholder = isViewingOrPreviewing && isAsyncSupport(conversionContext);
            contextMap.put(PARAM_PLACEHOLDER, placeholder);
            if (!placeholder)
            {
                JiraIssuesManager.Channel channel = jiraIssuesManager.retrieveXMLAsChannel(url, columnNames, appLink,
                        forceAnonymous, useCache);
                setupContextMapForStaticTable(contextMap, channel, appLink);
                if(matchColumnNameFromList(COLUMN_EPIC_LINK, columnNames, i18nColumnNames) ||
                        matchColumnNameFromList(COLUMN_EPIC_NAME, columnNames, i18nColumnNames) ||
                        matchColumnNameFromList(COLUMN_EPIC_COLOUR, columnNames, i18nColumnNames) ||
                        matchColumnNameFromList(COLUMN_EPIC_STATUS, columnNames, i18nColumnNames)){
                    populateTableEpicData(contextMap, appLink, channel, columnNames, i18nColumnNames);
                }
            }
            else
            {
                ClientId clientId = ClientId.fromElement(JiraIssuesType.TABLE, appLink.getId().get(), conversionContext.getEntity().getIdAsString(),
                        JiraIssueUtil.getUserKey(AuthenticatedUserThreadLocal.get()), String.valueOf(macroParams.get("jqlQuery")));
                contextMap.put("clientId", clientId);
                asyncJiraIssueBatchService.processRequestWithJql(clientId, macroParams, conversionContext, appLink);

                // Placeholder mode for table
                contextMap.put("trustedConnection", false);
            }
        }
        catch (CredentialsRequiredException e)
        {
            if (clearCache)
            {
                jiraCacheManager.clearJiraIssuesCache(url, columnNames, appLink, forceAnonymous, true);
            }
            // this exception only happens if the real jira data is fetched while users is not authenticated,
            // which means that asynchronous loading has been kicked-in
            populateContextMapForStaticTableByAnonymous(macroParams, contextMap, columnNames, url, appLink, forceAnonymous, useCache);
            contextMap.put("oAuthUrl", e.getAuthorisationURI().toString());
        }
        catch (MalformedRequestException e)
        {
            LOGGER.info("Can't get issues because issues key is not exist or user doesn't have permission to view", e);
            jiraExceptionHelper.throwMacroExecutionException(e, conversionContext);
        }
        catch (Exception e)
        {
            jiraExceptionHelper.throwMacroExecutionException(e, conversionContext);
        }
    }

    /**
     * This method is called inside the asynchronous call inside method
     * {@link #populateContextMapForStaticTable(Map, Map, List, String, ReadOnlyApplicationLink, boolean, boolean, ConversionContext, ImmutableMap)}
     */
    private void populateContextMapForStaticTableByAnonymous(Map<String, String> macroParams, Map<String, Object> contextMap, List<String> columnNames,
            String url, ReadOnlyApplicationLink appLink, boolean forceAnonymous, boolean useCache)
            throws MacroExecutionException
    {
        try
        {
            JiraIssuesManager.Channel channel = jiraIssuesManager.retrieveXMLAsChannelByAnonymous(url, columnNames,
                    appLink, forceAnonymous, useCache);
            setupContextMapForStaticTable(contextMap, channel, appLink);
        }
        catch (Exception e)
        {
            // issue/CONFDEV-21600: 'refresh' link should be shown for all cases
            // However, it will be visible if and only if totalIssues has a value
            contextMap.put(TOTAL_ISSUES, 0);
            LOGGER.info("Can't get jira issues by anonymous user from : "+ appLink);
            LOGGER.debug("More info", e);
        }
    }

    public void setupContextMapForStaticTable(Map<String, Object> contextMap, JiraIssuesManager.Channel channel, ReadOnlyApplicationLink appLink)
    {
        Element element = channel.getChannelElement();
        contextMap.put("trustedConnection", channel.isTrustedConnection());
        contextMap.put("trustedConnectionStatus", channel.getTrustedConnectionStatus());
        contextMap.put("channel", element);
        contextMap.put("entries", element.getChildren("item"));
        JiraUtil.checkAndCorrectDisplayUrl(element.getChildren(ITEM), appLink);
        try
        {
            if(element.getChild("issue") != null && element.getChild("issue").getAttribute("total") != null)
            {
                contextMap.put(TOTAL_ISSUES, element.getChild("issue").getAttribute("total").getIntValue());
            }
        }
        catch (DataConversionException e)
        {
            contextMap.put(TOTAL_ISSUES, element.getChildren("item").size());
        }
        contextMap.put("xmlXformer", xmlXformer);
        contextMap.put("jiraIssuesColumnManager", jiraIssuesColumnManager);
        contextMap.put("userLocale", getUserLocale(element.getChildText("language")));
        if (null != appLink)
        {
            contextMap.put(JIRA_SERVER_URL, JiraUtil.normalizeUrl(appLink.getDisplayUrl()));
        }
        else
        {
            try
            {
                URL sourceUrl = new URL(channel.getSourceUrl());
                String jiraServerUrl = sourceUrl.getProtocol() + "://" + sourceUrl.getAuthority();
                contextMap.put(JIRA_SERVER_URL, jiraServerUrl);
            }
            catch (MalformedURLException e)
            {
                LOGGER.debug("MalformedURLException thrown when retrieving sourceURL from the channel", e);
                LOGGER.info("Set jiraServerUrl to empty string");
                contextMap.put(JIRA_SERVER_URL, "");
            }
        }

        Locale locale = localeManager.getLocale(AuthenticatedUserThreadLocal.get());
        contextMap.put("dateFormat", new SimpleDateFormat(formatSettingsManager.getDateFormat(), locale));
    }

    private void populateContextMapForStaticCountIssues(Map<String, String> macroParams, Map<String, Object> contextMap, List<String> columnNames,
                                                        String url, ReadOnlyApplicationLink appLink, boolean forceAnonymous, boolean useCache, ConversionContext conversionContext) throws MacroExecutionException
    {
        if (isAsyncSupport(conversionContext))
        {
            ClientId clientId = ClientId.fromElement(JiraIssuesType.COUNT, appLink.getId().get(), conversionContext.getEntity().getIdAsString(),
                    JiraIssueUtil.getUserKey(AuthenticatedUserThreadLocal.get()), String.valueOf(macroParams.get("jqlQuery")));
            contextMap.put("clientId", clientId);
            asyncJiraIssueBatchService.processRequestWithJql(clientId, macroParams, conversionContext, appLink);
        }
        else
        {
            try
            {
                JiraIssuesManager.Channel channel = jiraIssuesManager.retrieveXMLAsChannel(url, columnNames, appLink, forceAnonymous, useCache);
                Element element = channel.getChannelElement();
                Element totalItemsElement = element.getChild("issue");
                String count = totalItemsElement != null ? totalItemsElement.getAttributeValue("total") : "" + element.getChildren("item").size();

                contextMap.put(COUNT, count);
            }
            catch (CredentialsRequiredException e)
            {
                contextMap.put(COUNT, getCountIssuesWithAnonymous(url, columnNames, appLink, forceAnonymous, useCache));
                contextMap.put("oAuthUrl", e.getAuthorisationURI().toString());
            }
            catch (MalformedRequestException e)
            {
                contextMap.put(COUNT, DEFAULT_JIRA_ISSUES_COUNT);
            }
            catch (Exception e)
            {
                jiraExceptionHelper.throwMacroExecutionException(e, conversionContext);
            }
        }
    }

    private String getCountIssuesWithAnonymous(String url, List<String> columnNames, ReadOnlyApplicationLink appLink, boolean forceAnonymous, boolean useCache) throws MacroExecutionException {
        try {
            JiraIssuesManager.Channel channel = jiraIssuesManager.retrieveXMLAsChannelByAnonymous(url, columnNames, appLink, forceAnonymous, useCache);
            Element element = channel.getChannelElement();
            Element totalItemsElement = element.getChild("issue");
            return totalItemsElement != null ? totalItemsElement.getAttributeValue("total") : "" + element.getChildren("item").size();
        }
        catch (Exception e)
        {
            LOGGER.info("Can not retrieve total issues by anonymous");
            return DEFAULT_JIRA_ISSUES_COUNT;
        }
    }

   /** Create context map for rendering issues with Flexi Grid.
    *
    * @param params JIRA Issues macro parameters
    * @param contextMap Map containing contexts for rendering issues in HTML
    * @param columns  A list of JIRA column names
    * @param useCache If true the macro will use a cache of JIRA issues retrieved from the JIRA query
    * @param forceAnonymous set flag to true if using trusted connection
    * @param url JIRA issues XML url
    * @throws MacroExecutionException thrown if Confluence failed to retrieve JIRA Issues
    */
   private void populateContextMapForDynamicTable(
                   Map<String, String> params, Map<String, Object> contextMap, List<JiraColumnInfo> columns,
                   boolean useCache, String url, ReadOnlyApplicationLink applink, boolean forceAnonymous) throws MacroExecutionException
   {
       StringBuffer urlBuffer = new StringBuffer(url);
       contextMap.put("resultsPerPage", getResultsPerPageParam(urlBuffer));

       // unfortunately this is ignored right now, because the javascript has not been made to handle this (which may require hacking and this should be a rare use-case)
       String startOn = getStartOnParam(params.get("startOn"), urlBuffer);
       contextMap.put("startOn",  new Integer(startOn));
       contextMap.put("sortOrder",  getSortOrderParam(urlBuffer));
       contextMap.put("sortField",  getSortFieldParam(urlBuffer));
       contextMap.put("useCache", useCache);

       // name must end in "Html" to avoid auto-encoding
       contextMap.put("retrieverUrlHtml", buildRetrieverUrl(columns, urlBuffer.toString(), applink, forceAnonymous));
   }

   private String getStartOnParam(String startOn, StringBuffer urlParam)
   {
       String pagerStart = JiraIssueUtil.filterOutParam(urlParam, "pager/start=");
       if (StringUtils.isNotEmpty(startOn))
       {
           return startOn.trim();
       }

       if (StringUtils.isNotEmpty(pagerStart))
       {
           return pagerStart;
       }
       return "0";
   }

   private String getSortOrderParam(StringBuffer urlBuffer)
   {
       String sortOrder = JiraIssueUtil.filterOutParam(urlBuffer, "sorter/order=");
       if (StringUtils.isNotEmpty(sortOrder))
       {
           return sortOrder.toLowerCase();
       }
       return "desc";
   }


   private String getSortFieldParam(StringBuffer urlBuffer)
   {
       String sortField = JiraIssueUtil.filterOutParam(urlBuffer, "sorter/field=");
       if (StringUtils.isNotEmpty(sortField))
       {
           return sortField;
       }
       return null;
   }

    private boolean shouldRenderInHtml(String renderModeParamValue, ConversionContext conversionContext) {
        return RenderContext.PDF.equals(conversionContext.getOutputType())
            || RenderContext.WORD.equals(conversionContext.getOutputType())
            || !DYNAMIC_RENDER_MODE.equals(renderModeParamValue)
            || RenderContext.EMAIL.equals(conversionContext.getOutputType())
            || RenderContext.FEED.equals(conversionContext.getOutputType())
            || RenderContext.HTML_EXPORT.equals(conversionContext.getOutputType());
    }

    protected boolean isAsyncSupport(ConversionContext conversionContext)
    {
        ContentEntityObject entity = conversionContext.getEntity();
        return getBooleanProperty(conversionContext.getProperty(PARAM_PLACEHOLDER, true))
                && !darkFeatureManager.isFeatureEnabledForCurrentUser(AsyncJiraIssueBatchService.DARK_FEATURE_DISABLE_ASYNC_LOADING_KEY)
                && RenderContextOutputType.DISPLAY.equals(conversionContext.getOutputType())
                && ConversionContextOutputDeviceType.DESKTOP.equals(conversionContext.getOutputDeviceType())
                && (entity.getTypeEnum() == ContentTypeEnum.BLOG || entity.getTypeEnum() == ContentTypeEnum.PAGE || entity.getTypeEnum() == ContentTypeEnum.COMMENT);
    }

    protected int getResultsPerPageParam(StringBuffer urlParam)
            throws MacroExecutionException
    {
        String tempMaxParam = JiraIssueUtil.filterOutParam(urlParam, "tempMax=");
        if (StringUtils.isNotEmpty(tempMaxParam))
        {
            int tempMax = Integer.parseInt(tempMaxParam);
            if (tempMax <= 0)
            {
                throw new MacroExecutionException("The tempMax parameter in the JIRA url must be greater than zero.");
            }
            return tempMax;
        }
        else
        {
            return 10;
        }
    }

    private String buildRetrieverUrl(Collection<JiraColumnInfo> columns,
            String url, ReadOnlyApplicationLink applicationLink, boolean forceAnonymous)
    {
        String baseUrl = settingsManager.getGlobalSettings().getBaseUrl();
        StringBuilder retrieverUrl = new StringBuilder(baseUrl);
        retrieverUrl.append("/plugins/servlet/issue-retriever?");
        retrieverUrl.append("url=").append(JiraUtil.utf8Encode(url));
        if (applicationLink != null)
        {
            retrieverUrl.append("&appId=").append(JiraUtil.utf8Encode(applicationLink.getId().toString()));
        }
        for (JiraColumnInfo columnInfo : columns)
        {
            retrieverUrl.append("&columns=").append(JiraUtil.utf8Encode(columnInfo.toString()));
        }
        retrieverUrl.append("&forceAnonymous=").append(forceAnonymous);
        retrieverUrl.append("&flexigrid=true");
        return retrieverUrl.toString();
    }

    public String execute(Map<String, String> parameters, String body, ConversionContext conversionContext) throws MacroExecutionException
    {
        Map<String, Object> contextMap = null;
        try
        {
            JiraRequestData jiraRequestData = JiraIssueUtil.parseRequestData(parameters, getI18NBean());
            String requestData = jiraRequestData.getRequestData();
            Type requestType = jiraRequestData.getRequestType();
            contextMap = MacroUtils.defaultVelocityContext();
            JiraIssuesType issuesType = JiraUtil.getJiraIssuesType(parameters, requestType, requestData);
            contextMap.put(ISSUE_TYPE, issuesType);
            ImmutableMap<String, ImmutableSet<String>> i18nColumnNames = jiraIssuesColumnManager.getI18nColumnNames();
            contextMap.put("i18nColumnNames", i18nColumnNames);
            List<String> columnNames = JiraIssueSortableHelper.getColumnNames(JiraUtil.getParamValue(parameters, COLUMNS, JiraUtil.PARAM_POSITION_1), i18nColumnNames);
            // it will be overided by below code. At here, we need default column first for exception case.
            contextMap.put(COLUMNS, columnNames);

            ReadOnlyApplicationLink applink = null;
            try
            {
                applink = applicationLinkResolver.resolve(requestType, requestData, parameters);
            }
            catch (TypeNotInstalledException tne)
            {
                jiraExceptionHelper.throwMacroExecutionException(tne, conversionContext);
            }

            boolean staticMode = !dynamicRenderModeEnabled(parameters, conversionContext);
            boolean isMobile = MOBILE.equals(conversionContext.getOutputDeviceType());
            createContextMapFromParams(parameters, contextMap, requestData, requestType, applink, staticMode, isMobile, issuesType, conversionContext, i18nColumnNames);

            if (isMobile)
            {
                return getRenderedTemplateMobile(contextMap, issuesType);
            }
            else
            {
                return getRenderedTemplate(contextMap, staticMode, issuesType);
            }
        }
        catch (Exception e)
        {
            throw new JiraIssueMacroException(e, contextMap);
        }
    }

    protected boolean dynamicRenderModeEnabled(Map<String, String> parameters, ConversionContext conversionContext)
    {
        // if "dynamic mode", then we'll need flexigrid
        return !shouldRenderInHtml(parameters.get(RENDER_MODE_PARAM), conversionContext);
    }

    private Locale getUserLocale(String language)
    {
        if (StringUtils.isNotEmpty(language))
        {
            if (language.contains("-"))
            {
                return new Locale(language.substring(0, 2), language.substring(language.indexOf('-') + 1));
            }
            else
            {
                return new Locale(language);// Just the language code only
            }
        }
        else
        {
            return Locale.getDefault();
        }
    }

    public BodyType getBodyType()
    {
        return BodyType.NONE;
    }

    public OutputType getOutputType()
    {
        return OutputType.BLOCK;
    }

    public String getResourcePath()
    {
        return resourcePath;
    }

    public void setResourcePath(String resourcePath) {
        this.resourcePath = resourcePath;
    }

    public JiraIssuesXmlTransformer getXmlXformer()
    {
        return xmlXformer;
    }

    private int getNextRefreshId()
    {
        return RANDOM.nextInt();
    }

    private boolean getBooleanProperty(Object value)
    {
        if (value instanceof Boolean)
        {
            return (Boolean) value;
        }
        else if (value instanceof String)
        {
            return BooleanUtils.toBoolean((String) value);
        }
        return false;
    }

    private void setRenderMode(Map<String, Object> contextMap, String outputType)
    {
        if (RenderContext.PDF.equals(outputType))
        {
            contextMap.put(PDF_EXPORT, Boolean.TRUE);
        }
        if (RenderContext.EMAIL.equals(outputType))
        {
            contextMap.put(EMAIL_RENDER, Boolean.TRUE);
        }
    }

    // render a single JIRA issue from a JDOM Element
    public String renderSingleJiraIssue(Map<String, String> parameters, ConversionContext conversionContext, Element issue, String displayUrl, String rpcUrl) throws Exception {
        Map<String, Object> contextMap = MacroUtils.defaultVelocityContext();
        String outputType = conversionContext.getOutputType();
        // added parameters for pdf export
        setRenderMode(contextMap, outputType);

        String showSummaryParam = JiraUtil.getParamValue(parameters, SHOW_SUMMARY, JiraUtil.SUMMARY_PARAM_POSITION);
        if (StringUtils.isEmpty(showSummaryParam))
        {
            contextMap.put(SHOW_SUMMARY, true);
        }
        else
        {
            contextMap.put(SHOW_SUMMARY, Boolean.parseBoolean(showSummaryParam));
        }

        JiraUtil.correctIconURL(issue, displayUrl, rpcUrl);

        setupContextMapForStaticSingleIssue(contextMap, issue, null);
        contextMap.put(CLICKABLE_URL, displayUrl + JIRA_BROWSE_URL + issue.getChild(KEY).getValue());


        boolean isMobile = MOBILE.equals(conversionContext.getOutputDeviceType());

        if (isMobile)
        {
            return getRenderedTemplateMobile(contextMap, JiraIssuesType.SINGLE);
        }
        return getRenderedTemplate(contextMap, true, JiraIssuesType.SINGLE);
    }
}
