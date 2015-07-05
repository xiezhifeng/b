package com.atlassian.confluence.extra.jira.services;

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
import com.atlassian.confluence.extra.jira.model.JiraBatchProcessor;
import com.atlassian.confluence.extra.jira.model.JiraBatchResponseData;
import com.atlassian.confluence.macro.MacroExecutionException;
import com.atlassian.confluence.macro.StreamableMacro;
import com.atlassian.confluence.macro.xhtml.MacroManager;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.sal.api.executor.ThreadLocalDelegateExecutorFactory;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.jdom.Element;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class DefaultAsyncJiraIssueBatchService implements AsyncJiraIssueBatchService
{
    private final JiraIssueBatchService jiraIssueBatchService;
    private final MacroManager macroManager;
    private final ThreadLocalDelegateExecutorFactory threadLocalDelegateExecutorFactory;
    private final StreamableMacroExecutor streamableMacroExecutor;
    private final JiraExceptionHelper jiraExceptionHelper;

    private final Cache<EntityServerCompositeKey, JiraBatchProcessor> jiraIssueFutureResult;

    public DefaultAsyncJiraIssueBatchService(JiraIssueBatchService jiraIssueBatchService, MacroManager macroManager,
                                             ThreadLocalDelegateExecutorFactory threadLocalDelegateExecutorFactory, StreamableMacroExecutor streamableMacroExecutor,
                                             JiraExceptionHelper jiraExceptionHelper)
    {
        this.jiraIssueBatchService = jiraIssueBatchService;
        this.macroManager = macroManager;
        this.threadLocalDelegateExecutorFactory = threadLocalDelegateExecutorFactory;
        this.streamableMacroExecutor = streamableMacroExecutor;
        this.jiraExceptionHelper = jiraExceptionHelper;
        jiraIssueFutureResult = CacheBuilder.newBuilder()
            .expireAfterWrite(30, TimeUnit.SECONDS)
            .build(new CacheLoader<EntityServerCompositeKey, JiraBatchProcessor>() {
                public JiraBatchProcessor load(EntityServerCompositeKey key) {
                    return null;
                }
            });
    }

    public JiraBatchResponseData getAsyncBatchResults(long entityId, String serverId) throws Exception
    {
        EntityServerCompositeKey key = new EntityServerCompositeKey(AuthenticatedUserThreadLocal.getUsername(), entityId, serverId);
        JiraBatchProcessor jiraBatchProcessor = jiraIssueFutureResult.getUnchecked(key);
        jiraIssueFutureResult.invalidate(key);

        Map<String, String> jiraIssueResult = jiraBatchProcessor.getFutureResult().get();
        JiraBatchResponseData jiraBatchResponseData = new JiraBatchResponseData();
        jiraBatchResponseData.setHtmlMacro(jiraIssueResult);
        jiraBatchResponseData.setServerId(serverId);
        jiraBatchResponseData.setIssueKeys(Lists.newArrayList(jiraIssueResult.keySet()));
        return jiraBatchResponseData;
    }

    @Override
    public JiraBatchProcessor processBatchRequest(ContentEntityObject entity, final String serverId, final Set<String> keys, final ConversionContext conversionContext)
    {
        final JiraBatchProcessor jiraBatchProcessor = new JiraBatchProcessor();
        Callable<Map<String, String>> jiraIssueCallable = threadLocalDelegateExecutorFactory.createCallable(new Callable<Map<String, String>>() {
            public Map<String, String> call() throws Exception
            {
                Map<String, Object> resultsMap = jiraIssueBatchService.getBatchResults(serverId, keys, conversionContext);
                Map<String, Element> elementMap = (Map<String, Element>) resultsMap.get(JiraIssueBatchService.ELEMENT_MAP);
                String jiraServerUrl = (String) resultsMap.get(JiraIssueBatchService.JIRA_SERVER_URL);
                StreamableMacro jiraIssuesMacro = (StreamableMacro) macroManager.getMacroByName("jira");

                Map<String, String> jiraResults = Maps.newHashMapWithExpectedSize(keys.size());
                Map<String, Map<String, String>> allMacroParameters = jiraBatchProcessor.getSafeParameters().get(10, TimeUnit.SECONDS);
                for (String jiraIssueKey : keys)
                {
                    Map<String, String> parameters = allMacroParameters.get(jiraIssueKey);
                    Element issueElement = elementMap.get(jiraIssueKey);
                    Future<String> futureHtmlMacro = streamableMacroExecutor.submit(new StreamableMacroFutureTask(jiraExceptionHelper, parameters, conversionContext, jiraIssuesMacro, AuthenticatedUserThreadLocal.get(), issueElement, jiraServerUrl, null));
                    jiraResults.put(jiraIssueKey, futureHtmlMacro.get());
                }
                return jiraResults;
            }
        });
        Future<Map<String, String>> futureResult = streamableMacroExecutor.submit(jiraIssueCallable);
        jiraBatchProcessor.setFutureResult(futureResult);
        jiraBatchProcessor.setIssueKeys(Lists.newArrayList(keys));
        jiraIssueFutureResult.asMap().put(new EntityServerCompositeKey(AuthenticatedUserThreadLocal.getUsername(), entity.getId(), serverId), jiraBatchProcessor);
        return jiraBatchProcessor;
    }

}
