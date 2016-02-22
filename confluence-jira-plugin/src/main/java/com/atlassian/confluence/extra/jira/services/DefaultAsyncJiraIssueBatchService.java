package com.atlassian.confluence.extra.jira.services;

import com.atlassian.applinks.api.ReadOnlyApplicationLink;
import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.content.render.xhtml.DefaultConversionContext;
import com.atlassian.confluence.content.render.xhtml.XhtmlException;
import com.atlassian.confluence.core.ContentEntityManager;
import com.atlassian.confluence.core.ContentEntityObject;
import com.atlassian.confluence.extra.jira.JiraIssuesMacro;
import com.atlassian.confluence.extra.jira.JiraIssuesManager;
import com.atlassian.confluence.extra.jira.StreamableJiraIssuesMacro;
import com.atlassian.confluence.extra.jira.api.services.AsyncJiraIssueBatchService;
import com.atlassian.confluence.extra.jira.api.services.JiraIssueBatchService;
import com.atlassian.confluence.extra.jira.api.services.JiraMacroFinderService;
import com.atlassian.confluence.extra.jira.executor.JiraExecutorFactory;
import com.atlassian.confluence.extra.jira.executor.StreamableMacroFutureTask;
import com.atlassian.confluence.extra.jira.helper.JiraExceptionHelper;
import com.atlassian.confluence.extra.jira.model.ClientId;
import com.atlassian.confluence.extra.jira.model.JiraResponseData;
import com.atlassian.confluence.extra.jira.util.JiraIssuePredicates;
import com.atlassian.confluence.extra.jira.util.JiraIssueUtil;
import com.atlassian.confluence.macro.MacroExecutionException;
import com.atlassian.confluence.macro.StreamableMacro;
import com.atlassian.confluence.macro.xhtml.MacroManager;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.confluence.xhtml.api.MacroDefinition;
import com.atlassian.vcache.*;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;
import org.apache.commons.lang.StringUtils;
import org.jdom.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;

import javax.annotation.ParametersAreNonnullByDefault;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;

@ParametersAreNonnullByDefault
public class DefaultAsyncJiraIssueBatchService implements AsyncJiraIssueBatchService, DisposableBean
{
    private static final Logger logger = LoggerFactory.getLogger(DefaultAsyncJiraIssueBatchService.class);
    private static final String CACHE_NAME = "com.atlassian.confluence.extra.jira.services.DefaultAsyncJiraIssueBatchService";
    private static final int THREAD_POOL_SIZE = Integer.getInteger("confluence.jira.issues.executor.poolsize", 5);
    private static final int EXECUTOR_QUEUE_SIZE = Integer.getInteger("confluence.jira.issues.executor.queuesize", 1000);
    private static final int CACHE_EXPIRE_AFTER_WRITE = Integer.getInteger("confluence.extra.jira.cache.async.write.expire", 120);
    private static final int BATCH_SIZE = 25;
    private static final String ISSUE_KEY_TABLE_PREFIX = "issue-table-";
    private static final String ISSUE_KEY_COUNT_PREFIX = "issue-count-";
    private final JiraIssueBatchService jiraIssueBatchService;
    private final MacroManager macroManager;
    private final JiraExceptionHelper jiraExceptionHelper;
    private final JvmCache<ClientId, JiraResponseData> jiraIssuesCache;

    private final ExecutorService jiraIssueExecutor;
    private final JiraIssuesManager jiraIssuesManager;
    private final ContentEntityManager contentEntityManager;
    private final JiraMacroFinderService jiraMacroFinderService;

    public DefaultAsyncJiraIssueBatchService(JiraIssueBatchService jiraIssueBatchService, MacroManager macroManager,
                                             JiraExecutorFactory executorFactory,
                                             JiraExceptionHelper jiraExceptionHelper, VCacheFactory vcacheFactory,
                                             JiraIssuesManager jiraIssuesManager,
                                             ContentEntityManager contentEntityManager,
                                             JiraMacroFinderService jiraMacroFinderService)
    {
        this.jiraIssueBatchService = jiraIssueBatchService;
        this.macroManager = macroManager;
        this.jiraIssueExecutor = executorFactory.newLimitedThreadPool(THREAD_POOL_SIZE, EXECUTOR_QUEUE_SIZE, "JIM Marshaller");
        this.jiraExceptionHelper = jiraExceptionHelper;
        this.jiraIssuesManager = jiraIssuesManager;
        this.contentEntityManager = contentEntityManager;
        this.jiraMacroFinderService = jiraMacroFinderService;
        this.jiraIssuesCache = vcacheFactory.getJvmCache(CACHE_NAME,
                new JvmCacheSettingsBuilder()
                        .defaultTtl(Duration.ofSeconds(CACHE_EXPIRE_AFTER_WRITE))
                        .maxEntries(500)
                        .build());
    }

