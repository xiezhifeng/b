package com.atlassian.confluence.extra.jira.cache;

import java.io.Serializable;

/**
 * Simple map of keys to string values
*/
public interface SimpleStringCache extends Serializable
{
    /**
     * Place a string in the map with the specified key
     * @param key Key to put
     * @param value Value to put
     */
    void put(Object key, String value);

    /**
     * Get the value of the key from the map
     * @param key Key to get
     * @return Value or null if no value exists for specified key
     */
    String get(Object key);

    /**
     * Remove a value from the map
     * @param key Key to remove
     */
    void remove(Object key);
}
