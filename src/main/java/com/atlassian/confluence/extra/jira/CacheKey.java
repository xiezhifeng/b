package com.atlassian.confluence.extra.jira;

import java.io.Serializable;
import java.util.Set;

import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;

/**
 * Used as the key for the jira issues macro cache.
 */
public final class CacheKey implements Serializable
{
    private final String url;
    private final Set columns;
    private final boolean showCount;
    private final String template;
    private final String userName;

    public CacheKey(String url, Set columns, boolean showCount, String template, boolean useTrustedConnection)
    {
        this.url = url;
        this.columns = columns;
        this.showCount = showCount;
        this.template = template;
        this.userName = useTrustedConnection ? AuthenticatedUserThreadLocal.getUsername() : null;
    }

    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || o.getClass() != CacheKey.class) return false;

        CacheKey cacheKey = (CacheKey) o;

        if (isShowCount() != cacheKey.isShowCount()) return false;
        if (getColumns() != null ? !getColumns().equals(cacheKey.getColumns()) : cacheKey.getColumns() != null) return false;
        if (getTemplate() != null ? !getTemplate().equals(cacheKey.getTemplate()) : cacheKey.getTemplate() != null) return false;
        if (getUrl() != null ? !getUrl().equals(cacheKey.getUrl()) : cacheKey.getUrl() != null) return false;
        if (getUserName() != null ? !getUserName().equals(cacheKey.getUserName()) : cacheKey.getUserName() != null) return false;

        return true;
    }

    public int hashCode()
    {
        int result;
        result = (url != null ? url.hashCode() : 0);
        result = 31 * result + (columns != null ? columns.hashCode() : 0);
        result = 31 * result + (showCount ? 1 : 0);
        result = 31 * result + (template != null ? template.hashCode() : 0);
        result = 31 * result + (userName != null ? userName.hashCode() : 0);
        return result;
    }

    String getUrl()
    {
        return url;
    }

    Set getColumns()
    {
        return columns;
    }

    boolean isShowCount()
    {
        return showCount;
    }

    String getTemplate()
    {
        return template;
    }

    String getUserName()
    {
        return userName;
    }

    public String toString()
    {
        return "url:"+url+" columns:"+columns.toString()+" showCount:"+showCount+" userName="+userName;
    }
}

