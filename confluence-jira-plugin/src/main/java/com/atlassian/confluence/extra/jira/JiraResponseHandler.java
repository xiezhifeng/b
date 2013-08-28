package com.atlassian.confluence.extra.jira;

import java.io.IOException;
import java.io.InputStream;

import com.atlassian.confluence.util.http.trust.TrustedConnectionStatus;

public interface JiraResponseHandler
{
    public enum HandlerType
    {
        STRING_HANDLER, CHANNEL_HANDLER
    }

    public void handleJiraResponse(InputStream in, TrustedConnectionStatus trustedConnectionStatus) throws IOException;
}