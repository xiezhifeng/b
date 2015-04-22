package com.atlassian.confluence.extra.jira.helper;

import java.net.ConnectException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.content.render.xhtml.ConversionContextOutputType;
import com.atlassian.confluence.extra.jira.ApplicationLinkResolver;
import com.atlassian.confluence.extra.jira.JiraIssuesMacro;
import com.atlassian.confluence.extra.jira.TrustedAppsException;
import com.atlassian.confluence.extra.jira.exception.AuthenticationException;
import com.atlassian.confluence.extra.jira.exception.JiraIssueMacroException;
import com.atlassian.confluence.extra.jira.exception.MalformedRequestException;
import com.atlassian.confluence.extra.jira.util.JiraIssueUtil;
import com.atlassian.confluence.extra.jira.util.JiraUtil;
import com.atlassian.confluence.languages.LocaleManager;
import com.atlassian.confluence.macro.MacroExecutionException;
import com.atlassian.confluence.plugins.jira.render.JiraIssueRender;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.confluence.util.i18n.I18NBean;
import com.atlassian.confluence.util.i18n.I18NBeanFactory;
import com.atlassian.confluence.util.velocity.VelocityUtils;

import com.atlassian.applinks.api.TypeNotInstalledException;

import com.google.common.collect.Maps;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

/**
 * This class responsible for converting a specific JIRA Issues Macros' exception
 * into the generic MacroExecutionException
 */
public class JiraExceptionHelper
{

    private static final Logger LOGGER = Logger.getLogger(JiraExceptionHelper.class);
    private static final String MACRO_NAME = "macroName";

    private final I18NBeanFactory i18NBeanFactory;
    private final LocaleManager localeManager;
    private final ApplicationLinkResolver applicationLinkResolver;

    private static final String EXCEPTION_MESSAGE = "exceptionMessage";
    private static final String TEMPLATE_PATH = "templates/extra/jira";
    private static final String JIRA_LINK_TEXT = "jiraLinkText";

    /**
     * Default constructor
     *
     * @param i18NBeanFactory the I18NBeanFactory instance, see {@link com.atlassian.confluence.util.i18n.I18NBeanFactory}
     * @param localeManager   the LocalManager instance, see {@link com.atlassian.confluence.languages.LocaleManager} for more details
     */
    public JiraExceptionHelper(final I18NBeanFactory i18NBeanFactory, final LocaleManager localeManager, final ApplicationLinkResolver applicationLinkResolver)
    {
        this.i18NBeanFactory = i18NBeanFactory;
        this.localeManager = localeManager;
        this.applicationLinkResolver = applicationLinkResolver;
    }

    /**
     * Wrap exception into MacroExecutionException.
     *
     * @param exception Any Exception thrown for whatever reason when Confluence could
     *                  not retrieve JIRA Issues
     * @throws com.atlassian.confluence.macro.MacroExecutionException A macro exception means that a macro has failed to execute
     *                                                                successfully
     */
    public void throwMacroExecutionException(final Exception exception, final ConversionContext conversionContext)
            throws MacroExecutionException
    {
        String i18nKey = null;
        List params = null;

        if (exception instanceof UnknownHostException)
        {
            i18nKey = "jiraissues.error.unknownhost";
            params = Arrays.asList(StringUtils.defaultString(exception.getMessage()));
        }
        else if (exception instanceof ConnectException)
        {
            i18nKey = "jiraissues.error.unabletoconnect";
            params = Arrays.asList(StringUtils.defaultString(exception.getMessage()));
        }
        else if (exception instanceof AuthenticationException)
        {
            i18nKey = "jiraissues.error.authenticationerror";
        }
        else if (exception instanceof MalformedRequestException)
        {
            // JIRA returns 400 HTTP code when it should have been a 401
            i18nKey = "jiraissues.error.notpermitted";
        }
        else if (exception instanceof TrustedAppsException)
        {
            i18nKey = "jiraissues.error.trustedapps";
            params = Collections.singletonList(exception.getMessage());
        }
        else if (exception instanceof TypeNotInstalledException)
        {
            i18nKey = "jirachart.error.applicationLinkNotExist";
            params = Collections.singletonList(exception.getMessage());
        }
        else
        {
            i18nKey = "jiraissues.unexpected.error";
        }

        if (i18nKey != null)
        {
            final String msg = getText(getText(i18nKey, params));
            LOGGER.info(msg);
            if (!ConversionContextOutputType.FEED.value().equals(conversionContext.getOutputType()))
            {
                LOGGER.debug("Macro execution exception: ", exception);
            }
            throw new MacroExecutionException(msg, exception);
        }
    }

