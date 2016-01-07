package com.atlassian.confluence.plugins.conluenceview.services.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.atlassian.confluence.api.model.Expansion;
import com.atlassian.confluence.api.model.Expansions;
import com.atlassian.confluence.api.model.content.Content;
import com.atlassian.confluence.api.model.link.LinkType;
import com.atlassian.confluence.api.model.pagination.PageResponse;
import com.atlassian.confluence.api.service.search.CQLSearchService;
import com.atlassian.confluence.extra.jira.model.ConfluencePage;
import com.atlassian.confluence.plugins.conluenceview.rest.results.ConfluencePagesSearchResult;
import com.atlassian.confluence.plugins.conluenceview.services.ConfluencePagesQuery;
import com.atlassian.confluence.plugins.conluenceview.services.ConfluencePagesService;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;

public class DefaultConfluencePagesService implements ConfluencePagesService
{
    public static final String PAGES_SEARCH_CQL = "id in (%s) and type = page order by lastModified desc";

    private final CQLSearchService searchService;

    public DefaultConfluencePagesService(CQLSearchService searchService)
    {
        this.searchService = searchService;
    }

    Map<String, String> requestCache = new HashMap<String, String>();

    public ConfluencePagesSearchResult search(final ConfluencePagesQuery query)
    {
        final String cql = buildCql(query);

        final PageResponse<Content> contents = searchService.searchContent(cql, new Expansion("history", new Expansions().prepend("lastUpdated")));

        final Collection<ConfluencePage> pages = new ArrayList<ConfluencePage>();
        for (Content content : contents)
        {
//            final Date lastUpdated = content.getHistory().getLastUpdatedRef().get().getWhen().toDate();
            pages.add(new ConfluencePage(content.getId().asLong(), content.getTitle(), content.getLinks().get(LinkType.WEB_UI).getPath(), new Date()));
        }

        final String cacheToken = StringUtils.isBlank(query.getCacheToken()) ? UUID.randomUUID().toString() : query.getCacheToken();
        requestCache.put(cacheToken, cql);

        return ConfluencePagesSearchResult.newBuilder().withCacheToken(cacheToken).withPages(pages).build();
    }

    private String buildCql(ConfluencePagesQuery query)
    {
        String pageIdsStr = "";

        List<Long> pageIds = query.getPageIds();

        if (CollectionUtils.isEmpty(pageIds))
        {
            String cacheToken = query.getCacheToken();
            if (StringUtils.isNotBlank(cacheToken))
            {
                pageIdsStr = requestCache.get(cacheToken);
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
        }

        return String.format(PAGES_SEARCH_CQL, pageIdsStr);
    }

}
