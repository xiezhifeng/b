package com.atlassian.confluence.extra.jira.handlers;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.atlassian.applinks.api.ApplicationLinkRequestFactory;
import com.atlassian.applinks.api.ApplicationLinkResponseHandler;
import com.atlassian.sal.api.net.Response;
import com.atlassian.sal.api.net.ResponseException;

public abstract class AbstractProxyResponseHandler implements ApplicationLinkResponseHandler
{

    protected final HttpServletRequest req;
    protected final ApplicationLinkRequestFactory requestFactory;
    protected final HttpServletResponse resp;
    
    protected AbstractProxyResponseHandler(HttpServletRequest req,
            ApplicationLinkRequestFactory requestFactory, HttpServletResponse resp)
    {
        super();
        this.req = req;
        this.requestFactory = requestFactory;
        this.resp = resp;
    }

    public Object handle(Response response) throws ResponseException
    {
        if (response.isSuccessful())
        {
            if (response.getStatusCode() >= 300 && response.getStatusCode() < 400)
            {
                return retryRequest(response);
            } else
            {
                return processSuccess(response);
            }
        } else
        {
            try
            {
                resp.sendError(response.getStatusCode(), response.getStatusText());
            } catch (IOException e)
            {
                throw new RuntimeException(e);
            }
        }
        return null;
    }
    
    protected abstract Object retryRequest(Response response) throws ResponseException;

    protected abstract Object processSuccess(Response response) throws ResponseException;

    public Object credentialsRequired(Response response)
            throws ResponseException
    {
        resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        resp.setHeader("WWW-Authenticate", "OAuth realm=\"" + requestFactory.getAuthorisationURI().toString() + "\"");
        return null;
    }        
}