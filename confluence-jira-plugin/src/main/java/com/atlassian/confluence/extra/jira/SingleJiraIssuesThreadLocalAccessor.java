package com.atlassian.confluence.extra.jira;

import com.google.common.collect.Maps;
import org.jdom.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class SingleJiraIssuesThreadLocalAccessor {
    private static final ThreadLocal<Map<String, Element>> mapThreadLocal = new ThreadLocal<Map<String, Element>>();
    private static final ThreadLocal<Map<String, String>> serverDisplayUrlMap = new ThreadLocal<Map<String, String>>();

    private static final Logger log = LoggerFactory.getLogger(SingleJiraIssuesThreadLocalAccessor.class);

    public static Map<String, Element> get() {
        return mapThreadLocal.get();
    }

    public static void putElement(String key, Element value)
    {
        Map<String, Element> internalMap = mapThreadLocal.get();
        if (internalMap == null)
        {
            log.debug("SingleJiraIssuessMapThreadLocal is not initialised. Could not insert ({}, {})", key, value);
            return;
        }

        internalMap.put(key, value);
    }

    public static void putAllElements(Map<String, Element> map)
    {
        Map<String, Element> internalMap = mapThreadLocal.get();
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
    public static Element getElement(String key)
    {
        Map<String, Element> internalMap = mapThreadLocal.get();
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
        if (mapThreadLocal.get() == null)
        {
            mapThreadLocal.set(Maps.<String, Element>newHashMap());
        }

        if (serverDisplayUrlMap.get() == null)
        {
            serverDisplayUrlMap.set(Maps.<String, String>newHashMap());
        }
    }

    /**
     * Clean up the mapThreadLocal for the current thread.
     */
    public static void dispose()
    {
        mapThreadLocal.remove();
        serverDisplayUrlMap.remove();
    }

    /**
     * Flush the contents of the mapThreadLocal, but do not clean up the mapThreadLocal itself.
     */
    public static void flush()
    {
        Map<String, Element> internalMap = mapThreadLocal.get();
        if (internalMap != null)
        {
            internalMap.clear();
        }

        Map<String, String> internalMap2 = serverDisplayUrlMap.get();
        if (internalMap2 != null)
        {
            internalMap2.clear();
        }
    }

    public static void putJiraServerUrl(String serverId, String jiraServerUrl) {
        Map<String, String> internalMap = serverDisplayUrlMap.get();
        if (internalMap == null)
        {
            log.debug("SingleJiraIssuessMapThreadLocal is not initialised. Could not insert ({}, {})", serverId, jiraServerUrl);
            return;
        }

        internalMap.put(serverId, jiraServerUrl);
    }

    public static String getJiraServerUrl(String serverId)
    {
        Map<String, String> internalMap = serverDisplayUrlMap.get();
        if (internalMap == null)
        {
            log.debug("SingleJiraIssuessMapThreadLocal is not initialised. Could not retrieve value for key {}", serverId);
            return null;
        }

        return internalMap.get(serverId);
    }
}
