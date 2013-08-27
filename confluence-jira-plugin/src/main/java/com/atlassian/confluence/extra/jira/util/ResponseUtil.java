package com.atlassian.confluence.extra.jira.util;

import javax.ws.rs.core.Response;

public class ResponseUtil
{
    /**
     * Build response in case user not mapping
     * 
     * @param oAuthenticationUri link to authenticate
     * @return Response response with unauthorized status
     */
    public static Response buildUnauthorizedResponse(String oAuthenticationUri)
    {
        return Response.status(Response.Status.UNAUTHORIZED)
                .header("WWW-Authenticate", "OAuth realm=\"" + oAuthenticationUri + "\"").build();
    }
}
