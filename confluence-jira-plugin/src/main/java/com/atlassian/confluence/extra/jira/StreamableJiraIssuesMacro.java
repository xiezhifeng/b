package com.atlassian.confluence.extra.jira;

import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.content.render.xhtml.Streamable;
import com.atlassian.confluence.content.render.xhtml.XhtmlException;
import com.atlassian.confluence.content.render.xhtml.macro.MacroMarshallingFactory;
import com.atlassian.confluence.core.ContentEntityObject;
import com.atlassian.confluence.core.FormatSettingsManager;
import com.atlassian.confluence.extra.jira.api.services.JiraIssueBatchService;
import com.atlassian.confluence.extra.jira.api.services.JiraMacroFinderService;
import com.atlassian.confluence.extra.jira.exception.UnsupportedJiraServerException;
import com.atlassian.confluence.extra.jira.executor.FutureStreamableConverter;
import com.atlassian.confluence.extra.jira.executor.StreamableMacroExecutor;
import com.atlassian.confluence.extra.jira.executor.StreamableMacroFutureTask;
import com.atlassian.confluence.extra.jira.helper.ImagePlaceHolderHelper;
import com.atlassian.confluence.extra.jira.helper.JiraExceptionHelper;
import com.atlassian.confluence.extra.jira.model.JiraBatchRequestData;
import com.atlassian.confluence.extra.jira.util.MapUtil;
import com.atlassian.confluence.languages.LocaleManager;
import com.atlassian.confluence.macro.EditorImagePlaceholder;
import com.atlassian.confluence.macro.MacroExecutionException;
import com.atlassian.confluence.macro.ResourceAware;
import com.atlassian.confluence.macro.StreamableMacro;
import com.atlassian.confluence.security.PermissionManager;
import com.atlassian.confluence.setup.settings.SettingsManager;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.confluence.util.i18n.I18NBeanFactory;
import com.atlassian.confluence.xhtml.api.MacroDefinition;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.jdom.Element;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

/**
 * A macro to import/fetch JIRA issues...
 */
public class StreamableJiraIssuesMacro extends JiraIssuesMacro implements StreamableMacro, EditorImagePlaceholder, ResourceAware
{
    private static final int MIN_SINGLE_ISSUES_ALLOWED = 5;

    private static final Logger LOGGER = Logger.getLogger(JiraIssuesMacro.class);

    private StreamableMacroExecutor executorService;

    private JiraMacroFinderService jiraMacroFinderService;

