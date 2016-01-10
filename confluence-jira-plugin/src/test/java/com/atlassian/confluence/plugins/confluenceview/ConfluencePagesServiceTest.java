package com.atlassian.confluence.plugins.confluenceview;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.atlassian.confluence.api.model.Expansion;
import com.atlassian.confluence.api.model.content.Content;
import com.atlassian.confluence.api.model.content.History;
import com.atlassian.confluence.api.model.content.Version;
import com.atlassian.confluence.api.model.content.id.ContentId;
import com.atlassian.confluence.api.model.link.Link;
import com.atlassian.confluence.api.model.link.LinkType;
import com.atlassian.confluence.api.model.pagination.PageResponse;
import com.atlassian.confluence.api.model.pagination.PageResponseImpl;
import com.atlassian.confluence.api.model.reference.Reference;
import com.atlassian.confluence.api.service.search.CQLSearchService;
import com.atlassian.confluence.plugins.conluenceview.rest.dto.ConfluencePagesSearchDto;
import com.atlassian.confluence.plugins.conluenceview.rest.exception.CacheTokenNotFoundException;
import com.atlassian.confluence.plugins.conluenceview.rest.exception.InvalidRequestException;
import com.atlassian.confluence.plugins.conluenceview.services.ConfluencePagesQuery;
import com.atlassian.confluence.plugins.conluenceview.services.ConfluencePagesService;
import com.atlassian.confluence.plugins.conluenceview.services.impl.DefaultConfluencePagesService;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.powermock.api.mockito.PowerMockito.when;

public class ConfluencePagesServiceTest
{
    ConfluencePagesService service;

    @Mock
    CQLSearchService cqlSearchService;

    @Mock
    Map<String, String> cache;

    PageResponse<Content> contents;
    Content content1, content2;

    private static final String TOKEN = "token";

    @Before
    public void setUp()
    {
        initMocks(this);
        final Date now = new Date();

        content1 = Content.builder().id(ContentId.valueOf("1"))
                .title("page 1")
                .addLink(new Link(LinkType.WEB_UI, "link 1"))
                .history(History.builder().lastUpdated(Reference.to(Version.builder().when(now).build())).build()).build();
        content2 = Content.builder().id(ContentId.valueOf("2"))
                .title("page 2")
                .addLink(new Link(LinkType.WEB_UI, "link 2"))
                .history(History.builder().lastUpdated(Reference.to(Version.builder().when(now).build())).build()).build();

        contents = new PageResponseImpl.Builder<Content>().add(content1).add(content2).build();
        service = new DefaultConfluencePagesService(cqlSearchService);

        ((DefaultConfluencePagesService)service).setRequestCache(cache);
    }

    @Test
    public void testEmptyCacheToken()
    {
        try
        {
            ConfluencePagesQuery query = ConfluencePagesQuery.newBuilder().build();

            service.search(query);
            assertTrue("This line of code is not expected to be executed", false);
        }
        catch (Exception e)
        {
            assertTrue(e instanceof InvalidRequestException);
            assertEquals("Request cache token cannot be empty", e.getMessage());
        }
    }

    @Test
    public void testCacheTokenNotFoundAndEmptyPageIds()
    {
        try
        {
            ConfluencePagesQuery query = ConfluencePagesQuery.newBuilder().withCacheToken(TOKEN).build();
            when(cache.get(TOKEN)).thenReturn(null);
            service.search(query);
            assertTrue("This line of code is not expected to be executed", false);
        }
        catch (Exception e)
        {
            assertTrue(e instanceof CacheTokenNotFoundException);
        }
    }

    @Test
    public void testCacheTokenNotFoundAndPageIds()
    {
        List<Long> pageIds = Arrays.asList(1l, 2l);

        final ConfluencePagesQuery query = ConfluencePagesQuery.newBuilder().withCacheToken(TOKEN).withPageIds(pageIds).build();

        String cql = "id in (1,2) and type = page order by lastModified desc";
        when(cqlSearchService.searchContent(eq(cql), isA(Expansion.class))).thenReturn(contents);

        when(cache.get(TOKEN)).thenReturn(null);
        final ConfluencePagesSearchDto result = service.search(query);
        assertEquals(2, result.getPages().size());
        // verify that request ids is cached
        Mockito.verify(cache, times(1)).put(TOKEN, cql);
    }

    @Test
    public void testWithCacheTokenInCache()
    {
        List<Long> pageIds = Arrays.asList(1l, 2l);

        final ConfluencePagesQuery query = ConfluencePagesQuery.newBuilder().withCacheToken(TOKEN).withPageIds(pageIds).build();

        String cql = "id in (1,2) and type = page order by lastModified desc";
        when(cache.get(TOKEN)).thenReturn(cql);

        when(cqlSearchService.searchContent(eq(cql), isA(Expansion.class))).thenReturn(contents);

        ConfluencePagesSearchDto result = service.search(query);
        assertEquals(2, result.getPages().size());

        Mockito.verify(cache, never()).put(TOKEN, cql);
    }
}
