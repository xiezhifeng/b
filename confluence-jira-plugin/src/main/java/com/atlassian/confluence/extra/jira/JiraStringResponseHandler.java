package com.atlassian.confluence.extra.jira;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;

import com.atlassian.confluence.util.http.trust.TrustedConnectionStatus;

public class JiraStringResponseHandler implements JiraResponseHandler
{
    String responseBody;

    public String getResponseBody()
    {
        return responseBody;
    }

    public void handleJiraResponse(InputStream in, TrustedConnectionStatus trustedConnectionStatus) throws IOException
    {
        try
        {
            responseBody = IOUtils.toString(in);
        } finally
        {
            IOUtils.closeQuietly(in);
        }
    }
}