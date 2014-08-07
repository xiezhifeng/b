package com.atlassian.confluence.extra.jira;

import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.applinks.api.ApplicationLinkService;
import com.atlassian.applinks.api.TypeNotInstalledException;
import com.atlassian.applinks.api.application.jira.JiraApplicationType;
import com.google.common.base.Function;
import org.apache.commons.lang.StringUtils;

import javax.annotation.Nullable;
import java.util.Map;

public class ApplicationLinkResolver
{

    private static final String XML_JQL_REGEX = ".+searchrequest-xml/temp/SearchRequest.+";

    private ApplicationLinkService appLinkService;

    /**
     * Gets applicationLink base on request data and type
     *
     * @param requestType is URL or JQL
     * @param requestData is Key or RpcUrl
     * @param typeSafeParams key/value pairs parameters
     * @return ApplicationLink applicationLink if it exist. Null if no application link and the url is XML search and is
     * also JQL follows regular expression .+searchrequest-xml/temp/SearchRequest.+
     * @throws TypeNotInstalledException if it can not find an application link base on url or server name in
     * parameters
     */
    public ApplicationLink resolve(JiraIssuesMacro.Type requestType, String requestData, Map<String, String> typeSafeParams)
            throws TypeNotInstalledException
    {
        // Make sure we actually have at least one applink configured, otherwise it's pointless to continue
        ApplicationLink primaryAppLink = appLinkService.getPrimaryApplicationLink(JiraApplicationType.class);
        if (primaryAppLink == null)
        {
            return null;
        }

        if (requestType == JiraIssuesMacro.Type.URL)
        {
            Iterable<ApplicationLink> applicationLinks = appLinkService.getApplicationLinks(JiraApplicationType.class);
            for (ApplicationLink applicationLink : applicationLinks)
            {
                if (requestData.startsWith(applicationLink.getRpcUrl().toString()) || requestData.startsWith(applicationLink.getDisplayUrl().toString()))
                {
                    return applicationLink;
                }
            }
            //support a case url is XML type and contains JQL
            if (requestData.matches(XML_JQL_REGEX))
            {
                return null;
            }
            String errorMessage = "Can not find an application link base on url of request data."; //
            throw new TypeNotInstalledException(errorMessage);
        }

        String serverName = typeSafeParams.get("server");

        // Firstly, try to find an applink matching one of the macro's server params
        ApplicationLink appLink = getAppLinkForServer(serverName, typeSafeParams.get("serverId"));
        if (appLink != null)
        {
            return appLink;
        }

        // Return the primary applink if the macro didn't specify a server, otherwise show an error
        if (StringUtils.isBlank(serverName))
        {
            return primaryAppLink;
        }
        else
        {
            String errorMessage = "Can not find an application link base on server name :" + serverName;
            throw new TypeNotInstalledException(errorMessage);
        }
    }

    public ApplicationLink getAppLinkForServer(String serverName, String serverId)
    {
        ApplicationLink appLink = null;

        if (StringUtils.isNotBlank(serverId))
        {
            appLink = getAppLink(serverId, new Function<ApplicationLink, String>()
            {
                @Override
                public String apply(@Nullable ApplicationLink input)
                {
                    if (input != null)
                    {
                        return input.getId().toString();
                    }
                    return null;
                }
            });
        }
        if (appLink == null && StringUtils.isNotBlank(serverName))
        {
            appLink = getAppLink(serverName, new Function<ApplicationLink, String>()
            {
                @Override
                public String apply(@Nullable ApplicationLink input)
                {
                    if (input != null)
                    {
                        return input.getName();
                    }
                    return null;
                }
            });
        }

        return appLink;
    }

    private ApplicationLink getAppLink(String matcher, Function<ApplicationLink, String> getProperty)
    {
        for (ApplicationLink applicationLink : appLinkService.getApplicationLinks(JiraApplicationType.class))
        {
            if (matcher.equals(getProperty.apply(applicationLink)))
            {
                return applicationLink;
            }
        }
        return null;
    }

    public void setApplicationLinkService(ApplicationLinkService appLinkService)
    {
        this.appLinkService = appLinkService;
    }
}
