package com.atlassian.confluence.extra.jira;

import junit.framework.TestCase;

import java.io.IOException;
import java.util.*;

/**
 */
public class TestJiraIssuesCacheCompression extends TestCase
{
    List columns;
    Map cache = new HashMap();

    public TestJiraIssuesCacheCompression()
    {
        columns = new ArrayList();
        columns.add("columns");
        columns.add("another");
    }

    public void testJiraIssuesMacroCompression() throws IOException
    {
        CacheKey key = new CacheKey("http://localhost", columns, false, false);
        
        CompressingStringCache compressingStringCache = new CompressingStringCache(cache);
        String content = "this is a test";
        compressingStringCache.put(key,  content);
        assertEquals(content, compressingStringCache.get(key));
    }

    public void testJiraIssuesMacroBigCompression() throws IOException
    {
        CompressingStringCache compressingStringCache = new CompressingStringCache(cache);
        CacheKey key = new CacheKey("http://localhost", columns, false, false);
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
        CacheKey key = new CacheKey("http://localhost", columns, false, false);

        CompressingStringCache compressingStringCache = new CompressingStringCache(cache);
        
        String content = "this is a test with a multibyte character: \u0370";
        compressingStringCache.put(key, content);
        assertEquals(content, compressingStringCache.get(key));
    }
}
