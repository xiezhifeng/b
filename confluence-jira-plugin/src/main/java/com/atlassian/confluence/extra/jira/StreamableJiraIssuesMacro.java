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
import org.jdom.Element;

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
                .executionTimeoutErrorMsg("jiraissues.error.timeout.execution")
                .connectionTimeoutErrorMsg("jiraissues.error.timeout.connection")
                .interruptedErrorMsg("jiraissues.error.interrupted").build();
    }

    private Future<String> marshallMacroInBackground(final Map<String, String> parameters, final ConversionContext context)
    {
        String serverId = parameters.get("serverId");
        String key = parameters.get("key");
        // if this macro is for rendering a single issue then we must get the resulting element from the SingleJiraIssuesThreadLocalAccessor
        // the element must be available now because we already request all JIRA issues as batches in the SingleJiraIssuesToViewTransformer.transform function
        if (key != null && serverId != null)
        {
            Element element = SingleJiraIssuesThreadLocalAccessor.getElement(serverId, key);
            String jiraServerUrl = SingleJiraIssuesThreadLocalAccessor.getJiraServerUrl(serverId);
            MacroExecutionException macroExecutionException = SingleJiraIssuesThreadLocalAccessor.getException(serverId);
            return executorService.submit(new StreamableMacroFutureTask(parameters, context, this, AuthenticatedUserThreadLocal.get(), element, jiraServerUrl, macroExecutionException));
        }
        return executorService.submit(new StreamableMacroFutureTask(parameters, context, this, AuthenticatedUserThreadLocal.get()));
    }

    public void setExecutorService(StreamableMacroExecutor executorService)
    {
        this.executorService = executorService;
    }
}
