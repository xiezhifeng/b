package com.atlassian.confluence.extra.jira.executor;

import java.util.Map;
import java.util.concurrent.Callable;

import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.extra.jira.JiraIssuesMacro;
import com.atlassian.confluence.extra.jira.exception.UnsupportedJiraServerException;
import com.atlassian.confluence.extra.jira.helper.JiraExceptionHelper;
import com.atlassian.confluence.macro.StreamableMacro;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.confluence.user.ConfluenceUser;

import org.jdom.Element;
import org.jfree.util.Log;

/**
 * A callable that executes a streamable macro in the current user context
 */
public class StreamableMacroFutureTask implements Callable<String>
{
    private final Map<String, String> parameters;
    private final ConversionContext context;
    private final StreamableMacro macro;
    private final ConfluenceUser user;
    private final Element element;
    private final String jiraServerUrl;
    private final Exception exception;

    public StreamableMacroFutureTask(final Map<String, String> parameters, final ConversionContext context, final StreamableMacro macro, final ConfluenceUser user)
    {
        this(parameters, context, macro, user, null, null, null);
    }

    public StreamableMacroFutureTask(final Map<String, String> parameters, final ConversionContext context, final StreamableMacro macro, final ConfluenceUser user, final Element element, final String jiraServerUrl, final Exception exception)
    {
        this.parameters = parameters;
        this.context = context;
        this.macro = macro;
        this.user = user;
        this.element = element;
        this.jiraServerUrl = jiraServerUrl;
        this.exception = exception;
    }

    // Exception should be automatically handled by the marshaling chain
    @Override
    public String call() throws Exception
    {
        final long remainingTimeout = context.getTimeout().getTime();
        Log.debug("remainingTimeout " + remainingTimeout);
        if (remainingTimeout <= 0)
        {
            Log.debug("skipping JIM execution");
            return JiraExceptionHelper.renderExceptionMessage("Unable to render JIRA issues macro, connection to JIRA has been timeout.");
        }
        try
        {
            AuthenticatedUserThreadLocal.set(user);
            if (element != null) // is single issue jira markup and in batch
            {
                final JiraIssuesMacro jiraIssuesMacro = (JiraIssuesMacro) macro;
                final String key = parameters.get(JiraIssuesMacro.KEY);
                return jiraIssuesMacro.renderSingleJiraIssue(parameters, context, element, jiraServerUrl, key);
            }
            else if (exception != null)
            {
                if (exception instanceof UnsupportedJiraServerException)
                {
                    // JIRA server is not supported for batch
                    return macro.execute(parameters, null, context);
                }
                return JiraExceptionHelper.renderExceptionMessage(exception.getMessage()); // something was wrong when sending batch request
            }
            // try to get the issue for anonymous/unauthenticated user
            // or for other normal cases  JiraIssuesMacro and JiraChartMacro
            return macro.execute(parameters, null, context);
        }
        catch (final Exception e)
        {
            return JiraExceptionHelper.renderExceptionMessage(e.getMessage());
        }
        finally
        {
            AuthenticatedUserThreadLocal.reset();
        }
    }
}