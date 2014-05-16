package com.atlassian.confluence.plugins.jiracharts.model;

import com.atlassian.confluence.extra.jira.model.Locatable;

public class JiraImageChartModel implements Locatable
{
    private String location;
    private String filterUrl;
    
    @Override
    public String getLocation()
    {
        return location;
    }
    public void setLocation(String location)
    {
        this.location = location;
    }
    public String getFilterUrl()
    {
        return filterUrl;
    }
    public void setFilterUrl(String filterUrl)
    {
        this.filterUrl = filterUrl;
    }
}