    private JiraIssueBatchService jiraIssueBatchService;

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
     * @param localeManager            see {@link com.atlassian.confluence.languages.LocaleManager}
     * @param executorService          see {@link com.atlassian.confluence.extra.jira.StreamableJiraIssuesMacro}
     */
    public StreamableJiraIssuesMacro(I18NBeanFactory i18NBeanFactory, JiraIssuesManager jiraIssuesManager, SettingsManager settingsManager, JiraIssuesColumnManager jiraIssuesColumnManager, TrustedApplicationConfig trustedApplicationConfig, PermissionManager permissionManager, ApplicationLinkResolver applicationLinkResolver, JiraIssuesDateFormatter jiraIssuesDateFormatter, MacroMarshallingFactory macroMarshallingFactory, JiraCacheManager jiraCacheManager, ImagePlaceHolderHelper imagePlaceHolderHelper, FormatSettingsManager formatSettingsManager, JiraIssueSortingManager jiraIssueSortingManager, JiraExceptionHelper jiraExceptionHelper, LocaleManager localeManager, StreamableMacroExecutor executorService, JiraMacroFinderService jiraMacroFinderService, JiraIssueBatchService jiraIssueBatchService)
    {
        super(i18NBeanFactory, jiraIssuesManager, settingsManager, jiraIssuesColumnManager, trustedApplicationConfig, permissionManager, applicationLinkResolver, jiraIssuesDateFormatter, macroMarshallingFactory, jiraCacheManager, imagePlaceHolderHelper, formatSettingsManager, jiraIssueSortingManager, jiraExceptionHelper, localeManager);
        this.executorService = executorService;
        this.jiraMacroFinderService = jiraMacroFinderService;
        this.jiraIssueBatchService = jiraIssueBatchService;
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

    /**
     * Private method responsible for submitting na new StreamableMacroFutureTask instance into the thread pool for
     * later processing
     *
     * @param parameters the macro parameters
     * @param conversionContext    the conversionContext associated with the macro
     * @return the Future (result) of the task
     */
    private Future<String> marshallMacroInBackground(final Map<String, String> parameters, final ConversionContext conversionContext)
    {
        ContentEntityObject entity = conversionContext.getEntity();
        String body = entity.getBodyContent().getBody();

            long finderStart = System.currentTimeMillis();
            // We find all MacroDefinitions for single JIRA issues in the body
        final Set<MacroDefinition> macroDefinitions;
        try
        {
            macroDefinitions = jiraMacroFinderService.findSingleJiraIssueMacros(body, conversionContext);

        LOGGER.debug("******* findSingleJiraIssueMacros time =" + (System.currentTimeMillis() - finderStart));
            // If the number of macro definitions is less than MIN_SINGLE_ISSUES_ALLOWED, we stop immediately because it's not worth to do
            // additional work for small results
            if (macroDefinitions.size() < MIN_SINGLE_ISSUES_ALLOWED)
            {
//                LOGGER.debug("******* transform time =" + (System.currentTimeMillis() - transformStart));
            }
            SingleJiraIssuesThreadLocalAccessor.setBatchProcessed(Boolean.TRUE); // Single JIRA issues will be processed in batch
            // We use a HashMultimap to store the [serverId: set of keys] pairs because duplicate serverId-key pair will not be stored
            Multimap<String, String> jiraServerIdToKeysMap = HashMultimap.create();

            HashMap<String, Map<String, String>> jiraServerIdToParameters = Maps.newHashMap();
            for (MacroDefinition macroDefinition : macroDefinitions)
            {
                String serverId = macroDefinition.getParameter(SERVER_ID);
                jiraServerIdToKeysMap.put(serverId, macroDefinition.getParameter(KEY));
                if (jiraServerIdToParameters.get(serverId) == null)
                {
                    jiraServerIdToParameters.put(serverId, MapUtil.copyOf(macroDefinition.getParameters()));
                }
            }
            for (String serverId : jiraServerIdToKeysMap.keySet())
            {
                Set<String> keys = (Set<String>) jiraServerIdToKeysMap.get(serverId);
                // make request to the same JIRA server for the whole set of keys and putElement the individual data of each key into the SingleJiraIssuesThreadLocalAccessor
                JiraBatchRequestData jiraBatchRequestData = new JiraBatchRequestData();
                try
                {
                    Map<String, Object> resultsMap = jiraIssueBatchService.getBatchResults(serverId, keys, conversionContext);
                    if (resultsMap != null)
                    {
                        Map<String, Element> elementMap = (Map<String, Element>) resultsMap.get(JiraIssueBatchService.ELEMENT_MAP);
                        String jiraServerUrl = (String) resultsMap.get(JiraIssueBatchService.JIRA_SERVER_URL);
                        // Store the results to TheadLocal maps for later use
                        jiraBatchRequestData.setElementMap(elementMap);
                        jiraBatchRequestData.setServerUrl(jiraServerUrl);
                    }
                }
                catch (MacroExecutionException macroExecutionException)
                {
                    jiraBatchRequestData.setException(macroExecutionException);
                }
                catch (UnsupportedJiraServerException unsupportedJiraServerException)
                {
                    jiraBatchRequestData.setException(unsupportedJiraServerException);
                }
                finally
                {
                    SingleJiraIssuesThreadLocalAccessor.putJiraBatchRequestData(serverId, jiraBatchRequestData);
                }
            }
//        LOGGER.debug("******* transform time =" + (System.currentTimeMillis() - transformStart));
        }
        catch (XhtmlException e)
        {
            e.printStackTrace();
        }


        Boolean batchProcessed = SingleJiraIssuesThreadLocalAccessor.getBatchProcessed();
        if (batchProcessed)
        {
            String serverId = parameters.get(SERVER_ID);
            String key = parameters.get(KEY);
            // if this macro is for rendering a single issue then we must get the resulting element from the SingleJiraIssuesThreadLocalAccessor
            // the element must be available now because we already request all JIRA issues as batches in the SingleJiraIssuesToViewTransformer.transform function
            if (key != null && serverId != null)
            {
                JiraBatchRequestData jiraBatchRequestData = SingleJiraIssuesThreadLocalAccessor.getJiraBatchRequestData(serverId);
                Map<String, Element> elementMap = jiraBatchRequestData.getElementMap();
                Element element = elementMap != null ? elementMap.get(key) : null;
                String jiraServerUrl = jiraBatchRequestData.getServerUrl();
                Exception exception = jiraBatchRequestData.getException();
                return executorService.submit(new StreamableMacroFutureTask(parameters, conversionContext, this, AuthenticatedUserThreadLocal.get(), element, jiraServerUrl, exception));
            }
        }
        return executorService.submit(new StreamableMacroFutureTask(parameters, conversionContext, this, AuthenticatedUserThreadLocal.get()));
    }
}
