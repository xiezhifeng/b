package com.atlassian.confluence.extra.jira.model;

import java.util.Date;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@XmlAccessorType (XmlAccessType.FIELD)
@XmlRootElement
public class ConfluencePage
{
    Long pageId;
    String pageTitle;
    String pageUrl;
    Date lastModified;

    public ConfluencePage(Long pageId, String pageTitle, String pageUrl, Date lastModified)
    {
        this.pageId = pageId;
        this.pageTitle = pageTitle;
        this.pageUrl = pageUrl;
        this.lastModified = lastModified;
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
}
