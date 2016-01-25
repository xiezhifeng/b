package com.atlassian.confluence.plugins.conluenceview.services.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.atlassian.applinks.api.EntityLink;
import com.atlassian.applinks.api.ReadOnlyApplicationLink;
import com.atlassian.applinks.api.ReadOnlyApplicationLinkService;
import com.atlassian.applinks.api.application.jira.JiraApplicationType;
import com.atlassian.applinks.api.event.EntityLinkAddedEvent;
import com.atlassian.applinks.api.event.EntityLinkDeletedEvent;
import com.atlassian.applinks.application.confluence.ConfluenceSpaceEntityTypeImpl;
import com.atlassian.applinks.application.jira.JiraProjectEntityTypeImpl;
import com.atlassian.applinks.host.spi.EntityReference;
import com.atlassian.applinks.host.spi.HostApplication;
import com.atlassian.applinks.host.spi.InternalHostApplication;
import com.atlassian.applinks.spi.link.MutatingEntityLinkService;
import com.atlassian.confluence.event.events.space.SpaceLogoUpdateEvent;
import com.atlassian.confluence.event.events.space.SpacePermissionsUpdateEvent;
import com.atlassian.confluence.event.events.space.SpaceRemoveEvent;
import com.atlassian.confluence.event.events.space.SpaceUpdateEvent;
import com.atlassian.confluence.plugins.conluenceview.rest.dto.LinkedSpaceDto;
import com.atlassian.confluence.plugins.conluenceview.rest.exception.InvalidRequestException;
import com.atlassian.confluence.plugins.conluenceview.services.ConfluenceJiraLinksService;
import com.atlassian.confluence.spaces.Space;
import com.atlassian.confluence.spaces.SpaceLogoManager;
import com.atlassian.confluence.spaces.SpaceManager;
import com.atlassian.confluence.spaces.SpacesQuery;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.confluence.user.ConfluenceUser;
import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.DisposableBean;

public class DefaultConfluenceJiraLinksService implements ConfluenceJiraLinksService, DisposableBean
{
    private final MutatingEntityLinkService entityLinkService;
    private final InternalHostApplication applinkHostApplication;
    private final HostApplication hostApplication;
    private final ReadOnlyApplicationLinkService appLinkService;
    private final Map<String, List<LinkedSpaceDto>> linkedSpaceMap;
    private final EventPublisher eventPublisher;
    private final SpaceManager spaceManager;
    private final SpaceLogoManager spaceLogoManager;

    public DefaultConfluenceJiraLinksService(MutatingEntityLinkService entityLinkService, InternalHostApplication applinkHostApplication,
            HostApplication hostApplication, ReadOnlyApplicationLinkService appLinkService, EventPublisher eventPublisher, SpaceManager spaceManager, SpaceLogoManager spaceLogoManager)
    {
        this.entityLinkService = entityLinkService;
        this.applinkHostApplication = applinkHostApplication;
        this.hostApplication = hostApplication;
        this.appLinkService = appLinkService;
        this.eventPublisher = eventPublisher;
        this.spaceManager = spaceManager;
        this.spaceLogoManager = spaceLogoManager;
        eventPublisher.register(this);

        linkedSpaceMap = new ConcurrentHashMap<String, List<LinkedSpaceDto>>();
    }

    @EventListener
    public void onEntityLinkAddedEvent(EntityLinkAddedEvent event)
    {
        final EntityLink entityLink = event.getEntityLink();
        if ((entityLink.getType().getClass() == JiraProjectEntityTypeImpl.class))
        {
            linkedSpaceMap.remove(getCacheKey(entityLink.getApplicationLink().getDisplayUrl().toString(), entityLink.getKey()));
        }
    }

    @EventListener
    public void onEntityLinkDeletedEvent(EntityLinkDeletedEvent event)
    {
        if (event.getEntityType().getClass() == JiraProjectEntityTypeImpl.class)
        {
            final ReadOnlyApplicationLink appLink = appLinkService.getApplicationLink(event.getApplicationId());
            if (appLink != null)
            {
                linkedSpaceMap.remove(getCacheKey(appLink.getDisplayUrl().toString(), event.getEntityKey()));
            }
        }
    }

    @EventListener
    public void onSpaceLogoUpdateEvent(SpaceLogoUpdateEvent event)
    {
        removeCacheThatHasSpace(event.getSpace().getKey());
    }

    @EventListener
    public void onSpaceUpdateEvent(SpaceUpdateEvent event)
    {
        removeCacheThatHasSpace(event.getSpace().getKey());
    }

