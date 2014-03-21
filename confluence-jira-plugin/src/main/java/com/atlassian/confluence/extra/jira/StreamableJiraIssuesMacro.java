package com.atlassian.confluence.extra.jira;

import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.content.render.xhtml.Streamable;
import com.atlassian.confluence.content.render.xhtml.macro.MacroMarshallingFactory;
import com.atlassian.confluence.core.FormatSettingsManager;
import com.atlassian.confluence.extra.jira.exception.UnsupportedJiraVersionException;
import com.atlassian.confluence.extra.jira.executor.FutureStreamableConverter;
import com.atlassian.confluence.extra.jira.executor.StreamableMacroExecutor;
import com.atlassian.confluence.extra.jira.executor.StreamableMacroFutureTask;
import com.atlassian.confluence.extra.jira.helper.ImagePlaceHolderHelper;
import com.atlassian.confluence.extra.jira.helper.JiraExceptionHelper;
import com.atlassian.confluence.languages.LocaleManager;
import com.atlassian.confluence.macro.EditorImagePlaceholder;
import com.atlassian.confluence.macro.MacroExecutionException;
import com.atlassian.confluence.macro.ResourceAware;
import com.atlassian.confluence.macro.StreamableMacro;
import com.atlassian.confluence.security.PermissionManager;
import com.atlassian.confluence.setup.settings.SettingsManager;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.confluence.util.i18n.I18NBeanFactory;
import org.jdom.Element;

import java.util.Map;
import java.util.concurrent.Future;

/**
 * A macro to import/fetch JIRA issues...
 */
public class StreamableJiraIssuesMacro extends JiraIssuesMacro implements StreamableMacro, EditorImagePlaceholder, ResourceAware
{
    private StreamableMacroExecutor executorService;

    /**
     * Default constructor to get all necessary beans injected
     *
     * @param i18NBeanFactory          see {@link com.atlassian.confluence.util.i18n.I18NBeanFactory}
     * @param jiraIssuesManager        see {@link com.atlassian.confluence.extra.jira.JiraIssuesManager}
     * @param settingsManager          see {@link com.atlassian.confluence.setup.settings.SettingsManager}
     * @param jiraIssuesColumnManager  see {@link com.atlassian.confluence.extra.jira.JiraIssuesColumnManager}
     * @param trustedApplicationConfig see {@link com.atlassian.confluence.extra.jira.TrustedApplicationConfig}
     * @param permissionManager        see {@link com.atlassian.confluence.security.PermissionManager}
     * @param applicationLinkResolver  see {@link com.atlassian.confluence.extra.jira.ApplicationLinkResolver}
     * @param jiraIssuesDateFormatter  see {@link com.atlassian.confluence.extra.jira.JiraIssuesDateFormatter}
     * @param macroMarshallingFactory  see {@link com.atlassian.confluence.content.render.xhtml.macro.MacroMarshallingFactory}
     * @param jiraCacheManager         see {@link com.atlassian.confluence.extra.jira.JiraCacheManager}
     * @param imagePlaceHolderHelper   see {@link com.atlassian.confluence.extra.jira.helper.ImagePlaceHolderHelper}
     * @param formatSettingsManager    see {@link com.atlassian.confluence.core.FormatSettingsManager}
     * @param jiraIssueSortingManager  see {@link com.atlassian.confluence.extra.jira.JiraIssueSortingManager}
     * @param jiraExceptionHelper      see {@link com.atlassian.confluence.extra.jira.helper.JiraExceptionHelper}
     * @param jiraConnectorManager     see {@link com.atlassian.confluence.extra.jira.JiraConnectorManager}
     * @param localeManager            see {@link com.atlassian.confluence.languages.LocaleManager}
     * @param executorService          see {@link com.atlassian.confluence.extra.jira.StreamableJiraIssuesMacro}
     */
    public StreamableJiraIssuesMacro(I18NBeanFactory i18NBeanFactory, JiraIssuesManager jiraIssuesManager, SettingsManager settingsManager, JiraIssuesColumnManager jiraIssuesColumnManager, TrustedApplicationConfig trustedApplicationConfig, PermissionManager permissionManager, ApplicationLinkResolver applicationLinkResolver, JiraIssuesDateFormatter jiraIssuesDateFormatter, MacroMarshallingFactory macroMarshallingFactory, JiraCacheManager jiraCacheManager, ImagePlaceHolderHelper imagePlaceHolderHelper, FormatSettingsManager formatSettingsManager, JiraIssueSortingManager jiraIssueSortingManager, JiraExceptionHelper jiraExceptionHelper, JiraConnectorManager jiraConnectorManager, LocaleManager localeManager, StreamableMacroExecutor executorService)
    {
        super(i18NBeanFactory, jiraIssuesManager, settingsManager, jiraIssuesColumnManager, trustedApplicationConfig, permissionManager, applicationLinkResolver, jiraIssuesDateFormatter, macroMarshallingFactory, jiraCacheManager, imagePlaceHolderHelper, formatSettingsManager, jiraIssueSortingManager, jiraExceptionHelper, jiraConnectorManager, localeManager);
        this.executorService = executorService;
    }

    public Streamable executeToStream(final Map<String, String> parameters, final Streamable body,
                                      final ConversionContext context) throws MacroExecutionException
    {
        final Future<String> futureResult = marshallMacroInBackground(parameters, context);

        return new FutureStreamableConverter.Builder(futureResult, context, getI18NBean())
                .executionErrorMsg("jiraissues.error.execution")
                .executionTimeoutErrorMsg("jiraissues.error.timeout.execution")
                .connectionTimeoutErrorMsg("jiraissues.error.timeout.connection")
                .interruptedErrorMsg("jiraissues.error.interrupted").build();
    }

    private Future<String> marshallMacroInBackground(final Map<String, String> parameters, final ConversionContext context)
    {
        String serverId = parameters.get(SERVER_ID);
        String key = parameters.get(KEY);
        // if this macro is for rendering a single issue then we must get the resulting element from the SingleJiraIssuesThreadLocalAccessor
        // the element must be available now because we already request all JIRA issues as batches in the SingleJiraIssuesToViewTransformer.transform function
        if (key != null && serverId != null)
        {
            Element element = SingleJiraIssuesThreadLocalAccessor.getElement(serverId, key);
            String jiraServerUrl = SingleJiraIssuesThreadLocalAccessor.getJiraServerUrl(serverId);
            Exception exception = SingleJiraIssuesThreadLocalAccessor.getException(serverId);
            if (exception instanceof UnsupportedJiraVersionException) {
                return executorService.submit(new StreamableMacroFutureTask(parameters, context, this, AuthenticatedUserThreadLocal.get(), null, null, exception));
            }
            return executorService.submit(new StreamableMacroFutureTask(parameters, context, this, AuthenticatedUserThreadLocal.get(), element, jiraServerUrl, exception));
        }
        return executorService.submit(new StreamableMacroFutureTask(parameters, context, this, AuthenticatedUserThreadLocal.get()));
    }
}
