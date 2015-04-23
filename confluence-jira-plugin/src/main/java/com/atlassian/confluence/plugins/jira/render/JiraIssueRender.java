package com.atlassian.confluence.plugins.jira.render;

import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.applinks.api.TypeNotInstalledException;
import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.extra.jira.*;
import com.atlassian.confluence.extra.jira.JiraIssuesMacro.JiraIssuesType;
import com.atlassian.confluence.extra.jira.exception.JiraIssueMacroException;
import com.atlassian.confluence.extra.jira.helper.JiraExceptionHelper;
import com.atlassian.confluence.extra.jira.helper.JiraJqlHelper;
import com.atlassian.confluence.extra.jira.util.JiraIssueUtil;
import com.atlassian.confluence.extra.jira.util.JiraUtil;
import com.atlassian.confluence.languages.LocaleManager;
import com.atlassian.confluence.macro.ImagePlaceholder;
import com.atlassian.confluence.macro.MacroExecutionException;
import com.atlassian.confluence.renderer.radeox.macros.MacroUtils;
import com.atlassian.confluence.security.Permission;
import com.atlassian.confluence.security.PermissionManager;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.confluence.util.i18n.I18NBean;
import com.atlassian.confluence.util.i18n.I18NBeanFactory;
import com.atlassian.renderer.RenderContext;
import com.atlassian.renderer.TokenType;
import org.apache.commons.lang.StringUtils;
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

    private I18NBeanFactory i18NBeanFactory;

    protected ApplicationLinkResolver applicationLinkResolver;

    protected JiraExceptionHelper jiraExceptionHelper;

    protected LocaleManager localeManager;

    protected JiraIssuesManager jiraIssuesManager;

    protected PermissionManager permissionManager;

    private TrustedApplicationConfig trustedApplicationConfig;

    String getText(String i18n)
    {
        return getI18NBean().getText(i18n);
    }

    public String renderMacro(JiraRequestData jiraRequest, Map<String, String> parameters, ConversionContext context) throws MacroExecutionException
    {
        Map<String, Object> contextMap = null;
        try
        {
            ApplicationLink applink = null;
            String requestData = jiraRequest.getRequestData();
            JiraIssuesMacro.Type requestType = jiraRequest.getRequestType();
            try
            {
                applink = applicationLinkResolver.resolve(requestType, requestData, parameters);
            }
            catch (TypeNotInstalledException tne)
            {
                jiraExceptionHelper.throwMacroExecutionException(tne, context);
            }
            contextMap = MacroUtils.defaultVelocityContext();
            jiraRequest.setIssuesType(JiraUtil.getJiraIssuesType(parameters, jiraRequest));
            contextMap.put(ISSUE_TYPE, jiraRequest.getIssuesType());
            parameters.put(JiraIssuesMacro.TOKEN_TYPE_PARAM, jiraRequest.getIssuesType() == JiraIssuesType.COUNT || requestType == JiraIssuesMacro.Type.KEY ? TokenType.INLINE.name() : TokenType.BLOCK.name());

            if (jiraRequest.getIssuesType() == JiraIssuesType.SINGLE)
            {
                contextMap.put(JiraIssuesMacro.KEY, getKeyFromRequest(jiraRequest));
            }
            String clickableUrl = JiraIssueUtil.getClickableUrl(jiraRequest, applink, parameters.get(BASE_URL));
            contextMap.put(JiraIssuesMacro.CLICKABLE_URL, clickableUrl);

            setupCommonContextMap(parameters, contextMap, jiraRequest, applink, context);

            return getTemplate(contextMap, JiraIssuesMacro.MOBILE.equals(context.getOutputDeviceType()));
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
     * Get template for desktop/pdf path to render
     * @param contextMap
     * @return template path
     */
    public abstract String getTemplate(final Map<String, Object> contextMap, boolean isMobileMode);

    //TODO: refactor this function
    public void setupCommonContextMap(Map<String, String> params, Map<String, Object> contextMap,
                                       JiraRequestData jiraRequestData, ApplicationLink applink,
                                       ConversionContext conversionContext) throws MacroExecutionException
    {
        String cacheParameter = JiraUtil.getParamValue(params, JiraIssuesMacro.CACHE, JiraUtil.PARAM_POSITION_2);

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
        String forceTrustWarningsStr = JiraUtil.getParamValue(params, "forceTrustWarnings", JiraUtil.PARAM_POSITION_5);
        if ("".equals(forceTrustWarningsStr))
        {
            forceTrustWarningsStr = "false";
        }

        String showSummaryParam = JiraUtil.getParamValue(params, JiraIssuesMacro.SHOW_SUMMARY, JiraUtil.SUMMARY_PARAM_POSITION);
        contextMap.put(JiraIssuesMacro.SHOW_SUMMARY, StringUtils.isEmpty(showSummaryParam) ? true : Boolean.parseBoolean(showSummaryParam));


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
        boolean isAdministrator = permissionManager.hasPermission(AuthenticatedUserThreadLocal.getUser(), Permission.ADMINISTER, PermissionManager.TARGET_APPLICATION);
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
            url = JiraJqlHelper.getXmlUrl(maximumIssues, jiraRequestData, jiraIssuesManager, getI18NBean(), applink);
        }
        else if (jiraRequestData.getRequestType() == JiraIssuesMacro.Type.URL)
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
        if (JiraIssuesType.TABLE.equals(jiraRequestData.getIssuesType()) && !JiraJqlHelper.isJqlKeyType(jiraRequestData.getRequestData()))
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
        populateSpecifyMacroType(contextMap, url, applink, forceAnonymous, useCache, conversionContext, jiraRequestData, params);
    }

    public abstract void populateSpecifyMacroType(Map<String, Object> contextMap, String url,
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

    public void setPermissionManager(PermissionManager permissionManager)
    {
        this.permissionManager = permissionManager;
    }

    public void setTrustedApplicationConfig(TrustedApplicationConfig trustedApplicationConfig)
    {
        this.trustedApplicationConfig = trustedApplicationConfig;
    }
}
