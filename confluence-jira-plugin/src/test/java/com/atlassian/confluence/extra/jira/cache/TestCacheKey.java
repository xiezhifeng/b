package com.atlassian.confluence.extra.jira.cache;

import com.atlassian.confluence.user.AuthenticatedUserThreadLocal;
import com.atlassian.confluence.user.ConfluenceUser;
import com.atlassian.confluence.user.ConfluenceUserImpl;
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
    String appId = "jira";

    public TestCacheKey()
    {
        columns = new ArrayList();
        columns.add("test");
    }

    public void testCacheKeyNotEquals() {
        List columns2 = new ArrayList();
        columns2.add("test2");
        CacheKey key1 = new CacheKey("http://www.google.com/", appId, columns, false,true, false, true);
        CacheKey key2 = new CacheKey("http://www.microsoft.com/",appId, columns2,true,true, false, true);
        assertTrue(!key1.equals(key2));
        assertTrue(key1.hashCode()!=key2.hashCode());
    }

    public void testCacheKeyEquals() {
        CacheKey key1 = new CacheKey("http://www.google.com/",appId, columns,false,true, false, true);
        CacheKey key2 = new CacheKey("http://www.google.com/",appId, columns,false,true, false, true);
        assertTrue(key1.equals(key2));
        assertTrue(key1.hashCode()==key2.hashCode());
    }

    public void testKeyWithTrustedConnection() {
        ConfluenceUser bob = new ConfluenceUserImpl("bob","bob","bob@atlassian.com");
        AuthenticatedUserThreadLocal.set(bob);
        CacheKey key1 = new CacheKey("http://www.google.com/",appId, columns,false,false, false, true);
        CacheKey key2 = new CacheKey("http://www.google.com/",appId, columns,false,false, false, true);
        assertTrue(key1.equals(key2));
        assertTrue(key1.hashCode()==key2.hashCode());

        AuthenticatedUserThreadLocal.set(bob);
        key1 = new CacheKey("http://www.google.com/",appId, columns,false,false, false, true);
        ConfluenceUser sam = new ConfluenceUserImpl("sam","sam","sam@atlassian.com");
        AuthenticatedUserThreadLocal.set(sam);
        key2 = new CacheKey("http://www.google.com/",appId, columns,false,false, false, true);
        assertFalse(key1.equals(key2));
        assertFalse(key1.hashCode()==key2.hashCode());
    }

    
}
