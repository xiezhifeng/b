package com.atlassian.confluence.extra.jira.services;

import com.atlassian.cache.Cache;
import com.atlassian.cache.CacheManager;
import com.atlassian.cache.CacheSettingsBuilder;
import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.extra.jira.JiraIssuesMacro;
import com.atlassian.confluence.extra.jira.api.services.AsyncJiraIssueBatchService;
import com.atlassian.confluence.extra.jira.api.services.JiraIssueBatchService;
import com.atlassian.confluence.extra.jira.executor.StreamableMacroFutureTask;
import com.atlassian.confluence.extra.jira.helper.JiraExceptionHelper;
import com.atlassian.confluence.extra.jira.model.JiraResponseData;
import com.atlassian.confluence.macro.StreamableMacro;
import com.atlassian.confluence.macro.xhtml.MacroManager;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.confluence.xhtml.api.MacroDefinition;
import com.atlassian.sal.api.executor.ThreadLocalDelegateExecutorFactory;
import com.atlassian.util.concurrent.ThreadFactories;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;
import org.jdom.Element;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class DefaultAsyncJiraIssueBatchService implements AsyncJiraIssueBatchService
{
    private static final int BATCH_SIZE = 25;
    private final JiraIssueBatchService jiraIssueBatchService;
    private final MacroManager macroManager;
    private final ThreadLocalDelegateExecutorFactory threadLocalDelegateExecutorFactory;
    private final JiraExceptionHelper jiraExceptionHelper;
    private final Cache jiraIssuesCache;

    private final ExecutorService jiraIssueExecutorService = Executors.newCachedThreadPool(ThreadFactories.named("JIM Marshaller-")
            .type(ThreadFactories.Type.USER).build());


    public DefaultAsyncJiraIssueBatchService(JiraIssueBatchService jiraIssueBatchService, MacroManager macroManager,
                                             ThreadLocalDelegateExecutorFactory threadLocalDelegateExecutorFactory,
                                             JiraExceptionHelper jiraExceptionHelper, CacheManager cacheManager)
    {
        this.jiraIssueBatchService = jiraIssueBatchService;
        this.macroManager = macroManager;
        this.threadLocalDelegateExecutorFactory = threadLocalDelegateExecutorFactory;
        this.jiraExceptionHelper = jiraExceptionHelper;
        jiraIssuesCache = cacheManager.getCache(DefaultAsyncJiraIssueBatchService.class.getName(), null,
                new CacheSettingsBuilder()
                        .remote()
                        .replicateViaCopy()
                        .replicateAsynchronously()
                        .maxEntries(500)
                        .unflushable()
                        .expireAfterWrite(2, TimeUnit.MINUTES)
                        .build()
        );
    }

    @Override
    public void processRequest(final String clientId, String serverId,
                               final Set<String> keys, final List<MacroDefinition> macroDefinitions,
                               final ConversionContext conversionContext)
    {
        // allocate an empty space for response data in the cache
        jiraIssuesCache.put(clientId, new JiraResponseData(serverId, keys.size()));

        List<List<String>> batchRequests = Lists.partition(Lists.newArrayList(keys), BATCH_SIZE);

        Callable<Map<String, List<String>>> jiraIssueBatchTask;
        for (final List<String> batchRequest : batchRequests)
        {
            jiraIssueBatchTask = buildBatchTask(clientId, serverId,
                                                batchRequest, macroDefinitions,
                                                conversionContext);

            jiraIssueExecutorService.submit(jiraIssueBatchTask);
        }
    }

    @Override
    public JiraResponseData getAsyncJiraResults(String clientId)
    {
        JiraResponseData jiraResponseData = (JiraResponseData) jiraIssuesCache.get(clientId);
        if (jiraResponseData != null && jiraResponseData.getStatus() == JiraResponseData.Status.COMPLETED)
        {
            jiraIssuesCache.remove(clientId);
        }
        return jiraResponseData;
    }

    private Callable<Map<String, List<String>>> buildBatchTask(final String clientId,
                                    final String serverId,
                                    final List<String> batchRequest,
                                    final List<MacroDefinition> macroDefinitions,
                                    final ConversionContext conversionContext)
    {
        final StreamableMacro jiraIssuesMacro = (StreamableMacro) macroManager.getMacroByName(JiraIssuesMacro.JIRA);

        return threadLocalDelegateExecutorFactory.createCallable(new Callable<Map<String, List<String>>>() {
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
                String jiraServerUrl = (String) issueResultsMap.get(JiraIssueBatchService.JIRA_SERVER_URL);

                for (MacroDefinition macroDefinition : macroDefinitions)
                {
                    String issueKey = macroDefinition.getParameter(JiraIssuesMacro.KEY);
                    if (batchRequest.contains(issueKey))
                    {
                        Element issueElement = (elementMap == null) ? null : elementMap.get(issueKey);
                        Future<String> futureHtmlMacro = jiraIssueExecutorService.submit(new StreamableMacroFutureTask(jiraExceptionHelper, macroDefinition.getParameters(), conversionContext, jiraIssuesMacro, AuthenticatedUserThreadLocal.get(), issueElement, jiraServerUrl, exception));
                        jiraResultMap.put(issueKey, futureHtmlMacro.get());
                    }
                }

                JiraResponseData cachedJiraResponseData = (JiraResponseData) jiraIssuesCache.get(clientId);
                cachedJiraResponseData.add(jiraResultMap);

                //notify all distributed cache when complete
                if (cachedJiraResponseData.getStatus() == JiraResponseData.Status.COMPLETED)
                {
                    jiraIssuesCache.put(clientId, cachedJiraResponseData);
                }

                return jiraResultMap;
            }
        });
    }
}
