package com.atlassian.confluence.extra.jira;

import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.atlassian.confluence.web.UrlBuilder;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.util.URIUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jdom.DataConversionException;
import org.jdom.Element;

import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.applinks.api.CredentialsRequiredException;
import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.content.render.xhtml.ConversionContextOutputType;
import com.atlassian.confluence.content.render.xhtml.DefaultConversionContext;
import com.atlassian.confluence.extra.jira.exception.AuthenticationException;
import com.atlassian.confluence.extra.jira.exception.MalformedRequestException;
import com.atlassian.confluence.languages.LocaleManager;
import com.atlassian.confluence.macro.DefaultImagePlaceholder;
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
import com.atlassian.plugin.webresource.WebResourceManager;
import com.atlassian.renderer.RenderContext;
import com.atlassian.renderer.TokenType;
import com.atlassian.renderer.v2.RenderMode;
import com.atlassian.renderer.v2.macro.BaseMacro;
import com.atlassian.renderer.v2.macro.MacroException;
import com.atlassian.sal.api.net.ResponseException;

/**
 * A macro to import/fetch JIRA issues...
 */
public class JiraIssuesMacro extends BaseMacro implements Macro, EditorImagePlaceholder, ResourceAware
{
    private static final Logger LOGGER = Logger.getLogger(JiraIssuesMacro.class);
    public static enum Type {KEY, JQL, URL};
    public static enum JiraIssuesType {SINGLE, COUNT, TABLE};

    private static String TOKEN_TYPE_PARAM = ": = | TOKEN_TYPE | = :";

    private static final String RENDER_MODE_PARAM = "renderMode";
    private static final String DYNAMIC_RENDER_MODE = "dynamic";
    private static final String DEFAULT_DATA_WIDTH = "100%";

    private static final String PROP_KEY_PREFIX = "jiraissues.column.";
    private static final List<String> DEFAULT_RSS_FIELDS = Arrays.asList(
            "type", "key", "summary", "assignee", "reporter", "priority",
            "status", "resolution", "created", "updated", "due");
    private static final List<String> NO_WRAPPED_TEXT_FIELDS = Arrays.asList(
            "key", "type", "priority", "status", "created", "updated", "due" );
    private static final List<String> DEFAULT_COLUMNS_FOR_SINGLE_ISSUE = Arrays.asList
            (new String[] { "summary", "type", "resolution", "status" });

    // Snagged from com.atlassian.jira.util.JiraKeyUtils. This is configurable
    // but this is the default and it's better than nothing.
    private static final String ISSUE_KEY_REGEX = "(^|[^a-zA-Z]|\n)(([A-Z][A-Z]+)-[0-9]+)";
    private static final String XML_KEY_REGEX = ".+/([A-Za-z]+-[0-9]+)/.+";
    private static final String URL_KEY_REGEX = ".+/(i#)?browse/([A-Za-z]+-[0-9]+)";
    private static final String URL_JQL_REGEX = ".+(jqlQuery|jql)=([^&]+)";
    private static final String FILTER_URL_REGEX = ".+(requestId|filter)=([^&]+)";
    private static final String FILTER_XML_REGEX = ".+searchrequest-xml/([0-9]+)/SearchRequest.+";
    private static final String POSITIVE_INTEGER_REGEX = "[0-9]+";

    private static final Pattern ISSUE_KEY_PATTERN = Pattern.compile(ISSUE_KEY_REGEX);
    private static final Pattern XML_KEY_PATTERN = Pattern.compile(XML_KEY_REGEX);
    private static final Pattern URL_KEY_PATTERN = Pattern.compile(URL_KEY_REGEX);
    private static final Pattern URL_JQL_PATTERN = Pattern.compile(URL_JQL_REGEX);
    private static final Pattern FILTER_URL_PATTERN = Pattern.compile(FILTER_URL_REGEX);
    private static final Pattern FILTER_XML_PATTERN = Pattern.compile(FILTER_XML_REGEX);

