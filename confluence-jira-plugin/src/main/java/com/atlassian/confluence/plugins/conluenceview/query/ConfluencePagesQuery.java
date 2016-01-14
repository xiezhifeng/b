package com.atlassian.confluence.plugins.conluenceview.query;

import java.util.List;

public class ConfluencePagesQuery extends PagingQuery
{
    List<Long> pageIds;
    String cacheToken;
    String searchString;

    private ConfluencePagesQuery(Builder builder)
    {
        super(builder);
        cacheToken = builder.cacheToken;
        pageIds = builder.pageIds;
        this.searchString = builder.searchString;
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

    public String getSearchString()
    {
        return searchString;
    }

    public static final class Builder extends PagingQuery.Builder
    {
        private String cacheToken;
        private List<Long> pageIds;
        private String searchString;

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

        public Builder withSearchString(String searchString)
        {
            this.searchString = searchString;
            return this;
        }

        public Builder withLimit(Integer limit)
        {
            super.withLimit(limit);
            return this;
        }

        public Builder withStart(Integer start)
        {
            super.withStart(start);
            return this;
        }

        public ConfluencePagesQuery build()
        {
            return new ConfluencePagesQuery(this);
        }
    }
}
