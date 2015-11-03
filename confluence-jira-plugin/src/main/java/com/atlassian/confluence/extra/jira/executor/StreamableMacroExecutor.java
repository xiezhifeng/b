package com.atlassian.confluence.extra.jira.executor;

import com.atlassian.confluence.extra.jira.StreamableJiraIssuesMacro;
import org.springframework.beans.factory.DisposableBean;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * Provides an executor service for the streamable macros in this plugin
 */
public class StreamableMacroExecutor implements MacroExecutorService, DisposableBean
{
    private final ExecutorService delegatingService;

    public StreamableMacroExecutor(JiraExecutorFactory factory)
    {
        delegatingService = factory.newLimitedThreadPool(
                StreamableJiraIssuesMacro.THREAD_POOL_SIZE,
                "Jira macros executor");
    }

    @Override
    public <T> Future<T> submit(Callable<T> task)
    {
        return delegatingService.submit(task);
    }

    @Override
    public void destroy() throws Exception
    {
        delegatingService.shutdown();
    }
}
