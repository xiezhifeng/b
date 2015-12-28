package com.atlassian.confluence.extra.jira.util;

import com.atlassian.applinks.api.*;
import com.atlassian.applinks.api.application.jira.JiraApplicationType;
import com.atlassian.applinks.api.auth.Anonymous;
import com.atlassian.applinks.api.auth.types.OAuthAuthenticationProvider;
import com.atlassian.applinks.spi.auth.AuthenticationConfigurationManager;
import com.atlassian.confluence.xhtml.api.MacroDefinition;
import com.atlassian.sal.api.net.Request;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

import org.apache.commons.lang3.StringUtils;

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
    public static ApplicationLinkRequest getApplicationLinkRequest(ReadOnlyApplicationLink applicationLink, Request.MethodType methodType, String url) throws CredentialsRequiredException
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
    public static Object[] getApplicationLinkRequestWithOauUrl(ReadOnlyApplicationLink applicationLink, Request.MethodType methodType, String url) throws CredentialsRequiredException
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
     * @param authenticationConfigurationManager
     * @return oau link
     */
    public static String getAuthUrl(AuthenticationConfigurationManager authenticationConfigurationManager, ReadOnlyApplicationLink applicationLink)
    {
        if(authenticationConfigurationManager.isConfigured(applicationLink.getId(), OAuthAuthenticationProvider.class))
        {
            try
            {
                applicationLink.createAuthenticatedRequestFactory().createRequest(Request.MethodType.GET, "");
            }
            catch (CredentialsRequiredException e)
            {
                // if an exception is thrown, we need to prompt for oauth
                return e.getAuthorisationURI().toString();
            }
        }
        return null;
    }

    /**
     * get application link
     * @param applicationLinkService
     * @param appId
     * @return ApplicationLink
     * @throws TypeNotInstalledException if can't get application link
     */
    public static ReadOnlyApplicationLink getApplicationLink(ReadOnlyApplicationLinkService applicationLinkService, String appId) throws TypeNotInstalledException
    {
        final ReadOnlyApplicationLink applicationLink = applicationLinkService.getApplicationLink(new ApplicationId(appId));
        if (applicationLink == null)
        {
            throw new TypeNotInstalledException("Can not get Application Link");
        }
        return applicationLink;
    }

    /**
     * Find application link match with the one defined in macro definition
     *
     * @param applicationLinkService
     * @param macroDefinition
     * @return
     */
    public static ReadOnlyApplicationLink findApplicationLink(
            final ReadOnlyApplicationLinkService applicationLinkService,
            final MacroDefinition macroDefinition) {
        return Iterables.find(applicationLinkService.getApplicationLinks(JiraApplicationType.class), new Predicate<ReadOnlyApplicationLink>()
        {
            public boolean apply(ReadOnlyApplicationLink input)
            {
                return StringUtils.equals(input.getName(), macroDefinition.getParameters().get("server"))
                        || StringUtils.equals(input.getId().get(), macroDefinition.getParameters().get("serverId"));
            }
        }, applicationLinkService.getPrimaryApplicationLink(JiraApplicationType.class));
    }

    /**
     * Try to find matched application link by using appLinkId or URl
     *
     * @param applicationLinkService
     * @param applinkId
     * @param fallbackUrl
     * @param failureMessage
     * @return
     */
    public static ReadOnlyApplicationLink findApplicationLink(
            final ReadOnlyApplicationLinkService applicationLinkService,
            final String applinkId,
            final String fallbackUrl,
            String failureMessage)
    {
        ReadOnlyApplicationLink applicationLink = null;

        applicationLink = applicationLinkService.getApplicationLink(new ApplicationId(applinkId));
        if (applicationLink == null && StringUtils.isNotBlank(fallbackUrl))
        {
            // Application links in OnDemand aren't set up using the host application ID, so we have to fall back to checking the referring URL:
            applicationLink = Iterables.find(applicationLinkService.getApplicationLinks(JiraApplicationType.class), new Predicate<ReadOnlyApplicationLink>()
            {
                public boolean apply(ReadOnlyApplicationLink input)
                {
                    return StringUtils.containsIgnoreCase(fallbackUrl, input.getDisplayUrl().toString());
                }
            });
        }

        return applicationLink;
    }
}
