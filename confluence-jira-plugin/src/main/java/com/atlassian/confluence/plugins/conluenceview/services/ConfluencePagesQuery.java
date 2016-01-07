package com.atlassian.confluence.plugins.conluenceview.services;

import java.util.List;

public class ConfluencePagesQuery
{
    List<Long> pageIds;
    String cacheToken;

    private ConfluencePagesQuery(Builder builder)
    {
        cacheToken = builder.cacheToken;
        pageIds = builder.pageIds;
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


    public static final class Builder
    {
        private String cacheToken;
        private List<Long> pageIds;

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

        public ConfluencePagesQuery build()
        {
            return new ConfluencePagesQuery(this);
        }
    }
}
