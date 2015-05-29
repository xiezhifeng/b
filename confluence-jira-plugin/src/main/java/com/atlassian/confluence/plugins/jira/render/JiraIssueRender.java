package com.atlassian.confluence.plugins.jira.render;

import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.applinks.api.TypeNotInstalledException;
import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.extra.jira.*;
import com.atlassian.confluence.extra.jira.JiraIssuesMacro.JiraIssuesType;
import com.atlassian.confluence.extra.jira.exception.JiraIssueMacroException;
import com.atlassian.confluence.extra.jira.helper.JiraExceptionHelper;
import com.atlassian.confluence.extra.jira.helper.JiraIssueSortableHelper;
import com.atlassian.confluence.extra.jira.helper.JiraJqlHelper;
import com.atlassian.confluence.extra.jira.model.JiraColumnInfo;
import com.atlassian.confluence.extra.jira.util.JiraIssuePdfExportUtil;
import com.atlassian.confluence.extra.jira.util.JiraUtil;
import com.atlassian.confluence.languages.LocaleManager;
import com.atlassian.confluence.macro.ImagePlaceholder;
import com.atlassian.confluence.macro.MacroExecutionException;
import com.atlassian.confluence.renderer.radeox.macros.MacroUtils;
import com.atlassian.confluence.security.Permission;
import com.atlassian.confluence.security.PermissionManager;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.confluence.util.GeneralUtil;
import com.atlassian.confluence.util.i18n.I18NBean;
import com.atlassian.confluence.util.i18n.I18NBeanFactory;
import com.atlassian.renderer.RenderContext;
import com.atlassian.renderer.TokenType;
import org.apache.commons.lang.StringUtils;

import java.util.List;
import java.util.Map;

/**
 * Base class for render jira issue
 */
public abstract class JiraIssueRender
{

    public static final String ISSUE_TYPE = "issueType";
    protected static final String TEMPLATE_MOBILE_PATH = "templates/mobile/extra/jira";
    protected static final String TEMPLATE_PATH = "templates/extra/jira";
    protected static final String EMAIL_RENDER = "email";
    protected static final String PDF_EXPORT = "pdfExport";

    private static final String IS_ADMINISTRATOR = "isAdministrator";
    private static final String IS_SOURCE_APP_LINK = "isSourceApplink";
    private static final String MAX_ISSUES_TO_DISPLAY = "maxIssuesToDisplay";
    private static final String BASE_URL = "baseurl";
    private static final String MAXIMUM_ISSUES = "maximumIssues";
    private static final String DEFAULT_DATA_WIDTH = "100%";

    private I18NBeanFactory i18NBeanFactory;

    protected ApplicationLinkResolver applicationLinkResolver;

    protected JiraExceptionHelper jiraExceptionHelper;

    protected LocaleManager localeManager;

    protected JiraIssuesManager jiraIssuesManager;

    protected JiraIssuesColumnManager jiraIssuesColumnManager;

    protected JiraIssueSortingManager jiraIssueSortingManager;

    protected PermissionManager permissionManager;

    private TrustedApplicationConfig trustedApplicationConfig;

    String getText(String i18n)
    {
        return getI18NBean().getText(i18n);
    }

    public String renderMacro(JiraRequestData jiraRequestData, Map<String, String> parameters, ConversionContext context) throws MacroExecutionException
    {
        Map<String, Object> contextMap = null;
        try
        {
            ApplicationLink applink = null;
            String requestData = jiraRequestData.getRequestData();
            JiraIssuesMacro.Type requestType = jiraRequestData.getRequestType();
            contextMap = MacroUtils.defaultVelocityContext();
            JiraIssuesType issuesType = JiraUtil.getJiraIssuesType(parameters, jiraRequestData);
            contextMap.put(ISSUE_TYPE, issuesType);
            List<String> columnNames = JiraIssueSortableHelper.getColumnNames(JiraUtil.getParamValue(parameters, JiraIssuesMacro.COLUMNS, JiraUtil.PARAM_POSITION_1));
            // it will be overided by below code. At here, we need default column first for exception case.
            contextMap.put(JiraIssuesMacro.COLUMNS, columnNames);
            try
            {
                applink = applicationLinkResolver.resolve(requestType, requestData, parameters);
            }
            catch (TypeNotInstalledException tne)
            {
                jiraExceptionHelper.throwMacroExecutionException(tne, context);
            }

            //TODO: why we need handle it if issue type is single or count
            Map<String, JiraColumnInfo> jiraColumns = jiraIssuesColumnManager.getColumnsInfoFromJira(applink);
            jiraRequestData.setRequestData(jiraIssueSortingManager.getRequestDataForSorting(parameters, requestData, requestType, jiraColumns, context, applink));
            parameters.put(JiraIssuesMacro.TOKEN_TYPE_PARAM, issuesType == JiraIssuesType.COUNT || requestType == JiraIssuesMacro.Type.KEY ? TokenType.INLINE.name() : TokenType.BLOCK.name());
            boolean isMobile = JiraIssuesMacro.MOBILE.equals(context.getOutputDeviceType());

            if (issuesType == JiraIssuesType.SINGLE)
            {
                contextMap.put(JiraIssuesMacro.KEY, getKeyFromRequest(jiraRequestData));
            }

            setupCommonContextMap(parameters, contextMap, jiraRequestData, applink, jiraColumns, issuesType, context);

            if (isMobile)
            {
                return getMobileTemplate(contextMap);
            }
            else
            {
                return getTemplate(contextMap);
            }
        }
        catch (Exception e)
        {
            throw new JiraIssueMacroException(e, contextMap);
        }
    }