    @Override
    public boolean reprocessRequest(final ClientId clientId) throws XhtmlException, MacroExecutionException
    {
        //avoid any history back/forward with different user
        if (!StringUtils.equals(clientId.getUserId(), JiraIssueUtil.getUserKey(AuthenticatedUserThreadLocal.get())))
        {
            return false;
        }
        final StreamableJiraIssuesMacro jiraIssuesMacro = (StreamableJiraIssuesMacro) macroManager.getMacroByName(JiraIssuesMacro.JIRA);

        ContentEntityObject entity = contentEntityManager.getById(Long.valueOf(clientId.getPageId()));
        if (clientId.getJiraIssuesType() == JiraIssuesMacro.JiraIssuesType.SINGLE)
        {
            ListMultimap<String, MacroDefinition> macroDefinitionByServer = jiraIssuesMacro.getSingleIssueMacroDefinitionByServer(entity);
            if (macroDefinitionByServer == null || macroDefinitionByServer.isEmpty())
            {
                return false;
            }
            processRequest(clientId,
                    clientId.getServerId(),
                    JiraIssueUtil.getIssueKeys(macroDefinitionByServer.get(clientId.getServerId())),
                    macroDefinitionByServer.get(clientId.getServerId()), new DefaultConversionContext(entity.toPageContext()));
        }
        else
        {
            Predicate<MacroDefinition> predicate = clientId.getJiraIssuesType() == JiraIssuesMacro.JiraIssuesType.COUNT ?
                    JiraIssuePredicates.isCountIssue : JiraIssuePredicates.isTableIssue;
            Predicate<MacroDefinition> jqlPredicate = Predicates.and(predicate, new Predicate<MacroDefinition>()
            {
                @Override
                public boolean apply(MacroDefinition macroDefinition)
                {
                    Map<String, String> parameters = macroDefinition.getParameters();
                    return StringUtils.equals(String.valueOf(parameters.get("jqlQuery")), clientId.getJqlQuery());
                }
            });

            List<MacroDefinition> macros = jiraMacroFinderService.findJiraMacros(entity, jqlPredicate);
            if (CollectionUtils.isEmpty(macros))
            {
                return false;
            }
            for (MacroDefinition macroDefinition : macros)
            {
                if (macroDefinition.getDefaultParameterValue() != null)
                {
                    macroDefinition.getParameters().put("0", macroDefinition.getDefaultParameterValue());
                }
                jiraIssuesMacro.execute(macroDefinition.getParameters(), "", new DefaultConversionContext(entity.toPageContext()));
            }
        }
        return true;
    }

    @Override
    public void processRequest(final ClientId clientId, String serverId,
                               final Set<String> keys, final List<MacroDefinition> macroDefinitions,
                               final ConversionContext conversionContext)
    {
        // allocate an empty space for response data in the cache
        Optional<JiraResponseData> existingJiraReponseData = jiraIssuesCache.get(clientId);
        if (existingJiraReponseData.isPresent())
        {
            existingJiraReponseData.get().increaseStackCount();
            return;
        }
        jiraIssuesCache.put(clientId, new JiraResponseData(serverId, keys.size()));

        List<List<String>> batchRequests = Lists.partition(Lists.newArrayList(keys), BATCH_SIZE);

        Callable<Map<String, List<String>>> jiraIssueBatchTask;
        for (final List<String> batchRequest : batchRequests)
        {
            jiraIssueBatchTask = buildBatchTask(clientId, serverId,
                                                batchRequest, macroDefinitions,
                                                conversionContext);

            try
            {
                jiraIssueExecutor.submit(jiraIssueBatchTask);
                logger.debug("Submitted task to thread pool. {}", jiraIssueExecutor.toString());
            }
            catch (RejectedExecutionException e)
            {
                logger.error("JIM Marshaller rejected task because there are more than 1000 tasks queued. {}", jiraIssueExecutor.toString(), e);
                throw e;
            }
        }
    }

