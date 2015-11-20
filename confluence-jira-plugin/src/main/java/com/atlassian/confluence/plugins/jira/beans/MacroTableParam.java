package com.atlassian.confluence.plugins.jira.beans;

import javax.ws.rs.FormParam;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Keep request parameter to render table
 */
@XmlRootElement
public class MacroTableParam
{
    private String wikiMarkup;
    private String columnName;
    private String order;
    private Boolean clearCache;

    public String getWikiMarkup()
    {
        return wikiMarkup;
    }

    public void setWikiMarkup(String wikiMarkup)
    {
        this.wikiMarkup = wikiMarkup;
    }

    public String getColumnName()
    {
        return columnName;
    }

    public void setColumnName(String columnName)
    {
        this.columnName = columnName;
    }

    public String getOrder()
    {
        return order;
    }

    public void setOrder(String order)
    {
        this.order = order;
    }

    public Boolean getClearCache()
    {
        return clearCache;
    }

    public void setClearCache(Boolean clearCache)
    {
        this.clearCache = clearCache;
    }
}
