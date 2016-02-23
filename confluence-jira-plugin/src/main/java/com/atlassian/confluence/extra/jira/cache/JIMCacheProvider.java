package com.atlassian.confluence.extra.jira.cache;

import com.atlassian.annotations.Internal;
import com.atlassian.confluence.extra.jira.JiraResponseHandler;
import com.atlassian.vcache.*;
import com.atlassian.vcache.marshallers.MarshallerFactory;

import java.time.Duration;

import javax.annotation.concurrent.ThreadSafe;

/**
 * Cache provider class  provides helper methods to get different caches in a thread-safe way (to avoid race
 * conditions on cache creation).
 */
@Internal
@ThreadSafe
public class JIMCacheProvider
{
    private static final String JIM_CACHE_NAME = "com.atlassian.confluence.extra.jira.JiraIssuesMacro";
    private static final String JIM_INSTANCE_CACHE_NAME = JIM_CACHE_NAME + ".instance";

    /**
     * Creates new or returns an existent cache
     *
     * @param vcacheFactory {@link com.atlassian.vcache.VCacheFactory}
     * @return new <code>DirectExternalCache</code> or existent one (if was already created)
     */
    public static DirectExternalCache<CompressingStringCache> getResponseCache(VCacheFactory vcacheFactory)
    {
        return vcacheFactory.getDirectExternalCache(JIM_CACHE_NAME,
                MarshallerFactory.serializableMarshaller(CompressingStringCache.class),
                new ExternalCacheSettingsBuilder().build());
    }

    /**
     * Creates new or returns an existent cache
     *
     * @param vcacheFactory {@link com.atlassian.vcache.VCacheFactory}
     * @return new <code>JvmCache</code> or existent one (if was already created)
     */
    public static JvmCache<CacheKey, JiraResponseHandler> getResponseHandlersCache(VCacheFactory vcacheFactory)
    {
        return vcacheFactory.getJvmCache(JIM_INSTANCE_CACHE_NAME,
                new JvmCacheSettingsBuilder().defaultTtl(Duration.ofMinutes(5)).build());
    }
}
