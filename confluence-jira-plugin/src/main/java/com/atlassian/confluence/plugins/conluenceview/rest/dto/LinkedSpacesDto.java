package com.atlassian.confluence.plugins.conluenceview.rest.dto;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@XmlAccessorType (XmlAccessType.FIELD)
@XmlRootElement
public class LinkedSpacesDto extends GenericResponseDto
{
    private List<LinkedSpaceDto> spaces;

    protected LinkedSpacesDto(Builder builder)
    {
        super(builder);
        this.spaces = builder.spaces;
    }

    public static Builder newBuilder()
    {
        return new Builder();
    }

    public static final class Builder extends GenericResponseDto.Builder
    {
        private List<LinkedSpaceDto> spaces;

        public Builder withSpaces(List<LinkedSpaceDto> spaces)
        {
            this.spaces = spaces;
            return this;
        }

        public LinkedSpacesDto build()
        {
            return new LinkedSpacesDto(this);
        }
    }

}
