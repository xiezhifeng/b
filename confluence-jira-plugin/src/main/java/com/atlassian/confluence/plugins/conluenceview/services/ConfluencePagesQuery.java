package com.atlassian.confluence.plugins.conluenceview.services;

import java.util.List;

public class ConfluencePagesQuery
{
    public static final int DEFAULT_LIMIT = 1;
    public static final int DEFAULT_START = 0;

    List<Long> pageIds;
    String cacheToken;
    Integer limit;
    Integer start;

    private ConfluencePagesQuery(Builder builder)
    {
        cacheToken = builder.cacheToken;
        pageIds = builder.pageIds;
        limit = builder.limit;
        start = builder.start;
    }

    public static Builder newBuilder()
    {
        return new Builder();
    }

    public List<Long> getPageIds()
    {
        return pageIds;
    }

    public String getCacheToken()
    {
        return cacheToken;
    }

    public Integer getLimit()
    {
        return limit == null ? DEFAULT_LIMIT : limit;
    }

    public Integer getStart()
    {
        return start == null ? DEFAULT_START : start;
    }

    public static final class Builder
    {
        private String cacheToken;
        private List<Long> pageIds;
        private Integer limit = DEFAULT_LIMIT;
        private Integer start = DEFAULT_START;

        private Builder()
        {
        }

        public Builder withCacheToken(String cacheToken)
        {
            this.cacheToken = cacheToken;
            return this;
        }

        public Builder withPageIds(List<Long> pageIds)
        {
            this.pageIds = pageIds;
            return this;
        }

        public Builder withLimit(Integer limit)
        {
            this.limit = limit;
            return this;
        }

        public Builder withStart(Integer start)
        {
            this.start = start;
            return this;
        }

        public ConfluencePagesQuery build()
        {
            return new ConfluencePagesQuery(this);
        }
    }
}