    @EventListener
    public void onSpacePermissionsUpdateEvent(SpacePermissionsUpdateEvent event)
    {
        linkedSpaceMap.clear();
    }

    @EventListener
    public void onSpaceRemoveEvent(SpaceRemoveEvent event)
    {
        removeCacheThatHasSpace(event.getSpace().getKey());
    }

    @Override
    public String getODApplicationLinkId()
    {
        return hostApplication.getId().get();
    }

    @Override
    public List<LinkedSpaceDto> getLinkedSpaces(String jiraUrl, String projectKey)
    {
        final ConfluenceUser user = AuthenticatedUserThreadLocal.get();
        if (user == null)
        {
            return Collections.emptyList();
        }

        if (StringUtils.isBlank(jiraUrl) || StringUtils.isBlank(projectKey))
        {
            throw new InvalidRequestException("JIRA url and project key cannot be empty");
        }

        String cacheKey = getCacheKey(jiraUrl, projectKey);

        List<LinkedSpaceDto> spaceDtos = linkedSpaceMap.get(cacheKey);
        if (spaceDtos != null)
        {
            return spaceDtos;
        }

        final ReadOnlyApplicationLink appLink = getAppLink(jiraUrl);
        if (appLink == null)
        {
            return Collections.emptyList();
        }

        List<String> spaceKeys = new ArrayList<String>();

        final Iterable<EntityReference> localEntities = applinkHostApplication.getLocalEntities();
        for (EntityReference localEntity : localEntities)
        {
            if (hasLinkToProject(localEntity, projectKey))
            {
                spaceKeys.add(localEntity.getKey());
            }
        }

        spaceDtos = new ArrayList<LinkedSpaceDto>();
        if (spaceKeys.size() > 0)
        {
            final List<Space> spaces = spaceManager.getAllSpaces(SpacesQuery.newQuery().withSpaceKeys(spaceKeys).build());
            for (Space space : spaces)
            {
                final String spaceLogo = spaceLogoManager.getLogoDownloadPath(space, user);
                spaceDtos.add(LinkedSpaceDto.newBuilder()
                        .withSpaceKey(space.getKey())
                        .withSpaceName(space.getDisplayTitle())
                        .withSpaceUrl(space.getUrlPath())
                        .withSpaceIcon(spaceLogo).build());
            }
        }

        linkedSpaceMap.put(cacheKey, spaceDtos);

        return spaceDtos;
    }

    /**
     * @param entity Confluence space link entity
     * @param projectKey JIRA project key
     * @return true if that space has link to JIRA project of "projectKey"
     */
    private boolean hasLinkToProject(EntityReference entity, String projectKey)
    {
        if (entity.getType().getClass() == ConfluenceSpaceEntityTypeImpl.class)
        {
            final Iterable<EntityLink> links = entityLinkService.getEntityLinksForKey(entity.getKey(), ConfluenceSpaceEntityTypeImpl.class, JiraProjectEntityTypeImpl.class);
            for (EntityLink link : links)
            {
                if (link.getKey().equals(projectKey))
                {
                    return true;
                }
            }
        }

        return false;
    }

    private ReadOnlyApplicationLink getAppLink(String jiraUrl)
    {
        Iterable<ReadOnlyApplicationLink> appLinks = appLinkService.getApplicationLinks(JiraApplicationType.class);

        if (appLinks == null)
        {
            return null;
        }

        for (ReadOnlyApplicationLink appLink : appLinks)
        {
            if (appLink.getDisplayUrl().toString().equals(jiraUrl))
            {
                return appLink;
            }
        }

        return null;
    }

    private String getCacheKey(String jiraUrl, String projectKey)
    {
        return jiraUrl + "/browser/" + projectKey;
    }

    private void removeCacheThatHasSpace(String spaceKey)
    {
        String toBeRemovedKey = null;

        for (Map.Entry<String, List<LinkedSpaceDto>> entry : linkedSpaceMap.entrySet())
        {
            final List<LinkedSpaceDto> spaceDtos = entry.getValue();
            if (spaceDtos != null)
            {
                for (LinkedSpaceDto spaceDto : spaceDtos)
                {
                    if (spaceDto.getSpaceKey().equals(spaceKey))
                    {
                        toBeRemovedKey = entry.getKey();
                        break;
                    }
                }
            }

            if (toBeRemovedKey != null)
            {
                break;
            }
        }

        if (toBeRemovedKey != null)
        {
            linkedSpaceMap.remove(toBeRemovedKey);
        }

    }

    @Override
    public void destroy() throws Exception
    {
        eventPublisher.unregister(this);
    }
}
