package com.atlassian.confluence.plugins.jira;

import com.google.common.collect.HashMultimap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

public class SingleJiraIssuesMapThreadLocal {
    private static final ThreadLocal<HashMultimap<String, String>> mapThreadLocal = new ThreadLocal<HashMultimap<String, String>>();
    private static final Logger log = LoggerFactory.getLogger(SingleJiraIssuesMapThreadLocal.class);

    public static void put(String key, String value)
    {
        HashMultimap<String, String> cacheMap = mapThreadLocal.get();
        if (cacheMap == null)
        {
            log.debug("SingleJiraIssuessMapThreadLocal is not initialised. Could not insert ({}, {})", key, value);
            return;
        }

        cacheMap.put(key, value);
    }

    /**
     * Retrieve an object from the mapThreadLocal
     *
     * @param key the mapThreadLocal key
     * @return the appropriate cached value, or null if no value could be found, or the mapThreadLocal is not initialised
     */
    public static Set<String> get(String key)
    {
        HashMultimap<String, String> cacheMap = mapThreadLocal.get();
        if (cacheMap == null)
        {
            log.debug("SingleJiraIssuessMapThreadLocal is not initialised. Could not retrieve value for key {}", key);
            return null;
        }

        return cacheMap.get(key);
    }

    /**
     * Initialise the mapThreadLocal for the current thread
     */
    public static void init()
    {
        if (mapThreadLocal.get() != null)
        {
            log.warn("SingleJiraIssuessMapThreadLocal is already initialised. Ignoring reinitialisation attempt.");
            return;
        }

        mapThreadLocal.set(HashMultimap.<String, String>create());
    }

    /**
     * Clean up the mapThreadLocal for the current thread.
     */
    public static void dispose()
    {
        mapThreadLocal.remove();
    }

    /**
     * Flush the contents of the mapThreadLocal, but do not clean up the mapThreadLocal itself.
     */
    public static void flush()
    {
        HashMultimap<String, String> cacheMap = mapThreadLocal.get();
        if (cacheMap == null)
        {
            log.debug("SingleJiraIssuessMapThreadLocal is not initialised. Ignoring attempt to flush it.");
            return;
        }

        cacheMap.clear();
    }
}
