package com.atlassian.confluence.plugins.conluenceview.rest.params;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

@XmlAccessorType (XmlAccessType.FIELD)
@XmlRootElement
@JsonIgnoreProperties (ignoreUnknown = true)
public class PagesSearchParam
{
    List<Long> pageIds;
    String cacheToken;

    public String getCacheToken()
    {
        return cacheToken;
    }

    public List<Long> getPageIds()
    {
        return pageIds;
    }
}
