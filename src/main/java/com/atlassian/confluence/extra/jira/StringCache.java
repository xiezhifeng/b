package com.atlassian.confluence.extra.jira;

import com.atlassian.user.impl.cache.Cache;

/**
 * Thin wrapper for caching strings in a standard cache
*/
class StringCache implements SimpleStringCache
{
    private final Cache wrappedCache;

    private StringCache(Cache wrappedCache)
    {
        this.wrappedCache = wrappedCache;
    }

    public void put(Object key, String value)
    {
        wrappedCache.put(key, value);
    }

    public String get(Object key)
    {
        return (String) wrappedCache.get(key);
    }

    public void remove(Object key)
    {
        wrappedCache.remove(key);
    }
}
