package com.atlassian.confluence.extra.jira;

import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class SingleJiraIssuesMapThreadLocal {
    private static final ThreadLocal<Map<String, String>> mapThreadLocal = new ThreadLocal<Map<String, String>>();
    private static final Logger log = LoggerFactory.getLogger(SingleJiraIssuesMapThreadLocal.class);

    public static void put(String key, String value)
    {
        Map<String, String> internalMap = mapThreadLocal.get();
        if (internalMap == null)
        {
            log.debug("SingleJiraIssuessMapThreadLocal is not initialised. Could not insert ({}, {})", key, value);
            return;
        }

        internalMap.put(key, value);
    }

    public static void putAll(Map<String, String> map)
    {
        Map<String, String> internalMap = mapThreadLocal.get();
        if (internalMap == null)
        {
            log.debug("SingleJiraIssuessMapThreadLocal is not initialised. Could not insert {}", map);
            return;
        }
        internalMap.putAll(map);
    }

    /**
     * Retrieve an object from the mapThreadLocal
     *
     * @param key the mapThreadLocal key
     * @return the appropriate cached value, or null if no value could be found, or the mapThreadLocal is not initialised
     */
    public static String get(String key)
    {
        Map<String, String> internalMap = mapThreadLocal.get();
        if (internalMap == null)
        {
            log.debug("SingleJiraIssuessMapThreadLocal is not initialised. Could not retrieve value for key {}", key);
            return null;
        }

        return internalMap.get(key);
    }

    /**
     * Initialise the mapThreadLocal for the current thread
     */
    public static void init()
    {
        if (mapThreadLocal.get() != null)
        {
            //log.warn("SingleJiraIssuessMapThreadLocal is already initialised. Ignoring reinitialisation attempt.");
            return;
        }

        mapThreadLocal.set(Maps.<String, String>newHashMap());
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
        Map<String, String> internalMap = mapThreadLocal.get();
        if (internalMap == null)
        {
            log.debug("SingleJiraIssuessMapThreadLocal is not initialised. Ignoring attempt to flush it.");
            return;
        }

        internalMap.clear();
    }
}
