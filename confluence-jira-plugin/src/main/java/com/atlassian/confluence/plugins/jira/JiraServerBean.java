package com.atlassian.confluence.plugins.jira;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class JiraServerBean
{
    @XmlElement
    private String id;
    
    @XmlElement 
    private String name;
    
    @XmlElement
    private boolean selected;
    
    @XmlElement
    private String authUrl;
    
    @XmlElement
    private String url;

    @XmlElement
    private Long buildNumber;

    public JiraServerBean(String id, String url, String name, boolean selected, String authUrl, Long buildNumber)
    {
        this.id = id;
        this.url = url;
        this.name = name;
        this.selected = selected;
        this.authUrl = authUrl;
        this.buildNumber = buildNumber;
    }

    public String getId()
    {
        return id;
    }

    public void setId(String id)
    {
        this.id = id;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public boolean isSelected()
    {
        return selected;
    }

    public void setSelected(boolean selected)
    {
        this.selected = selected;
    }

    public String getAuthUrl()
    {
        return authUrl;
    }

    public void setAuthUrl(String authUrl)
    {
        this.authUrl = authUrl;
    }

    public String getUrl()
    {
        return url;
    }

    public void setUrl(String url)
    {
        this.url = url;
    }

    public Long getBuildNumber()
    {
        return buildNumber;
    }

    public void setBuildNumber(Long buildNumber)
    {
        this.buildNumber = buildNumber;
    }
}
