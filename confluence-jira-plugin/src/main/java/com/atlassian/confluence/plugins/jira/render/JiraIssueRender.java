package com.atlassian.confluence.plugins.jira.render;

import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.applinks.api.TypeNotInstalledException;
import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.extra.jira.*;
import com.atlassian.confluence.extra.jira.JiraIssuesMacro.JiraIssuesType;
import com.atlassian.confluence.extra.jira.exception.JiraIssueMacroException;
import com.atlassian.confluence.extra.jira.helper.JiraContextMapSetupHelper;
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

    public String renderMacro(JiraRequestData jiraRequest, ConversionContext context) throws MacroExecutionException
    {
        Map<String, Object> contextMap = null;
        try
        {
            ApplicationLink applink = null;
            JiraIssuesMacro.Type requestType = jiraRequest.getRequestType();
            Map<String, String> parameters = jiraRequest.getParameters();
            try
            {
                applink = applicationLinkResolver.resolve(jiraRequest);
            }
            catch (TypeNotInstalledException tne)
            {
                jiraExceptionHelper.throwMacroExecutionException(tne, context);
            }
            contextMap = MacroUtils.defaultVelocityContext();
            contextMap.put(ISSUE_TYPE, jiraRequest.getIssuesType());
            parameters.put(JiraIssuesMacro.TOKEN_TYPE_PARAM, jiraRequest.getIssuesType() == JiraIssuesType.COUNT || requestType == JiraIssuesMacro.Type.KEY ? TokenType.INLINE.name() : TokenType.BLOCK.name());

            if (jiraRequest.getIssuesType() == JiraIssuesType.SINGLE)
            {
                contextMap.put(JiraIssuesMacro.KEY, getKeyFromRequest(jiraRequest));
            }
            String clickableUrl = JiraIssueUtil.getClickableUrl(jiraRequest, applink, parameters.get(BASE_URL));
            contextMap.put(JiraIssuesMacro.CLICKABLE_URL, clickableUrl);

            setupCommonContextMap(contextMap, jiraRequest, applink, context);

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
    public void setupCommonContextMap(Map<String, Object> contextMap,
                                       JiraRequestData jiraRequestData, ApplicationLink applink,
                                       ConversionContext conversionContext) throws MacroExecutionException
    {
        Map<String, String> params = jiraRequestData.getParameters();

        if (RenderContext.EMAIL.equals(conversionContext.getOutputType()))
        {
            contextMap.put(EMAIL_RENDER, Boolean.TRUE);
        }

        JiraContextMapSetupHelper.setupForceAnonymousAndUseCache(jiraRequestData, applink);

        // and maybe this should change to position 4 -- see comment for
        // anonymousStr above
        String forceTrustWarningsStr = JiraUtil.getParamValue(params, "forceTrustWarnings", JiraUtil.PARAM_POSITION_5);
        if ("".equals(forceTrustWarningsStr))
        {
            forceTrustWarningsStr = "false";
        }

        String showSummaryParam = JiraUtil.getParamValue(params, JiraIssuesMacro.SHOW_SUMMARY, JiraUtil.SUMMARY_PARAM_POSITION);
        contextMap.put(JiraIssuesMacro.SHOW_SUMMARY, StringUtils.isEmpty(showSummaryParam) ? true : Boolean.parseBoolean(showSummaryParam));

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

        JiraContextMapSetupHelper.setupURL(jiraRequestData, applink, maximumIssues, getI18NBean(), jiraIssuesManager);

        //add returnMax parameter to retrieve the limitation of jira issues returned
        contextMap.put("returnMax", "true");

        //SpecificType
        populateSpecifyMacroType(contextMap, applink, conversionContext, jiraRequestData);
    }

    public abstract void populateSpecifyMacroType(Map<String, Object> contextMap, ApplicationLink appLink, ConversionContext conversionContext, JiraRequestData jiraRequestData)
            throws MacroExecutionException;

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
