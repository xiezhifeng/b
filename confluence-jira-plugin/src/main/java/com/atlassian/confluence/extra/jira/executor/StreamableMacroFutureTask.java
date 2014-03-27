package com.atlassian.confluence.extra.jira.executor;

import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.extra.jira.JiraIssuesMacro;
import com.atlassian.confluence.extra.jira.exception.UnsupportedJiraServerException;
import com.atlassian.confluence.macro.StreamableMacro;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.confluence.user.ConfluenceUser;
import org.jdom.Element;

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
    private final Element element;
    private final String jiraServerUrl;
    private final Exception exception;

    public StreamableMacroFutureTask(Map<String, String> parameters, ConversionContext context, StreamableMacro macro, ConfluenceUser user)
    {
        this(parameters, context, macro, user, null, null, null);
    }

    public StreamableMacroFutureTask(Map<String, String> parameters, ConversionContext context, StreamableMacro macro, ConfluenceUser user, Element element, String jiraServerUrl, Exception exception)
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
    public String call() throws Exception
    {
        try
        {
            AuthenticatedUserThreadLocal.set(user);
            if (element != null) // is single issue jira markup and in batch
            {
                String key = parameters.get(JiraIssuesMacro.KEY);
                JiraIssuesMacro jiraIssuesMacro = (JiraIssuesMacro) macro;
                return jiraIssuesMacro.renderSingleJiraIssue(parameters, context, element, jiraServerUrl, key);
            }
            else if (exception != null)
            {
                if (exception instanceof UnsupportedJiraServerException)
                {
                    // JIRA server is not supported for batch
                    return macro.execute(parameters, null, context);
                }
                return exception.getMessage(); // something was wrong when sending batch request
            }
            // try to get the issue for anonymous/unauthenticated user
            // or for other normal cases  JiraIssuesMacro and JiraChartMacro
            return macro.execute(parameters, null, context);
        }
        finally
        {
            AuthenticatedUserThreadLocal.reset();
        }
    }
}