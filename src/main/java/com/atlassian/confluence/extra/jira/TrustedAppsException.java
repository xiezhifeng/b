package com.atlassian.confluence.extra.jira;

import com.atlassian.sal.api.net.ResponseException;

public class TrustedAppsException extends ResponseException
{

    public TrustedAppsException(String message)
    {
        super(message);
    }

}
