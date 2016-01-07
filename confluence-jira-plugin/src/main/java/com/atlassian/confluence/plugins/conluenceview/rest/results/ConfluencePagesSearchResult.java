package com.atlassian.confluence.plugins.conluenceview.rest.results;

import java.util.Collection;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import com.atlassian.confluence.extra.jira.model.ConfluencePage;

@XmlAccessorType (XmlAccessType.FIELD)
@XmlRootElement
public class ConfluencePagesSearchResult
{
    String cacheToken;
    Collection<ConfluencePage> pages;

    private ConfluencePagesSearchResult(Builder builder)
    {
        cacheToken = builder.cacheToken;
        pages = builder.pages;
    }

    public String getCacheToken()
    {
        return cacheToken;
    }

    public Collection<ConfluencePage> getPages()
    {
        return pages;
    }

    public static Builder newBuilder()
    {
        return new Builder();
    }

    public static final class Builder
    {
        private String cacheToken;
        private Collection<ConfluencePage> pages;

        private Builder()
        {
        }

        public Builder withCacheToken(String cacheToken)
        {
            this.cacheToken = cacheToken;
            return this;
        }

        public Builder withPages(Collection<ConfluencePage> pages)
        {
            this.pages = pages;
            return this;
        }

        public ConfluencePagesSearchResult build()
        {
            return new ConfluencePagesSearchResult(this);
        }
    }
}
