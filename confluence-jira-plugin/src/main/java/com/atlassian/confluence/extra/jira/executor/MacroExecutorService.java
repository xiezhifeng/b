package com.atlassian.confluence.extra.jira.executor;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

/**
* TODO
 */
public interface MacroExecutorService
{
    <T> Future<T> submit(Callable<T> task);
}
