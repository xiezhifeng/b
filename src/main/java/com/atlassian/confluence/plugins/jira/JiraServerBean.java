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
    private boolean isAdministrator;

    public JiraServerBean(String id, String url, String name, boolean selected, String authUrl, boolean isAdministrator)
    {
        super();
        this.id = id;
        this.url = url;
        this.name = name;
        this.selected = selected;
        this.authUrl = authUrl;
        this.isAdministrator = isAdministrator;
    }
    
    public JiraServerBean(boolean isAdministrator) {
    	super();
    	this.isAdministrator = isAdministrator;
    }
    
}
