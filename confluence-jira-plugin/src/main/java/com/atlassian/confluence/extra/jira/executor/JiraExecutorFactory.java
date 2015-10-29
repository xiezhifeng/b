package com.atlassian.confluence.extra.jira.executor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.atlassian.sal.api.executor.ThreadLocalDelegateExecutorFactory;
import com.atlassian.util.concurrent.ThreadFactories;

/**
 * Creates executors, with all the default features we need.
 */
public class JiraExecutorFactory
{
    private static final int THREAD_POOL_IDE_TIME_SECONDS = Integer.getInteger("jira.executor.idletime.seconds", 60);
    private final ThreadLocalDelegateExecutorFactory threadLocalDelegateExecutorFactory;

    public JiraExecutorFactory(ThreadLocalDelegateExecutorFactory threadLocalDelegateExecutorFactory)
    {
        this.threadLocalDelegateExecutorFactory = threadLocalDelegateExecutorFactory;
    }

    /**
     * Creates a ThreadPool backed ExecutorService that has only daemon threads, and an empty initial pool size.
     * <p>
     * The ExecutorService will use the {@link com.atlassian.sal.api.executor.ThreadLocalDelegateExecutorFactory} to
     * ensure all relevant thread context is available in the worker thread.
     * </p>
     * <p>
     * Will wait up to 60 seconds (configurable via jira.executor.idletime.seconds system property) for idle threads to
     * get more work before tearing them down again.
     * </p>
     * @param maxThreadPoolSize how many threads the pool can grow to.
     * @param name the name of the thread pool. Will be prefixed to each thread in the pool.
     * @return the executor service.
     */
    public ExecutorService newLimitedThreadPool(int maxThreadPoolSize, String name)
    {
        ExecutorService baseService = new ThreadPoolExecutor(0, maxThreadPoolSize,
                THREAD_POOL_IDE_TIME_SECONDS, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(),
                ThreadFactories.named(name)
                        .type(ThreadFactories.Type.DAEMON)
                        .build());
        return threadLocalDelegateExecutorFactory.createExecutorService(baseService);
    }
}