    /**
     * Return the image placeholder
     * @param jiraRequestData
     * @param parameters parameters
     * @param resourcePath path to image
     * @return ImagePlaceholder
     */
    public abstract ImagePlaceholder getImagePlaceholder(JiraRequestData jiraRequestData, Map<String, String> parameters, String resourcePath);

    /**
     * Get mobile template path to render
     * @param contextMap
     * @return mobile template path
     */
    public abstract String getMobileTemplate(final Map<String, Object> contextMap);

    /**
     * Get template for desktop/pdf path to render
     * @param contextMap
     * @return template path
     */
    public abstract String getTemplate(final Map<String, Object> contextMap);

    //TODO: refactor this function
    public void setupCommonContextMap(Map<String, String> params, Map<String, Object> contextMap,
                                       JiraRequestData jiraRequestData, ApplicationLink applink,
                                       Map<String,JiraColumnInfo> jiraColumns, JiraIssuesType issuesType, ConversionContext conversionContext) throws MacroExecutionException
    {
        String clickableUrl = getClickableUrl(jiraRequestData, applink, params.get(BASE_URL));
        contextMap.put(JiraIssuesMacro.CLICKABLE_URL, clickableUrl);



        //TODO: review COLUMNS object need for single/count or not
        List<String> columnNames = JiraIssueSortableHelper.getColumnNames(JiraUtil.getParamValue(params, JiraIssuesMacro.COLUMNS, JiraUtil.PARAM_POSITION_1));
        List<JiraColumnInfo> columns = jiraIssuesColumnManager.getColumnInfo(params, jiraColumns, applink);
        contextMap.put(JiraIssuesMacro.COLUMNS, columns);

        String cacheParameter = JiraUtil.getParamValue(params, JiraIssuesMacro.CACHE, JiraUtil.PARAM_POSITION_2);
        // added parameters for pdf export
        if (RenderContext.PDF.equals(conversionContext.getOutputType()))
        {
            contextMap.put(PDF_EXPORT, Boolean.TRUE);
            JiraIssuePdfExportUtil.addedHelperDataForPdfExport(contextMap, columnNames != null ? columnNames.size() : 0);
        }
        //Only define the Title param if explicitly defined.
        if (params.containsKey(JiraIssuesMacro.TITLE))
        {
            contextMap.put(JiraIssuesMacro.TITLE, GeneralUtil.htmlEncode(params.get(JiraIssuesMacro.TITLE)));
        }

        if (RenderContext.EMAIL.equals(conversionContext.getOutputType()))
        {
            contextMap.put(EMAIL_RENDER, Boolean.TRUE);
        }
        // maybe this should change to position 3 now that the former 3 param
        // got deleted, but that could break
        // backward compatibility of macros currently in use
        String anonymousStr = JiraUtil.getParamValue(params, JiraIssuesMacro.ANONYMOUS, JiraUtil.PARAM_POSITION_4);
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

        String width = params.get(JiraIssuesMacro.WIDTH);
        if (width == null)
        {
            width = DEFAULT_DATA_WIDTH;
        }
        else if(!width.contains("%") && !width.contains("px"))
        {
            width += "px";
        }
        contextMap.put(JiraIssuesMacro.WIDTH, width);

        String heightStr = JiraUtil.getParamValue(params, JiraIssuesMacro.HEIGHT, JiraUtil.PARAM_POSITION_6);
        if (!StringUtils.isEmpty(heightStr) && StringUtils.isNumeric(heightStr))
        {
            contextMap.put(JiraIssuesMacro.HEIGHT, heightStr);
        }

        String showSummaryParam = JiraUtil.getParamValue(params, JiraIssuesMacro.SHOW_SUMMARY, JiraUtil.SUMMARY_PARAM_POSITION);
        if (StringUtils.isEmpty(showSummaryParam))
        {
            contextMap.put(JiraIssuesMacro.SHOW_SUMMARY, true);
        }
        else
        {
            contextMap.put(JiraIssuesMacro.SHOW_SUMMARY, Boolean.parseBoolean(showSummaryParam));
        }

        boolean forceAnonymous = Boolean.valueOf(anonymousStr)
                || (jiraRequestData.getRequestType() == JiraIssuesMacro.Type.URL && SeraphUtils.isUserNamePasswordProvided(jiraRequestData.getRequestData()));

        // support rendering macros which were created without applink by legacy macro
        if (applink == null)
        {
            forceAnonymous = true;
        }

        boolean showTrustWarnings = Boolean.valueOf(forceTrustWarningsStr) || isTrustWarningsEnabled();
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
        if (jiraRequestData.isStaticMode())
        {
            String maximumIssuesStr = StringUtils.defaultString(params.get(MAXIMUM_ISSUES), String.valueOf(JiraUtil.DEFAULT_NUMBER_OF_ISSUES));
            // only affect in static mode otherwise using default value as previous
            maximumIssues = JiraUtil.getMaximumIssues(maximumIssuesStr);
        }
        contextMap.put(MAX_ISSUES_TO_DISPLAY, maximumIssues);

        String url = null;
        if (applink != null)
        {
            url = getXmlUrl(maximumIssues, jiraRequestData.getRequestData(), jiraRequestData.getRequestType(), applink);
        } else if (jiraRequestData.getRequestType() == JiraIssuesMacro.Type.URL)
        {
            url = jiraRequestData.getRequestData();
        }

        // support querying with 'no applink' ONLY IF we have base url
        if (url == null && applink == null)
        {
            throw new MacroExecutionException(getText("jiraissues.error.noapplinks"));
        }

        //add returnMax parameter to retrieve the limitation of jira issues returned
        contextMap.put("returnMax", "true");

        boolean userAuthenticated = AuthenticatedUserThreadLocal.get() != null;
        boolean useCache;
        if (JiraIssuesType.TABLE.equals(issuesType) && !JiraJqlHelper.isJqlKeyType(jiraRequestData.getRequestData()))
        {
            useCache = StringUtils.isBlank(cacheParameter)
                    || cacheParameter.equals("on")
                    || Boolean.valueOf(cacheParameter);
        }
        else
        {
            useCache = userAuthenticated ? forceAnonymous : true; // always cache single issue and count if user is not authenticated
        }

        //SpecificType
        populateSpecifyMacroType(contextMap, columnNames, url, applink, forceAnonymous, useCache, conversionContext, jiraRequestData, params);
    }

