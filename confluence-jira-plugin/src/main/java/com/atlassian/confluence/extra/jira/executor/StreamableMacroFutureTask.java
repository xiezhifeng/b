package com.atlassian.confluence.extra.jira.executor;

import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.macro.MacroExecutionException;
import com.atlassian.confluence.macro.StreamableMacro;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.confluence.user.ConfluenceUser;

import java.util.Map;
import java.util.concurrent.Callable;

/**
 * A callable that executes a streamable macro in the current user context
  */
public class StreamableMacroFutureTask implements Callable<String>
{
    private final Map<String, String> parameters;
    private final ConversionContext context;
    private final StreamableMacro macro;
    private final ConfluenceUser user;

    public StreamableMacroFutureTask(Map<String, String> parameters, ConversionContext context, StreamableMacro macro, ConfluenceUser user)
    {
        this.parameters = parameters;
        this.context = context;
        this.macro = macro;
        this.user = user;
    }

    // MacroExecutionException should be automatically handled by the marshaling chain
    public String call() throws MacroExecutionException
    {
        try
        {
            AuthenticatedUserThreadLocal.set(user);
            return macro.execute(parameters, null, context);
        }
        finally
        {
            AuthenticatedUserThreadLocal.reset();
        }
    }
}