package com.atlassian.confluence.plugins.conluenceview.rest.dto;

import java.util.Date;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@XmlAccessorType (XmlAccessType.FIELD)
@XmlRootElement
public class ConfluencePageDto
{
    private Long pageId;
    private String pageTitle;
    private String pageUrl;
    private Date lastModified;
    private List<String> labels;
    private String author;
    private String lastModifier;

    private ConfluencePageDto(Builder builder)
    {
        author = builder.author;
        pageId = builder.pageId;
        pageTitle = builder.pageTitle;
        pageUrl = builder.pageUrl;
        lastModified = builder.lastModified;
        labels = builder.labels;
        lastModifier = builder.lastModifier;
    }

    public static Builder newBuilder()
    {
        return new Builder();
    }

    public Long getPageId()
    {
        return pageId;
    }

    public String getPageTitle()
    {
        return pageTitle;
    }

    public String getPageUrl()
    {
        return pageUrl;
    }

    public Date getLastModified()
    {
        return lastModified;
    }

    public List<String> getLabels()
    {
        return labels;
    }

    public String getAuthor()
    {
        return author;
    }

    public String getLastModifier()
    {
        return lastModifier;
    }

    public static final class Builder
    {
        private String author;
        private Long pageId;
        private String pageTitle;
        private String pageUrl;
        private Date lastModified;
        private List<String> labels;
        private String lastModifier;

        private Builder()
        {
        }

        public Builder withAuthor(String val)
        {
            author = val;
            return this;
        }

        public Builder withPageId(Long val)
        {
            pageId = val;
            return this;
        }

        public Builder withPageTitle(String val)
        {
            pageTitle = val;
            return this;
        }

        public Builder withPageUrl(String val)
        {
            pageUrl = val;
            return this;
        }

        public Builder withLastModified(Date val)
        {
            lastModified = val;
            return this;
        }

        public Builder withLabels(List<String> val)
        {
            labels = val;
            return this;
        }

        public Builder withLastModifier(String val)
        {
            lastModifier = val;
            return this;
        }

        public ConfluencePageDto build()
        {
            return new ConfluencePageDto(this);
        }
    }
}
