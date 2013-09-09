package com.atlassian.confluence.extra.jira.executor;

import com.atlassian.util.concurrent.ThreadFactories;
import org.springframework.beans.factory.DisposableBean;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Provides an executor service for the streamable macros in this plugin
 */
public class StreamableMacroExecutor implements MacroExecutorService, DisposableBean
{
    final ExecutorService delegatingService =
            Executors.newFixedThreadPool(Integer.getInteger("jira.executor.threadpool.size", 4),
                    ThreadFactories.named("Jira macros executor")
                            .type(ThreadFactories.Type.DAEMON).build());

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
