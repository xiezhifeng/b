package com.atlassian.confluence.extra.jira.util;

import com.atlassian.applinks.api.ApplicationId;
import com.atlassian.applinks.api.ReadOnlyApplicationLink;
import com.atlassian.applinks.api.ReadOnlyApplicationLinkService;
import com.atlassian.applinks.api.application.jira.JiraApplicationType;
import com.atlassian.confluence.xhtml.api.MacroDefinition;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

import org.apache.commons.lang3.StringUtils;

public class ApplicationLinkHelper
{
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
