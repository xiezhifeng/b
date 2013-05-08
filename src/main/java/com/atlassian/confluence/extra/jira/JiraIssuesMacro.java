package com.atlassian.confluence.extra.jira;

import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.util.URIUtil;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jdom.Element;

import com.atlassian.applinks.api.ApplicationId;
import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.applinks.api.ApplicationLinkService;
import com.atlassian.applinks.api.CredentialsRequiredException;
import com.atlassian.applinks.api.application.jira.JiraApplicationType;
import com.atlassian.cache.Cache;
import com.atlassian.cache.CacheManager;
import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.content.render.xhtml.DefaultConversionContext;
import com.atlassian.confluence.extra.jira.cache.CacheKey;
import com.atlassian.confluence.extra.jira.cache.SimpleStringCache;
import com.atlassian.confluence.extra.jira.exception.AuthenticationException;
import com.atlassian.confluence.extra.jira.exception.MalformedRequestException;
import com.atlassian.confluence.macro.DefaultImagePlaceholder;
import com.atlassian.confluence.macro.EditorImagePlaceholder;
import com.atlassian.confluence.macro.ImagePlaceholder;
import com.atlassian.confluence.macro.Macro;
import com.atlassian.confluence.macro.MacroExecutionException;
import com.atlassian.confluence.macro.ResourceAware;
import com.atlassian.confluence.renderer.radeox.macros.MacroUtils;
import com.atlassian.confluence.security.Permission;
import com.atlassian.confluence.security.PermissionManager;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.confluence.util.GeneralUtil;
import com.atlassian.confluence.util.i18n.I18NBean;
import com.atlassian.confluence.util.i18n.I18NBeanFactory;
import com.atlassian.confluence.util.velocity.VelocityUtils;
import com.atlassian.confluence.web.context.HttpContext;
import com.atlassian.plugin.webresource.WebResourceManager;
import com.atlassian.renderer.RenderContext;
import com.atlassian.renderer.TokenType;
import com.atlassian.renderer.v2.RenderMode;
import com.atlassian.renderer.v2.macro.BaseMacro;
import com.atlassian.renderer.v2.macro.MacroException;
import com.google.common.base.Strings;

/**
 * A macro to import/fetch JIRA issues...
 */
public class JiraIssuesMacro extends BaseMacro implements Macro, EditorImagePlaceholder, ResourceAware
{
    private static final Logger log = Logger.getLogger(JiraIssuesMacro.class);
    public static enum Type {KEY, JQL, URL};

    private static String TOKEN_TYPE_PARAM = ": = | TOKEN_TYPE | = :";

    private static final Logger LOG = Logger.getLogger(JiraIssuesMacro.class);

    private static final String RENDER_MODE_PARAM = "renderMode";
    private static final String DYNAMIC_RENDER_MODE = "dynamic";
    private static final String DEFAULT_DATA_WIDTH = "100%";

    private static final String PROP_KEY_PREFIX = "jiraissues.column.";
    private static final List<String> DEFAULT_RSS_FIELDS = Arrays.asList(
            "type", "key", "summary", "assignee", "reporter", "priority",
            "status", "resolution", "created", "updated", "due");
    private static final Set<String> WRAPPED_TEXT_FIELDS = new HashSet<String>(
            Arrays.asList("summary", "component", "version", "description"));
    private static final List<String> DEFAULT_COLUMNS_FOR_SINGLE_ISSUE = Arrays.asList
            (new String[] { "summary", "type", "resolution", "status" });

    // Snagged from com.atlassian.jira.util.JiraKeyUtils. This is configurable
    // but this is the default and it's better than nothing.
    private static final String issueKeyRegex = "(^|[^a-zA-Z]|\n)(([A-Z][A-Z]+)-[0-9]+)";

    private static final Pattern issueKeyPattern = Pattern
            .compile(issueKeyRegex);

    private static final int PARAM_POSITION_1 = 1;
    private static final int PARAM_POSITION_2 = 2;
    private static final int PARAM_POSITION_4 = 4;
    private static final int PARAM_POSITION_5 = 5;
    private static final int PARAM_POSITION_6 = 6;
    private static final String PLACEHOLDER_SERVLET = "/plugins/servlet/count-image-generator";
    private static final String JIRA_TABLE_DISPLAY_PLACEHOLDER_IMG_PATH = "/download/resources/confluence.extra.jira/jira-table.png";
    private static final String DEFAULT_RESULTS_PER_PAGE = "10";

    private static final String TEMPLATE_PATH = "templates/extra/jira";
    private static final String DEFAULT_JIRA_ISSUES_COUNT = "X";

