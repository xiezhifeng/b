package com.atlassian.confluence.extra.jira;

import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.applinks.api.TypeNotInstalledException;
import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.content.render.xhtml.Streamable;
import com.atlassian.confluence.content.render.xhtml.XhtmlException;
import com.atlassian.confluence.core.ContentEntityObject;
import com.atlassian.confluence.extra.jira.api.services.JiraIssueBatchService;
import com.atlassian.confluence.extra.jira.api.services.JiraMacroFinderService;
import com.atlassian.confluence.extra.jira.exception.UnsupportedJiraServerException;
import com.atlassian.confluence.extra.jira.executor.FutureStreamableConverter;
import com.atlassian.confluence.extra.jira.executor.StreamableMacroExecutor;
import com.atlassian.confluence.extra.jira.executor.StreamableMacroFutureTask;
import com.atlassian.confluence.extra.jira.helper.JiraExceptionHelper;
import com.atlassian.confluence.extra.jira.model.EntityServerCompositeKey;
import com.atlassian.confluence.extra.jira.model.JiraBatchRequestData;
import com.atlassian.confluence.extra.jira.util.JiraUtil;
import com.atlassian.confluence.extra.jira.util.MapUtil;
import com.atlassian.confluence.languages.LocaleManager;
import com.atlassian.confluence.macro.EditorImagePlaceholder;
import com.atlassian.confluence.macro.MacroExecutionException;
import com.atlassian.confluence.macro.ResourceAware;
import com.atlassian.confluence.macro.StreamableMacro;
import com.atlassian.confluence.plugins.jira.render.JiraIssueRenderFactory;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.confluence.util.i18n.I18NBeanFactory;
import com.atlassian.confluence.xhtml.api.MacroDefinition;
import com.atlassian.renderer.RenderContextOutputType;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import org.jdom.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

/**
 * A macro to import/fetch JIRA issues...
 */
public class StreamableJiraIssuesMacro extends JiraIssuesMacro implements StreamableMacro, EditorImagePlaceholder, ResourceAware
{
    private static final Logger LOGGER = LoggerFactory.getLogger(StreamableJiraIssuesMacro.class);

    public static final int THREAD_POOL_SIZE = Integer.getInteger("jira.executor.threadpool.size", 4);

    private StreamableMacroExecutor executorService;
    private JiraMacroFinderService jiraMacroFinderService;
    private JiraIssueBatchService jiraIssueBatchService;
    private ApplicationLinkResolver applicationLinkResolver;
    private JiraExceptionHelper jiraExceptionHelper;

    /**
     * Default constructor to get all necessary beans injected
     *
     * @param i18NBeanFactory
     * @param applicationLinkResolver
     * @param jiraExceptionHelper
     * @param localeManager
     * @param executorService
     * @param jiraMacroFinderService
     * @param jiraIssueBatchService
     */
    public StreamableJiraIssuesMacro(I18NBeanFactory i18NBeanFactory, ApplicationLinkResolver applicationLinkResolver, JiraExceptionHelper jiraExceptionHelper, LocaleManager localeManager,
                                     JiraIssueRenderFactory jiraIssueRenderFactory, StreamableMacroExecutor executorService, JiraMacroFinderService jiraMacroFinderService, JiraIssueBatchService jiraIssueBatchService)
    {
        super(i18NBeanFactory, localeManager, jiraIssueRenderFactory);
        this.executorService = executorService;
        this.jiraMacroFinderService = jiraMacroFinderService;
        this.jiraIssueBatchService = jiraIssueBatchService;
        this.jiraExceptionHelper = jiraExceptionHelper;
        this.applicationLinkResolver = applicationLinkResolver;
    }

