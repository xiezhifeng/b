package com.atlassian.confluence.extra.jira.cache;

import junit.framework.TestCase;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 */
public class TestJiraIssuesCacheCompression extends TestCase
{
    private static final String PLUGIN_VERSION = "6.0.0";

    List columns;
    ConcurrentHashMap cache = new ConcurrentHashMap();
    String appId = "jira";

    public TestJiraIssuesCacheCompression()
    {
        columns = new ArrayList();
        columns.add("columns");
        columns.add("another");
    }

    public void testJiraIssuesMacroCompression() throws IOException
    {
        CacheKey key = new CacheKey("http://localhost", appId, columns, false, false, false, true, PLUGIN_VERSION);
        
        CompressingStringCache compressingStringCache = new CompressingStringCache(cache);
        String content = "this is a test";
        compressingStringCache.put(key,  content);
        assertEquals(content, compressingStringCache.get(key));
    }

    public void testJiraIssuesMacroBigCompression() throws IOException
    {
        CompressingStringCache compressingStringCache = new CompressingStringCache(cache);
        CacheKey key = new CacheKey("http://localhost", appId, columns, false, false, false, true, PLUGIN_VERSION);
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
        CacheKey key = new CacheKey("http://localhost", appId, columns, false, false, false, true, PLUGIN_VERSION);

        CompressingStringCache compressingStringCache = new CompressingStringCache(cache);
        
        String content = "this is a test with a multibyte character: \u0370";
        compressingStringCache.put(key, content);
        assertEquals(content, compressingStringCache.get(key));
    }

    /**
     * Making sure that CompressingStringCache can be serialized and deserialized and then used. This will happen
     * with clustered Confluence.
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public void testSerialization() throws IOException, ClassNotFoundException
    {
        CompressingStringCache compressingStringCache = new CompressingStringCache(cache);
        compressingStringCache.put("testkey","testvalue");

        //serialize the compressingStringCache
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
        objectOutputStream.writeObject(compressingStringCache);
        objectOutputStream.close();

        //deserialize the compressingStringCache
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());
        ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);
        CompressingStringCache deserializedCompressingStringCache = (CompressingStringCache) objectInputStream.readObject();
        objectInputStream.close();

        // make sure it still works
        deserializedCompressingStringCache.put("testkey2","testvalue2");
        assertEquals("testvalue",deserializedCompressingStringCache.get("testkey"));
    }
}
