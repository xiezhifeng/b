package com.atlassian.confluence.extra.jira.exception;

import com.atlassian.confluence.macro.MacroExecutionException;

import java.util.Map;

public class JiraIssueMacroException extends MacroExecutionException
{
    private Map<String, Object> contextMap;

    public JiraIssueMacroException(Throwable cause, Map<String, Object> contextMap)
    {
        super(cause);
        this.contextMap = contextMap;
    }

    public Map<String, Object> getContextMap()
    {
        return contextMap;
    }
}
