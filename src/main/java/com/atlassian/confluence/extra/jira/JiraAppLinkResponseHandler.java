package com.atlassian.confluence.extra.jira;

import java.io.IOException;

import com.atlassian.applinks.api.ApplicationLinkResponseHandler;
import com.atlassian.applinks.api.AuthorisationURIGenerator;
import com.atlassian.applinks.api.CredentialsRequiredException;
import com.atlassian.confluence.extra.jira.JiraResponseHandler.HandlerType;
import com.atlassian.confluence.extra.jira.util.JiraUtil;
import com.atlassian.sal.api.net.Response;
import com.atlassian.sal.api.net.ResponseException;

public class JiraAppLinkResponseHandler implements ApplicationLinkResponseHandler
{

    private final JiraResponseHandler responseHandler;
    private AuthorisationURIGenerator requestFactory;

    public JiraAppLinkResponseHandler(HandlerType handlerType, String url, AuthorisationURIGenerator requestFactory)
    {
        super();
        this.requestFactory = requestFactory;
        responseHandler = JiraUtil.createResponseHanlder(handlerType, url);
    }

    public Object handle(Response resp) throws ResponseException
    {
        try
        {
            if ("ERROR".equals(resp.getHeader("X-Seraph-Trusted-App-Status")))
            {
                String taError = resp.getHeader("X-Seraph-Trusted-App-Error");
                throw new TrustedAppsException(taError);
            }
            JiraUtil.checkForErrors(resp.isSuccessful(), resp.getStatusCode(), resp.getStatusText());
            responseHandler.handleJiraResponse(resp.getResponseBodyAsStream(), null);
            return responseHandler;
        } catch (IOException e)
        {
            throw new ResponseException(e);
        }
    }

    public Object credentialsRequired(Response response) throws ResponseException
    {
        throw new ResponseException(new CredentialsRequiredException(requestFactory, ""));
        // return null;
    }

    public JiraResponseHandler getResponseHandler()
    {
        return responseHandler;
    }

}
