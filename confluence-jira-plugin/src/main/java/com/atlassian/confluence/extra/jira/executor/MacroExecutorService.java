package com.atlassian.confluence.extra.jira.executor;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

/**
* Provides an executor service execute when make macro is streamable macro.
 */
public interface MacroExecutorService
{
    <T> Future<T> submit(Callable<T> task);
}
