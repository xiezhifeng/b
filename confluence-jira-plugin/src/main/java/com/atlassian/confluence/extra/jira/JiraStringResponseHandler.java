package com.atlassian.confluence.extra.jira;

import com.atlassian.confluence.util.http.trust.TrustedConnectionStatus;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;

public class JiraStringResponseHandler implements JiraResponseHandler, Serializable
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