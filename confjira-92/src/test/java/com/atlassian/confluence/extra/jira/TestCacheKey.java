package com.atlassian.confluence.extra.jira;

import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.user.impl.DefaultUser;
import junit.framework.TestCase;

import java.util.ArrayList;
import java.util.List;

/**
 * Since the CacheKey is used as a key in a map, it's equals and hashcode methods
 * should work propely.
 */
public class TestCacheKey extends TestCase
{
    List columns;

    public TestCacheKey()
    {
        columns = new ArrayList();
        columns.add("test");
    }

    public void testCacheKeyNotEquals() {
        List columns2 = new ArrayList();
        columns2.add("test2");
        CacheKey key1 = new CacheKey("http://www.google.com/",columns,false,false);
        CacheKey key2 = new CacheKey("http://www.microsoft.com/",columns2,true,false);
        assertTrue(!key1.equals(key2));
        assertTrue(key1.hashCode()!=key2.hashCode());
    }

    public void testCacheKeyEquals() {
        CacheKey key1 = new CacheKey("http://www.google.com/",columns,false,false);
        CacheKey key2 = new CacheKey("http://www.google.com/",columns,false,false);
        assertTrue(key1.equals(key2));
        assertTrue(key1.hashCode()==key2.hashCode());
    }

    public void testKeyWithTrustedConnection() {
        DefaultUser bob = new DefaultUser("bob");
        AuthenticatedUserThreadLocal.setUser(bob);
        CacheKey key1 = new CacheKey("http://www.google.com/",columns,false,true);
        CacheKey key2 = new CacheKey("http://www.google.com/",columns,false,true);
        assertTrue(key1.equals(key2));
        assertTrue(key1.hashCode()==key2.hashCode());

        AuthenticatedUserThreadLocal.setUser(bob);
        key1 = new CacheKey("http://www.google.com/",columns,false,true);
        DefaultUser sam = new DefaultUser("sam");
        AuthenticatedUserThreadLocal.setUser(sam);
        key2 = new CacheKey("http://www.google.com/",columns,false,true);
        assertFalse(key1.equals(key2));
        assertFalse(key1.hashCode()==key2.hashCode());
    }

    
}
