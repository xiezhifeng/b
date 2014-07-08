package com.atlassian.confluence.extra.jira;

import com.atlassian.confluence.util.http.trust.TrustedConnectionStatus;

import java.io.IOException;
import java.io.InputStream;

public interface JiraResponseHandler
{
    public enum HandlerType
    {
        STRING_HANDLER, CHANNEL_HANDLER
    }

    public void handleJiraResponse(InputStream in, TrustedConnectionStatus trustedConnectionStatus) throws IOException;
}