    /**
     * In this method, batch requests for single JIRA issues will be sent
     *
     * @param parameters        the macro parameters
     * @param body              the macro body
     * @param conversionContext the page's conversion context
     * @return the Streamable representing the macro's rendered content
     * @throws MacroExecutionException
     */
    public Streamable executeToStream(final Map<String, String> parameters, final Streamable body,
                                      final ConversionContext conversionContext) throws MacroExecutionException
    {
        ContentEntityObject entity = conversionContext.getEntity();
        if (parameters != null && JiraUtil.getSingleIssueKey(parameters) != null && entity != null)
        {
            trySingleIssuesBatching(conversionContext, entity);
        }

        final Future<String> futureResult = marshallMacroInBackground(parameters, conversionContext, entity);

        return new FutureStreamableConverter.Builder(futureResult, conversionContext, getI18NBean())
                .executionErrorMsg("jiraissues.error.execution")
                .executionTimeoutErrorMsg("jiraissues.error.timeout.execution")
                .connectionTimeoutErrorMsg("jiraissues.error.timeout.connection")
                .interruptedErrorMsg("jiraissues.error.interrupted").build();
    }

    /**
     * This method sends batch requests to JIRA server and store results into the ThreadLocal map
     * managed by the SingleJiraIssuesThreadLocalAccessor
     *
     * @param conversionContext the page's conversion context
     * @throws MacroExecutionException
     */
    private void trySingleIssuesBatching(ConversionContext conversionContext, ContentEntityObject entity) throws MacroExecutionException
    {
        long entityId = entity.getId();
        // Temporarily skip processing if JIMs are rendered for email
        if (conversionContext.getOutputDeviceType().equals(RenderContextOutputType.EMAIL))
        {
            return;
        }
        if (SingleJiraIssuesThreadLocalAccessor.isBatchProcessed(entityId))
        {
            return;
        }
        long batchStart = System.currentTimeMillis();
        try
        {
            String content = entity.getBodyContent().getBody();
            // We find all MacroDefinitions for single JIRA issues in the body
            final Set<MacroDefinition> singleIssueMacroDefinitions;
            try
            {
                long finderStart = System.currentTimeMillis();
                singleIssueMacroDefinitions = this.jiraMacroFinderService.findSingleJiraIssueMacros(content, conversionContext);
                if (LOGGER.isDebugEnabled())
                {
                    LOGGER.debug("******* findSingleJiraIssueMacros time = {}", System.currentTimeMillis() - finderStart);
                }
                // We use a HashMultimap to store the [serverId: set of keys] pairs because duplicate serverId-key pair will not be stored
                Multimap<String, String> jiraServerIdToKeysMap = HashMultimap.create();

                HashMap<String, Map<String, String>> jiraServerIdToParameters = Maps.newHashMap();

                // Collect all possible server IDs from the macro definitions
                for (MacroDefinition singleIssueMacroDefinition : singleIssueMacroDefinitions)
                {
                    String serverId = null;
                    String key = singleIssueMacroDefinition.getParameter(JiraIssuesMacro.KEY);
                    try
                    {
                        serverId = getServerIdFromKey(singleIssueMacroDefinition.getParameters(), key, conversionContext);
                    }
                    catch (MacroExecutionException e) // suppress this exception
                    {
                        if (LOGGER.isDebugEnabled())
                        {
                            LOGGER.debug(e.toString());
                        }
                    }
                    if (serverId != null)
                    {
                        jiraServerIdToKeysMap.put(serverId, key);
                        if (jiraServerIdToParameters.get(serverId) == null)
                        {
                            jiraServerIdToParameters.put(serverId, MapUtil.copyOf(singleIssueMacroDefinition.getParameters()));
                        }
                    }
                }
                for (String serverId : jiraServerIdToKeysMap.keySet())
                {
                    Set<String> keys = (Set<String>) jiraServerIdToKeysMap.get(serverId);
                    // make request to the same JIRA server for the whole set of keys
                    // and putElement the individual data of each key into the SingleJiraIssuesThreadLocalAccessor
                    JiraBatchRequestData jiraBatchRequestData = new JiraBatchRequestData();
                    try
                    {
                        Map<String, Object> resultsMap = this.jiraIssueBatchService.getBatchResults(serverId, keys, conversionContext);
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
                        SingleJiraIssuesThreadLocalAccessor.putJiraBatchRequestData(new EntityServerCompositeKey(entityId, serverId), jiraBatchRequestData);
                    }
                }
            }
            catch (XhtmlException e)
            {
                if (LOGGER.isDebugEnabled())
                {
                    LOGGER.debug(e.toString());
                }
                throw new MacroExecutionException(e.getCause());
            }
        }
        finally
        {
            SingleJiraIssuesThreadLocalAccessor.setBatchProcessedMapThreadLocal(entityId, Boolean.TRUE); // Single JIRA issues will be processed in batch
            if (LOGGER.isDebugEnabled())
            {
                LOGGER.debug("******* batch time = {}", System.currentTimeMillis() - batchStart);
            }
        }

    }

