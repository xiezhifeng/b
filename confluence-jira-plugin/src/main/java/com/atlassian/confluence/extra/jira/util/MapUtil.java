package com.atlassian.confluence.extra.jira.util;

import com.google.common.collect.Maps;

import java.util.Map;

/**
 * Utility class for manipulating maps
 */
public class MapUtil
{

    /**
     * Copy a map from an existing one or create a new map
     * @param map the map to be copied
     * @param <K> type of the key
     * @param <V> type of the value
     * @return the copied map or a new map
     */
    public static <K, V> Map<K, V> copyOf(Map<K, V> map)
    {
        if (map == null)
            return Maps.newHashMap();
        return Maps.newHashMap(map);
    }
}
