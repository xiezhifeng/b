package com.atlassian.confluence.extra.jira.cache;

import com.atlassian.confluence.extra.jira.JiraIssuesMacro;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Cache that compresses and uncompresses values using GZip. Compression is transparent to clients.
 *
 */
public class CompressingStringCache implements SimpleStringCache
{
    private static final Logger log = Logger.getLogger(JiraIssuesMacro.class);
    private final ConcurrentHashMap wrappedCache;

    public CompressingStringCache(ConcurrentHashMap wrappedCache)
    {
        this.wrappedCache = wrappedCache;
    }

    public void put(Object key, String value)
    {
        byte[] stringBytes;
        try
        {
            stringBytes = value.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e)
        {
            // This should be impossible as all VMs must support UTF-8
            throw new RuntimeException("UTF-8 not supported");
        }

        if (log.isDebugEnabled())
        {
            log.debug("compressing [ " + stringBytes.length + " ] bytes for storage in the cache");
        }
        long start = System.currentTimeMillis();
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        GZIPOutputStream out = null;
        try
        {
            out = new GZIPOutputStream(buf);
            out.write(stringBytes, 0, stringBytes.length);
            out.finish();
            out.flush();
            out.close();
        }
        catch (IOException ex)
        {
            throw new RuntimeException("Exception while compressing cache content", ex);
        }
        finally
        {
            IOUtils.closeQuietly(out);
        }

        byte[] data = buf.toByteArray();
        if (log.isDebugEnabled())
        {
            log.debug((System.currentTimeMillis() - start) + ": compressed to [ " + data.length + " ]");
        }
        wrappedCache.put(key, data);
    }

    public String get(Object key)
    {
        GZIPInputStream in = null;
        try
        {
            byte[] data = (byte[]) wrappedCache.get(key);
            if (data == null)
            {
                return null;
            }

            if (log.isDebugEnabled())
            {
                log.debug("decompressing [ " + data.length + " ] bytes into html");
            }

            long start = System.currentTimeMillis();
            ByteArrayInputStream bin = new ByteArrayInputStream(data);
            in = new GZIPInputStream(bin);
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            IOUtils.copy(in, buf);
            byte[] uncompressedData = buf.toByteArray();

            if (log.isDebugEnabled())
            {
                log.debug((System.currentTimeMillis() - start) + ": decompressed to [ " + uncompressedData.length + " ]");
            }
            return new String(uncompressedData, "UTF-8");
        }
        catch (IOException e)
        {
            log.debug("Exception while uncompressing cache data", e);
            // if for any reason the cached data can not be decompressed
            // return null so the application thinks its not cached and
            // continues.  this might happen if the plugin is upgraded
            // from a version that doesn't cache to this version.
            return null;
        }
        finally
        {
            IOUtils.closeQuietly(in);
        }

    }

    public void remove(Object key)
    {
        wrappedCache.remove(key);
    }
}
