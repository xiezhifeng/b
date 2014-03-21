package com.atlassian.confluence.extra.jira;


import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.applinks.api.CredentialsRequiredException;
import com.atlassian.applinks.api.TypeNotInstalledException;
import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.content.render.xhtml.DefaultConversionContext;
import com.atlassian.confluence.content.render.xhtml.Streamable;
import com.atlassian.confluence.content.render.xhtml.XhtmlException;
import com.atlassian.confluence.content.render.xhtml.definition.RichTextMacroBody;
import com.atlassian.confluence.content.render.xhtml.macro.MacroMarshallingFactory;
import com.atlassian.confluence.core.FormatSettingsManager;
import com.atlassian.confluence.extra.jira.exception.MalformedRequestException;
import com.atlassian.confluence.extra.jira.helper.ImagePlaceHolderHelper;
import com.atlassian.confluence.extra.jira.helper.JiraExceptionHelper;
import com.atlassian.confluence.extra.jira.helper.JiraIssueSortableHelper;
import com.atlassian.confluence.extra.jira.helper.JiraJqlHelper;
import com.atlassian.confluence.extra.jira.model.JiraColumnInfo;
import com.atlassian.confluence.extra.jira.util.JiraIssuePdfExportUtil;
import com.atlassian.confluence.extra.jira.util.JiraUtil;
import com.atlassian.confluence.languages.LocaleManager;
import com.atlassian.confluence.macro.EditorImagePlaceholder;
import com.atlassian.confluence.macro.ImagePlaceholder;
import com.atlassian.confluence.macro.Macro;
import com.atlassian.confluence.macro.MacroExecutionException;
import com.atlassian.confluence.macro.ResourceAware;
import com.atlassian.confluence.renderer.radeox.macros.MacroUtils;
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
import com.atlassian.renderer.TokenType;
import com.atlassian.renderer.v2.RenderMode;
import com.atlassian.renderer.v2.macro.BaseMacro;
import com.atlassian.renderer.v2.macro.MacroException;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.util.URIUtil;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.RandomUtils;
import org.apache.log4j.Logger;
import org.jdom.DataConversionException;
import org.jdom.Element;

import java.io.IOException;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
/**
 * A macro to import/fetch JIRA issues...
 */
public class JiraIssuesMacro extends BaseMacro implements Macro, EditorImagePlaceholder, ResourceAware
{
    private static final Logger LOGGER = Logger.getLogger(JiraIssuesMacro.class);

    public static enum Type {KEY, JQL, URL}
    public static enum JiraIssuesType {SINGLE, COUNT, TABLE}

    private static final String TOKEN_TYPE_PARAM = ": = | TOKEN_TYPE | = :";

    private static final String RENDER_MODE_PARAM = "renderMode";
    private static final String DYNAMIC_RENDER_MODE = "dynamic";
    private static final String DEFAULT_DATA_WIDTH = "100%";

    private static final List<String> DEFAULT_COLUMNS_FOR_SINGLE_ISSUE = Arrays.asList(
            "summary", "type", "resolution", "status");

    private static final String POSITIVE_INTEGER_REGEX = "[0-9]+";

    // All context map's keys and parameters should be defined here to avoid unexpected typos and make the code clearer and easier for maintenance
    public static final String JIRA = "jira";
    private static final String SERVER = "server";
    public static final String SERVER_ID = "serverId";
    private static final String JIRA_URL_KEY_PARAM = "url";
    private static final String JQL_QUERY = "jqlQuery";
    public static final String KEY = "key";
    private static final String CACHE = "cache";
    public static final String ITEM ="item";

    private static final String ENABLE_REFRESH = "enableRefresh";
    private static final String TOTAL_ISSUES = "totalIssues";
    private static final String COLUMNS = "columns";
    private static final String TITLE = "title";
    private static final String ANONYMOUS = "anonymous";
    private static final String WIDTH = "width";
    private static final String HEIGHT = "height";
    public static final String SHOW_SUMMARY = "showSummary";