    /**
     * Get the internationalized text by a key
     *
     * @param i18n the key associated with the text
     * @return internationalized text
     */
    public String getText(final String i18n)
    {
        return getI18NBean().getText(i18n);
    }

    /**
     * Get the internationalized text by a key with substitutions
     *
     * @param i18n          the key associated with the text
     * @param substitutions the substitution list
     * @return internationalized text
     */
    public String getText(final String i18n, final List substitutions)
    {
        return getI18NBean().getText(i18n, substitutions);
    }

    /**
     * Get the i18NBean instance
     *
     * @return the I18NBean instance, see {@link com.atlassian.confluence.util.i18n.I18NBean} for more details
     */
    protected I18NBean getI18NBean()
    {
        if (null != AuthenticatedUserThreadLocal.get())
        {
            return i18NBeanFactory.getI18NBean(localeManager.getLocale(AuthenticatedUserThreadLocal.get()));
        }
        return i18NBeanFactory.getI18NBean();
    }

    public static String renderExceptionMessage(final String exceptionMessage)
    {
        final Map<String, Object> contextMap = Maps.newHashMap();
        contextMap.put(MACRO_NAME, "JIRA Issues Macro");
        contextMap.put(EXCEPTION_MESSAGE, exceptionMessage);
        return VelocityUtils.getRenderedTemplate(TEMPLATE_PATH + "/exception.vm", contextMap);
    }

    public String renderBatchingJIMExceptionMessage(final String exceptionMessage, final Map<String, String> parameters)
    {
        final Map<String, Object> contextMap = Maps.newHashMap();
        contextMap.put(MACRO_NAME, "JIRA Issues Macro");
        contextMap.put(EXCEPTION_MESSAGE, exceptionMessage);
        String key = JiraUtil.getSingleIssueKey(parameters);
        if (StringUtils.isNotBlank(key))
        {
            contextMap.put(JiraIssuesMacro.CLICKABLE_URL, getJiraUrlOfBatchingIssues(parameters, key));
            contextMap.put(JIRA_LINK_TEXT, key);
        }

        return VelocityUtils.getRenderedTemplate(TEMPLATE_PATH + "/exception.vm", contextMap);
    }

    public String renderJIMExceptionMessage(Exception e)
    {
        final Map<String, Object> contextMap = Maps.newHashMap();
        contextMap.put(MACRO_NAME, "JIRA Issues Macro");
        contextMap.put(EXCEPTION_MESSAGE, e.getMessage());

        if (e instanceof JiraIssueMacroException && ((JiraIssueMacroException) e).getContextMap() != null)
        {
            contextMap.put(EXCEPTION_MESSAGE, e.getCause().getMessage());
            setupErrorJiraLink(contextMap, ((JiraIssueMacroException) e).getContextMap());
        }

        return VelocityUtils.getRenderedTemplate(TEMPLATE_PATH + "/exception.vm", contextMap);
    }

    private void setupErrorJiraLink(final Map<String, Object> contextMap, final Map<String, Object> jiraIssueMap)
    {
        Object clickableURL = jiraIssueMap.get(JiraIssuesMacro.CLICKABLE_URL);
        Object issueTypeObject = jiraIssueMap.get(JiraIssueRender.ISSUE_TYPE);
        if (clickableURL == null || issueTypeObject == null) return;

        contextMap.put(JiraIssuesMacro.CLICKABLE_URL, clickableURL);
        JiraIssuesMacro.JiraIssuesType issuesType = (JiraIssuesMacro.JiraIssuesType) issueTypeObject;
        switch (issuesType)
        {
            case SINGLE:
                contextMap.put(JIRA_LINK_TEXT, jiraIssueMap.get(JiraIssuesMacro.KEY));
                break;
            default:
                contextMap.put(JIRA_LINK_TEXT, getText("view.in.jira"));
                break;
        }
    }

    public String renderTimeoutMessage()
    {
        return renderExceptionMessage(getI18NBean().getText("jiraissues.error.timeout.execution"));
    }

    private String getJiraUrlOfBatchingIssues(final Map<String, String> parameters, String key)
    {
        try
        {
            ApplicationLink appLink = applicationLinkResolver.resolve(JiraIssuesMacro.Type.KEY, key, parameters);
            return JiraIssueUtil.getClickableUrl(key, JiraIssuesMacro.Type.KEY, appLink, null);
        }
        catch (TypeNotInstalledException e)
        {
            return null;
        }
    }
}
