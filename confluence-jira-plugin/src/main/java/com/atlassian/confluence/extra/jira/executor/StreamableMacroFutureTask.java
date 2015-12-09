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

/**
 * A callable that executes a streamable macro in the current user context
 */
public class StreamableMacroFutureTask implements Callable<String>
{
    private final Map<String, String> parameters;
    private final ConversionContext context;
    private final StreamableMacro macro;
    private final Element element;
    private final String jiraServerUrl;
    private final Exception exception;
    private final JiraExceptionHelper jiraExceptionHelper;

    public StreamableMacroFutureTask(final JiraExceptionHelper jiraExceptionHelper, final Map<String, String> parameters, final ConversionContext context,
            final StreamableMacro macro, final ConfluenceUser user)
    {
        this(jiraExceptionHelper, parameters, context, macro, user, null, null, null);
    }

    public StreamableMacroFutureTask(final JiraExceptionHelper jiraExceptionHelper, final Map<String, String> parameters, final ConversionContext context,
            final StreamableMacro macro, final ConfluenceUser user, final Element element, final String jiraServerUrl, final Exception exception)
    {
        this.parameters = parameters;
        this.context = context;
        this.macro = macro;
        this.element = element;
        this.jiraServerUrl = jiraServerUrl;
        this.exception = exception;
        this.jiraExceptionHelper = jiraExceptionHelper;
    }

    // Exception should be automatically handled by the marshaling chain
    @Override
    public String call() throws Exception
    {
        return renderValue();
    }

    public String renderValue()
    {
        final long remainingTimeout = context.getTimeout().getTime();
        if (remainingTimeout <= 0)
        {
            return jiraExceptionHelper.renderTimeoutMessage(parameters);
        }
        try
        {
            if (element != null) // is single issue jira markup and in batch
            {
                final JiraIssuesMacro jiraIssuesMacro = (JiraIssuesMacro) macro;
                return jiraIssuesMacro.renderSingleJiraIssue(parameters, context, element, jiraServerUrl);
            }
            else if (exception != null)
            {
                if (exception instanceof UnsupportedJiraServerException)
                {
                    // JIRA server is not supported for batch
                    return macro.execute(parameters, null, context);
                }
                return jiraExceptionHelper.renderBatchingJIMExceptionMessage(exception.getMessage(), parameters); // something was wrong when sending batch request
            }
            // try to get the issue for anonymous/unauthenticated user
            // or for other normal cases  JiraIssuesMacro and JiraChartMacro
            return macro.execute(parameters, null, context);
        }
        catch (final Exception e)
        {
            return jiraExceptionHelper.renderNormalJIMExceptionMessage(e);
        }
    }
}