    /**
     * Private method responsible for submitting a new StreamableMacroFutureTask instance into the thread pool for
     * later processing
     *
     * @param parameters        the macro parameters
     * @param conversionContext the conversionContext associated with the macro
     * @return the Future (result) of the task
     */
    private Future<String> marshallMacroInBackground(final Map<String, String> parameters, final ConversionContext conversionContext, final ContentEntityObject entity) throws MacroExecutionException
    {
        // if this macro is for rendering a single issue then we must get the resulting element from the SingleJiraIssuesThreadLocalAccessor
        // the element must be available now because we already request all JIRA issues as batches in trySingleIssuesBatching
        String key = JiraUtil.getSingleIssueKey(parameters);
        if (key != null && entity != null)
        {
            try
            {
                String serverId = getServerIdFromKey(parameters, key, conversionContext);
                if (serverId != null)
                {
                    long entityId = entity.getId();
                    JiraBatchRequestData jiraBatchRequestData = SingleJiraIssuesThreadLocalAccessor.getJiraBatchRequestData(new EntityServerCompositeKey(entityId, serverId));
                    if (jiraBatchRequestData != null)
                    {
                        Map<String, Element> elementMap = jiraBatchRequestData.getElementMap();
                        Element element = elementMap != null ? elementMap.get(key) : null;
                        String jiraServerUrl = jiraBatchRequestData.getServerUrl();
                        Exception exception = jiraBatchRequestData.getException();
                        return executorService.submit(new StreamableMacroFutureTask(jiraExceptionHelper, parameters, conversionContext, this, AuthenticatedUserThreadLocal.get(), element, jiraServerUrl, exception));
                    }
                }
                else
                {
                    // Couldn't get the app link, delegating to JiraIssuesMacro to render the error message
                    return executorService.submit(new StreamableMacroFutureTask(jiraExceptionHelper, parameters, conversionContext, this, AuthenticatedUserThreadLocal.get()));
                }
            }
            catch (MacroExecutionException macroExecutionException)
            {
                if (LOGGER.isDebugEnabled())
                {
                    LOGGER.debug(macroExecutionException.toString());
                }
                final String exceptionMessage = macroExecutionException.getMessage();
                return executorService.submit(new Callable<String>()
                {
                    @Override
                    public String call() throws Exception
                    {
                        return jiraExceptionHelper.renderBatchingJIMExceptionMessage(exceptionMessage, parameters);
                    }
                });
            }
        }

        return executorService.submit(new StreamableMacroFutureTask(jiraExceptionHelper, parameters, conversionContext, this, AuthenticatedUserThreadLocal.get()));
    }

    private String getServerIdFromKey(Map<String, String> parameters, String issueKey, ConversionContext conversionContext) throws MacroExecutionException
    {
        // We get the server ID through the ApplicationLink object because there can be no serverId and/or server specified
        // in the macro markup
        try
        {
            ApplicationLink applicationLink = this.applicationLinkResolver.resolve(Type.KEY, issueKey, parameters);
            if (applicationLink != null)
            {
                return applicationLink.getId().toString();
            }
        }
        catch (TypeNotInstalledException e)
        {
            jiraExceptionHelper.throwMacroExecutionException(e, conversionContext);
        }
        return null;
    }
}
