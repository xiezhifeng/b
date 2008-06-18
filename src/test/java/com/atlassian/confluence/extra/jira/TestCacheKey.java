package com.atlassian.confluence.extra.jira;

import junit.framework.TestCase;

import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.user.impl.DefaultUser;

import java.util.Set;
import java.util.LinkedHashSet;

/**
 * Since the CacheKey is used as a key in a map, it's equals and hashcode methods
 * should work propely.
 */
public class TestCacheKey extends TestCase
{
    Set columns;

    public TestCacheKey()
    {
        columns = new LinkedHashSet();
        columns.add("test");
    }

    public void testCacheKeyNotEquals() {
        Set columns2 = new LinkedHashSet();
        columns2.add("test2");
        CacheKey key1 = new CacheKey("http://www.google.com/",columns,false,null,false);
        CacheKey key2 = new CacheKey("http://www.microsoft.com/",columns2,true,"template", false);
        assertTrue(!key1.equals(key2));
        assertTrue(key1.hashCode()!=key2.hashCode());
    }

    public void testCacheKeyEquals() {
        CacheKey key1 = new CacheKey("http://www.google.com/",columns,false,"test", false);
        CacheKey key2 = new CacheKey("http://www.google.com/",columns,false,"test", false);
        assertTrue(key1.equals(key2));
        assertTrue(key1.hashCode()==key2.hashCode());
    }

    public void testKeyWithTrustedConnection() {
        DefaultUser bob = new DefaultUser("bob");
        AuthenticatedUserThreadLocal.setUser(bob);
        CacheKey key1 = new CacheKey("http://www.google.com/",columns,false,"test", true);
        CacheKey key2 = new CacheKey("http://www.google.com/",columns,false,"test", true);
        assertTrue(key1.equals(key2));
        assertTrue(key1.hashCode()==key2.hashCode());

        AuthenticatedUserThreadLocal.setUser(bob);
        key1 = new CacheKey("http://www.google.com/",columns,false,"test", true);
        DefaultUser sam = new DefaultUser("sam");
        AuthenticatedUserThreadLocal.setUser(sam);
        key2 = new CacheKey("http://www.google.com/",columns,false,"test", true);
        assertFalse(key1.equals(key2));
        assertFalse(key1.hashCode()==key2.hashCode());
    }

    
}
