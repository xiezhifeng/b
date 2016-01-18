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

    public ConfluencePageDto(Long pageId, String pageTitle, String pageUrl, Date lastModified, List<String> labels)
    {
        this.pageId = pageId;
        this.pageTitle = pageTitle;
        this.pageUrl = pageUrl;
        this.lastModified = lastModified;
        this.labels = labels;
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
}