    private static final List<String> MACRO_PARAMS = Arrays.asList(
            "count","columns","title","renderMode","cache","width",
            "height","server","serverId","anonymous","baseurl", "showSummary", com.atlassian.renderer.v2.macro.Macro.RAW_PARAMS_KEY, "maximumIssues");
    private static final int PARAM_POSITION_1 = 1;
    private static final int PARAM_POSITION_2 = 2;
    private static final int PARAM_POSITION_4 = 4;
    private static final int PARAM_POSITION_5 = 5;
    private static final int PARAM_POSITION_6 = 6;
    private static final int SUMMARY_PARAM_POSITION = 7;
    private static final String PLACEHOLDER_SERVLET = "/plugins/servlet/image-generator";
    private static final String JIRA_TABLE_DISPLAY_PLACEHOLDER_IMG_PATH = "/download/resources/confluence.extra.jira/jira-table.png";
    private static final String JIRA_ISSUES_RESOURCE_PATH = "jiraissues-xhtml";
    private static final String JIRA_ISSUES_SINGLE_MACRO_TEMPLATE = "{jiraissues:key=%s}";
    private static final String JIRA_SINGLE_MACRO_TEMPLATE = "{jira:key=%s}";
    private static final String JIRA_URL_KEY_PARAM = "url";

    private static final String TEMPLATE_PATH = "templates/extra/jira";
    private static final String TEMPLATE_MOBILE_PATH = "templates/mobile/extra/jira";
    private static final String DEFAULT_JIRA_ISSUES_COUNT = "0";
    private static final String JIRA_SINGLE_ISSUE_IMG_SERVLET_PATH_TEMPLATE = "/plugins/servlet/confluence/placeholder/macro?definition=%s&locale=%s";
    private static final String XML_SEARCH_REQUEST_URI = "/sr/jira.issueviews:searchrequest-xml/temp/SearchRequest.xml";


    private final JiraIssuesXmlTransformer xmlXformer = new JiraIssuesXmlTransformer();

    private I18NBeanFactory i18NBeanFactory;

    private JiraIssuesManager jiraIssuesManager;

    private SettingsManager settingsManager;

    private JiraIssuesColumnManager jiraIssuesColumnManager;

    private WebResourceManager webResourceManager;

    private TrustedApplicationConfig trustedApplicationConfig;

    private String resourcePath;

    private PermissionManager permissionManager;

    private ApplicationLinkResolver applicationLinkResolver;

    private JiraIssuesDateFormatter jiraIssuesDateFormatter;

    private FlexigridResponseGenerator flexigridResponseGenerator;

    private LocaleManager localeManager;
    
    protected I18NBean getI18NBean()
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
            String requestData = jiraRequestData.getRequestData();
            Type requestType = jiraRequestData.getRequestType();
            JiraIssuesType issuesType = getJiraIssuesType(parameters, requestType, requestData);

