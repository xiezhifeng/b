package com.atlassian.confluence.extra.jira;

import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.applinks.api.ApplicationLinkService;
import com.atlassian.applinks.api.application.jira.JiraApplicationType;
import com.atlassian.confluence.extra.jira.exception.ApplicationLinkException;
import com.atlassian.confluence.macro.MacroExecutionException;
import com.atlassian.confluence.util.i18n.I18NBeanFactory;
import com.google.common.base.Function;
import com.google.common.collect.Lists;

import org.apache.commons.lang.StringUtils;

import javax.annotation.Nullable;

import java.util.List;
import java.util.Map;

public class ApplicationLinkResolver
{
    private ApplicationLinkService appLinkService;
    private ProjectKeyCache projectKeyCache;
    private I18NBeanFactory i18NBeanFactory;

    public ApplicationLink resolve(JiraIssuesMacro.Type requestType, String requestData, Map<String, String> typeSafeParams) throws MacroExecutionException
    {
        // Make sure we actually have at least one applink configured, otherwise it's pointless to continue
        ApplicationLink primaryAppLink = appLinkService.getPrimaryApplicationLink(JiraApplicationType.class);
        if (primaryAppLink == null)
        {
            throw new ApplicationLinkException(getText("jiraissues.error.noapplinks"));
        }

        if (requestType == JiraIssuesMacro.Type.URL)
        {
            Iterable<ApplicationLink> applicationLinks = appLinkService.getApplicationLinks(JiraApplicationType.class);
            for (ApplicationLink applicationLink : applicationLinks)
            {
                if (requestData.indexOf(applicationLink.getRpcUrl().toString()) == 0)
                {
                    return applicationLink;
                }
            }

            throw new MacroExecutionException(getText("jiraissues.error.noapplinks"));
        }

        String serverName = typeSafeParams.get("server");

        // Firstly, try to find an applink matching one of the macro's server params
        ApplicationLink appLink = getAppLinkForServer(serverName, typeSafeParams.get("serverId"));
        if (appLink != null)
        {
            return appLink;
        }

        // Secondly, try to find an applink matching the issue key
        if (requestType == JiraIssuesMacro.Type.KEY) {
            appLink = getAppLinkForIssueKey(requestData);
            if (appLink != null)
            {
                return appLink;
            }
        }

        // Return the primary applink if the macro didn't specify a server, otherwise show an error
        if (StringUtils.isBlank(serverName))
        {
            return primaryAppLink;
        }
        else
        {
            throw new ApplicationLinkException(getText("jiraissues.error.nonamedapplink", Lists.newArrayList(serverName)));
        }
    }

    private ApplicationLink getAppLinkForServer(String serverName, String serverId)
    {
        ApplicationLink appLink = null;

        if (StringUtils.isNotBlank(serverId))
        {
            appLink = getAppLink(serverId, new Function<ApplicationLink, String>()
            {
                @Override
                public String apply(@Nullable ApplicationLink input)
                {
                    return input.getId().toString();
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
                    return input.getName();
                }
            });
        }

        return appLink;
    }

    private ApplicationLink getAppLinkForIssueKey(String key) throws MacroExecutionException
    {
        String[] split = key.split("-");
        if (split.length != 2)
        {
            throw new MacroExecutionException(getText("jiraissues.error.invalidkey", Lists.newArrayList(key)));
        }

        String projectKey = split[0];
        return projectKeyCache.getAppForKey(projectKey);
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

    private String getText(String key)
    {
        return i18NBeanFactory.getI18NBean().getText(key);
    }

    private String getText(String key, List<String> substitutions)
    {
        return i18NBeanFactory.getI18NBean().getText(key, substitutions);
    }

    public void setProjectKeyCache(ProjectKeyCache projectKeyCache)
    {
        this.projectKeyCache = projectKeyCache;
    }

    public void setApplicationLinkService(ApplicationLinkService appLinkService)
    {
        this.appLinkService = appLinkService;
    }

    public void setI18NBeanFactory(I18NBeanFactory i18NBeanFactory)
    {
        this.i18NBeanFactory = i18NBeanFactory;
    }
}
