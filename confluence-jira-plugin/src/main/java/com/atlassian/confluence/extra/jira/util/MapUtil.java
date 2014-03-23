package com.atlassian.confluence.extra.jira.util;

import com.google.common.collect.Maps;

import java.util.Map;

/**
 * Utility class for manipulating maps
 */
public class MapUtil {
    public static <K, V> Map<K, V> copyOf(Map<K, V> map)
    {
        if (map == null)
            return Maps.newHashMap();
        return Maps.newHashMap(map);
    }
}
