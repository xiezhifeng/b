package com.atlassian.confluence.extra.jira;

import com.google.common.collect.Maps;
import org.jdom.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class SingleJiraIssuesThreadLocalAccessor {
    // serverElementMapThreadLocal is a map of:
    // key = serverId
    // value = Map of (JIRA Issue Key, JDOM Element) pairs
    private static final ThreadLocal<Map<String, Map<String, Element>>> serverElementMapThreadLocal = new ThreadLocal<Map<String, Map<String, Element>>>();

    // serverUrlMapThreadLocal is a map of:
    // key = serverId
    // value = jiraServerUrl
    private static final ThreadLocal<Map<String, String>> serverUrlMapThreadLocal = new ThreadLocal<Map<String, String>>();

    private static final Logger log = LoggerFactory.getLogger(SingleJiraIssuesThreadLocalAccessor.class);

    public static Map<String, Element> getElementMap(String serverId) {
        Map<String, Map<String, Element>> serverElementMap = serverElementMapThreadLocal.get();
        return serverElementMap == null ? null : serverElementMap.get(serverId);
    }

    public static void putElement(String serverId, String key, Element value)
    {
        Map<String, Map<String, Element>> serverElementMap = serverElementMapThreadLocal.get();
        if (serverElementMap == null)
        {
            log.debug("SingleJiraIssuessMapThreadLocal is not initialised. Could not insert ({}, {})", key, value);
            return;
        }
        Map<String, Element> elementMap = serverElementMap.get(serverId);
        if (elementMap != null)
        {
            elementMap.put(key, value);
        }
    }

    public static void putAllElements(String serverId, Map<String, Element> map)
    {
        Map<String, Map<String, Element>> serverElementMap = serverElementMapThreadLocal.get();
        if (serverElementMap == null)
        {
            log.debug("SingleJiraIssuessMapThreadLocal is not initialised. Could not insert {}", map);
            return;
        }
        serverElementMap.put(serverId, map);
    }

    /**
     * Retrieve an object from the serverElementMapThreadLocal
     *
     * @param key the serverElementMapThreadLocal key
     * @return the appropriate cached value, or null if no value could be found, or the serverElementMapThreadLocal is not initialised
     */
    public static Element getElement(String serverId, String key)
    {
        Map<String, Map<String, Element>> internalMap = serverElementMapThreadLocal.get();
        if (internalMap == null)
        {
            log.debug("SingleJiraIssuessMapThreadLocal is not initialised. Could not retrieve value for key {}", key);
            return null;
        }
        Map<String, Element> elementMap = internalMap.get(serverId);
        return elementMap != null ? elementMap.get(key) : null;
    }

    /**
     * Initialise the serverElementMapThreadLocal for the current thread
     */
    public static void init()
    {
        if (serverElementMapThreadLocal.get() == null)
        {
            serverElementMapThreadLocal.set(Maps.<String, Map<String, Element>>newHashMap());
        }

        if (serverUrlMapThreadLocal.get() == null)
        {
            serverUrlMapThreadLocal.set(Maps.<String, String>newHashMap());
        }
    }

    /**
     * Clean up the serverElementMapThreadLocal for the current thread.
     */
    public static void dispose()
    {
        serverElementMapThreadLocal.remove();
        serverUrlMapThreadLocal.remove();
    }

    /**
     * Flush the contents of the serverElementMapThreadLocal, but do not clean up the serverElementMapThreadLocal itself.
     */
    public static void flush()
    {
        Map<String, Map<String, Element>> serverElementMap = serverElementMapThreadLocal.get();
        if (serverElementMap != null)
        {
            serverElementMap.clear();
        }

        Map<String, String> serverUrlMap = serverUrlMapThreadLocal.get();
        if (serverUrlMap != null)
        {
            serverUrlMap.clear();
        }
    }

    public static void putJiraServerUrl(String serverId, String jiraServerUrl) {
        Map<String, String> serverUrlMap = serverUrlMapThreadLocal.get();
        if (serverUrlMap == null)
        {
            log.debug("SingleJiraIssuessMapThreadLocal is not initialised. Could not insert ({}, {})", serverId, jiraServerUrl);
            return;
        }
        serverUrlMap.put(serverId, jiraServerUrl);
    }

    public static String getJiraServerUrl(String serverId)
    {
        Map<String, String> serverUrlMap = serverUrlMapThreadLocal.get();
        if (serverUrlMap == null)
        {
            log.debug("SingleJiraIssuessMapThreadLocal is not initialised. Could not retrieve value for key {}", serverId);
            return null;
        }
        return serverUrlMap.get(serverId);
    }

    public static Map<String, String> getServerUrlMap() {
        return serverUrlMapThreadLocal.get();
    }
}
