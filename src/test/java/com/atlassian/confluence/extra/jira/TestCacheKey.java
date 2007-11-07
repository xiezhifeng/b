package com.atlassian.confluence.extra.jira;

import junit.framework.TestCase;

/**
 * Since the CacheKey is used as a key in a map, it's equals and hashcode methods
 * should work propely.
 */
public class TestCacheKey extends TestCase
{
    public void testCacheKeyNotEquals() {
        CacheKey key1 = new CacheKey("http://www.google.com/","test",false,null);
        CacheKey key2 = new CacheKey("http://www.microsoft.com/","test2",true,"template");
        assertTrue(!key1.equals(key2));
        assertTrue(key1.hashCode()!=key2.hashCode());
    }

    public void testCacheKeyEquals() {
        CacheKey key1 = new CacheKey("http://www.google.com/","test",false,"test");
        CacheKey key2 = new CacheKey("http://www.google.com/","test",false,"test");
        assertTrue(key1.equals(key2));
        assertTrue(key1.hashCode()==key2.hashCode());
    }
}