            switch (issuesType)
            {
                case SINGLE:
                    String key = requestData;
                    if(requestType == Type.URL)
                    {
                        key = getKeyFromURL(requestData);
                    }
                    return getSingleImagePlaceHolder(key);

                case COUNT:
                    return getCountImagePlaceHolder(parameters, requestType, requestData);

                case TABLE:
                    return new DefaultImagePlaceholder(JIRA_TABLE_DISPLAY_PLACEHOLDER_IMG_PATH, null, false);
            }
        }
        catch (MacroExecutionException e)
        {
            LOGGER.error("Error generate macro placeholder", e);
        }
        //return default placeholder
        return null;
    }

    private ImagePlaceholder getCountImagePlaceHolder(Map<String, String> params, Type requestType, String requestData)
    {
        String url = requestData;
        ApplicationLink appLink = null;
        String totalIssues;
        try
        {
            String jql = null;
            appLink = applicationLinkResolver.resolve(requestType, requestData, params);
            switch (requestType) {
                case JQL:
                    jql = requestData;
                    break;

                case URL:
                    if (requestData.matches(FILTER_URL_REGEX) || requestData.matches(FILTER_XML_REGEX))
                    {
                        jql = getJQLFromFilter(appLink, url);
                    }
                    else if (requestData.matches(URL_JQL_REGEX))
                    {
                        jql = getJQLFromJQLURL(url);
                    }
                    break;
            }

            if(jql != null)
            {
                url = appLink.getRpcUrl() + XML_SEARCH_REQUEST_URI + "?jqlQuery=" + utf8Encode(jql) + "&tempMax=0";
            }

            boolean forceAnonymous = params.get("anonymous") != null && Boolean.parseBoolean(params.get("anonymous"));
            JiraIssuesManager.Channel channel = jiraIssuesManager.retrieveXMLAsChannel(url, new ArrayList<String>(), appLink, forceAnonymous, false);
            totalIssues = flexigridResponseGenerator.generate(channel, new ArrayList<String>(), 0, true, true);

        }
        catch (CredentialsRequiredException e)
        {
            LOGGER.info("Continues request by anonymous user");
            totalIssues = getTotalIssuesByAnonymous(url, appLink);
        }
        catch (Exception e)
        {
            LOGGER.error("Error generate count macro placeholder: " + e.getMessage(), e);
            totalIssues = "-1";
        }
        return new DefaultImagePlaceholder(PLACEHOLDER_SERVLET + "?totalIssues=" + totalIssues, null, false);
    }

    private String getTotalIssuesByAnonymous(String url, ApplicationLink appLink) {
        try
        {
            JiraIssuesManager.Channel channel = jiraIssuesManager.retrieveXMLAsChannelByAnonymous(
                    url, new ArrayList<String>(), appLink, false, false);
            return  flexigridResponseGenerator.generate(channel, new ArrayList<String>(), 0, true, true);
        }
        catch (Exception e)
        {
            LOGGER.info("Can't retrive issues by anonymous");
            return "-1";
        }
    }

    private ImagePlaceholder getSingleImagePlaceHolder(String key) {
        String macro = resourcePath.contains(JIRA_ISSUES_RESOURCE_PATH) ?
                String.format(JIRA_ISSUES_SINGLE_MACRO_TEMPLATE, key) : String.format(JIRA_SINGLE_MACRO_TEMPLATE, key);
        byte[] encoded = Base64.encodeBase64(macro.getBytes());
        String locale = localeManager.getSiteDefaultLocale().toString();
        String placeHolderUrl = String.format(JIRA_SINGLE_ISSUE_IMG_SERVLET_PATH_TEMPLATE, new String(encoded), locale);

        return new DefaultImagePlaceholder(placeHolderUrl, null, false);
    }

    private JiraIssuesType getJiraIssuesType(Map<String, String> params, Type requestType, String requestData)
    {
        if(requestType == Type.KEY || requestData.matches(XML_KEY_REGEX) || requestData.matches(URL_KEY_REGEX))
        {
            return JiraIssuesType.SINGLE;
        }

        if ("true".equalsIgnoreCase(params.get("count")))
        {
            return JiraIssuesType.COUNT;
        }
        return JiraIssuesType.TABLE;
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

        if(params.containsKey("jqlQuery"))
        {
            return createJiraRequestData(params.get("jqlQuery"), Type.JQL);
        }

        if(params.containsKey("key"))
        {
            return createJiraRequestData(params.get("key"), Type.KEY);
        }

        String requestData = getPrimaryParam(params);
        if (requestData.startsWith("http")) {
            return createJiraRequestData(requestData, Type.URL);
        }

        Matcher keyMatcher = ISSUE_KEY_PATTERN.matcher(requestData);
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
                    boolean staticMode, boolean isMobile, ConversionContext conversionContext) throws MacroExecutionException
    {

        List<String> columnNames = getColumnNames(getParam(params,"columns", PARAM_POSITION_1));
        List<ColumnInfo> columns = getColumnInfo(columnNames);
        contextMap.put("columns", columns);
        String cacheParameter = getParam(params, "cache", PARAM_POSITION_2);

        //Only define the Title param if explicitly defined.
        if (params.containsKey("title"))
        {
            contextMap.put("title", GeneralUtil.htmlEncode(params.get("title")));
        }

        // maybe this should change to position 3 now that the former 3 param
        // got deleted, but that could break
        // backward compatibility of macros currently in use
        String anonymousStr = getParam(params, "anonymous", PARAM_POSITION_4);
        if ("".equals(anonymousStr))
        {
            anonymousStr = "false";
        }

        // and maybe this should change to position 4 -- see comment for
        // anonymousStr above
        String forceTrustWarningsStr = getParam(params, "forceTrustWarnings",
                PARAM_POSITION_5);
        if ("".equals(forceTrustWarningsStr))
        {
            forceTrustWarningsStr = "false";
        }

        String width = params.get("width");
        if(width == null)
        {
            width = DEFAULT_DATA_WIDTH;
        }
        else if(!width.contains("%") && !width.contains("px"))
        {
            width += "px";
        }
        contextMap.put("width", width);

        String heightStr = getParam(params, "height", PARAM_POSITION_6);
        if (!StringUtils.isEmpty(heightStr) && StringUtils.isNumeric(heightStr))
        {
            contextMap.put("height", heightStr);
        }
        
        String showSummaryParam = getParam(params, "showSummary", SUMMARY_PARAM_POSITION);
        if (StringUtils.isEmpty(showSummaryParam))
        {
            contextMap.put("showSummary", true);
        } else
        {
            contextMap.put("showSummary", Boolean.parseBoolean(showSummaryParam));
        }
        

        boolean useCache = StringUtils.isBlank(cacheParameter)
                || cacheParameter.equals("on")
                || Boolean.valueOf(cacheParameter);
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
        contextMap.put("isAdministrator", isAdministrator);
        contextMap.put("isSourceApplink", applink != null);

        // Prepare the maxIssuesToDisplay for velocity template
        String maximumIssuesStr = StringUtils.defaultString(params.get("maximumIssues"), "20");
        int maximumIssues = 20;
        if (staticMode)
        {
            // only affect in static mode otherwise using default value as previous
            maximumIssues = Integer.parseInt(maximumIssuesStr);
            if (maximumIssues > 1000){
                maximumIssues = 1000;
            }
        }
        contextMap.put("maxIssuesToDisplay", maximumIssues);

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

        String baseurl = params.get("baseurl");
        
        String clickableUrl = getClickableUrl(requestData, requestType, applink, baseurl);
        contextMap.put("clickableUrl", clickableUrl);
        
        // this is where the magic happens
        // the `staticMode` variable refers to the "old" plugin when the user was able to choose
        // between Dynamic ( staticMode == false ) and Static mode ( staticMode == true ). For backward compatibily purpose, we are supposed to keep it

        JiraIssuesType issuesType = getJiraIssuesType(params, requestType, requestData);

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
    }

    private void setKeyInContextMap(String requestData, Type requestType, Map<String, Object> contextMap)
    {
        String key = requestData;
        if(requestType == Type.URL)
        {
            key = getKeyFromURL(requestData);
        }
        contextMap.put("key", key);
    }

    private String getKeyFromURL(String url)
    {
        String key = getValueByRegEx(url, XML_KEY_PATTERN, 1);
        if(key != null)
        {
            return key;
        }

        key = getValueByRegEx(url, URL_KEY_PATTERN, 2);
        return key != null ? key : url;
    }

    private String getFilterIdFromURL(String url) throws MacroExecutionException
    {
        String filterId = getValueByRegEx(url, FILTER_URL_PATTERN, 2);
        if(filterId != null)
        {
            return filterId;
        }

        filterId = getValueByRegEx(url, FILTER_XML_PATTERN, 1);
        return filterId != null ? filterId : url;
    }

    private String getValueByRegEx(String data, Pattern pattern, int group)
    {
        Matcher matcher = pattern.matcher(data);
        if(matcher.find())
        {
            return matcher.group(group);
        }

        return null;
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
            setupContextMapForStaticSingleIssue(contextMap, channel);
        }
        catch (CredentialsRequiredException e)
        {
            populateContextMapWhenUserNotMappingToJira(contextMap, url, applink, forceAnonymous, e
                    .getAuthorisationURI().toString(), useCache, conversionContext);
        }
        catch (MalformedRequestException e)
        {
            contextMap.put("isNoPermissionToView", true);
        }
        catch (Exception e)
        {
            throwMacroExecutionException(e, conversionContext);
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
            setupContextMapForStaticSingleIssue(contextMap, channel);
        }
        catch (Exception e)
        {
            throwMacroExecutionException(e, conversionContext);
        }
    }

    private void setupContextMapForStaticSingleIssue(Map<String, Object> contextMap, JiraIssuesManager.Channel channel)
    {
        Element element = channel.getChannelElement();
        Element issue = element.getChild("item");

        contextMap.put("clickableUrl", issue.getChild("link").getValue());
        Element resolution = issue.getChild("resolution");
        contextMap.put("resolved", resolution != null && !"-1".equals(resolution.getAttributeValue("id")));
        contextMap.put("iconUrl", issue.getChild("type").getAttributeValue("iconUrl"));
        contextMap.put("key", issue.getChild("key").getValue());
        contextMap.put("summary", issue.getChild("summary").getValue());
        Element status = issue.getChild("status");
        contextMap.put("status", status.getValue());
        contextMap.put("statusIcon", status.getAttributeValue("iconUrl"));
    }

    private String getXmlUrl(int maximumIssues, String requestData, Type requestType,
            ApplicationLink applink) throws MacroExecutionException {
        StringBuffer sf = new StringBuffer(normalizeUrl(applink.getRpcUrl()));
        sf.append(XML_SEARCH_REQUEST_URI).append("?tempMax=")
                .append(maximumIssues).append("&").append("jqlQuery=");

        switch (requestType) {
        case URL:
            if (requestData.matches(FILTER_XML_REGEX) || requestData.matches(FILTER_URL_REGEX))
            {
                String jql = getJQLFromFilter(applink, requestData);
                sf.append(utf8Encode(jql));
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
                String jql = getJQLFromJQLURL(requestData);
                if (jql != null)
                {
                    sf.append(utf8Encode(jql));
                    return sf.toString();
                }
                else if(requestData.matches(URL_KEY_REGEX) || requestData.matches(XML_KEY_REGEX))
                {
                        String key = getKeyFromURL(requestData);
                        return buildKeyJiraUrl(key, applink);
                }
            }
        case JQL:
            sf.append(utf8Encode(requestData));
            return sf.toString();
        case KEY:
            return buildKeyJiraUrl(requestData, applink);

        }
        throw new MacroExecutionException("Invalid url");
    }

    private String getJQLFromFilter(ApplicationLink appLink, String url) throws MacroExecutionException {
        String filterId = getFilterIdFromURL(url);
        try {
            return jiraIssuesManager.retrieveJQLFromFilter(filterId, appLink);
        }
        catch (ResponseException e)
        {
            throw new MacroExecutionException(getText("insert.jira.issue.message.nofilter"), e);
        }
    }

    private String buildKeyJiraUrl(String key, ApplicationLink applink)
    {
        String encodedQuery = utf8Encode("key in (" + key + ")");
        return normalizeUrl(applink.getRpcUrl())
                + "/sr/jira.issueviews:searchrequest-xml/temp/SearchRequest.xml?jqlQuery="
                + encodedQuery;
    }

    private String getJQLFromJQLURL(String requestData)
    {
        String jql = getValueByRegEx(requestData, URL_JQL_PATTERN, 2);
        if(jql != null)
        {
            try
            {
                // make sure we won't encode it twice
                jql = URLDecoder.decode(jql, "UTF-8");
            } catch (UnsupportedEncodingException e)
            {
                LOGGER.warn("unable to decode jql: " + jql);
            }
        }
        return jql;
    }

    private String normalizeUrl(URI rpcUrl) {
        String baseUrl = rpcUrl.toString();
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    private String getClickableUrl(String requestData, Type requestType,
            ApplicationLink applink, String baseurl)

    {
        String clickableUrl = null;
        switch (requestType) 
        {
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
        {
            clickableUrl = rebaseUrl(clickableUrl, baseurl.trim());
        }
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
    private void throwMacroExecutionException(Exception exception, ConversionContext conversionContext)
            throws MacroExecutionException {
        String i18nKey = null;
        List params = null;

        if (exception instanceof UnknownHostException) {
            i18nKey = "jiraissues.error.unknownhost";
            params = Arrays.asList(StringUtils.defaultString(exception.getMessage()));
        } else if (exception instanceof ConnectException) {
            i18nKey = "jiraissues.error.unabletoconnect";
            params = Arrays.asList(StringUtils.defaultString(exception.getMessage()));
        } else if (exception instanceof AuthenticationException) {
            i18nKey = "jiraissues.error.authenticationerror";
        } else if (exception instanceof MalformedRequestException) {
            // JIRA returns 400 HTTP code when it should have been a 401
            i18nKey = "jiraissues.error.notpermitted";
        } else if (exception instanceof TrustedAppsException) {
            i18nKey = "jiraissues.error.trustedapps";
            params = Collections.singletonList(exception.getMessage());
        }

        if (i18nKey != null)
        {
            String msg = getText(getText(i18nKey, params));
            LOGGER.warn(msg);
            LOGGER.debug("More info : ", exception);
            throw new MacroExecutionException(msg, exception);
        }
        else
        {
            if ( ! ConversionContextOutputType.FEED.value().equals(conversionContext.getOutputType()))
            {
                LOGGER.error("Macro execution exception: ", exception);
            }
            throw new MacroExecutionException(exception);
        }
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
        try
        {
            JiraIssuesManager.Channel channel = jiraIssuesManager.retrieveXMLAsChannel(url, columnNames, appLink,
                    forceAnonymous, useCache);
            setupContextMapForStaticTable(contextMap, channel);
        }
        catch (CredentialsRequiredException e)
        {
            populateContextMapForStaticTableByAnonymous(contextMap, columnNames, url, appLink, forceAnonymous, useCache);
            contextMap.put("xmlXformer", xmlXformer);
            contextMap.put("jiraIssuesManager", jiraIssuesManager);
            contextMap.put("jiraIssuesColumnManager", jiraIssuesColumnManager);
            contextMap.put("oAuthUrl", e.getAuthorisationURI().toString());
        }
        catch (MalformedRequestException e)
        {
            LOGGER.info("Can't get issues because issues key is not exist or user doesn't have permission to view", e);
        }
        catch (Exception e)
        {
            throwMacroExecutionException(e, conversionContext);
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
            setupContextMapForStaticTable(contextMap, channel);
        }
        catch (Exception e)
        {
            LOGGER.warn("can't get jira issues by anonymous user", e);
        }
    }

    private void setupContextMapForStaticTable(Map<String, Object> contextMap, JiraIssuesManager.Channel channel)
    {
        Element element = channel.getChannelElement();
        contextMap.put("trustedConnection", channel.isTrustedConnection());
        contextMap.put("trustedConnectionStatus", channel.getTrustedConnectionStatus());
        contextMap.put("channel", element);
        contextMap.put("entries", element.getChildren("item"));
        try
        {
            if(element.getChild("issue") != null && element.getChild("issue").getAttribute("total") != null)
            {
                contextMap.put("totalIssues", element.getChild("issue").getAttribute("total").getIntValue());
            }
        }
        catch (DataConversionException e)
        {
            contextMap.put("totalIssues", element.getChildren("item").size());
        }
        contextMap.put("xmlXformer", xmlXformer);
        contextMap.put("jiraIssuesManager", jiraIssuesManager);
        contextMap.put("jiraIssuesColumnManager", jiraIssuesColumnManager);
        contextMap.put("jiraIssuesDateFormatter", jiraIssuesDateFormatter);
        contextMap.put("userLocale", getUserLocale(element.getChildText("language")));
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

            contextMap.put("count", count);
            contextMap.put("resultsPerPage", getResultsPerPageParam(new StringBuffer(url)));
            contextMap.put("useCache", useCache);
            contextMap.put("retrieverUrlHtml", buildRetrieverUrl(getColumnInfo(columnNames), url, appLink, forceAnonymous));
        }
        catch (CredentialsRequiredException e)
        {
            contextMap.put("count", getCountIssuesWithAnonymous(url, columnNames, appLink, forceAnonymous, useCache));
            contextMap.put("oAuthUrl", e.getAuthorisationURI().toString());
        }
        catch (MalformedRequestException e)
        {
            contextMap.put("count", DEFAULT_JIRA_ISSUES_COUNT);
        }
        catch (Exception e)
        {
            throwMacroExecutionException(e, conversionContext);
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
                   Map<String, String> params, Map<String, Object> contextMap, List<ColumnInfo> columns,
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

   protected String getParam(Map<String, String> params, String paramName, int paramPosition)
    {
        String param = params.get(paramName);
        if (param == null)
        {
            param = StringUtils.defaultString(params.get(String.valueOf(paramPosition)));
        }

        return param.trim();
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
            if(!StringUtils.isBlank(key) && !MACRO_PARAMS.contains(key)) {
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
            {
                displayName = columnName;
            }

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
        String baseUrl = settingsManager.getGlobalSettings().getBaseUrl();
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
            return !NO_WRAPPED_TEXT_FIELDS.contains(getKey().toLowerCase());
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
        JiraRequestData jiraRequestData = parseRequestData(parameters);
        String requestData = jiraRequestData.getRequestData();
        Type requestType = jiraRequestData.getRequestType();
        ApplicationLink applink = null;
        try 
        {
            applink = applicationLinkResolver.resolve(requestType, requestData, parameters);
        } 
        catch (MacroExecutionException mee)
        {
            // ignore this, we'll try to treat this as anonymous request IF url parameter is provided.
        }
        
        try
        {
            Map<String, Object> contextMap = MacroUtils.defaultVelocityContext();
            JiraIssuesType issuesType = getJiraIssuesType(parameters, requestType, requestData);
            parameters.put(TOKEN_TYPE_PARAM, issuesType == JiraIssuesType.COUNT || requestType == Type.KEY ? TokenType.INLINE.name() : TokenType.BLOCK.name());
            boolean staticMode = shouldRenderInHtml(parameters.get(RENDER_MODE_PARAM), conversionContext);
            boolean isMobile = "mobile".equals(conversionContext.getOutputDeviceType());
            createContextMapFromParams(parameters, contextMap, requestData, requestType, applink, staticMode, isMobile, conversionContext);

            if(isMobile) {
                webResourceManager.requireResource("confluence.extra.jira:mobile-browser-resources");
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

    public void setSettingsManager(SettingsManager settingsManager)
    {
        this.settingsManager = settingsManager;
    }

}
