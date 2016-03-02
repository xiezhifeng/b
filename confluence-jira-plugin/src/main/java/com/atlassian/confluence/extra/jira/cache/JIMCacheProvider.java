package com.atlassian.confluence.extra.jira.cache;

import com.atlassian.annotations.Internal;
import com.atlassian.confluence.extra.jira.JiraChannelResponseHandler;
import com.atlassian.confluence.extra.jira.JiraStringResponseHandler;
import com.atlassian.vcache.DirectExternalCache;
import com.atlassian.vcache.ExternalCacheSettingsBuilder;
import com.atlassian.vcache.VCacheFactory;
import com.atlassian.vcache.marshallers.MarshallerFactory;

import java.time.Duration;

import javax.annotation.concurrent.ThreadSafe;

import static java.util.Objects.requireNonNull;

/**
 * Cache provider class  provides helper methods to get different caches in a thread-safe way (to avoid race
 * conditions on cache creation).
 */
@Internal
@ThreadSafe
public class JIMCacheProvider
{
    private static final String JIM_CACHE_NAME = "com.atlassian.confluence.extra.jira.JiraIssuesMacro";
    private static final String JIM_CHANNEL_RESPONSE_CACHE_NAME = JIM_CACHE_NAME + ".channel";
    private static final String JIM_STRING_RESPONSE_CACHE_NAME = JIM_CACHE_NAME + ".string";

    /**
     * Creates new or returns an existent cache
     *
     * @param vcacheFactory {@link com.atlassian.vcache.VCacheFactory}
     * @return new <code>DirectExternalCache</code> or existent one (if was already created)
     */
    public static DirectExternalCache<CompressingStringCache> getResponseCache(VCacheFactory vcacheFactory)
    {
        return requireNonNull(vcacheFactory).getDirectExternalCache(JIM_CACHE_NAME,
                MarshallerFactory.serializableMarshaller(CompressingStringCache.class),
                new ExternalCacheSettingsBuilder().build());
    }

    /**
     * Creates new or returns an existent cache
     *
     * @param vcacheFactory {@link com.atlassian.vcache.VCacheFactory}
     * @return new <code>DirectExternalCache</code> or existent one (if was already created)
     */
    public static DirectExternalCache<JiraChannelResponseHandler> getChannelResponseHandlersCache(VCacheFactory
            vcacheFactory)
    {
        return requireNonNull(vcacheFactory).getDirectExternalCache(JIM_CHANNEL_RESPONSE_CACHE_NAME,
                MarshallerFactory.serializableMarshaller(JiraChannelResponseHandler.class),
                new ExternalCacheSettingsBuilder().defaultTtl(Duration.ofMinutes(5)).build());
    }

    /**
     * Creates new or returns an existent cache
     *
     * @param vcacheFactory {@link com.atlassian.vcache.VCacheFactory}
     * @return new <code>DirectExternalCache</code> or existent one (if was already created)
     */
    public static DirectExternalCache<JiraStringResponseHandler> getStringResponseHandlersCache(VCacheFactory
            vcacheFactory)
    {
        return requireNonNull(vcacheFactory).getDirectExternalCache(JIM_STRING_RESPONSE_CACHE_NAME,
                MarshallerFactory.serializableMarshaller(JiraStringResponseHandler.class),
                new ExternalCacheSettingsBuilder().defaultTtl(Duration.ofMinutes(5)).build());
    }
}