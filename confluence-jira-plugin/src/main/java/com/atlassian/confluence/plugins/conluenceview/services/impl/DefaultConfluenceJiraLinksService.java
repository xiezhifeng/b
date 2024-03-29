package com.atlassian.confluence.plugins.conluenceview.services.impl;

import com.atlassian.applinks.api.EntityLink;
import com.atlassian.applinks.api.ReadOnlyApplicationLink;
import com.atlassian.applinks.api.ReadOnlyApplicationLinkService;
import com.atlassian.applinks.api.application.jira.JiraApplicationType;
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
import com.atlassian.confluence.plugins.conluenceview.rest.dto.LinkedSpaceDto;
import com.atlassian.confluence.plugins.conluenceview.rest.exception.InvalidRequestException;
import com.atlassian.confluence.plugins.conluenceview.services.ConfluenceJiraLinksService;
import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.confluence.user.ConfluenceUser;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DefaultConfluenceJiraLinksService implements ConfluenceJiraLinksService
{
    private static final int MAX_SPACES = 100;

    private final MutatingEntityLinkService entityLinkService;
    private final InternalHostApplication applinkHostApplication;
    private final HostApplication hostApplication;
    private final ReadOnlyApplicationLinkService appLinkService;
    private final SpaceService spaceService;

    public DefaultConfluenceJiraLinksService(MutatingEntityLinkService entityLinkService, InternalHostApplication applinkHostApplication,
             HostApplication hostApplication, ReadOnlyApplicationLinkService appLinkService,
             SpaceService spaceService)
    {
        this.entityLinkService = entityLinkService;
        this.applinkHostApplication = applinkHostApplication;
        this.hostApplication = hostApplication;
        this.appLinkService = appLinkService;
        this.spaceService = spaceService;
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

        List<LinkedSpaceDto> spaceDtos = new ArrayList<>();
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
}
