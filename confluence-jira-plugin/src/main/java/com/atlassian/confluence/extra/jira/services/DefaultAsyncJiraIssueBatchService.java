package com.atlassian.confluence.extra.jira.services;

import com.atlassian.cache.Cache;
import com.atlassian.cache.CacheManager;
import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.core.ContentEntityObject;
import com.atlassian.confluence.extra.jira.JiraIssuesMacro;
import com.atlassian.confluence.extra.jira.api.services.AsyncJiraIssueBatchService;
import com.atlassian.confluence.extra.jira.api.services.JiraIssueBatchService;
import com.atlassian.confluence.extra.jira.exception.UnsupportedJiraServerException;
import com.atlassian.confluence.extra.jira.executor.StreamableMacroExecutor;
import com.atlassian.confluence.extra.jira.executor.StreamableMacroFutureTask;
import com.atlassian.confluence.extra.jira.helper.JiraExceptionHelper;
import com.atlassian.confluence.extra.jira.model.EntityServerCompositeKey;
import com.atlassian.confluence.extra.jira.model.JiraBatchResponseData;
import com.atlassian.confluence.macro.MacroExecutionException;
import com.atlassian.confluence.macro.StreamableMacro;
import com.atlassian.confluence.macro.xhtml.MacroManager;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.confluence.xhtml.api.MacroDefinition;
import com.atlassian.sal.api.executor.ThreadLocalDelegateExecutorFactory;
import com.google.common.collect.*;
import org.jdom.Element;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class DefaultAsyncJiraIssueBatchService implements AsyncJiraIssueBatchService
{
    private final int BATCH_SIZE = 20;
    private final JiraIssueBatchService jiraIssueBatchService;
    private final MacroManager macroManager;
    private final ThreadLocalDelegateExecutorFactory threadLocalDelegateExecutorFactory;
    private final StreamableMacroExecutor streamableMacroExecutor;
    private final JiraExceptionHelper jiraExceptionHelper;
    private final Cache jiraIssueResult;

    public DefaultAsyncJiraIssueBatchService(JiraIssueBatchService jiraIssueBatchService, MacroManager macroManager,
                                             ThreadLocalDelegateExecutorFactory threadLocalDelegateExecutorFactory, StreamableMacroExecutor streamableMacroExecutor,
                                             JiraExceptionHelper jiraExceptionHelper, CacheManager cacheManager)
    {
        this.jiraIssueBatchService = jiraIssueBatchService;
        this.macroManager = macroManager;
        this.threadLocalDelegateExecutorFactory = threadLocalDelegateExecutorFactory;
        this.streamableMacroExecutor = streamableMacroExecutor;
        this.jiraExceptionHelper = jiraExceptionHelper;
        jiraIssueResult = cacheManager.getCache(JiraIssuesMacro.class.getName());
    }

    public JiraBatchResponseData getAsyncBatchResults(long entityId, String serverId) throws Exception
    {
        EntityServerCompositeKey key = new EntityServerCompositeKey(AuthenticatedUserThreadLocal.getUsername(), entityId, serverId);
        JiraBatchResponseData jiraBatchResponseData = (JiraBatchResponseData) jiraIssueResult.get(key);
        if (jiraBatchResponseData == null)
        {
            throw new IllegalStateException(String.format("Jira issues for this entity/server (%s/%s) is not available", entityId, serverId));
        }
        if (jiraBatchResponseData.getBatchStatus() == JiraBatchResponseData.BatchStatus.COMPLETED)
        {
            jiraIssueResult.remove(key);
            jiraBatchResponseData.setServerId(serverId);
        }
        return jiraBatchResponseData;
    }

    @Override
    public void processBatchRequest(final ContentEntityObject entity, final String serverId, final Set<String> keys, final List<MacroDefinition> macroDefinitions, final ConversionContext conversionContext)
    {
        final StreamableMacro jiraIssuesMacro = (StreamableMacro) macroManager.getMacroByName(JiraIssuesMacro.JIRA);
        final EntityServerCompositeKey entityServerCompositeKey = new EntityServerCompositeKey(AuthenticatedUserThreadLocal.getUsername(), entity.getId(), serverId);
        Callable<Map<String, List<String>>> jiraIssueCallable = threadLocalDelegateExecutorFactory.createCallable(new Callable<Map<String, List<String>>>() {
            public Map<String, List<String>> call() throws Exception
            {
                ListMultimap<String, String> jiraResults = ArrayListMultimap.create();
                try
                {
                    Map<String, Object> resultsMap = getJiraIssues(serverId, keys, conversionContext);
                    Map<String, Element> elementMap = (Map<String, Element>) resultsMap.get(JiraIssueBatchService.ELEMENT_MAP);
                    String jiraServerUrl = (String) resultsMap.get(JiraIssueBatchService.JIRA_SERVER_URL);

                    for (MacroDefinition macroDefinition : macroDefinitions)
                    {
                        String issueKey = macroDefinition.getParameter(JiraIssuesMacro.KEY);
                        Element issueElement = (elementMap == null) ? null : elementMap.get(issueKey);
                        Future<String> futureHtmlMacro = streamableMacroExecutor.submit(new StreamableMacroFutureTask(jiraExceptionHelper, macroDefinition.getParameters(), conversionContext, jiraIssuesMacro, AuthenticatedUserThreadLocal.get(), issueElement, jiraServerUrl, null));
                        jiraResults.get(issueKey).add(futureHtmlMacro.get());
                    }
                }
                catch (Exception ex) //getJiraIssues throw exception
                {
                    for (MacroDefinition macroDefinition : macroDefinitions)
                    {
                        String issueKey = macroDefinition.getParameter(JiraIssuesMacro.KEY);
                        Future<String> futureHtmlMacro = streamableMacroExecutor.submit(new StreamableMacroFutureTask(jiraExceptionHelper, macroDefinition.getParameters(), conversionContext, jiraIssuesMacro, AuthenticatedUserThreadLocal.get(), null, null, ex));
                        jiraResults.get(issueKey).add(futureHtmlMacro.get());
                    }
                }

                //avoid checking error convertion, 'asMap' will return a Map<String, Collection<String>>, however it 's Map<String, List<String>> as expectation
                Map<String, List<String>> jiraResultMap = (Map) jiraResults.asMap();
                jiraIssueResult.put(entityServerCompositeKey, new JiraBatchResponseData(jiraResultMap));
                return jiraResultMap;
            }
        });

        streamableMacroExecutor.submit(jiraIssueCallable);
        jiraIssueResult.put(entityServerCompositeKey, new JiraBatchResponseData());
    }

    private Map<String, Object> getJiraIssues(final String serverId, Set<String> keys, final ConversionContext conversionContext) throws ExecutionException, InterruptedException, UnsupportedJiraServerException, MacroExecutionException
    {
        if (keys.size() <= BATCH_SIZE)
        {
            return jiraIssueBatchService.getBatchResults(serverId, keys, conversionContext);
        }
        List<List<String>> batchRequests = Lists.partition(Lists.newArrayList(keys), BATCH_SIZE);
        List<Future<Map<String, Object>>> futureResults = Lists.newArrayList();
        for (final List<String> issueKeys: batchRequests)
        {
            Callable<Map<String, Object>> subCallable = threadLocalDelegateExecutorFactory.createCallable(new Callable<Map<String, Object>>()
            {
                @Override
                public Map<String, Object> call() throws Exception
                {
                    return jiraIssueBatchService.getBatchResults(serverId, ImmutableSet.copyOf(issueKeys), conversionContext);
                }
            });
            futureResults.add(streamableMacroExecutor.submit(subCallable));
        }
        Map<String, Object> resultsMap = Maps.newHashMap();
        for(Future<Map<String, Object>> future: futureResults)
        {
            resultsMap.putAll(future.get());
        }
        return resultsMap;
    }
}