    public void processRequestWithJql(final ClientId clientId, final Map<String, String> macroParams, final ConversionContext conversionContext,
                                      final ReadOnlyApplicationLink appLink) throws MacroExecutionException
    {
        Optional<JiraResponseData> existingJiraReponseData = jiraIssuesCache.get(clientId);
        if (existingJiraReponseData.isPresent())
        {
            existingJiraReponseData.get().increaseStackCount();
            return;
        }
        final StreamableMacro jiraIssuesMacro = (StreamableMacro) macroManager.getMacroByName(JiraIssuesMacro.JIRA);

        final JiraResponseData jiraResponseData = new JiraResponseData(appLink.getId().get(), 1);
        jiraIssuesCache.put(clientId, jiraResponseData);
        Callable<Map<String, List<String>>> jiraTableCallable = new Callable<Map<String, List<String>>>() {
            public Map<String, List<String>> call() throws Exception
            {
                ConversionContext newConvertionContext = new DefaultConversionContext(conversionContext.getPageContext());
                newConvertionContext.setProperty(JiraIssuesMacro.PARAM_PLACEHOLDER, false);
                newConvertionContext.setProperty(JiraIssuesMacro.CLIENT_ID, clientId.toString());
                StreamableMacroFutureTask streamableMacroFutureTask = new StreamableMacroFutureTask(jiraExceptionHelper, macroParams, newConvertionContext,
                        jiraIssuesMacro, AuthenticatedUserThreadLocal.get());

                MultiMap jiraResultMap = new MultiValueMap();
                String asyncKey = (Boolean.parseBoolean(macroParams.get("count")) ? ISSUE_KEY_COUNT_PREFIX : ISSUE_KEY_TABLE_PREFIX) + clientId;
                jiraResultMap.put(asyncKey, streamableMacroFutureTask.renderValue());
                jiraIssuesCache.get(clientId).ifPresent(value -> value.add(jiraResultMap));
                return jiraResultMap;
            }
        };
        jiraIssueExecutor.submit(jiraTableCallable);

    }

    @Override
    public JiraResponseData getAsyncJiraResults(ClientId clientId)
    {
        Optional<JiraResponseData> jiraResponseData = jiraIssuesCache.get(clientId);
        jiraResponseData.ifPresent(data -> {
            if (data.getStatus() == JiraResponseData.Status.COMPLETED)
            {
                if (data.decreaseStackCount() == 0)
                {
                    jiraIssuesCache.remove(clientId);
                }
            }
        });
        return jiraResponseData.orElse(null);
    }

    private Callable<Map<String, List<String>>> buildBatchTask(final ClientId clientId,
                                    final String serverId,
                                    final List<String> batchRequest,
                                    final List<MacroDefinition> macroDefinitions,
                                    final ConversionContext conversionContext)
    {
        final StreamableMacro jiraIssuesMacro = (StreamableMacro) macroManager.getMacroByName(JiraIssuesMacro.JIRA);

        return new Callable<Map<String, List<String>>>() {
            public Map<String, List<String>> call() throws Exception
            {
                Map<String, Object> issueResultsMap;
                Exception exception = null;
                try
                {
                    issueResultsMap = jiraIssueBatchService.getBatchResults(serverId, ImmutableSet.copyOf(batchRequest), conversionContext);
                }
                catch (Exception e)
                {
                    issueResultsMap = Maps.newHashMap();
                    exception = e;
                }

                //take the result and render
                MultiMap jiraResultMap = new MultiValueMap();
                Map<String, Element> elementMap = (Map<String, Element>) issueResultsMap.get(JiraIssueBatchService.ELEMENT_MAP);

                String jiraDisplayUrl = (String) issueResultsMap.get(JiraIssueBatchService.JIRA_DISPLAY_URL);
                String jiraRpcUrl = (String) issueResultsMap.get(JiraIssueBatchService.JIRA_RPC_URL);

                for (MacroDefinition macroDefinition : macroDefinitions)
                {
                    String issueKey = macroDefinition.getParameter(JiraIssuesMacro.KEY);
                    if (batchRequest.contains(issueKey))
                    {
                        Element issueElement = (elementMap == null) ? null : elementMap.get(issueKey);
                        jiraResultMap.put(issueKey, new StreamableMacroFutureTask(jiraExceptionHelper, macroDefinition.getParameters(), conversionContext, jiraIssuesMacro, AuthenticatedUserThreadLocal.get(), issueElement, jiraDisplayUrl, jiraRpcUrl, exception).renderValue());
                    }
                }

                jiraIssuesCache.get(clientId).ifPresent(data -> data.add(jiraResultMap));
                return jiraResultMap;
            }
        };
    }

    @Override
    public void destroy() throws Exception
    {
        jiraIssueExecutor.shutdown();
    }
}
