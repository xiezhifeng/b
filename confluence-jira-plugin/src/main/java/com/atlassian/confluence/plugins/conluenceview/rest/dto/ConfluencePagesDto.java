package com.atlassian.confluence.plugins.conluenceview.rest.dto;

import java.util.Collection;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@XmlAccessorType (XmlAccessType.FIELD)
@XmlRootElement
public class ConfluencePagesDto extends GenericResponseDto
{
    Collection<ConfluencePageDto> pages;

    private ConfluencePagesDto(Builder builder)
    {
        super(builder);
        pages = builder.pages;
    }

    public Collection<ConfluencePageDto> getPages()
    {
        return pages;
    }

    public static Builder newBuilder()
    {
        return new Builder();
    }

    public static final class Builder extends GenericResponseDto.Builder
    {
        private Collection<ConfluencePageDto> pages;

        public Builder withPages(Collection<ConfluencePageDto> pages)
        {
            this.pages = pages;
            return this;
        }

        public ConfluencePagesDto build()
        {
            return new ConfluencePagesDto(this);
        }
    }
}
