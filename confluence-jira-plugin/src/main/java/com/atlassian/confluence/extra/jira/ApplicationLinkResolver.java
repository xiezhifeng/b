package com.atlassian.confluence.extra.jira;

import com.atlassian.applinks.api.ApplicationLink;
import com.atlassian.applinks.api.ApplicationLinkService;
import com.atlassian.applinks.api.TypeNotInstalledException;
import com.atlassian.applinks.api.application.jira.JiraApplicationType;
import com.atlassian.applinks.api.event.ApplicationLinkEvent;
import com.atlassian.cache.Cache;
import com.atlassian.cache.CacheLoader;
import com.atlassian.cache.CacheManager;
import com.atlassian.cache.CacheSettingsBuilder;
import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.google.common.base.Function;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.DisposableBean;

import javax.annotation.Nullable;
import java.util.Iterator;
import java.util.Map;

public class ApplicationLinkResolver implements DisposableBean
{

    private static final Logger LOGGER = Logger.getLogger(ApplicationLinkResolver.class);
    private static final String XML_JQL_REGEX = ".+searchrequest-xml/temp/SearchRequest.+";
    private static final String APPLICATION_LINK_CACHE = ApplicationLinkResolver.class.getName();

    private final Cache<String, Iterable<ApplicationLink>> cachedApplicationLinks;
    private final EventPublisher eventPublisher;

    public ApplicationLinkResolver(EventPublisher eventPublisher, final ApplicationLinkService appLinkService, CacheManager cacheManager)
    {
        cachedApplicationLinks = cacheManager.getCache(APPLICATION_LINK_CACHE, new CacheLoader<String, Iterable<ApplicationLink>>()
        {
            @Override
            public Iterable<ApplicationLink> load(String cacheKey)
            {
                return appLinkService.getApplicationLinks(JiraApplicationType.class);
            }
        }, new CacheSettingsBuilder().local().build());
        this.eventPublisher = eventPublisher;
        eventPublisher.register(this);
    }

    @EventListener
    public void applicationLinkChanged(ApplicationLinkEvent event)
    {
        LOGGER.info("Change on application link = " + event.getApplicationLink().getName());
        cachedApplicationLinks.removeAll();
    }

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
        ApplicationLink primaryAppLink = getPrimaryApplicationLink();
        if (primaryAppLink == null)
        {
            return null;
        }
        if (StringUtils.isBlank(requestData)) // it's meaningless to find an AppLink for no request data
        {
            String errorMessage = "No request data supplied";
            throw new TypeNotInstalledException(errorMessage);
        }

        if (requestType == JiraIssuesMacro.Type.URL)
        {
            Iterable<ApplicationLink> applicationLinks = getJIRAApplicationLinks();
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
        for (ApplicationLink applicationLink : getJIRAApplicationLinks())
        {
            if (matcher.equals(getProperty.apply(applicationLink)))
            {
                return applicationLink;
            }
        }
        return null;
    }

    private Iterable<ApplicationLink> getJIRAApplicationLinks()
    {
        return cachedApplicationLinks.get(APPLICATION_LINK_CACHE);
    }

    private ApplicationLink getPrimaryApplicationLink()
    {
        final Iterator<ApplicationLink> iterator = getJIRAApplicationLinks().iterator();

        if (!iterator.hasNext())
        {
            return null;
        }

        while (iterator.hasNext())
        {
            final ApplicationLink application = iterator.next();
            if (application.isPrimary())
            {
                return application;
            }
        }
        return null;
    }

    public void destroy() throws Exception
    {
        eventPublisher.unregister(this);
        cachedApplicationLinks.removeAll();
    }
}
