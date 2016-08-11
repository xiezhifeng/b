package com.atlassian.confluence.extra.jira.helper;

import java.net.ConnectException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.atlassian.applinks.api.ReadOnlyApplicationLink;
import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.content.render.xhtml.ConversionContextOutputType;
import com.atlassian.confluence.extra.jira.ApplicationLinkResolver;
import com.atlassian.confluence.extra.jira.JiraIssuesMacro;
import com.atlassian.confluence.extra.jira.TrustedAppsException;

import com.atlassian.confluence.extra.jira.exception.AuthenticationException;
import com.atlassian.confluence.extra.jira.exception.JiraPermissionException;
import com.atlassian.confluence.extra.jira.exception.JiraRuntimeException;
import com.atlassian.confluence.extra.jira.exception.MalformedRequestException;
import com.atlassian.confluence.extra.jira.exception.JiraIssueMacroException;
import com.atlassian.confluence.extra.jira.exception.JiraIssueDataException;
import com.atlassian.confluence.extra.jira.util.JiraIssueUtil;
import com.atlassian.confluence.extra.jira.util.JiraUtil;
import com.atlassian.confluence.languages.LocaleManager;
import com.atlassian.confluence.macro.MacroExecutionException;
import com.atlassian.confluence.renderer.radeox.macros.MacroUtils;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.confluence.util.i18n.I18NBean;
import com.atlassian.confluence.util.i18n.I18NBeanFactory;
import com.atlassian.confluence.util.velocity.VelocityUtils;

import com.atlassian.applinks.api.TypeNotInstalledException;

import com.google.common.collect.Maps;

