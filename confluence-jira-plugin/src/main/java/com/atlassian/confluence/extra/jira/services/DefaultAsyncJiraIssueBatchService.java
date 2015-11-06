package com.atlassian.confluence.extra.jira.services;

import com.atlassian.cache.Cache;
import com.atlassian.cache.CacheEntryAdapter;
import com.atlassian.cache.CacheEntryEvent;
import com.atlassian.cache.CacheEntryListener;
import com.atlassian.cache.CacheManager;
import com.atlassian.cache.CacheSettingsBuilder;
import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.extra.jira.JiraIssuesMacro;
import com.atlassian.confluence.extra.jira.api.services.AsyncJiraIssueBatchService;
import com.atlassian.confluence.extra.jira.api.services.JiraIssueBatchService;
import com.atlassian.confluence.extra.jira.executor.JiraExecutorFactory;
import com.atlassian.confluence.extra.jira.executor.StreamableMacroExecutor;
import com.atlassian.confluence.extra.jira.executor.StreamableMacroFutureTask;
import com.atlassian.confluence.extra.jira.helper.JiraExceptionHelper;
import com.atlassian.confluence.extra.jira.model.JiraResponseData;
import com.atlassian.confluence.macro.StreamableMacro;
import com.atlassian.confluence.macro.xhtml.MacroManager;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.confluence.xhtml.api.MacroDefinition;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.collections.MultiMap;
import org.apache.commons.collections.map.MultiValueMap;
import org.apache.commons.lang.StringUtils;
import org.jdom.Element;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

public class DefaultAsyncJiraIssueBatchService implements AsyncJiraIssueBatchService, InitializingBean, DisposableBean
{
    private static final Logger logger = LoggerFactory.getLogger(DefaultAsyncJiraIssueBatchService.class);
    private static final int THREAD_POOL_SIZE = Integer.getInteger("confluence.jira.issues.executor.poolsize", 5);
    private static final int EXECUTOR_QUEUE_SIZE = Integer.getInteger("confluence.jira.issues.executor.queuesize", 1000);
    private static final int BATCH_SIZE = 25;
    private final JiraIssueBatchService jiraIssueBatchService;
    private final MacroManager macroManager;
    private final JiraExceptionHelper jiraExceptionHelper;
    private Cache jiraIssuesCache;
    private CacheEntryListener cacheEntryListener;

    private final ExecutorService jiraIssueExecutor;
    private final StreamableMacroExecutor streamableMacroExecutor;

    public DefaultAsyncJiraIssueBatchService(JiraIssueBatchService jiraIssueBatchService, MacroManager macroManager,
                                             JiraExecutorFactory executorFactory,
                                             JiraExceptionHelper jiraExceptionHelper, CacheManager cacheManager, StreamableMacroExecutor streamableMacroExecutor)
    {
        this.jiraIssueBatchService = jiraIssueBatchService;
        this.macroManager = macroManager;
        this.jiraIssueExecutor = executorFactory.newLimitedThreadPool(THREAD_POOL_SIZE, EXECUTOR_QUEUE_SIZE, "JIM Marshaller-");
        this.jiraExceptionHelper = jiraExceptionHelper;
        this.streamableMacroExecutor = streamableMacroExecutor;
        jiraIssuesCache = cacheManager.getCache(DefaultAsyncJiraIssueBatchService.class.getName(), null,
                new CacheSettingsBuilder()
                        .local()
                        .maxEntries(500)
                        .unflushable()
                        .expireAfterAccess(1, TimeUnit.MINUTES)
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
                String jiraServerUrl = (String) issueResultsMap.get(JiraIssueBatchService.JIRA_SERVER_URL);

                for (MacroDefinition macroDefinition : macroDefinitions)
                {
                    String issueKey = macroDefinition.getParameter(JiraIssuesMacro.KEY);
                    if (batchRequest.contains(issueKey))
                    {
                        Element issueElement = (elementMap == null) ? null : elementMap.get(issueKey);

                        try
                        {
                            Future<String> futureHtmlMacro = streamableMacroExecutor.submit(new StreamableMacroFutureTask(jiraExceptionHelper, macroDefinition.getParameters(), conversionContext, jiraIssuesMacro, AuthenticatedUserThreadLocal.get(), issueElement, jiraServerUrl, exception));
                            logger.debug("Submitted task to thread pool. {}", jiraIssueExecutor.toString());
                            jiraResultMap.put(issueKey, futureHtmlMacro.get());
                        }
                        catch (RejectedExecutionException e)
                        {
                            logger.error("JIM Marshaller rejected task because there are more than 1000 tasks queued. {}", jiraIssueExecutor.toString(), e);
                            throw e;
                        }
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
        };
    }

    @Override
    public void afterPropertiesSet() throws Exception
    {
        jiraIssuesCache.removeAll();
        cacheEntryListener = new CacheEntryAdapter()
        {
            @Override
            public void onAdd(CacheEntryEvent cacheEntryEvent)
            {
                logCacheEntry("Add", cacheEntryEvent);
            }

            @Override
            public void onEvict(CacheEntryEvent cacheEntryEvent)
            {
                logCacheEntry("Evict", cacheEntryEvent);
            }

            @Override
            public void onRemove(CacheEntryEvent cacheEntryEvent)
            {
                logCacheEntry("Remove", cacheEntryEvent);
            }
        };
        jiraIssuesCache.addListener(cacheEntryListener, false);
    }

    private void logCacheEntry(String message, CacheEntryEvent cacheEntryEvent)
    {
        logger.debug(String.format("Handle '%s' with key = %s", message, cacheEntryEvent.getKey()));
        logger.debug(String.format("Total keys = %s, All keys = [%s]", jiraIssuesCache.getKeys().size(), StringUtils.join(jiraIssuesCache.getKeys(), ",")));
    }

    @Override
    public void destroy() throws Exception
    {
        jiraIssueExecutor.shutdown();
        jiraIssuesCache.removeAll();
        jiraIssuesCache.removeListener(cacheEntryListener);
    }
}
