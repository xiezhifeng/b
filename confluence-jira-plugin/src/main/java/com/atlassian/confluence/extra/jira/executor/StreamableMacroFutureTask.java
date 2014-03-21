package com.atlassian.confluence.extra.jira.executor;

import com.atlassian.confluence.content.render.xhtml.ConversionContext;
import com.atlassian.confluence.extra.jira.JiraIssuesMacro;
import com.atlassian.confluence.extra.jira.util.JiraUtil;
import com.atlassian.confluence.macro.MacroExecutionException;
import com.atlassian.confluence.macro.StreamableMacro;
import com.atlassian.confluence.renderer.radeox.macros.MacroUtils;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.confluence.user.ConfluenceUser;
import com.atlassian.confluence.util.velocity.VelocityUtils;
import org.apache.commons.lang.StringUtils;
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
    private final JiraIssuesMacro macro;
    private final ConfluenceUser user;
    private final Element element;
    private final String jiraServerUrl;
    private final MacroExecutionException macroExecutionException;

    public StreamableMacroFutureTask(Map<String, String> parameters, ConversionContext context, JiraIssuesMacro macro, ConfluenceUser user)
    {
        this.parameters = parameters;
        this.context = context;
        this.macro = macro;
        this.user = user;
        this.element = null;
        this.jiraServerUrl = null;
        this.macroExecutionException = null;
    }

    public StreamableMacroFutureTask(Map<String, String> parameters, ConversionContext context, JiraIssuesMacro macro, ConfluenceUser user, Element element, String jiraServerUrl, MacroExecutionException macroExecutionException)
    {
        this.parameters = parameters;
        this.context = context;
        this.macro = macro;
        this.user = user;
        this.element = element;
        this.jiraServerUrl = jiraServerUrl;
        this.macroExecutionException = macroExecutionException;
    }

    // MacroExecutionException should be automatically handled by the marshaling chain
    public String call() throws MacroExecutionException
    {
        try
        {
            AuthenticatedUserThreadLocal.set(user);
            String key = parameters.get(JiraIssuesMacro.KEY);
            String serverId = parameters.get(JiraIssuesMacro.SERVER_ID);
            if (key != null && serverId != null) // is single issue jira markup
            {
                if (element != null)
                {

                    return macro.renderSingleJiraIssue(parameters, element, jiraServerUrl, key);
                }
                else
                {
                    throw macroExecutionException; // exception thrown for the whole batch
                }
            }
            return macro.execute(parameters, null, context);
        }
        finally
        {
            AuthenticatedUserThreadLocal.reset();
        }
    }
}