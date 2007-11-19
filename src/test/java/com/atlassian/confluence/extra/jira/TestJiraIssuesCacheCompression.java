package com.atlassian.confluence.extra.jira;

import com.atlassian.confluence.cache.MemoryCache;
import com.atlassian.user.impl.cache.Cache;
import junit.framework.TestCase;

import java.io.IOException;

/**
 */
public class TestJiraIssuesCacheCompression extends TestCase
{
    public void testJiraIssuesMacroCompression() throws IOException
    {
        Cache cache = new MemoryCache();
        CacheKey key = new CacheKey("http://localhost", "columns", false, "");
        
        CompressingStringCache compressingStringCache = new CompressingStringCache(cache);
        String content = "this is a test";
        compressingStringCache.put(key,  content);
        assertEquals(content, compressingStringCache.get(key));
    }

    public void testJiraIssuesMacroBigCompression() throws IOException
    {
        Cache cache = new MemoryCache();
        CompressingStringCache compressingStringCache = new CompressingStringCache(cache);
        CacheKey key = new CacheKey("http://localhost", "columns", false, "");
        StringBuffer buf = new StringBuffer();
        String str = "this is a test";
        buf.append(str);
        for(int i=0;i<100;i++) {
            buf.append(str);
        }

        compressingStringCache.put(key, buf.toString());
        assertEquals(buf.toString(), compressingStringCache.get(key));
    }

    public void testJiraIssuesMacroMultibyteEncoding() throws IOException
    {
        Cache cache = new MemoryCache();
        CacheKey key = new CacheKey("http://localhost", "columns", false, "");

        CompressingStringCache compressingStringCache = new CompressingStringCache(cache);
        
        String content = "this is a test with a multibyte character: \u0370";
        compressingStringCache.put(key, content);
        assertEquals(content, compressingStringCache.get(key));
    }
}
