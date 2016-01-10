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
import com.atlassian.confluence.api.model.link.LinkType;
import com.atlassian.confluence.api.model.pagination.PageRequest;
import com.atlassian.confluence.api.model.pagination.PageResponse;
import com.atlassian.confluence.api.model.pagination.SimplePageRequest;
import com.atlassian.confluence.api.service.search.CQLSearchService;
import com.atlassian.confluence.extra.jira.model.ConfluencePage;
import com.atlassian.confluence.plugins.conluenceview.rest.dto.ConfluencePagesSearchDto;
import com.atlassian.confluence.plugins.conluenceview.rest.exception.CacheTokenNotFoundException;
import com.atlassian.confluence.plugins.conluenceview.rest.exception.InvalidRequestException;
import com.atlassian.confluence.plugins.conluenceview.services.ConfluencePagesQuery;
import com.atlassian.confluence.plugins.conluenceview.services.ConfluencePagesService;

import com.google.common.annotations.VisibleForTesting;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

public class DefaultConfluencePagesService implements ConfluencePagesService
{
    public static final String PAGES_SEARCH_CQL = "id in (%s) and type = page order by lastModified desc";

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

    public ConfluencePagesSearchDto search(final ConfluencePagesQuery query)
    {
        validate(query);

        final String cql = buildCql(query);

        PageRequest request = new SimplePageRequest(query.getStart(), query.getLimit());
        final PageResponse<Content> contents = searchService.searchContent(cql, request, new Expansion("history", new Expansions().prepend("lastUpdated")));

        final Collection<ConfluencePage> pages = new ArrayList<ConfluencePage>();
        for (Content content : contents)
        {
            final Date lastUpdated = content.getHistory().getLastUpdatedRef().get().getWhen().toDate();
            pages.add(new ConfluencePage(content.getId().asLong(), content.getTitle(), content.getLinks().get(LinkType.WEB_UI).getPath(), lastUpdated));
        }

        return ConfluencePagesSearchDto.newBuilder().withPages(pages).build();
    }

    private String buildCql(ConfluencePagesQuery query)
    {
        String pageIdsStr = "";

        String token = query.getCacheToken();

        // get cql from cache by token
        String cql = requestCache.get(token);

        final List<Long> pageIds = query.getPageIds();

        if (CollectionUtils.isEmpty(pageIds))
        {
            if (StringUtils.isBlank(cql))
            {
                // tell client to send pageIds in next request
                throw new CacheTokenNotFoundException();
            }
        }
        else
        {
            StringBuffer buffer = new StringBuffer("");

            for (Long pageId : pageIds)
            {
                buffer.append(pageId + ",");
            }

            pageIdsStr = buffer.toString();
            if (pageIdsStr.contains(","))
            {
                pageIdsStr = pageIdsStr.substring(0, pageIdsStr.length() - 1);
            }

            cql = String.format(PAGES_SEARCH_CQL, pageIdsStr);

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
