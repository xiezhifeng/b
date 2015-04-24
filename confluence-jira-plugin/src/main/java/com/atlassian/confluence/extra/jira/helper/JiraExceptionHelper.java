package com.atlassian.confluence.extra.jira.helper;

import java.net.ConnectException;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.content.render.xhtml.ConversionContextOutputType;
import com.atlassian.confluence.extra.jira.TrustedAppsException;
import com.atlassian.confluence.extra.jira.exception.AuthenticationException;
import com.atlassian.confluence.extra.jira.exception.MalformedRequestException;
import com.atlassian.confluence.languages.LocaleManager;
import com.atlassian.confluence.macro.MacroExecutionException;
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
    private static final String ISSUE_ID = "issueId";

    private final I18NBeanFactory i18NBeanFactory;
    private final LocaleManager localeManager;
    private static final String EXCEPTION_MESSAGE = "exceptionMessage";
    private static final String TEMPLATE_PATH = "templates/extra/jira";

    /**
     * Default constructor
     *
     * @param i18NBeanFactory the I18NBeanFactory instance, see {@link com.atlassian.confluence.util.i18n.I18NBeanFactory}
     * @param localeManager   the LocalManager instance, see {@link com.atlassian.confluence.languages.LocaleManager} for more details
     */
    public JiraExceptionHelper(final I18NBeanFactory i18NBeanFactory, final LocaleManager localeManager)
    {
        this.i18NBeanFactory = i18NBeanFactory;
        this.localeManager = localeManager;
    }

    /**
     * Explain an Throwable with clear/localized message intend to show to the end users.
     *
     * @param t The throwable to explain
     *
     * @return The explaining message
     */
    public String explainException(final Throwable t)
    {
        if (t instanceof MacroExecutionException)
        {
            return t.getMessage();
        }
        else
        {
            String i18nKey;
            List params = null;
            if (t instanceof UnknownHostException)
            {
                i18nKey = "jiraissues.error.unknownhost";
                params = Collections.singletonList(StringUtils.defaultString(t.getMessage()));
            }
            else if (t instanceof ConnectException)
            {
                i18nKey = "jiraissues.error.unabletoconnect";
                params = Collections.singletonList(StringUtils.defaultString(t.getMessage()));
            }
            else if (t instanceof AuthenticationException)
            {
                i18nKey = "jiraissues.error.authenticationerror";
            }
            else if (t instanceof MalformedRequestException)
            {
                // JIRA returns 400 HTTP code when it should have been a 401
                i18nKey = "jiraissues.error.notpermitted";
            }
            else if (t instanceof TrustedAppsException)
            {
                i18nKey = "jiraissues.error.trustedapps";
                params = Collections.singletonList(t.getMessage());
            }
            else if (t instanceof TypeNotInstalledException)
            {
                i18nKey = "jirachart.error.applicationLinkNotExist";
                params = Collections.singletonList(t.getMessage());
            }
            else
            {
                i18nKey = "jiraissues.unexpected.error";
            }
            return  getText(getText(i18nKey, params));
        }
    }

    public String renderSingleIssueTimeoutMessage(String issueKey)
    {
        return renderSingleIssueExceptionMessage(issueKey, getI18NBean().getText("jiraissues.error.timeout.execution"));
    }

    public String renderTimeoutMessage()
    {
        return renderExceptionMessage(getI18NBean().getText("jiraissues.error.timeout.execution"));
    }

//    public String renderException(Throwable t)
//    {
//        renderExceptionMessage()
//    }

    /**
     * Wrap exception into MacroExecutionException.
     *
     * @param t Any Throwable thrown for whatever reason when Confluence could
     *                  not retrieve JIRA Issues
     * @throws com.atlassian.confluence.macro.MacroExecutionException A macro exception means that a macro has failed to execute
     *                                                                successfully
     */
    public void throwMacroExecutionException(final Throwable t, final ConversionContext conversionContext)
            throws MacroExecutionException
    {
        if (t instanceof MacroExecutionException)
        {
            throw (MacroExecutionException) t;
        }
        String msg = explainException(t);
        if (!ConversionContextOutputType.FEED.value().equals(conversionContext.getOutputType()))
        {
            LOGGER.debug("Macro execution exception: ", t);
        }
        throw new MacroExecutionException(msg, t);
    }

    public String renderException(final Throwable t)
    {
        return renderExceptionMessage(explainException(t));
    }

    public String renderSingleIssueException(String issueId, final Throwable t)
    {
        return renderSingleIssueExceptionMessage(issueId, explainException(t));
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

    public static String renderSingleIssueExceptionMessage(final String issueId, final String exceptionMessage)
    {
        final Map<String, Object> contextMap = Maps.newHashMap();
        contextMap.put(MACRO_NAME, "JIRA Issues Macro");
        contextMap.put(ISSUE_ID, issueId);
        contextMap.put(EXCEPTION_MESSAGE, exceptionMessage);
        return VelocityUtils.getRenderedTemplate(TEMPLATE_PATH + "/singleIssueException.vm", contextMap);
    }
}
