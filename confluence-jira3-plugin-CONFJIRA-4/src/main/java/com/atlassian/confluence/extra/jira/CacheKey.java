package com.atlassian.confluence.extra.jira;

import java.io.Serializable;
import java.util.Set;

import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;

/**
 * Used as the key for the jira issues macro cache.
 */
public final class CacheKey implements Serializable
{
    private final String partialUrl;
    private final Set columns;
    private final boolean showCount;
    private final String userName;

    public CacheKey(String partialUrl, Set columns, boolean showCount, boolean useTrustedConnection)
    {
        this.partialUrl = partialUrl;
        this.columns = columns;
        this.showCount = showCount;
        this.userName = useTrustedConnection ? AuthenticatedUserThreadLocal.getUsername() : null;
    }

    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || o.getClass() != CacheKey.class) return false;

        CacheKey cacheKey = (CacheKey) o;

        if (isShowCount() != cacheKey.isShowCount()) return false;
        if (getColumns() != null ? !getColumns().equals(cacheKey.getColumns()) : cacheKey.getColumns() != null) return false;
        if (getPartialUrl() != null ? !getPartialUrl().equals(cacheKey.getPartialUrl()) : cacheKey.getPartialUrl() != null) return false;
        if (getUserName() != null ? !getUserName().equals(cacheKey.getUserName()) : cacheKey.getUserName() != null) return false;

        return true;
    }

    public int hashCode()
    {
        int result;
        result = (partialUrl != null ? partialUrl.hashCode() : 0);
        result = 31 * result + (columns != null ? columns.hashCode() : 0);
        result = 31 * result + (showCount ? 1 : 0);
        result = 31 * result + (userName != null ? userName.hashCode() : 0);
        return result;
    }

    String getPartialUrl()
    {
        return partialUrl;
    }

    Set getColumns()
    {
        return columns;
    }

    boolean isShowCount()
    {
        return showCount;
    }

    String getUserName()
    {
        return userName;
    }

    public String toString()
    {
        return "partialUrl:"+ partialUrl +" columns:"+columns.toString()+" showCount:"+showCount+" userName="+userName;
    }
}

