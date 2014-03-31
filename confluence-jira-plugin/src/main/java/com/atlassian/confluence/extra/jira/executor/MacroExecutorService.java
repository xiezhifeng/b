package com.atlassian.confluence.extra.jira.executor;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

/**
* Provides an executor service execute when make macro is streamable macro.
 */
public interface MacroExecutorService
{
    /**
     * This method submits a Callable to a (typically fixed) thread pool for later processing
     * @param task
     * @param <T>
     * @return
     */
    <T> Future<T> submit(Callable<T> task);
}
