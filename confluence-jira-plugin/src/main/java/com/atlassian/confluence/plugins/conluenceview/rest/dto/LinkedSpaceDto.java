package com.atlassian.confluence.plugins.conluenceview.rest.dto;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@XmlAccessorType (XmlAccessType.FIELD)
@XmlRootElement
public class LinkedSpaceDto
{
    String spaceKey;
    String spaceName;
    String spaceUrl;
    String spaceIcon;

    private LinkedSpaceDto(Builder builder)
    {
        spaceIcon = builder.spaceIcon;
        spaceKey = builder.spaceKey;
        spaceName = builder.spaceName;
        spaceUrl = builder.spaceUrl;
    }

    public static Builder newBuilder()
    {
        return new Builder();
    }

    public String getSpaceKey()
    {
        return spaceKey;
    }

    public String getSpaceIcon()
    {
        return spaceIcon;
    }

    public String getSpaceName()
    {
        return spaceName;
    }

    public String getSpaceUrl()
    {
        return spaceUrl;
    }


    public static final class Builder
    {
        private String spaceIcon;
        private String spaceKey;
        private String spaceName;
        private String spaceUrl;

        private Builder()
        {
        }

        public Builder withSpaceIcon(String val)
        {
            spaceIcon = val;
            return this;
        }

        public Builder withSpaceKey(String val)
        {
            spaceKey = val;
            return this;
        }

        public Builder withSpaceName(String val)
        {
            spaceName = val;
            return this;
        }

        public Builder withSpaceUrl(String val)
        {
            spaceUrl = val;
            return this;
        }

        public LinkedSpaceDto build()
        {
            return new LinkedSpaceDto(this);
        }
    }
}
