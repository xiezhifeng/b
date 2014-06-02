package com.atlassian.confluence.extra.jira.helper;

import com.atlassian.applinks.api.TypeNotInstalledException;
import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.content.render.xhtml.ConversionContextOutputType;
import com.atlassian.confluence.extra.jira.JiraIssuesMacro;
import com.atlassian.confluence.extra.jira.TrustedAppsException;
import com.atlassian.confluence.extra.jira.exception.AuthenticationException;
import com.atlassian.confluence.extra.jira.exception.MalformedRequestException;
import com.atlassian.confluence.languages.LocaleManager;
import com.atlassian.confluence.macro.MacroExecutionException;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.confluence.util.i18n.I18NBean;
import com.atlassian.confluence.util.i18n.I18NBeanFactory;
import com.atlassian.confluence.util.velocity.VelocityUtils;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import java.net.ConnectException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Maps;

/**
 * This class responsible for converting a specific JIRA Issues Macros' exception
 * into the generic MacroExecutionException
 */
public class JiraExceptionHelper
{

    private static final Logger LOGGER = Logger.getLogger(JiraExceptionHelper.class);

    private I18NBeanFactory i18NBeanFactory;
    private LocaleManager localeManager;
    private static final String EXCEPTION_MESSAGE = "exceptionMessage";
    private static final String TEMPLATE_PATH = "templates/extra/jira";

    /**
     * Default constructor
     *
     * @param i18NBeanFactory the I18NBeanFactory instance, see {@link com.atlassian.confluence.util.i18n.I18NBeanFactory}
     * @param localeManager   the LocalManager instance, see {@link com.atlassian.confluence.languages.LocaleManager} for more details
     */
    public JiraExceptionHelper(I18NBeanFactory i18NBeanFactory, LocaleManager localeManager)
    {
        this.i18NBeanFactory = i18NBeanFactory;
        this.localeManager = localeManager;
    }

    /**
     * Wrap exception into MacroExecutionException.
     *
     * @param exception Any Exception thrown for whatever reason when Confluence could
     *                  not retrieve JIRA Issues
     * @throws com.atlassian.confluence.macro.MacroExecutionException A macro exception means that a macro has failed to execute
     *                                                                successfully
     */
    public void throwMacroExecutionException(Exception exception, ConversionContext conversionContext)
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

        if (i18nKey != null)
        {
            String msg = getText(getText(i18nKey, params));
            LOGGER.info(msg);
            LOGGER.debug("More info : ", exception);
            throw new MacroExecutionException(msg, exception);
        }
        else
        {
            if (!ConversionContextOutputType.FEED.value().equals(conversionContext.getOutputType()))
            {
                LOGGER.error("Macro execution exception: ", exception);
            }
            throw new MacroExecutionException(exception);
        }
    }

    /**
     * Get the internationalized text by a key
     *
     * @param i18n the key associated with the text
     * @return internationalized text
     */
    public String getText(String i18n)
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
    public String getText(String i18n, List substitutions)
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

    public static String renderExceptionMessage(String exceptionMessage)
    {
        Map<String, Object> contextMap = Maps.newHashMap();
        contextMap.put(EXCEPTION_MESSAGE, exceptionMessage);
        return VelocityUtils.getRenderedTemplate(TEMPLATE_PATH + "/exception.vm", contextMap);
    }
}
