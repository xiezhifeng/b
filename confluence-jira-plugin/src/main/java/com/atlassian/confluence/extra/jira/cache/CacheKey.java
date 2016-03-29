package com.atlassian.confluence.extra.jira.cache;

import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.google.common.base.Objects;

import java.io.Serializable;
import java.util.List;

/**
 * Used as the key for the jira issues macro cache.
 */
public final class CacheKey implements Serializable
{
    private final String partialUrl;
    private final List<String> columns;
    private final boolean showCount;
    private final String userName;
    private final boolean forFlexigrid;
    private final String appId;
    private final boolean mapped;
    private final String version;

    public CacheKey(String partialUrl, String appId, List<String> columns, boolean showCount, boolean forceAnonymous,
            boolean forFlexigrid, boolean mapped, String version)
    {
        this.appId = appId;
        this.partialUrl = partialUrl;
        this.columns = columns;
        this.showCount = showCount;
        this.userName = !forceAnonymous ? AuthenticatedUserThreadLocal.getUsername() : null;
        this.forFlexigrid = forFlexigrid;
        this.mapped = mapped;
        this.version = version;
    }
    
    public String getPartialUrl()
    {
        return partialUrl;
    }

    public List<String> getColumns()
    {
        return columns;
    }

    public boolean isShowCount()
    {
        return showCount;
    }
    
    public boolean isMapped()
    {
        return mapped;
    }

    public String getUserName()
    {
        return userName;
    }

    public String getVersion()
    {
        return version;
    }

    public String toString()
    {
        return "partialUrl:"+ partialUrl +" columns:"+columns.toString()+" showCount:"+showCount+" userName="+userName+" isMapped="+isMapped();
    }

    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((appId == null) ? 0 : appId.hashCode());
        result = prime * result + ((columns == null) ? 0 : columns.hashCode());
        result = prime * result + (forFlexigrid ? 1231 : 1237);
        result = prime * result + (mapped ? 1231 : 1237);
        result = prime * result + ((partialUrl == null) ? 0 : partialUrl.hashCode());
        result = prime * result + (showCount ? 1231 : 1237);
        result = prime * result + ((userName == null) ? 0 : userName.hashCode());
        result = prime * result + ((version == null) ? 0 : version.hashCode());
        return result;
    }

    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        CacheKey other = (CacheKey) obj;
        if (appId == null)
        {
            if (other.appId != null)
                return false;
        } else if (!appId.equals(other.appId))
            return false;
        if (columns == null)
        {
            if (other.columns != null)
                return false;
        } else if (!columns.equals(other.columns))
            return false;
        if (forFlexigrid != other.forFlexigrid)
            return false;
        if (mapped != other.mapped)
            return false;
        if (partialUrl == null)
        {
            if (other.partialUrl != null)
                return false;
        } else if (!partialUrl.equals(other.partialUrl))
            return false;
        if (showCount != other.showCount)
            return false;
        if (userName == null)
        {
            if (other.userName != null)
                return false;
        } else if (!userName.equals(other.userName))
            return false;
        if (version == null)
        {
            if (other.version != null)
                return false;
        } else if (!version.equals(other.version))
            return false;
        return true;
    }

    public String toKey()
    {
        return Objects.toStringHelper(this)
                .addValue(partialUrl)
                .addValue(columns)
                .addValue(showCount)
                .addValue(userName)
                .addValue(forFlexigrid)
                .addValue(appId)
                .addValue(mapped)
                .addValue(version)
                .toString();
    }
}