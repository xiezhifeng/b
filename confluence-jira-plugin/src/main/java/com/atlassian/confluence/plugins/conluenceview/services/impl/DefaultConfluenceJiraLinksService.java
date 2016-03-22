package com.atlassian.confluence.plugins.conluenceview.services.impl;

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
import com.atlassian.confluence.api.model.Expansion;
import com.atlassian.confluence.api.model.content.Space;
import com.atlassian.confluence.api.model.pagination.PageResponse;
import com.atlassian.confluence.api.model.pagination.SimplePageRequest;
import com.atlassian.confluence.api.service.content.SpaceService;
import com.atlassian.confluence.event.events.space.SpaceLogoUpdateEvent;
import com.atlassian.confluence.event.events.space.SpacePermissionsUpdateEvent;
import com.atlassian.confluence.event.events.space.SpaceRemoveEvent;
import com.atlassian.confluence.event.events.space.SpaceUpdateEvent;
import com.atlassian.confluence.plugins.conluenceview.rest.dto.LinkedSpaceDto;
import com.atlassian.confluence.plugins.conluenceview.rest.exception.InvalidRequestException;
import com.atlassian.confluence.plugins.conluenceview.services.ConfluenceJiraLinksService;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.confluence.user.ConfluenceUser;
import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.atlassian.util.concurrent.Lazy;
import com.atlassian.util.concurrent.Supplier;
import com.atlassian.vcache.ExternalCacheSettingsBuilder;
import com.atlassian.vcache.PutPolicy;
import com.atlassian.vcache.StableReadExternalCache;
import com.atlassian.vcache.VCacheFactory;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;

import static com.atlassian.vcache.VCacheUtils.fold;
import static com.atlassian.vcache.marshallers.MarshallerFactory.serializableMarshaller;
import static java.util.function.Function.identity;

public class DefaultConfluenceJiraLinksService implements ConfluenceJiraLinksService, DisposableBean
{
    private static final Logger log = LoggerFactory.getLogger(DefaultConfluenceJiraLinksService.class);

    private static final int MAX_SPACES = 100;

    private static final int CACHE_EXPIRE_TIME = 10; // 10 minutes
    private final MutatingEntityLinkService entityLinkService;
    private final InternalHostApplication applinkHostApplication;
    private final HostApplication hostApplication;
    private final ReadOnlyApplicationLinkService appLinkService;
    private final EventPublisher eventPublisher;
    private final SpaceService spaceService;
    private final Supplier<StableReadExternalCache<List<LinkedSpaceDto>>> cacheRef;

    public DefaultConfluenceJiraLinksService(MutatingEntityLinkService entityLinkService, InternalHostApplication applinkHostApplication,
             HostApplication hostApplication, ReadOnlyApplicationLinkService appLinkService,
             EventPublisher eventPublisher, SpaceService spaceService, VCacheFactory cacheFactory)
    {
        this.entityLinkService = entityLinkService;
        this.applinkHostApplication = applinkHostApplication;
        this.hostApplication = hostApplication;
        this.appLinkService = appLinkService;
        this.eventPublisher = eventPublisher;
        this.spaceService = spaceService;
        this.cacheRef = Lazy.supplier(() -> createCache(cacheFactory));
        eventPublisher.register(this);
    }

    private static StableReadExternalCache<List<LinkedSpaceDto>> createCache(VCacheFactory cacheFactory) {
        return (StableReadExternalCache) cacheFactory.getStableReadExternalCache(
                LinkedSpaceDto.class.getName(),
                serializableMarshaller(ArrayList.class),
                new ExternalCacheSettingsBuilder()
                        .defaultTtl(Duration.of(CACHE_EXPIRE_TIME, ChronoUnit.MINUTES))
                        .build());
    }

    @EventListener
    public void onEntityLinkAddedEvent(EntityLinkAddedEvent event)
    {
        final EntityLink entityLink = event.getEntityLink();
        if ((entityLink.getType().getClass() == JiraProjectEntityTypeImpl.class))
        {
            removeFromCache(getCacheKey(entityLink.getApplicationLink().getDisplayUrl().toString(), entityLink.getKey()));
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
                removeFromCache(getCacheKey(appLink.getDisplayUrl().toString(), event.getEntityKey()));
            }
        }
    }

    @EventListener
    public void onSpaceLogoUpdateEvent(SpaceLogoUpdateEvent event)
    {
        clearCache();
    }

    @EventListener
    public void onSpaceUpdateEvent(SpaceUpdateEvent event)
    {
        clearCache();
    }

    @EventListener
    public void onSpacePermissionsUpdateEvent(SpacePermissionsUpdateEvent event)
    {
        clearCache();
    }

    @EventListener
    public void onSpaceRemoveEvent(SpaceRemoveEvent event)
    {
        clearCache();
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
            throw new RuntimeException("User is not authenticated");
        }

        if (StringUtils.isBlank(jiraUrl) || StringUtils.isBlank(projectKey))
        {
            throw new InvalidRequestException("JIRA url and project key cannot be empty");
        }

        String cacheKey = getCacheKey(jiraUrl, projectKey);

        List<LinkedSpaceDto> spaceDtos = fold(
                getCache().get(cacheKey),
                identity(),
                t -> {
                    log.warn("Failed to retrieve value for key '{}': {}", cacheKey, t.getMessage(), t);
                    return Optional.empty();
                }).orElse(null);
        if (spaceDtos != null)
        {
            return spaceDtos;
        }

        final ReadOnlyApplicationLink appLink = getAppLink(jiraUrl);
        if (appLink == null)
        {
            return Collections.emptyList();
        }

        List<String> spaceKeys = new ArrayList<>();

        final Iterable<EntityReference> localEntities = applinkHostApplication.getLocalEntities();
        for (EntityReference localEntity : localEntities)
        {
            if (hasLinkToProject(localEntity, projectKey))
            {
                spaceKeys.add(localEntity.getKey());
            }
        }

        spaceDtos = new ArrayList<>();
        if (spaceKeys.size() > 0)
        {
            PageResponse<com.atlassian.confluence.api.model.content.Space> spaces = spaceService.find(
                    new Expansion("icon"))
                    .withKeys(spaceKeys.toArray(new String[spaceKeys.size()]))
                    .fetchMany(new SimplePageRequest(0, MAX_SPACES));

            for (Space space : spaces)
            {
                spaceDtos.add(LinkedSpaceDto.newBuilder()
                        .withSpaceKey(space.getKey())
                        .withSpaceName(space.getName())
                        .withSpaceUrl("/display/" + space.getKey())
                        .withSpaceIcon(space.getIconRef().get().getPath()).build());
            }
        }

        fold(getCache().put(cacheKey, spaceDtos, PutPolicy.PUT_ALWAYS),
                identity(),
                t -> {
                    log.warn("Failed to put for key '{}': {}", cacheKey, t.getMessage(), t);
                    return false;
                });

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

    private void clearCache() {
        fold(getCache().removeAll(), identity(), t -> {
            log.warn("Failed to clear cache: {}", t.getMessage(), t);
            return null;
        });
    }

    private void removeFromCache(String cacheKey) {
        fold(getCache().remove(cacheKey), identity(), t -> {
            log.warn("Failed to remove key '{}' from cache: {}", cacheKey, t.getMessage(), t);
            return null;
        });
    }

    @Override
    public void destroy() throws Exception
    {
        eventPublisher.unregister(this);
    }

    private StableReadExternalCache<List<LinkedSpaceDto>> getCache()
    {
        return cacheRef.get();
    }
}
