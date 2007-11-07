package com.atlassian.confluence.extra.jira;

import com.atlassian.confluence.cache.MemoryCache;
import com.atlassian.user.impl.cache.Cache;
import junit.framework.TestCase;

import java.io.IOException;

/**
 */
public class TestJiraIssuesMacro extends TestCase
{
    public void testJiraIssuesMacroCompression() throws IOException
    {
        JiraIssuesMacro macro = new JiraIssuesMacro();
        Cache cache = new MemoryCache();
        CacheKey key = new CacheKey("http://localhost", "columns", false, "");
        String content = "this is a test";
        macro.putToCache(key, cache, content);
        assertEquals(content,macro.getFromCache(key, cache));
    }

    public void testJiraIssuesMacroBigCompression() throws IOException
    {
        JiraIssuesMacro macro = new JiraIssuesMacro();
        Cache cache = new MemoryCache();
        CacheKey key = new CacheKey("http://localhost", "columns", false, "");
        StringBuffer buf = new StringBuffer();
        String str = "this is a test";
        buf.append(str);
        for(int i=0;i<100;i++) {
            buf.append(str);
        }

        macro.putToCache(key, cache, buf.toString());
        assertEquals(buf.toString(),macro.getFromCache(key, cache));
    }
}
