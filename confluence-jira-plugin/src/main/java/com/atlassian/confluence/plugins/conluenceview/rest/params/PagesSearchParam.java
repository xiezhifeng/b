package com.atlassian.confluence.plugins.conluenceview.rest.params;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.map.annotate.JsonSerialize;

@XmlAccessorType (XmlAccessType.FIELD)
@XmlRootElement
@JsonIgnoreProperties (ignoreUnknown = true)
@JsonSerialize (include = JsonSerialize.Inclusion.NON_NULL)
public class PagesSearchParam
{
    List<Long> pageIds;
    String cacheToken;

    Integer limit;
    Integer start;
    String searchString;

    public String getCacheToken()
    {
        return cacheToken;
    }

    public List<Long> getPageIds()
    {
        return pageIds;
    }

    public Integer getLimit()
    {
        return limit;
    }

    public Integer getStart()
    {
        return start;
    }

    public String getSearchString()
    {
        return searchString;
    }
}