    public abstract void populateSpecifyMacroType(Map<String, Object> contextMap, List<String> columnNames, String url,
                                                  ApplicationLink appLink, boolean forceAnonymous, boolean useCache, ConversionContext conversionContext, JiraRequestData jiraRequestData, Map<String, String> params) throws MacroExecutionException;

    protected String getKeyFromRequest(JiraRequestData jiraRequestData)
    {
        String key = jiraRequestData.getRequestData();
        if(jiraRequestData.getRequestType() == JiraIssuesMacro.Type.URL)
        {
            key = JiraJqlHelper.getKeyFromURL(jiraRequestData.getRequestData());
        }
        return key;
    }

    private String getXmlUrl(int maximumIssues, String requestData, JiraIssuesMacro.Type requestType,
                             ApplicationLink applicationLink) throws MacroExecutionException {
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

    private boolean isTrustWarningsEnabled()
    {
        return null != trustedApplicationConfig && trustedApplicationConfig.isTrustWarningsEnabled();
    }

    public void setLocaleManager(LocaleManager localeManager)
    {
        this.localeManager = localeManager;
    }

    public void setJiraIssuesManager(JiraIssuesManager jiraIssuesManager)
    {
        this.jiraIssuesManager = jiraIssuesManager;
    }

    public void setI18NBeanFactory(I18NBeanFactory i18NBeanFactory)
    {
        this.i18NBeanFactory = i18NBeanFactory;
    }

    public void setApplicationLinkResolver(ApplicationLinkResolver applicationLinkResolver)
    {
        this.applicationLinkResolver = applicationLinkResolver;
    }

    public void setJiraExceptionHelper(JiraExceptionHelper jiraExceptionHelper)
    {
        this.jiraExceptionHelper = jiraExceptionHelper;
    }

    public I18NBean getI18NBean()
    {
        if (null != AuthenticatedUserThreadLocal.get())
        {
            return i18NBeanFactory.getI18NBean(localeManager.getLocale(AuthenticatedUserThreadLocal.get()));
        }
        return i18NBeanFactory.getI18NBean();
    }

    public void setJiraIssueSortingManager(JiraIssueSortingManager jiraIssueSortingManager)
    {
        this.jiraIssueSortingManager = jiraIssueSortingManager;
    }

    public void setJiraIssuesColumnManager(JiraIssuesColumnManager jiraIssuesColumnManager)
    {
        this.jiraIssuesColumnManager = jiraIssuesColumnManager;
    }

    public void setPermissionManager(PermissionManager permissionManager)
    {
        this.permissionManager = permissionManager;
    }

    public void setTrustedApplicationConfig(TrustedApplicationConfig trustedApplicationConfig)
    {
        this.trustedApplicationConfig = trustedApplicationConfig;
    }

    private String getClickableUrl(JiraRequestData jiraRequestData, ApplicationLink applicationLink, String baseurl)
    {
        String clickableUrl = null;
        switch (jiraRequestData.getRequestType())
        {
            case URL:
                clickableUrl = makeClickableUrl(jiraRequestData.getRequestData());
                break;
            case JQL:
                clickableUrl = JiraUtil.normalizeUrl(applicationLink.getDisplayUrl())
                        + "/secure/IssueNavigator.jspa?reset=true&jqlQuery="
                        + JiraUtil.utf8Encode(jiraRequestData.getRequestData());
                break;
            case KEY:
                clickableUrl = JiraUtil.normalizeUrl(applicationLink.getDisplayUrl()) + "/browse/"
                        + JiraUtil.utf8Encode(jiraRequestData.getRequestData());
                break;
        }
        if (StringUtils.isNotEmpty(baseurl))
        {
            clickableUrl = rebaseUrl(clickableUrl, baseurl.trim());
        }
        return appendSourceParam(clickableUrl);
    }

    private String buildKeyJiraUrl(String key, ApplicationLink applicationLink)
    {
        String encodedQuery = JiraUtil.utf8Encode("key in (" + key + ")");
        return JiraUtil.normalizeUrl(applicationLink.getRpcUrl())
                + "/sr/jira.issueviews:searchrequest-xml/temp/SearchRequest.xml?jqlQuery="
                + encodedQuery + "&returnMax=true";
    }

    private String appendSourceParam(String clickableUrl)
    {
        String operator = clickableUrl.contains("?") ? "&" : "?";
        return clickableUrl + operator + "src=confmacro";
    }

    private String rebaseUrl(String clickableUrl, String baseUrl)
    {
        return clickableUrl.replaceFirst("^" + // only at start of string
                        ".*?" + // minimum number of characters (the schema) followed
                        // by...
                        "://" + // literally: colon-slash-slash
                        "[^/]+", // one or more non-slash characters (the hostname)
                baseUrl);
    }

    public static String makeClickableUrl(String url)
    {
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
        linkString = linkString.replaceFirst("sr/jira.issueviews:searchrequest-xml/temp/SearchRequest.xml\\?", "secure/IssueNavigator.jspa?reset=true&");
        linkString = linkString.replaceFirst("sr/jira.issueviews:searchrequest-xml/temp/SearchRequest.xml", "secure/IssueNavigator.jspa?reset=true");
        linkString = linkString.replaceFirst("sr/jira.issueviews:searchrequest-xml/[0-9]+/SearchRequest-([0-9]+).xml\\?", "secure/IssueNavigator.jspa?requestId=$1&");
        linkString = linkString.replaceFirst("sr/jira.issueviews:searchrequest-xml/[0-9]+/SearchRequest-([0-9]+).xml", "secure/IssueNavigator.jspa?requestId=$1");
        return linkString;
    }

    public static String filterOutParam(StringBuffer baseUrl, final String filter) {
        int tempMaxParamLocation = baseUrl.indexOf(filter);
        if (tempMaxParamLocation != -1)
        {
            String value;
            int nextParam = baseUrl.indexOf("&", tempMaxParamLocation);
            // finding start of next param, if there is one. can't be ? because
            // filter
            // is before any next param
            if (nextParam != -1)
            {
                value = baseUrl.substring(tempMaxParamLocation + filter.length(), nextParam);
                baseUrl.delete(tempMaxParamLocation, nextParam + 1);
            }
            else
            {
                value = baseUrl.substring(tempMaxParamLocation + filter.length(),
                        baseUrl.length());
                // tempMaxParamLocation-1 to remove ?/& since
                // it won't be used by next param in this case

                baseUrl.delete(tempMaxParamLocation - 1, baseUrl.length());
            }
            return value;
        }
        else
        {
            return null;
        }
    }

}