    private static final String IS_ADMINISTRATOR = "isAdministrator";
    private static final String IS_SOURCE_APP_LINK = "isSourceApplink";
    private static final String MAX_ISSUES_TO_DISPLAY = "maxIssuesToDisplay";
    private static final String BASE_URL = "baseurl";
    private static final String MAXIMUM_ISSUES = "maximumIssues";
    private static final String CLICKABLE_URL = "clickableUrl";
    private static final String IS_NO_PERMISSION_TO_VIEW = "isNoPermissionToView";
    private static final String ISSUE_TYPE = "issueType";
    private static final String COUNT = "count";
    private static final String ICON_URL = "iconUrl";
    public static final String JIRA_SERVER_URL = "jiraServerUrl";
    // End of context map keys

    private static final List<String> MACRO_PARAMS = Arrays.asList(
            COUNT, COLUMNS, TITLE, RENDER_MODE_PARAM, CACHE, WIDTH,
            HEIGHT, SERVER, SERVER_ID, ANONYMOUS, BASE_URL, SHOW_SUMMARY, com.atlassian.renderer.v2.macro.Macro.RAW_PARAMS_KEY, MAXIMUM_ISSUES, TOKEN_TYPE_PARAM);

    private static final String TEMPLATE_PATH = "templates/extra/jira";
    private static final String TEMPLATE_MOBILE_PATH = "templates/mobile/extra/jira";
    private static final String DEFAULT_JIRA_ISSUES_COUNT = "0";

    private static final String EMAIL_RENDER = "email";
    private static final String PDF_EXPORT = "pdfExport";

    private final JiraIssuesXmlTransformer xmlXformer = new JiraIssuesXmlTransformer();

    private I18NBeanFactory i18NBeanFactory;

    private JiraIssuesManager jiraIssuesManager;

    private SettingsManager settingsManager;

    private JiraIssuesColumnManager jiraIssuesColumnManager;

    private TrustedApplicationConfig trustedApplicationConfig;

    private String resourcePath;

    private PermissionManager permissionManager;

    private ApplicationLinkResolver applicationLinkResolver;

    private JiraIssuesDateFormatter jiraIssuesDateFormatter;

    private LocaleManager localeManager;

    private MacroMarshallingFactory macroMarshallingFactory;

    private JiraCacheManager jiraCacheManager;

    private ImagePlaceHolderHelper imagePlaceHolderHelper;

    private FormatSettingsManager formatSettingsManager;

    private JiraIssueSortingManager jiraIssueSortingManager;

    private JiraExceptionHelper jiraExceptionHelper;

    public void setJiraExceptionHelper(JiraExceptionHelper jiraExceptionHelper) {
        this.jiraExceptionHelper = jiraExceptionHelper;
    }

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

    String getText(String i18n, List substitutions)
    {
        return getI18NBean().getText(i18n, substitutions);
    }

    public void setLocaleManager(LocaleManager localeManager)
    {
        this.localeManager = localeManager;
    }

    @Override
    public TokenType getTokenType(Map parameters, String body,
            RenderContext context) {
        String tokenTypeString = (String) parameters.get(TOKEN_TYPE_PARAM);
        if (org.apache.commons.lang.StringUtils.isBlank(tokenTypeString)) {
            return TokenType.INLINE_BLOCK;
        }
        for (TokenType value : TokenType.values()) {
            if (value.toString().equals(tokenTypeString)) {
                return TokenType.valueOf(tokenTypeString);
            }
        }
        return TokenType.INLINE_BLOCK;
    }