import org.apache.commons.lang3.StringUtils;
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
        else if (exception instanceof ConnectException || exception instanceof SocketException)
        {
            i18nKey = "jiraissues.error.unabletoconnect";
            params = Arrays.asList(StringUtils.defaultString(exception.getMessage()));
        }
        else if (exception instanceof AuthenticationException)
        {
            i18nKey = "jiraissues.error.authenticationerror";
        }
        else if (exception instanceof MalformedRequestException || exception instanceof JiraPermissionException)
        {
            // JIRA returns 400 HTTP code when it should have been a 401
            i18nKey = "jiraissues.error.notpermitted";
        }
        else if (exception instanceof TrustedAppsException)
        {
            i18nKey = "jiraissues.error.trustedapps";
            params = Collections.singletonList(exception.getMessage());
        }
        else if (exception instanceof TypeNotInstalledException) {
            i18nKey = "jirachart.error.applicationLinkNotExist";
            params = Collections.singletonList(exception.getMessage());
        }
        else if (exception instanceof JiraRuntimeException)
        {
            i18nKey = "jiraissues.error.request.handling";
            params = Collections.singletonList(exception.getMessage());
        }
        else if(exception instanceof JiraIssueDataException)
        {
            i18nKey = "jiraissues.error.nodata";
        }
        else if (exception instanceof SocketTimeoutException)
        {
            i18nKey = "jiraissues.error.timeout.connection";
        }
        else
        {
            i18nKey = "jiraissues.unexpected.error";
            if (!ConversionContextOutputType.FEED.value().equals(conversionContext.getOutputType()))
            {
                LOGGER.error("Macro execution exception: ", exception);
            }
        }

        final String msg = getText(i18nKey, params);
        throw new MacroExecutionException(msg, exception);

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
        return renderJiraIssueException(new JiraExceptionBean(exceptionMessage));
    }

    public String renderBatchingJIMExceptionMessage(final String exceptionMessage, final Map<String, String> parameters)
    {
        JiraExceptionBean exceptionBean = new JiraExceptionBean(exceptionMessage);
        exceptionBean.setIssueType(JiraIssuesMacro.JiraIssuesType.SINGLE);

        String key = JiraUtil.getSingleIssueKey(parameters);
        if (StringUtils.isNotBlank(key))
        {
            exceptionBean.setClickableUrl(getJiraUrlOfBatchingIssues(parameters, key));
            exceptionBean.setJiraLinkText(key);
        }

        return renderJiraIssueException(exceptionBean);
    }

    public String renderNormalJIMExceptionMessage(Exception e)
    {
        JiraExceptionBean exceptionBean = new JiraExceptionBean(e.getMessage());
        if (e instanceof JiraIssueMacroException && ((JiraIssueMacroException) e).getContextMap() != null)
        {
            exceptionBean.setMessage(e.getCause().getMessage());
            setupErrorJiraLink(exceptionBean, ((JiraIssueMacroException) e).getContextMap());
        }
        return renderJiraIssueException(exceptionBean);
    }

    private static String renderJiraIssueException(JiraExceptionBean exceptionBean)
    {
        final Map<String, Object> contextMap = MacroUtils.defaultVelocityContext();

        contextMap.put(MACRO_NAME, "JIRA Issues Macro");
        contextMap.put(EXCEPTION_MESSAGE, exceptionBean.getMessage());
        contextMap.put(JiraIssuesMacro.ISSUE_TYPE, exceptionBean.getIssueType());
        contextMap.put(JiraIssuesMacro.COLUMNS, exceptionBean.getColumns());

        if(StringUtils.isNotBlank(exceptionBean.getClickableUrl()))
        {
            contextMap.put(JiraIssuesMacro.CLICKABLE_URL, exceptionBean.getClickableUrl());
            contextMap.put(JIRA_LINK_TEXT, exceptionBean.getJiraLinkText());
        }

        return VelocityUtils.getRenderedTemplate(TEMPLATE_PATH + "/exception.vm", contextMap);
    }

    private void setupErrorJiraLink(JiraExceptionBean exceptionBean, final Map<String, Object> jiraIssueMap)
    {
        Object clickableURL = jiraIssueMap.get(JiraIssuesMacro.CLICKABLE_URL);
        if (clickableURL != null)
        {
            exceptionBean.setClickableUrl(clickableURL.toString());
        }

        Object issueTypeObject = jiraIssueMap.get(JiraIssuesMacro.ISSUE_TYPE);
        if (issueTypeObject != null)
        {
            JiraIssuesMacro.JiraIssuesType issuesType = (JiraIssuesMacro.JiraIssuesType) issueTypeObject;
            exceptionBean.setIssueType(issuesType);

            switch (issuesType)
            {
                case SINGLE:
                    exceptionBean.setJiraLinkText(jiraIssueMap.get(JiraIssuesMacro.KEY).toString());
                    break;
                default:
                    exceptionBean.setJiraLinkText(getText("view.these.issues.jira"));
                    break;
            }
        }

        Object issueColumnsObject = jiraIssueMap.get(JiraIssuesMacro.COLUMNS);
        if (issueColumnsObject != null)
        {
            List<String> issueColumns = (List<String>) issueColumnsObject;
            exceptionBean.setColumns(issueColumns);
        }

    }

    public String renderTimeoutMessage(final Map<String, String> parameters)
    {
        return renderBatchingJIMExceptionMessage(getI18NBean().getText("jiraissues.error.timeout.execution"), parameters);
    }

    private String getJiraUrlOfBatchingIssues(final Map<String, String> parameters, String key)
    {
        try
        {
            ReadOnlyApplicationLink appLink = applicationLinkResolver.resolve(JiraIssuesMacro.Type.KEY, key, parameters);
            return appLink == null ? null : JiraIssueUtil.getClickableUrl(key, JiraIssuesMacro.Type.KEY, appLink, null);
        }
        catch (TypeNotInstalledException e)
        {
            return null;
        }
    }

    static class JiraExceptionBean
    {
        private String message;
        private String jiraLinkText;
        private String clickableUrl;
        private List<String> columns;

        private JiraIssuesMacro.JiraIssuesType issueType = JiraIssuesMacro.JiraIssuesType.SINGLE;

        public JiraExceptionBean(String message)
        {
            this.message = message;
        }

        public JiraExceptionBean(String message, String jiraLinkText, String clickableUrl)
        {
            this(message);
            this.jiraLinkText = jiraLinkText;
            this.clickableUrl = clickableUrl;
        }

        public String getMessage()
        {
            return message;
        }

        public void setMessage(String message)
        {
            this.message = message;
        }

        public String getJiraLinkText()
        {
            return jiraLinkText;
        }

        public void setJiraLinkText(String jiraLinkText)
        {
            this.jiraLinkText = jiraLinkText;
        }

        public String getClickableUrl()
        {
            return clickableUrl;
        }

        public void setClickableUrl(String clickableUrl)
        {
            this.clickableUrl = clickableUrl;
        }

        public void setIssueType(JiraIssuesMacro.JiraIssuesType issueType)
        {
            this.issueType = issueType;
        }

        public JiraIssuesMacro.JiraIssuesType getIssueType()
        {
            return issueType;
        }

        public void setColumns(List<String> columns)
        {
            this.columns = columns;
        }

        public List<String> getColumns()
        {
            return columns;
        }
    }
}
