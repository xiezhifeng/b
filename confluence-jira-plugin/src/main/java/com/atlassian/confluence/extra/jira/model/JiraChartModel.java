package com.atlassian.confluence.extra.jira.model;

public class JiraChartModel implements Locatable
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
