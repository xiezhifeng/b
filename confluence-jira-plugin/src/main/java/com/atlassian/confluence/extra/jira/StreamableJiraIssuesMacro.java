package com.atlassian.confluence.extra.jira;

import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.content.render.xhtml.Streamable;
import com.atlassian.confluence.extra.jira.executor.FutureStreamableConverter;
import com.atlassian.confluence.extra.jira.executor.StreamableMacroExecutor;
import com.atlassian.confluence.extra.jira.executor.StreamableMacroFutureTask;
import com.atlassian.confluence.macro.EditorImagePlaceholder;
import com.atlassian.confluence.macro.MacroExecutionException;
import com.atlassian.confluence.macro.ResourceAware;
import com.atlassian.confluence.macro.StreamableMacro;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;

import java.util.Map;
import java.util.concurrent.Future;

/**
 * A macro to import/fetch JIRA issues...
 */
public class StreamableJiraIssuesMacro extends JiraIssuesMacro implements StreamableMacro, EditorImagePlaceholder, ResourceAware
{
    private StreamableMacroExecutor executorService;

    public Streamable executeToStream(final Map<String, String> parameters, final Streamable body,
            final ConversionContext context) throws MacroExecutionException
    {
        final Future<String> futureResult = marshallMacroInBackground(parameters, context);

        return new FutureStreamableConverter.Builder(futureResult, context, getI18NBean())
                .executionErrorMsg("jiraissues.error.execution")
                .timeoutErrorMsg("jiraissues.error.timeout")
                .interruptedErrorMsg("jiraissues.error.interrupted").build();
    }

    private Future<String> marshallMacroInBackground(final Map<String, String> parameters, final ConversionContext context)
    {
        return executorService.submit(new StreamableMacroFutureTask(parameters, context, this, AuthenticatedUserThreadLocal.get()));
    }

    public void setExecutorService(StreamableMacroExecutor executorService)
    {
        this.executorService = executorService;
    }
}
