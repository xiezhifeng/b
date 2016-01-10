package com.atlassian.confluence.plugins.conluenceview.rest.dto;

import java.util.Collection;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import com.atlassian.confluence.extra.jira.model.ConfluencePage;

@XmlAccessorType (XmlAccessType.FIELD)
@XmlRootElement
public class ConfluencePagesSearchDto extends GenericResponseDto
{
    Collection<ConfluencePage> pages;

    private ConfluencePagesSearchDto(Builder builder)
    {
        super(builder.status, builder.errorMessage);
        pages = builder.pages;
    }

    public Collection<ConfluencePage> getPages()
    {
        return pages;
    }

    public static Builder newBuilder()
    {
        return new Builder();
    }

    public static final class Builder extends GenericResponseDto.Builder
    {
        private Collection<ConfluencePage> pages;

        public Builder withPages(Collection<ConfluencePage> pages)
        {
            this.pages = pages;
            return this;
        }

        public ConfluencePagesSearchDto build()
        {
            return new ConfluencePagesSearchDto(this);
        }
    }
}