    private final JiraIssuesXmlTransformer xmlXformer = new JiraIssuesXmlTransformer();

    private I18NBeanFactory i18NBeanFactory;

    private JiraIssuesManager jiraIssuesManager;

    private JiraIssuesColumnManager jiraIssuesColumnManager;

    private ApplicationLinkService appLinkService;

    private WebResourceManager webResourceManager;

    private TrustedApplicationConfig trustedApplicationConfig;

    private String resourcePath;

    private HttpContext httpContext;

    private PermissionManager permissionManager;

    private ApplicationLinkResolver applicationLinkResolver;

    private JiraIssuesDateFormatter jiraIssuesDateFormatter;

    private FlexigridResponseGenerator flexigridResponseGenerator;

    private JiraIssuesUrlManager jiraIssuesUrlManager;

    private CacheManager cacheManager;

    private I18NBean getI18NBean()
    {
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

    public void setJiraIssuesResponseGenerator(FlexigridResponseGenerator jiraIssuesResponseGenerator)
    {
        this.flexigridResponseGenerator = jiraIssuesResponseGenerator;
    }

    public void setJiraIssuesUrlManager(JiraIssuesUrlManager jiraIssuesUrlManager)
    {
        this.jiraIssuesUrlManager = jiraIssuesUrlManager;
    }

    public void setCacheManager(CacheManager cacheManager)
    {
        this.cacheManager = cacheManager;
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

    @Override
    public ImagePlaceholder getImagePlaceholder(Map<String, String> parameters, ConversionContext conversionContext)
    {
        if ("true".equalsIgnoreCase(parameters.get("count")))
        {
            String appId = parameters.get("serverId");
            String jqlQuery = parameters.get("jqlQuery");
            try
            {
                ApplicationLink appLink = appLinkService.getApplicationLink(new ApplicationId(appId));
                if (appLink == null)
                {
                    log.error("Error generate count macro placeholder because of there is no app link ");
                    return new DefaultImagePlaceholder(PLACEHOLDER_SERVLET + "?totalIssues=-1", null, false);
                }
                String url = appLink.getDisplayUrl() + "/sr/jira.issueviews:searchrequest-xml/temp/SearchRequest.xml?jqlQuery="
                        + URLEncoder.encode(jqlQuery, "UTF-8") + "&tempMax=0";
                CacheKey key = createDefaultIssuesCacheKey(appId, url);
                SimpleStringCache subCacheForKey = getSubCacheForKey(key);
                String totalIssues;
                if (subCacheForKey != null && subCacheForKey.get(0) != null)
                {
                    totalIssues = subCacheForKey.get(0);
                }
                else
                {
                    JiraIssuesManager.Channel channel = jiraIssuesManager.retrieveXMLAsChannel(url, new ArrayList<String>(), appLink, false);
                    totalIssues = flexigridResponseGenerator.generate(channel, new ArrayList<String>(), 0, true, true);
                }
                return new DefaultImagePlaceholder(PLACEHOLDER_SERVLET + "?totalIssues=" + totalIssues, null, false);
            }
            catch (Exception e)
            {
                log.error("Error generate count macro placeholder: " + e.getMessage(), e);
                return new DefaultImagePlaceholder(PLACEHOLDER_SERVLET + "?totalIssues=-1", null, false);
            }
        }

        if (!Strings.isNullOrEmpty(parameters.get("jqlQuery")))
        {
            return new DefaultImagePlaceholder(JIRA_TABLE_DISPLAY_PLACEHOLDER_IMG_PATH, null, false);
        }

        return null;
    }

    private SimpleStringCache getSubCacheForKey(CacheKey key)
    {
        Cache cacheCache = cacheManager.getCache(JiraIssuesMacro.class.getName());
        SimpleStringCache subCacheForKey = null;
        try
        {
            subCacheForKey = (SimpleStringCache) cacheCache.get(key);
        }
        catch (ClassCastException cce)
        {
            log.warn("Unable to get cached data with key " + key + ". The cached data will be purged ('" + cce.getMessage() + ")");
            cacheCache.remove(key);
        }

        return subCacheForKey;
    }

    private CacheKey createDefaultIssuesCacheKey(String appId, String url)
    {
        String jiraIssueUrl = jiraIssuesUrlManager.getJiraXmlUrlFromFlexigridRequest(url, DEFAULT_RESULTS_PER_PAGE, null, null);
        return new CacheKey(jiraIssueUrl, appId, DEFAULT_RSS_FIELDS, true, false, true);
    }

    public boolean hasBody()
    {
        return false;
    }

    public RenderMode getBodyRenderMode() {
        return RenderMode.NO_RENDER;
    }

    public void setWebResourceManager(WebResourceManager webResourceManager) {
        this.webResourceManager = webResourceManager;
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

    public void setApplicationLinkService(ApplicationLinkService appLinkService) {
        this.appLinkService = appLinkService;
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
            return execute((Map<String, String>) params, body, new DefaultConversionContext(renderContext));
        }
        catch (MacroExecutionException e)
        {
            throw new MacroException(e);
        }
    }

    protected JiraRequestData parseRequestData(Map params)
            throws MacroExecutionException {
        Map<String, String> typeSafeParams = (Map<String, String>) params;
        // look for the url param first
        String requestData = typeSafeParams.get("url");
        Type requestType = Type.URL;
        if (requestData == null) {
            // look for the jqlQuery param if we didn't find a url
            requestData = typeSafeParams.get("jqlQuery");
            requestType = Type.JQL;
            if (requestData == null) {
                // no jqlquery or url, maybe a key
                requestData = typeSafeParams.get("key");
                requestType = Type.KEY;
                if (requestData == null) {
                    // None of the 3 types were explicitly set, try and figure
                    // out what they want from
                    // the first argument set.
                    requestData = getPrimaryParam(params);
                    if (StringUtils.isBlank(requestData)) {
                        throw new MacroExecutionException(
                                getText("jiraissues.error.urlnotspecified"));
                    }
                    // Look for a url
                    if (requestData.startsWith("http")) {
                        requestType = Type.URL;
                    } else {
                        // first try to match the issue key regex if that fails,
                        // assume it's a jqlQuery.
                        Matcher keyMatcher = issueKeyPattern
                                .matcher(requestData);
                        if (keyMatcher.find() && keyMatcher.start() == 0) {
                            requestType = Type.KEY;
                        } else {
                            requestType = Type.JQL;
                        }
                    }
                }
            }
        }
        if (requestType == Type.KEY) {
            if (requestData.indexOf(',') != -1) {
                // assume this is a list of issues and convert to a JQL query
                requestType = Type.JQL;
                requestData = "issuekey in (" + requestData + ")";
            }
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
                    boolean staticMode, boolean showCount) throws MacroExecutionException
    {

        List<String> columnNames = getColumnNames(getParam(params,"columns", PARAM_POSITION_1));
        List<ColumnInfo> columns = getColumnInfo(columnNames);
        contextMap.put("columns", columns);
        String cacheParameter = getParam(params, "cache", PARAM_POSITION_2);

        //Only define the Title param if explicitly defined.
        if (params.containsKey("title"))
            contextMap
                    .put("title", GeneralUtil.htmlEncode(params.get("title")));

        // maybe this should change to position 3 now that the former 3 param
        // got deleted, but that could break
        // backward compatibility of macros currently in use
        String anonymousStr = getParam(params, "anonymous", PARAM_POSITION_4);
        if ("".equals(anonymousStr))
            anonymousStr = "false";

        // and maybe this should change to position 4 -- see comment for
        // anonymousStr above
        String forceTrustWarningsStr = getParam(params, "forceTrustWarnings",
                PARAM_POSITION_5);
        if ("".equals(forceTrustWarningsStr))
            forceTrustWarningsStr = "false";

        contextMap.put("width", StringUtils.defaultString(params.get("width"),
                DEFAULT_DATA_WIDTH));
        String heightStr = getParam(params, "height", PARAM_POSITION_6);
        if (StringUtils.isEmpty(heightStr) || !StringUtils.isNumeric(heightStr))
            heightStr = null;

        boolean useCache = StringUtils.isBlank(cacheParameter)
                || cacheParameter.equals("on")
                || Boolean.valueOf(cacheParameter);
        boolean forceAnonymous = Boolean.valueOf(anonymousStr)
                || (requestType == Type.URL && SeraphUtils
                        .isUserNamePasswordProvided(requestData));
        boolean showTrustWarnings = Boolean.valueOf(forceTrustWarningsStr)
                || isTrustWarningsEnabled();
        contextMap.put("showTrustWarnings", showTrustWarnings);

        String baseurl = params.get("baseurl");
        String clickableUrl = getClickableUrl(requestData, requestType,
                applink, baseurl);
        contextMap.put("clickableUrl", clickableUrl);

        // The template needs to know whether it should escape HTML fields and
        // display a warning
        boolean isAdministrator = permissionManager.hasPermission(
                AuthenticatedUserThreadLocal.getUser(), Permission.ADMINISTER,
                PermissionManager.TARGET_APPLICATION);
        contextMap.put("isAdministrator", isAdministrator);
        contextMap.put("isSourceApplink", applink != null);

        String url = getXmlUrl(requestData, requestType, applink);

        // this is where the magic happens
        // the `staticMode` variable refers to the "old" plugin when the user was able to choose
        // between Dynamic ( staticMode == false ) and Static mode ( staticMode == true ). For backward compatibily purpose, we are supposed to keep it
        if (!staticMode)
        {
            if (applink != null)
                contextMap.put("applink", applink);

            if (requestType == Type.KEY) {
                contextMap.put("key", requestData);
            }
            else
            {
                populateContextMapForDynamicTable(params, contextMap, columns, heightStr, useCache, url, applink, forceAnonymous);
            }
        }
        else
        {
            if (requestType == Type.KEY)
            {
                contextMap.put("key", requestData);
                populateContextMapForStaticSingleIssue(contextMap, url, applink, forceAnonymous);
            }
            else if (showCount)
            {
                populateContextMapForStaticCountIssues(contextMap, columnNames, url, applink, forceAnonymous, useCache);
            }
            else
            {
                populateContextMapForStaticTable(contextMap, columnNames, url, applink, forceAnonymous);
            }
        }
    }

    private String getRenderedTemplate(final Map<String, Object> contextMap, final Type requestType, final boolean staticMode, final boolean showCount)
            throws MacroExecutionException
    {
        if(staticMode)
        {
            return renderStaticTemplate(requestType, contextMap, showCount);
        }

        return renderDynamicTemplate(requestType, contextMap, showCount);
    }

    private String renderStaticTemplate(final Type requestType, final Map<String, Object> contextMap, final boolean showCount)
    {
        if (requestType == Type.KEY)
        {
            return VelocityUtils.getRenderedTemplate(TEMPLATE_PATH + "/staticsinglejiraissue.vm", contextMap);
        }
        else if(showCount)
        {
            return VelocityUtils.getRenderedTemplate(TEMPLATE_PATH + "/staticShowCountJiraissues.vm", contextMap);
        }
        else {
            return VelocityUtils.getRenderedTemplate(TEMPLATE_PATH + "/staticJiraIssues.vm", contextMap);
        }

    }

    private String renderDynamicTemplate(final Type requestType, final Map<String, Object> contextMap, final boolean showCount)
    {
        if (requestType == Type.KEY)
        {
            return VelocityUtils.getRenderedTemplate(TEMPLATE_PATH + "/singlejiraissue.vm", contextMap);
        }
        else if(showCount)
        {
            return VelocityUtils.getRenderedTemplate(TEMPLATE_PATH + "/showCountJiraissues.vm", contextMap);
        }
        else
        {
            return VelocityUtils.getRenderedTemplate(TEMPLATE_PATH + "/dynamicJiraIssues.vm", contextMap);
        }
    }

    private boolean isAnonymousUser() {
        return AuthenticatedUserThreadLocal.getUser() == null;
    }

    private void populateContextMapForStaticSingleIssue(
            Map<String, Object> contextMap, String url,
            ApplicationLink applink, boolean forceAnonymous)
            throws MacroExecutionException {
        JiraIssuesManager.Channel channel;
        try {
            channel = jiraIssuesManager.retrieveXMLAsChannel(
                    url, DEFAULT_COLUMNS_FOR_SINGLE_ISSUE, applink, forceAnonymous);
            setupContextMapForStaticSingleIssue(contextMap, channel);
        }
        catch (CredentialsRequiredException e)
        {
            populateContextMapWhenUserNotMappingToJira(contextMap, url, applink, forceAnonymous, e.getAuthorisationURI().toString());
        }
        catch (MalformedRequestException e)
        {
            contextMap.put("isNoPermissionToView", true);
            if(isAnonymousUser())
            {
                contextMap.put("isAnonymous", true);
            }
        }
        catch (Exception e)
        {
            throwMacroExecutionException(e);
        }
    }

    private void populateContextMapWhenUserNotMappingToJira(Map<String, Object> contextMap,
                                      String url,ApplicationLink applink, boolean forceAnonymous, String errorMessage) {
        try
        {
            populateContextMapForStaticSingleIssueAnonymous(contextMap, url, applink, forceAnonymous);
        }
        catch (MacroExecutionException e)
        {
            contextMap.put("oAuthUrl", errorMessage);
        }
    }

    private void populateContextMapForStaticSingleIssueAnonymous(
            Map<String, Object> contextMap, String url,
            ApplicationLink applink, boolean forceAnonymous)
            throws MacroExecutionException {
        JiraIssuesManager.Channel channel;
        try {
            channel = jiraIssuesManager.retrieveXMLAsChannelByAnonymous(
                    url, DEFAULT_COLUMNS_FOR_SINGLE_ISSUE, applink, forceAnonymous);
            setupContextMapForStaticSingleIssue(contextMap, channel);
        }
        catch (Exception e) {
            throwMacroExecutionException(e);
        }
    }

    private void setupContextMapForStaticSingleIssue(Map<String, Object> contextMap, JiraIssuesManager.Channel channel) {
        Element element = channel.getChannelElement();
        Element issue = element.getChild("item");

        contextMap.put("clickableUrl", issue.getChild("link").getValue());
        contextMap.put("resolved", !issue.getChild("resolution")
                .getAttributeValue("id").equals("-1"));
        contextMap.put("iconUrl",
                issue.getChild("type").getAttributeValue("iconUrl"));
        contextMap.put("key", issue.getChild("key").getValue());
        contextMap.put("summary", issue.getChild("summary").getValue());
        Element status = issue.getChild("status");
        contextMap.put("status", status.getValue());
        contextMap.put("statusIcon", status.getAttributeValue("iconUrl"));
    }

    private String getXmlUrl(String requestData, Type requestType,
            ApplicationLink applink) throws MacroExecutionException {
        switch (requestType) {
        case URL:
            return requestData.trim();
        case JQL:
            return normalizeUrl(applink.getRpcUrl())
                    + "/sr/jira.issueviews:searchrequest-xml/temp/SearchRequest.xml?jqlQuery="
                    + utf8Encode(requestData);
        case KEY:
            String encodedQuery = utf8Encode("key in (" + requestData + ")");
            return normalizeUrl(applink.getRpcUrl())
                    + "/sr/jira.issueviews:searchrequest-xml/temp/SearchRequest.xml?jqlQuery="
                    + encodedQuery;
        }
        throw new MacroExecutionException("Invalid url");
    }

    private String normalizeUrl(URI rpcUrl) {
        String baseUrl = rpcUrl.toString();
        return baseUrl.endsWith("/") ? baseUrl.substring(0,
                baseUrl.length() - 1) : baseUrl;
    }

    private String getClickableUrl(String requestData, Type requestType,
            ApplicationLink applink, String baseurl)

    {
        String clickableUrl = null;
        switch (requestType) {
        case URL:
            clickableUrl = makeClickableUrl(requestData);
            break;
        case JQL:
            clickableUrl = normalizeUrl(applink.getDisplayUrl())
                    + "/secure/IssueNavigator.jspa?reset=true&jqlQuery="
                    + utf8Encode(requestData);
            break;
        case KEY:
            clickableUrl = normalizeUrl(applink.getDisplayUrl()) + "/browse/"
                    + utf8Encode(requestData);
            break;
        }
        if (StringUtils.isNotEmpty(baseurl))
            clickableUrl = rebaseUrl(clickableUrl, baseurl.trim());
        return clickableUrl;
    }

    /**
     * Wrap exception into MacroExecutionException. This exception then will be
     * processed by AtlassianRenderer.
     *
     * @param exception
     *            Any Exception thrown for whatever reason when Confluence could
     *            not retrieve JIRA Issues
     * @throws MacroExecutionException
     *             A macro exception means that a macro has failed to execute
     *             successfully
     */
    private void throwMacroExecutionException(Exception exception)
            throws MacroExecutionException {
        // CONFJIRA-154 - misleading error message for IOException
        String i18nKey = "jiraissues.error.unabletodeterminesort";
        List params = null;

        if (exception instanceof UnknownHostException) {
            i18nKey = "jiraissues.error.unknownhost";
            params = Arrays.asList(StringUtils.defaultString(exception
                    .getMessage()));
        } else if (exception instanceof ConnectException) {
            i18nKey = "jiraissues.error.unabletoconnect";
            params = Arrays.asList(StringUtils.defaultString(exception
                    .getMessage()));
        } else if (exception instanceof AuthenticationException) {
            i18nKey = "jiraissues.error.authenticationerror";
        } else if (exception instanceof MalformedRequestException) {
            // JIRA returns 400 HTTP code when it should have been a 401
            i18nKey = "jiraissues.error.notpermitted";
        } else if (exception instanceof TrustedAppsException) {
            i18nKey = "jiraissues.error.trustedapps";
            params = Collections.singletonList(exception.getMessage());
        }

        LOG.error("Macro execution exception: ", exception);
        throw new MacroExecutionException(getText(i18nKey, params), exception);
    }

    /**
     * Create context map for rendering issues in HTML.
     *
     * @param contextMap Map containing contexts for rendering issues in HTML
     * @param columnNames
     * @param url JIRA issues XML url
     * @param appLink not null if using trusted connection
     * @throws MacroExecutionException thrown if Confluence failed to retrieve JIRA Issues
     */
    private void populateContextMapForStaticTable(Map<String, Object> contextMap, List<String> columnNames,
                                                  String url, ApplicationLink appLink, boolean forceAnonymous)
            throws MacroExecutionException
    {
        try
        {
            if (isAnonymousUser())
            {
                contextMap.put("isAnonymous", true);
            }

            JiraIssuesManager.Channel channel = jiraIssuesManager.retrieveXMLAsChannel(url, columnNames, appLink, forceAnonymous);
            setupContextMapForStaticTable(contextMap, channel);
        }
        catch (CredentialsRequiredException e)
        {
            populateContextMapForStaticTableByAnonymous(contextMap,columnNames, url, appLink, forceAnonymous);
            contextMap.put("xmlXformer", xmlXformer);
            contextMap.put("jiraIssuesManager", jiraIssuesManager);
            contextMap.put("jiraIssuesColumnManager", jiraIssuesColumnManager);
            contextMap.put("oAuthUrl", e.getAuthorisationURI().toString());
        }
        catch (MalformedRequestException e)
        {
            log.info("Can't get issues because issues key is not exist or user doesn't have permission to view", e);
        }
        catch (Exception e)
        {
            throwMacroExecutionException(e);
        }
    }

    private void populateContextMapForStaticTableByAnonymous(Map<String, Object> contextMap, List<String> columnNames,
                                                             String url, ApplicationLink appLink, boolean forceAnonymous)
            throws MacroExecutionException
    {
        try
        {
            JiraIssuesManager.Channel channel = jiraIssuesManager.retrieveXMLAsChannelByAnonymous(url, columnNames, appLink, forceAnonymous);
            setupContextMapForStaticTable(contextMap, channel);
        }
        catch (Exception e)
        {
            log.warn("can't get jira issues by anonymous user", e);
        }
    }

    private void setupContextMapForStaticTable(Map<String, Object> contextMap, JiraIssuesManager.Channel channel)
    {
        Element element = channel.getChannelElement();
        contextMap.put("trustedConnection", channel.isTrustedConnection());
        contextMap.put("trustedConnectionStatus", channel.getTrustedConnectionStatus());
        contextMap.put("channel", element);
        contextMap.put("entries", element.getChildren("item"));
        contextMap.put("xmlXformer", xmlXformer);
        contextMap.put("jiraIssuesManager", jiraIssuesManager);
        contextMap.put("jiraIssuesColumnManager", jiraIssuesColumnManager);
        contextMap.put("jiraIssuesDateFormatter", jiraIssuesDateFormatter);
        contextMap.put("userLocale", getUserLocale(element.getChildText("language")));
    }

    private void populateContextMapForStaticCountIssues(Map<String, Object> contextMap, List<String> columnNames,
                                                        String url, ApplicationLink appLink, boolean forceAnonymous, boolean useCache) throws MacroExecutionException
    {
        try
        {
            String count = DEFAULT_JIRA_ISSUES_COUNT;
            if (isAnonymousUser())
            {
                contextMap.put("isAnonymous", true);
            }
            else
            {
                JiraIssuesManager.Channel channel = jiraIssuesManager.retrieveXMLAsChannel(url, columnNames, appLink, forceAnonymous);
                Element element = channel.getChannelElement();
                Element totalItemsElement = element.getChild("issue");
                count = totalItemsElement != null ? totalItemsElement.getAttributeValue("total") : ""
                        + element.getChildren("item").size();
            }
            contextMap.put("count", count);
            contextMap.put("resultsPerPage", getResultsPerPageParam(new StringBuffer(url)));
            contextMap.put("useCache", useCache);
            contextMap.put("retrieverUrlHtml", buildRetrieverUrl(getColumnInfo(columnNames), url, appLink, forceAnonymous));
        }
        catch (CredentialsRequiredException e)
        {
            contextMap.put("count", DEFAULT_JIRA_ISSUES_COUNT);
            contextMap.put("oAuthUrl", e.getAuthorisationURI().toString());
        }
        catch (MalformedRequestException e)
        {
            contextMap.put("count", DEFAULT_JIRA_ISSUES_COUNT);
        }
        catch (Exception e)
        {
            throwMacroExecutionException(e);
        }
    }

   /** Create context map for rendering issues with Flexi Grid.
    *
    * @param params JIRA Issues macro parameters
    * @param contextMap Map containing contexts for rendering issues in HTML
    * @param columns  A list of JIRA column names
    * @param heightStr The height in pixels of the table displaying the JIRA issues
    * @param useCache If true the macro will use a cache of JIRA issues retrieved from the JIRA query
    * @param forceAnonymous set flag to true if using trusted connection
    * @param url JIRA issues XML url
    * @throws MacroExecutionException thrown if Confluence failed to retrieve JIRA Issues
    */
   private void populateContextMapForDynamicTable(
                   Map<String, String> params, Map<String, Object> contextMap, List<ColumnInfo> columns,
                   String heightStr, boolean useCache, String url, ApplicationLink applink, boolean forceAnonymous) throws MacroExecutionException
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

       if (null != heightStr)
           contextMap.put("height",  heightStr);

   }

   private String getStartOnParam(String startOn, StringBuffer urlParam)
   {
       String pagerStart = filterOutParam(urlParam,"pager/start=");
       if(StringUtils.isNotEmpty(startOn))
           return startOn.trim();
       else
       {
           if (StringUtils.isNotEmpty(pagerStart))
               return pagerStart;
           else
               return "0";
       }
   }

   private String getSortOrderParam(StringBuffer urlBuffer)
   {
       String sortOrder = filterOutParam(urlBuffer,"sorter/order=");
       if (StringUtils.isNotEmpty(sortOrder))
           return sortOrder.toLowerCase();
       else
           return "desc";
   }


   private String getSortFieldParam(StringBuffer urlBuffer)
   {
       String sortField = filterOutParam(urlBuffer,"sorter/field=");
       if (StringUtils.isNotEmpty(sortField))
           return sortField;
       else
           return null;
   }

   protected String getParam(Map<String, String> params, String paramName, int paramPosition)
    {
        String param = params.get(paramName);
        if (param == null)
            param = StringUtils.defaultString(params.get(String
                    .valueOf(paramPosition)));

        return param.trim();
    }

    // url needs its own method because in the v2 macros params with equals
    // don't get saved into the map with numbered keys such as "0", unlike the
    // old macros
    protected String getPrimaryParam(Map<String, String> params) {
        String url = params.get("data");
        if (url == null) {
            String allParams = params
                    .get(com.atlassian.renderer.v2.macro.Macro.RAW_PARAMS_KEY);
            int barIndex = allParams.indexOf('|');
            if (barIndex != -1)
                url = allParams.substring(0, barIndex);
            else
                url = allParams;
        }
        return url.trim();
    }

    // for CONF-1672
    protected String cleanUrlParentheses(String url) {
        if (url.indexOf('(') > 0)
            url = url.replaceAll("\\(", "%28");

        if (url.indexOf(')') > 0)
            url = url.replaceAll("\\)", "%29");

        if (url.indexOf("&amp;") > 0)
            url = url.replaceAll("&amp;", "&");

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
            return null;
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

    protected List<ColumnInfo> getColumnInfo(List<String> columnNames) {

        List<ColumnInfo> info = new ArrayList<ColumnInfo>();
        for (String columnName : columnNames) {
            String key = jiraIssuesColumnManager
                    .getCanonicalFormOfBuiltInField(columnName);

            String i18nKey = PROP_KEY_PREFIX + key;
            String displayName = getText(i18nKey);

            // getText() unexpectedly returns the i18nkey if a value isn't found
            if (StringUtils.isBlank(displayName) || displayName.equals(i18nKey))
                displayName = columnName;

            info.add(new ColumnInfo(key, displayName));
        }

        return info;
    }

    protected List<String> getColumnNames(String columnsParameter) {
        List<String> columnNames = DEFAULT_RSS_FIELDS;

        if (StringUtils.isNotBlank(columnsParameter)) {
            columnNames = new ArrayList<String>();
            List<String> keys = Arrays.asList(StringUtils.split(
                    columnsParameter, ",;"));
            for (String key : keys) {
                if (StringUtils.isNotBlank(key)) {
                    columnNames.add(key);
                }
            }

            if (columnNames.isEmpty()) {
                columnNames = DEFAULT_RSS_FIELDS;
            }
        }
        return columnNames;
    }

    private String buildRetrieverUrl(Collection<ColumnInfo> columns,
            String url, ApplicationLink applink, boolean forceAnonymous) {
        HttpServletRequest req = httpContext.getRequest();
        String baseUrl = req.getContextPath();
        StringBuffer retrieverUrl = new StringBuffer(baseUrl);
        retrieverUrl.append("/plugins/servlet/issue-retriever?");
        retrieverUrl.append("url=").append(utf8Encode(url));
        if (applink != null) {
            retrieverUrl.append("&appId=").append(
                    utf8Encode(applink.getId().toString()));
        }
        for (ColumnInfo columnInfo : columns) {
            retrieverUrl.append("&columns=").append(
                    utf8Encode(columnInfo.toString()));
        }
        retrieverUrl.append("&forceAnonymous=").append(forceAnonymous);
        retrieverUrl.append("&flexigrid=true");
        return retrieverUrl.toString();
    }

    public static String utf8Encode(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // will never happen in a standard java runtime environment
            throw new RuntimeException(
                    "You appear to not be running on a standard Java Runtime Environment");
        }
    }

    public static class ColumnInfo {
        private static final String CLASS_NO_WRAP = "columns nowrap";
        private static final String CLASS_WRAP = "columns";

        private String title;
        private String rssKey;

        public ColumnInfo() {
        }

        public ColumnInfo(String rssKey) {
            this(rssKey, rssKey);
        }

        public ColumnInfo(String rssKey, String title) {
            this.rssKey = rssKey;
            this.title = title;
        }

        public String getTitle() {
            return title;
        }

        public String getKey() {
            return this.rssKey;
        }

        public String getHtmlClassName() {
            return (shouldWrap() ? CLASS_WRAP : CLASS_NO_WRAP);
        }

        public boolean shouldWrap() {
            return WRAPPED_TEXT_FIELDS.contains(getKey().toLowerCase());
        }

        public String toString() {
            return getKey();
        }

        public boolean equals(Object obj) {
            if (obj instanceof String) {
                String str = (String) obj;
                return this.rssKey.equalsIgnoreCase(str);
            } else if (obj instanceof ColumnInfo) {
                ColumnInfo that = (ColumnInfo) obj;
                return this.rssKey.equalsIgnoreCase(that.rssKey);
            }

            return false;
        }

        public int hashCode() {
            return this.rssKey.hashCode();
        }
    }

    public String execute(Map<String, String> parameters, String body, ConversionContext conversionContext) throws MacroExecutionException
    {
        try
        {
            webResourceManager.requireResource("confluence.extra.jira:web-resources");
            JiraRequestData jiraRequestData = parseRequestData(parameters);

            String requestData = jiraRequestData.getRequestData();
            Type requestType = jiraRequestData.getRequestType();

            Map<String, String> typeSafeParams = (Map<String, String>) parameters;
            boolean requiresApplink = requestType == Type.KEY || requestType == Type.JQL;
            ApplicationLink applink = null;
            if (requiresApplink)
            {
                applink = applicationLinkResolver.resolve(requestType, requestData, typeSafeParams);
            }
            else // if requestType == Type.URL
            {
                Iterable<ApplicationLink> applicationLinks = appLinkService.getApplicationLinks(JiraApplicationType.class);
                for (ApplicationLink applicationLink : applicationLinks)
                {
                    if (requestData.indexOf(applicationLink.getRpcUrl().toString()) == 0)
                    {
                        applink = applicationLink;
                        break;
                    }
                }
            }

            Map<String, Object> contextMap = MacroUtils.defaultVelocityContext();
            boolean showCount = BooleanUtils.toBoolean(typeSafeParams.get("count"));
            parameters.put(TOKEN_TYPE_PARAM, showCount || requestType == Type.KEY ? TokenType.INLINE.name() : TokenType.BLOCK.name());
            boolean staticMode = shouldRenderInHtml(typeSafeParams.get(RENDER_MODE_PARAM), conversionContext);

            createContextMapFromParams(typeSafeParams, contextMap, requestData, requestType, applink, staticMode, showCount);
            return getRenderedTemplate(contextMap, requestType, staticMode, showCount);
        }
        catch (MacroExecutionException mee)
        {
            // just catch and rethrow to filter out of the catch all.
            throw mee;
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
                return new Locale(language.substring(0, 2), language.substring(language.indexOf('-') + 1));
            else
                return new Locale(language);// Just the language code only
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

    /** Should be autowired by Spring. */
    public void setHttpContext(HttpContext httpContext) {
        this.httpContext = httpContext;
    }

    public void setPermissionManager(PermissionManager permissionManager) {
        this.permissionManager = permissionManager;
    }

    public PermissionManager getPermissionManager() {
        return this.permissionManager;
    }

    public void setApplicationLinkResolver(
            ApplicationLinkResolver applicationLinkResolver) {
        this.applicationLinkResolver = applicationLinkResolver;
    }

    public JiraIssuesXmlTransformer getXmlXformer()
    {
        return xmlXformer;
    }
}
