package com.atlassian.confluence.extra.jira.util;

import com.atlassian.applinks.api.*;
import com.atlassian.applinks.api.auth.Anonymous;
import com.atlassian.sal.api.net.Request;

/**
 * Jira connector utils
 */
public class JiraConnectorUtils
{

    private JiraConnectorUtils()
    {

    }

    /**
     * Get applicationLinkRequest for full mode (user authenticated and anonymous)
     * @param applicationLink
     * @param methodType
     * @param url
     * @return ApplicationLinkRequest
     * @throws CredentialsRequiredException
     */
    public static ApplicationLinkRequest getApplicationLinkRequest(ApplicationLink applicationLink, Request.MethodType methodType, String url) throws CredentialsRequiredException
    {
        ApplicationLinkRequest applicationLinkRequest;
        try
        {
            final ApplicationLinkRequestFactory requestFactory = applicationLink.createAuthenticatedRequestFactory();
            applicationLinkRequest = requestFactory.createRequest(methodType, url);
        }
        catch (CredentialsRequiredException e)
        {
            final ApplicationLinkRequestFactory requestFactory = applicationLink.createAuthenticatedRequestFactory(Anonymous.class);
            applicationLinkRequest = requestFactory.createRequest(methodType, url);
        }
        return applicationLinkRequest;
    }

    /**
     * Get applicationLinkRequest for full mode (user authenticated and anonymous) with oau url
     * @param applicationLink
     * @param methodType
     * @param url
     * @return object array with index 0 is ApplicationLinkRequest and index 1 is oaulink
     * @throws CredentialsRequiredException
     */
    public static Object[] getApplicationLinkRequestWithOauUrl(ApplicationLink applicationLink, Request.MethodType methodType, String url) throws CredentialsRequiredException
    {
        ApplicationLinkRequest applicationLinkRequest;
        String oauUrl = null;
        try
        {
            final ApplicationLinkRequestFactory requestFactory = applicationLink.createAuthenticatedRequestFactory();
            applicationLinkRequest = requestFactory.createRequest(methodType, url);
        }
        catch (CredentialsRequiredException e)
        {
            oauUrl = e.getAuthorisationURI().toString();
            final ApplicationLinkRequestFactory requestFactory = applicationLink.createAuthenticatedRequestFactory(Anonymous.class);
            applicationLinkRequest = requestFactory.createRequest(methodType, url);
        }
        return new Object[] {applicationLinkRequest, oauUrl};
    }

    /**
     * Get oau url
     * @param applicationLink
     * @return oau link
     */
    public static String getAuthUrl(ApplicationLink applicationLink)
    {
        try
        {
            applicationLink.createAuthenticatedRequestFactory().createRequest(Request.MethodType.GET, "");
            return null;
        }
        catch (CredentialsRequiredException e)
        {
            // if an exception is thrown, we need to prompt for oauth
            return e.getAuthorisationURI().toString();
        }
    }

    /**
     * get application link
     * @param applicationLinkService
     * @param appId
     * @return ApplicationLink
     * @throws TypeNotInstalledException if can't get application link
     */
    public static ApplicationLink getApplicationLink(ApplicationLinkService applicationLinkService, String appId) throws TypeNotInstalledException
    {
        final ApplicationLink applicationLink = applicationLinkService.getApplicationLink(new ApplicationId(appId));
        if (applicationLink == null)
        {
            throw new TypeNotInstalledException("Can not get Application Link");
        }
        return applicationLink;
    }
}