    public ImagePlaceholder getImagePlaceholder(Map<String, String> parameters, ConversionContext conversionContext)
    {
        try
        {
            JiraRequestData jiraRequestData = parseRequestData(parameters);
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

    public RenderMode getBodyRenderMode() {
        return RenderMode.NO_RENDER;
    }

    public void setI18NBeanFactory(I18NBeanFactory i18NBeanFactory) {
        this.i18NBeanFactory = i18NBeanFactory;
    }

    public void setJiraIssuesManager(JiraIssuesManager jiraIssuesManager) {
        this.jiraIssuesManager = jiraIssuesManager;
    }

    public void setJiraIssuesColumnManager(
            JiraIssuesColumnManager jiraIssuesColumnManager) {
        this.jiraIssuesColumnManager = jiraIssuesColumnManager;
    }

    public void setTrustedApplicationConfig(
            TrustedApplicationConfig trustedApplicationConfig) {
        this.trustedApplicationConfig = trustedApplicationConfig;
    }

    public void setJiraIssuesDateFormatter(JiraIssuesDateFormatter jiraIssuesDateFormatter) {
        this.jiraIssuesDateFormatter = jiraIssuesDateFormatter;
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

    protected JiraRequestData parseRequestData(Map<String, String> params) throws MacroExecutionException {

        if(params.containsKey(JIRA_URL_KEY_PARAM))
        {
            return createJiraRequestData(params.get(JIRA_URL_KEY_PARAM), Type.URL);
        }

        if(params.containsKey(JQL_QUERY))
        {
            return createJiraRequestData(params.get(JQL_QUERY), Type.JQL);
        }

        if(params.containsKey(KEY))
        {
            return createJiraRequestData(params.get(KEY), Type.KEY);
        }

        String requestData = getPrimaryParam(params);
        if (requestData.startsWith("http")) {
            return createJiraRequestData(requestData, Type.URL);
        }

        Matcher keyMatcher = JiraJqlHelper.ISSUE_KEY_PATTERN.matcher(requestData);
        if (keyMatcher.find() && keyMatcher.start() == 0) {
            return createJiraRequestData(requestData, Type.KEY);
        }

        return createJiraRequestData(requestData, Type.JQL);
    }

    private JiraRequestData createJiraRequestData(String requestData, Type requestType) throws MacroExecutionException
    {
        if (requestType == Type.KEY && requestData.indexOf(',') != -1) {
            String jql = "issuekey in (" + requestData + ")";
            return new JiraRequestData(jql, Type.JQL);
        }

        if (requestType == Type.URL)
        {
            try
            {
                new URL(requestData);
                requestData = URIUtil.decode(requestData);
                requestData = URIUtil.encodeQuery(requestData);
            }
            catch(MalformedURLException e)
            {
                throw new MacroExecutionException(getText("jiraissues.error.invalidurl", Arrays.asList(requestData)), e);
            }
            catch (URIException e)
            {
                throw new MacroExecutionException(e);
            }

            requestData = cleanUrlParentheses(requestData).trim().replaceFirst("/sr/jira.issueviews:searchrequest.*-rss/", "/sr/jira.issueviews:searchrequest-xml/");
        }
        return new JiraRequestData(requestData, requestType);
    }

    protected void createContextMapFromParams(Map<String, String> params, Map<String, Object> contextMap,
                    String requestData, Type requestType, ApplicationLink applink,
                    boolean staticMode, boolean isMobile, Map<String,JiraColumnInfo> jiraColumns, ConversionContext conversionContext) throws MacroExecutionException
    {

        List<String> columnNames = JiraIssueSortableHelper.getColumnNames(JiraUtil.getParamValue(params, COLUMNS, JiraUtil.PARAM_POSITION_1));
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
        if(width == null)
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

        // Prepare the maxIssuesToDisplay for velocity template
        int maximumIssues = JiraUtil.DEFAULT_NUMBER_OF_ISSUES;
        if (staticMode)
        {
            String maximumIssuesStr = StringUtils.defaultString(params.get(MAXIMUM_ISSUES), String.valueOf(JiraUtil.DEFAULT_NUMBER_OF_ISSUES));
            // only affect in static mode otherwise using default value as previous
            maximumIssues = JiraUtil.getMaximumIssues(maximumIssuesStr);
        }
        contextMap.put(MAX_ISSUES_TO_DISPLAY, maximumIssues);

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

        String baseurl = params.get(BASE_URL);

        String clickableUrl = getClickableUrl(requestData, requestType, applink, baseurl);
        contextMap.put(CLICKABLE_URL, clickableUrl);

        // this is where the magic happens
        // the `staticMode` variable refers to the "old" plugin when the user was able to choose
        // between Dynamic ( staticMode == false ) and Static mode ( staticMode == true ). For backward compatibily purpose, we are supposed to keep it

        JiraIssuesType issuesType = JiraUtil.getJiraIssuesType(params, requestType, requestData);
        contextMap.put(ISSUE_TYPE, issuesType);
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
                    setKeyInContextMap(requestData, requestType, contextMap);
                    populateContextMapForStaticSingleIssue(contextMap, url, applink, forceAnonymous, useCache, conversionContext);
                    break;

                case COUNT:
                    populateContextMapForStaticCountIssues(contextMap, columnNames, url, applink, forceAnonymous, useCache, conversionContext);
                    break;

                case TABLE:
                    contextMap.put("singleIssueTable", JiraJqlHelper.isJqlKeyType(requestData));
                    populateContextMapForStaticTable(contextMap, columnNames, url, applink, forceAnonymous, useCache, conversionContext);
                    break;
            }
        }
        else
        {
            if (applink != null) {
                contextMap.put("applink", applink);
            }

            if (issuesType == JiraIssuesType.SINGLE)
            {
                setKeyInContextMap(requestData, requestType, contextMap);
            }
            else
            {
                populateContextMapForDynamicTable(params, contextMap, columns, useCache, url, applink, forceAnonymous);
            }
        }

        if (issuesType == JiraIssuesType.TABLE)
        {
            int refreshId = getNextRefreshId();

            contextMap.put("refreshId", new Integer(refreshId));
            MacroDefinition macroDefinition = new MacroDefinition("jira", new RichTextMacroBody(""), null, params);
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
    }

    private void setKeyInContextMap(String requestData, Type requestType, Map<String, Object> contextMap)
    {
        String key = requestData;
        if(requestType == Type.URL)
        {
            key = JiraJqlHelper.getKeyFromURL(requestData);
        }
        contextMap.put("key", key);
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

    private String getRenderedTemplate(final Map<String, Object> contextMap, final boolean staticMode, final JiraIssuesType issuesType)
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
            ApplicationLink applink, boolean forceAnonymous, boolean useCache, ConversionContext conversionContext)
            throws MacroExecutionException
    {
        JiraIssuesManager.Channel channel;
        try
        {
            channel = jiraIssuesManager.retrieveXMLAsChannel(url, DEFAULT_COLUMNS_FOR_SINGLE_ISSUE, applink,
                    forceAnonymous, useCache);
            setupContextMapForStaticSingleIssue(contextMap, channel.getChannelElement().getChild(ITEM), applink);
        }
        catch (CredentialsRequiredException e)
        {
            populateContextMapWhenUserNotMappingToJira(contextMap, url, applink, forceAnonymous, e
                    .getAuthorisationURI().toString(), useCache, conversionContext);
        }
        catch (MalformedRequestException e)
        {
            contextMap.put(IS_NO_PERMISSION_TO_VIEW, true);
        }
        catch (Exception e)
        {
            jiraExceptionHelper.throwMacroExecutionException(e, conversionContext);
        }
    }

    private void populateContextMapWhenUserNotMappingToJira(Map<String, Object> contextMap, String url,
            ApplicationLink applink, boolean forceAnonymous, String errorMessage, boolean useCache, ConversionContext conversionContext)
    {
        try
        {
            populateContextMapForStaticSingleIssueAnonymous(contextMap, url, applink, forceAnonymous, useCache, conversionContext);
        }
        catch (MacroExecutionException e)
        {
            contextMap.put("oAuthUrl", errorMessage);
        }
    }

    private void populateContextMapForStaticSingleIssueAnonymous(
            Map<String, Object> contextMap, String url,
            ApplicationLink applink, boolean forceAnonymous, boolean useCache, ConversionContext conversionContext)
            throws MacroExecutionException {
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

    private void setupContextMapForStaticSingleIssue(Map<String, Object> contextMap, Element issue, ApplicationLink applicationLink)
    {
        Element resolution = issue.getChild("resolution");
        Element status = issue.getChild("status");

        JiraUtil.checkAndCorrectIconURL(issue, applicationLink);

        contextMap.put("resolved", resolution != null && !"-1".equals(resolution.getAttributeValue("id")));
        contextMap.put(ICON_URL, issue.getChild("type").getAttributeValue(ICON_URL));
        contextMap.put("key", issue.getChild("key").getValue());
        contextMap.put("summary", issue.getChild("summary").getValue());
        contextMap.put("status", status.getValue());
        contextMap.put("statusIcon", status.getAttributeValue(ICON_URL));

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
            ApplicationLink applicationLink) throws MacroExecutionException {
        StringBuilder sf = new StringBuilder(JiraUtil.normalizeUrl(applicationLink.getRpcUrl()));
        sf.append(JiraJqlHelper.XML_SEARCH_REQUEST_URI).append("?tempMax=")
                .append(maximumIssues).append("&returnMax=true&jqlQuery=");

        switch (requestType) {
        case URL:
            if (JiraJqlHelper.isUrlFilterType(requestData))
            {
                String jql = JiraJqlHelper.getJQLFromFilter(applicationLink, requestData, jiraIssuesManager, getI18NBean());
                sf.append(JiraUtil.utf8Encode(jql));
                return sf.toString();
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
                    sf.append(JiraUtil.utf8Encode(jql));
                    return sf.toString();
                }
                else if(JiraJqlHelper.isUrlKeyType(requestData))
                {
                    String key = JiraJqlHelper.getKeyFromURL(requestData);
                    return buildKeyJiraUrl(key, applicationLink);
                }
            }
        case JQL:
            sf.append(JiraUtil.utf8Encode(requestData));
            return sf.toString();
        case KEY:
            return buildKeyJiraUrl(requestData, applicationLink);

        }
        throw new MacroExecutionException("Invalid url");
    }

    private String buildKeyJiraUrl(String key, ApplicationLink applicationLink)
    {
        String encodedQuery = JiraUtil.utf8Encode("key in (" + key + ")");
        return JiraUtil.normalizeUrl(applicationLink.getRpcUrl())
                + "/sr/jira.issueviews:searchrequest-xml/temp/SearchRequest.xml?jqlQuery="
                + encodedQuery + "&returnMax=true";
    }


    private String getClickableUrl(String requestData, Type requestType,
            ApplicationLink applicationLink, String baseurl)

    {
        String clickableUrl = null;
        switch (requestType)
        {
        case URL:
            clickableUrl = makeClickableUrl(requestData);
            break;
        case JQL:
            clickableUrl = JiraUtil.normalizeUrl(applicationLink.getDisplayUrl())
            + "/secure/IssueNavigator.jspa?reset=true&jqlQuery="
            + JiraUtil.utf8Encode(requestData);
            break;
        case KEY:
            clickableUrl = JiraUtil.normalizeUrl(applicationLink.getDisplayUrl()) + "/browse/"
                    + JiraUtil.utf8Encode(requestData);
            break;
        }
        if (StringUtils.isNotEmpty(baseurl))
        {
            clickableUrl = rebaseUrl(clickableUrl, baseurl.trim());
        }
        return appendSourceParam(clickableUrl);
    }

    private String appendSourceParam(String clickableUrl)
    {
        String operator = clickableUrl.contains("?") ? "&" : "?";
        return clickableUrl + operator + "src=confmacro";
    }

    /**
     * Create context map for rendering issues in HTML.
     *
     * @param contextMap
     *            Map containing contexts for rendering issues in HTML
     * @param columnNames
     * @param url
     *            JIRA issues XML url
     * @param appLink
     *            not null if using trusted connection
     * @param useCache
     * @throws MacroExecutionException
     *             thrown if Confluence failed to retrieve JIRA Issues
     */
    private void populateContextMapForStaticTable(Map<String, Object> contextMap, List<String> columnNames, String url,
            ApplicationLink appLink, boolean forceAnonymous, boolean useCache, ConversionContext conversionContext) throws MacroExecutionException
    {
        boolean clearCache = getBooleanProperty(conversionContext.getProperty(DefaultJiraCacheManager.PARAM_CLEAR_CACHE));
        try
        {
            if (RenderContext.DISPLAY.equals(conversionContext.getOutputType()) ||
                    RenderContext.PREVIEW.equals(conversionContext.getOutputType()))
            {
                contextMap.put(ENABLE_REFRESH, Boolean.TRUE);
            }
            if (StringUtils.isNotBlank((String) conversionContext.getProperty("orderColumnName")) && StringUtils.isNotBlank((String) conversionContext.getProperty("order")))
            {
                contextMap.put("orderColumnName", conversionContext.getProperty("orderColumnName"));
                contextMap.put("order", conversionContext.getProperty("order"));
            }
            if (clearCache)
            {
                jiraCacheManager.clearJiraIssuesCache(url, columnNames, appLink, forceAnonymous, false);
            }

            JiraIssuesManager.Channel channel = jiraIssuesManager.retrieveXMLAsChannel(url, columnNames, appLink,
                    forceAnonymous, useCache);
            setupContextMapForStaticTable(contextMap, channel, appLink);
        }
        catch (CredentialsRequiredException e)
        {
            if (clearCache)
            {
                jiraCacheManager.clearJiraIssuesCache(url, columnNames, appLink, forceAnonymous, true);
            }
            populateContextMapForStaticTableByAnonymous(contextMap, columnNames, url, appLink, forceAnonymous, useCache);
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

    private void populateContextMapForStaticTableByAnonymous(Map<String, Object> contextMap, List<String> columnNames,
            String url, ApplicationLink appLink, boolean forceAnonymous, boolean useCache)
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

    private void setupContextMapForStaticTable(Map<String, Object> contextMap, JiraIssuesManager.Channel channel, ApplicationLink appLink)
    {
        Element element = channel.getChannelElement();
        contextMap.put("trustedConnection", channel.isTrustedConnection());
        contextMap.put("trustedConnectionStatus", channel.getTrustedConnectionStatus());
        contextMap.put("channel", element);
        contextMap.put("entries", element.getChildren("item"));
        JiraUtil.checkAndCorrectDisplayUrl(element.getChildren("item"), appLink);
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
        contextMap.put("jiraIssuesManager", jiraIssuesManager);
        contextMap.put("jiraIssuesColumnManager", jiraIssuesColumnManager);
        contextMap.put("jiraIssuesDateFormatter", jiraIssuesDateFormatter);
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

    private void populateContextMapForStaticCountIssues(Map<String, Object> contextMap, List<String> columnNames,
                                                        String url, ApplicationLink appLink, boolean forceAnonymous, boolean useCache, ConversionContext conversionContext) throws MacroExecutionException
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

    private String getCountIssuesWithAnonymous(String url, List<String> columnNames, ApplicationLink appLink, boolean forceAnonymous, boolean useCache) throws MacroExecutionException {
        try {
            JiraIssuesManager.Channel channel = jiraIssuesManager.retrieveXMLAsChannelByAnonymous(url, columnNames, appLink, forceAnonymous, useCache);
            Element element = channel.getChannelElement();
            Element totalItemsElement = element.getChild("issue");
            return totalItemsElement != null ? totalItemsElement.getAttributeValue("total") : "" + element.getChildren("item").size();
        } catch (Exception e) {
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
                   boolean useCache, String url, ApplicationLink applink, boolean forceAnonymous) throws MacroExecutionException
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
       String pagerStart = filterOutParam(urlParam,"pager/start=");
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
       String sortOrder = filterOutParam(urlBuffer, "sorter/order=");
       if (StringUtils.isNotEmpty(sortOrder))
       {
           return sortOrder.toLowerCase();
       }
       return "desc";
   }


   private String getSortFieldParam(StringBuffer urlBuffer)
   {
       String sortField = filterOutParam(urlBuffer, "sorter/field=");
       if (StringUtils.isNotEmpty(sortField))
       {
           return sortField;
       }
       return null;
   }

    // url needs its own method because in the v2 macros params with equals
    // don't get saved into the map with numbered keys such as "0", unlike the
    // old macros
    protected String getPrimaryParam(Map<String, String> params) throws MacroExecutionException {
        if(params.get("data") != null)
        {
            return params.get("data").trim();
        }

        Set<String> keys = params.keySet();
        for(String key : keys)
        {
            if(StringUtils.isNotBlank(key) && !MACRO_PARAMS.contains(key))
            {
                return key.matches(POSITIVE_INTEGER_REGEX) ? params.get(key) : key + "=" + params.get(key);
            }
        }

        throw new MacroExecutionException(getText("jiraissues.error.invalidMacroFormat"));
    }

    // for CONF-1672
    protected String cleanUrlParentheses(String url) {
        if (url.indexOf('(') > 0)
        {
            url = url.replaceAll("\\(", "%28");
        }

        if (url.indexOf(')') > 0)
        {
            url = url.replaceAll("\\)", "%29");
        }

        if (url.indexOf("&amp;") > 0)
        {
            url = url.replaceAll("&amp;", "&");
        }

        return url;
    }

    private boolean shouldRenderInHtml(String renderModeParamValue, ConversionContext conversionContext) {
        return RenderContext.PDF.equals(conversionContext.getOutputType())
            || RenderContext.WORD.equals(conversionContext.getOutputType())
            || !DYNAMIC_RENDER_MODE.equals(renderModeParamValue)
            || RenderContext.EMAIL.equals(conversionContext.getOutputType())
            || RenderContext.FEED.equals(conversionContext.getOutputType())
            || RenderContext.HTML_EXPORT.equals(conversionContext.getOutputType());
    }

    protected int getResultsPerPageParam(StringBuffer urlParam)
            throws MacroExecutionException {
        String tempMaxParam = filterOutParam(urlParam, "tempMax=");
        if (StringUtils.isNotEmpty(tempMaxParam)) {
            int tempMax = Integer.parseInt(tempMaxParam);
            if (tempMax <= 0)
            {
                throw new MacroExecutionException("The tempMax parameter in the JIRA url must be greater than zero.");
            }
            return tempMax;
        } else {
            return 10;
        }
    }

    protected static String filterOutParam(StringBuffer baseUrl,
            final String filter) {
        int tempMaxParamLocation = baseUrl.indexOf(filter);
        if (tempMaxParamLocation != -1) {
            String value;
            int nextParam = baseUrl.indexOf("&", tempMaxParamLocation);
            // finding start of next param, if there is one. can't be ? because
            // filter
            // is before any next param
            if (nextParam != -1) {
                value = baseUrl.substring(
                        tempMaxParamLocation + filter.length(), nextParam);
                baseUrl.delete(tempMaxParamLocation, nextParam + 1);
            } else {
                value = baseUrl.substring(
                        tempMaxParamLocation + filter.length(),
                        baseUrl.length());
                // tempMaxParamLocation-1 to remove ?/& since
                // it won't be used by next param in this case

                baseUrl.delete(tempMaxParamLocation - 1, baseUrl.length());
            }
            return value;
        } else
        {
            return null;
        }
    }

    public String rebaseUrl(String clickableUrl, String baseUrl) {
        return clickableUrl.replaceFirst("^" + // only at start of string
                ".*?" + // minimum number of characters (the schema) followed
                        // by...
                "://" + // literally: colon-slash-slash
                "[^/]+", // one or more non-slash characters (the hostname)
                baseUrl);
    }

    protected static String makeClickableUrl(String url) {
        StringBuffer link = new StringBuffer(url);
        filterOutParam(link, "view="); // was removing only view=rss but this
                                       // way is okay as long as there's not
                                       // another kind of view= that we should
                                       // keep
        filterOutParam(link, "decorator="); // was removing only decorator=none
                                            // but this way is okay as long as
                                            // there's not another kind of
                                            // decorator= that we should keep
        filterOutParam(link, "os_username=");
        filterOutParam(link, "os_password=");
        filterOutParam(link, "returnMax=");

        String linkString = link.toString();
        linkString = linkString
                .replaceFirst(
                        "sr/jira.issueviews:searchrequest-xml/temp/SearchRequest.xml\\?",
                        "secure/IssueNavigator.jspa?reset=true&");
        linkString = linkString.replaceFirst(
                "sr/jira.issueviews:searchrequest-xml/temp/SearchRequest.xml",
                "secure/IssueNavigator.jspa?reset=true");
        linkString = linkString
                .replaceFirst(
                        "sr/jira.issueviews:searchrequest-xml/[0-9]+/SearchRequest-([0-9]+).xml\\?",
                        "secure/IssueNavigator.jspa?requestId=$1&");
        linkString = linkString
                .replaceFirst(
                        "sr/jira.issueviews:searchrequest-xml/[0-9]+/SearchRequest-([0-9]+).xml",
                        "secure/IssueNavigator.jspa?requestId=$1");
        return linkString;
    }

    private String buildRetrieverUrl(Collection<JiraColumnInfo> columns,
            String url, ApplicationLink applink, boolean forceAnonymous) {
        String baseUrl = settingsManager.getGlobalSettings().getBaseUrl();
        StringBuilder retrieverUrl = new StringBuilder(baseUrl);
        retrieverUrl.append("/plugins/servlet/issue-retriever?");
        retrieverUrl.append("url=").append(JiraUtil.utf8Encode(url));
        if (applink != null) {
            retrieverUrl.append("&appId=").append(
                    JiraUtil.utf8Encode(applink.getId().toString()));
        }
        for (JiraColumnInfo columnInfo : columns) {
            retrieverUrl.append("&columns=").append(
                    JiraUtil.utf8Encode(columnInfo.toString()));
        }
        retrieverUrl.append("&forceAnonymous=").append(forceAnonymous);
        retrieverUrl.append("&flexigrid=true");
        return retrieverUrl.toString();
    }

    public String execute(Map<String, String> parameters, String body, ConversionContext conversionContext) throws MacroExecutionException
    {
        JiraRequestData jiraRequestData = parseRequestData(parameters);
        String requestData = jiraRequestData.getRequestData();
        Type requestType = jiraRequestData.getRequestType();
        ApplicationLink applink = null;
        try
        {
            applink = applicationLinkResolver.resolve(requestType, requestData, parameters);
        }
        catch (TypeNotInstalledException tne)
        {
            jiraExceptionHelper.throwMacroExecutionException(tne, conversionContext);
        }
        Map<String, JiraColumnInfo> jiraColumns = jiraIssuesColumnManager.getColumnsInfoFromJira(applink);

        requestData = jiraIssueSortingManager.getRequestDataForSorting(parameters, requestData, requestType, jiraColumns, conversionContext, applink);

        try
        {
            Map<String, Object> contextMap = MacroUtils.defaultVelocityContext();
            JiraIssuesType issuesType = JiraUtil.getJiraIssuesType(parameters, requestType, requestData);
            parameters.put(TOKEN_TYPE_PARAM, issuesType == JiraIssuesType.COUNT || requestType == Type.KEY ? TokenType.INLINE.name() : TokenType.BLOCK.name());
            boolean staticMode = shouldRenderInHtml(parameters.get(RENDER_MODE_PARAM), conversionContext);
            boolean isMobile = "mobile".equals(conversionContext.getOutputDeviceType());
            createContextMapFromParams(parameters, contextMap, requestData, requestType, applink, staticMode, isMobile, jiraColumns, conversionContext);

            if(isMobile) {
                return getRenderedTemplateMobile(contextMap, issuesType);
            } else {
                return getRenderedTemplate(contextMap, staticMode, issuesType);
            }
        }
        catch (Exception e)
        {
            throw new MacroExecutionException(e);
        }
    }

    private Locale getUserLocale(String language)
    {
        if (StringUtils.isNotEmpty(language))
        {
            if (language.contains("-"))
            {
                return new Locale(language.substring(0, 2), language.substring(language.indexOf('-') + 1));
            }
            else {
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

    public void setPermissionManager(PermissionManager permissionManager) {
        this.permissionManager = permissionManager;
    }

    public void setApplicationLinkResolver(
            ApplicationLinkResolver applicationLinkResolver) {
        this.applicationLinkResolver = applicationLinkResolver;
    }

    public JiraIssuesXmlTransformer getXmlXformer()
    {
        return xmlXformer;
    }

    public void setSettingsManager(SettingsManager settingsManager)
    {
        this.settingsManager = settingsManager;
    }

    public void setMacroMarshallingFactory(MacroMarshallingFactory macroMarshallingFactory)
    {
        this.macroMarshallingFactory = macroMarshallingFactory;
    }

    public void setJiraCacheManager(JiraCacheManager jiraCacheManager)
    {
        this.jiraCacheManager = jiraCacheManager;
    }

    public void setImagePlaceHolderHelper(ImagePlaceHolderHelper imagePlaceHolderHelper)
    {
        this.imagePlaceHolderHelper = imagePlaceHolderHelper;
    }

    public void setFormatSettingsManager(FormatSettingsManager formatSettingsManager)
    {
        this.formatSettingsManager = formatSettingsManager;
    }

    public void setJiraIssueSortingManager(JiraIssueSortingManager jiraIssueSortingManager)
    {
        this.jiraIssueSortingManager = jiraIssueSortingManager;
    }

    private int getNextRefreshId()
    {
        return RandomUtils.nextInt();
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

    // render the content of the JDOM Element got from the SingleJiraIssuesMapThreadLocal
    public String renderSingleJiraIssue(Map<String, String> parameters, Element issue, String serverUrl, String key)
    {
        Map<String, Object> contextMap = MacroUtils.defaultVelocityContext();
        String showSummaryParam = JiraUtil.getParamValue(parameters, SHOW_SUMMARY, JiraUtil.SUMMARY_PARAM_POSITION);
        if (StringUtils.isEmpty(showSummaryParam))
        {
            contextMap.put(SHOW_SUMMARY, true);
        }
        else
        {
            contextMap.put(SHOW_SUMMARY, Boolean.parseBoolean(showSummaryParam));
        }
        setupContextMapForStaticSingleIssue(contextMap, issue, null);
        contextMap.put(CLICKABLE_URL, serverUrl + key);
        return VelocityUtils.getRenderedTemplate(TEMPLATE_PATH + "/staticsinglejiraissue.vm", contextMap);
    }
}
