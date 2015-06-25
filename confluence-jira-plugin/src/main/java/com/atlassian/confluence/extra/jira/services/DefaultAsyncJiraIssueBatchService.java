package com.atlassian.confluence.extra.jira.services;

import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.applinks.api.CredentialsRequiredException;
import com.atlassian.cache.CacheManager;
import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.core.ContentEntityObject;
import com.atlassian.confluence.extra.jira.*;
import com.atlassian.confluence.extra.jira.api.services.AsyncJiraIssueBatchService;
import com.atlassian.confluence.extra.jira.api.services.JiraIssueBatchService;
import com.atlassian.confluence.extra.jira.cache.CacheKey;
import com.atlassian.confluence.extra.jira.exception.MalformedRequestException;
import com.atlassian.confluence.extra.jira.exception.UnsupportedJiraServerException;
import com.atlassian.confluence.extra.jira.executor.StreamableMacroExecutor;
import com.atlassian.confluence.extra.jira.helper.JiraExceptionHelper;
import com.atlassian.confluence.extra.jira.helper.JiraJqlHelper;
import com.atlassian.confluence.extra.jira.model.EntityServerCompositeKey;
import com.atlassian.confluence.extra.jira.model.JiraBatchResponseData;
import com.atlassian.confluence.extra.jira.model.JiraColumnInfo;
import com.atlassian.confluence.extra.jira.util.JiraUtil;
import com.atlassian.confluence.macro.MacroExecutionException;
import com.atlassian.confluence.macro.xhtml.MacroManager;
import com.atlassian.confluence.plugins.jira.JiraServerBean;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.confluence.user.ConfluenceUser;
import com.atlassian.sal.api.executor.ThreadLocalDelegateExecutorFactory;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.log4j.Logger;
import org.jdom.Element;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;

public class DefaultAsyncJiraIssueBatchService implements AsyncJiraIssueBatchService
{
//    private static final Logger LOGGER = Logger.getLogger(DefaultAsyncJiraIssueBatchService.class);

    private final JiraIssueBatchService jiraIssueBatchService;
    private final MacroManager macroManager;
    private final CacheManager cacheManager;
    private final ThreadLocalDelegateExecutorFactory threadLocalDelegateExecutorFactory;
    private final StreamableMacroExecutor streamableMacroExecutor;


    private final Cache<EntityServerCompositeKey, Future<Map<String, String>>> jiraIssueFutureResult;

    public DefaultAsyncJiraIssueBatchService(JiraIssueBatchService jiraIssueBatchService, MacroManager macroManager, CacheManager cacheManager,
                                             ThreadLocalDelegateExecutorFactory threadLocalDelegateExecutorFactory, StreamableMacroExecutor streamableMacroExecutor)
    {
        this.jiraIssueBatchService = jiraIssueBatchService;
        this.macroManager = macroManager;
        this.cacheManager = cacheManager;
        this.threadLocalDelegateExecutorFactory = threadLocalDelegateExecutorFactory;
        this.streamableMacroExecutor = streamableMacroExecutor;
        jiraIssueFutureResult = CacheBuilder.newBuilder()
            .expireAfterWrite(30, TimeUnit.SECONDS)
            .build(new CacheLoader<EntityServerCompositeKey, Future<Map<String, String>>>() {
                public Future<Map<String, String>> load(EntityServerCompositeKey key) {
                    return null;
                }
            });
    }



    public JiraBatchResponseData getAsyncBatchResults(long entityId, String serverId) throws Exception
    {
        EntityServerCompositeKey key = new EntityServerCompositeKey(AuthenticatedUserThreadLocal.getUsername(), entityId, serverId);
        Future<Map<String, String>> futureResult = jiraIssueFutureResult.getUnchecked(key);
//        jiraIssueFutureResult.invalidate(key);

        Map<String, String> jiraIssueResult = futureResult.get();
        JiraBatchResponseData jiraBatchResponseData = new JiraBatchResponseData();
        jiraBatchResponseData.setHtmlMacro(jiraIssueResult);
        jiraBatchResponseData.setServerId(serverId);
        jiraBatchResponseData.setIssueKeys(Lists.newArrayList(jiraIssueResult.keySet()));
        return jiraBatchResponseData;
    }

    @Override
    public void processBatchRequest(ContentEntityObject entityObject, String serverId, Set<String> keys, ConversionContext conversionContext)
    {
        handleAyncRequestData(entityObject, serverId, keys, conversionContext);
    }

    private void handleAyncRequestData(final ContentEntityObject entity, final String serverId, final Set<String> keys, final ConversionContext conversionContext)
    {
        Callable<Map<String, String>> jiraIssueCallable = threadLocalDelegateExecutorFactory.createCallable(new Callable<Map<String, String>>() {
            public Map<String, String> call() throws Exception
            {
                try {
                    Map<String, Object> resultsMap = jiraIssueBatchService.getBatchResults(serverId, keys, conversionContext);
                    Map<String, Element> elementMap = (Map<String, Element>) resultsMap.get(JiraIssueBatchService.ELEMENT_MAP);
                    String jiraServerUrl = (String) resultsMap.get(JiraIssueBatchService.JIRA_SERVER_URL);
                    JiraIssuesMacro jiraIssuesMacro = (JiraIssuesMacro) macroManager.getMacroByName("jira");

                    Map<String, String> jiraResults = Maps.newHashMapWithExpectedSize(keys.size());
                    for (String jiraIssueKey : elementMap.keySet())
                    {
                        Element issueElement = elementMap.get(jiraIssueKey);
                        Map<String, String> parameters = Maps.newHashMap();
                        parameters.put("key", jiraIssueKey);
                        parameters.put("showSummary", Boolean.TRUE.toString());
                        parameters.put("serverId", serverId);
                        String htmlMacro = jiraIssuesMacro.renderSingleJiraIssue(parameters, conversionContext, issueElement, jiraServerUrl);
                        jiraResults.put(jiraIssueKey, htmlMacro);
                    }
                    return jiraResults;

                } catch (MacroExecutionException e) {
                    e.printStackTrace();
                } catch (UnsupportedJiraServerException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }
        });
        Future<Map<String, String>> result = streamableMacroExecutor.submit(jiraIssueCallable);
        jiraIssueFutureResult.asMap().put(new EntityServerCompositeKey(AuthenticatedUserThreadLocal.getUsername(), entity.getId(), serverId), result);
    }

}
