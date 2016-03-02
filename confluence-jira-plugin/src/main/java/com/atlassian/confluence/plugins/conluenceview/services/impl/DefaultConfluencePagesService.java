package com.atlassian.confluence.plugins.conluenceview.services.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.atlassian.confluence.api.model.Expansion;
import com.atlassian.confluence.api.model.Expansions;
import com.atlassian.confluence.api.model.content.Content;
import com.atlassian.confluence.api.model.content.Label;
import com.atlassian.confluence.api.model.content.Version;
import com.atlassian.confluence.api.model.link.LinkType;
import com.atlassian.confluence.api.model.pagination.PageRequest;
import com.atlassian.confluence.api.model.pagination.PageResponse;
import com.atlassian.confluence.api.model.pagination.SimplePageRequest;
import com.atlassian.confluence.api.service.search.CQLSearchService;
import com.atlassian.confluence.plugins.conluenceview.query.ConfluencePagesQuery;
import com.atlassian.confluence.plugins.conluenceview.rest.dto.ConfluencePageDto;
import com.atlassian.confluence.plugins.conluenceview.rest.dto.ConfluencePagesDto;
import com.atlassian.confluence.plugins.conluenceview.rest.exception.CacheTokenNotFoundException;
import com.atlassian.confluence.plugins.conluenceview.rest.exception.InvalidRequestException;
import com.atlassian.confluence.plugins.conluenceview.services.ConfluencePagesService;
import com.atlassian.confluence.rest.api.model.RestList;

import com.google.common.annotations.VisibleForTesting;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

public class DefaultConfluencePagesService implements ConfluencePagesService
{
    private static final String PAGES_SEARCH_BY_ID_CQL = "id in (%s) and type = page order by lastModified desc";
    private static final String PAGES_SEARCH_BY_TEXT_CQL = "text ~ \"%s\"";

    private final CQLSearchService searchService;
    private Map<String, String> requestCache;

    public DefaultConfluencePagesService(CQLSearchService searchService)
    {
        this.searchService = searchService;
        requestCache = new HashMap<String, String>();
    }

    @VisibleForTesting
    public void setRequestCache(Map<String, String> requestCache)
    {
        this.requestCache = requestCache;
    }

    public ConfluencePagesDto getPagesInSpace(final ConfluencePagesQuery query) {
        String cql = "type = page and space = '" + query.getSpaceKey() + "' order by lastModified desc";

        return getPages(cql, query.getStart(), query.getLimit());
    }

    public ConfluencePagesDto getPagesByIds(final ConfluencePagesQuery query)
    {
        validate(query);

        String cql = buildCql(query.getCacheToken(), query.getPageIds());

        if (StringUtils.isNotBlank(query.getSearchString()))
        {
            cql = String.format(PAGES_SEARCH_BY_TEXT_CQL, query.getSearchString().trim()) + " and " + cql;
        }

        return getPages(cql, query.getStart(), query.getLimit());
    }

    private ConfluencePagesDto getPages(String cql, int start, int limit) {
        PageRequest request = new SimplePageRequest(start, limit);

        final PageResponse<Content> contents = searchService.searchContent(cql, request,
                new Expansion("history", new Expansions().prepend("lastUpdated")),
                new Expansion("metadata", new Expansions().prepend("labels")));

        final Collection<ConfluencePageDto> pages = new ArrayList<ConfluencePageDto>();
        for (Content content : contents)
        {
            final ConfluencePageDto.Builder builder = ConfluencePageDto.newBuilder();

            final Version lastUpdatedVersion = content.getHistory().getLastUpdatedRef().get();
            // if version 1 (has not been edited)
            if (lastUpdatedVersion.getNumber() == 1)
            {
                builder.withAuthor(content.getHistory().getCreatedBy().getDisplayName());
            }

            builder.withLastModifier(lastUpdatedVersion.getBy().getDisplayName());

            final Date lastUpdated = lastUpdatedVersion.getWhen().toDate();
            builder.withLastModified(lastUpdated);

            builder.withPageId(content.getId().asLong());
            builder.withPageTitle(content.getTitle());
            builder.withPageUrl(content.getLinks().get(LinkType.WEB_UI).getPath());

            final Map<String, Object> metadata = content.getMetadata();
            List<String> labelList = new ArrayList<String>();
            if (metadata != null)
            {
                if (metadata.get("labels") != null)
                {
                    List<Label> labels = ((RestList) metadata.get("labels")).getResults();
                    for (Label label : labels)
                    {
                        labelList.add(label.getLabel());
                    }
                }
            }
            builder.withLabels(labelList);

            pages.add(builder.build());
        }

        return ConfluencePagesDto.newBuilder().withPages(pages).build();
    }

    /**
     * Build cql from pageIds and put to cache to reuse
     * @param token cache token
     * @param pageIds list of page ids
     * @return cql
     */
    private String buildCql(String token, List<Long> pageIds)
    {
        String pageIdsStr = "";

        // get cql from cache by token
        String cql = requestCache.get(token);

        if (pageIds == null || pageIds.isEmpty())
        {
            if (StringUtils.isBlank(cql))
            {
                // tell client to send pageIds in next request
                throw new CacheTokenNotFoundException();
            }
        }
        else
        {
            pageIdsStr = StringUtils.join(pageIds, ",");

            cql = String.format(PAGES_SEARCH_BY_ID_CQL, pageIdsStr);

            requestCache.put(token, cql);
        }

        return cql;
    }

    private void validate(final ConfluencePagesQuery query)
    {
        if (StringUtils.isBlank(query.getCacheToken()))
        {
            throw new InvalidRequestException("Request cache token cannot be empty");
        }
    }

}
