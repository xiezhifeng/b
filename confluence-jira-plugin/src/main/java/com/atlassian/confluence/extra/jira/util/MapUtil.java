package com.atlassian.confluence.extra.jira.util;

import java.util.HashMap;
import java.util.Map;

public class MapUtil {
    public static <K, V> Map<K, V> copyOf(Map<K, V> map)
    {
        if (map == null)
            return new HashMap<K, V>();
        return new HashMap<K, V>(map);
    }
}
