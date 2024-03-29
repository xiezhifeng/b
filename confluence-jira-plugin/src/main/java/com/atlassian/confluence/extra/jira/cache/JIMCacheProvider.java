package com.atlassian.confluence.extra.jira.cache;

import com.atlassian.annotations.Internal;
import com.atlassian.confluence.extra.jira.JiraChannelResponseHandler;
import com.atlassian.confluence.extra.jira.JiraStringResponseHandler;
import com.atlassian.marshalling.jdk.JavaSerializationMarshalling;
import com.atlassian.vcache.DirectExternalCache;
import com.atlassian.vcache.ExternalCacheSettingsBuilder;
import com.atlassian.vcache.VCacheFactory;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.ThreadSafe;
import java.time.Duration;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * Cache provider class  provides helper methods to get different caches in a thread-safe way (to avoid race
 * conditions on cache creation).
 */
@Internal
@ThreadSafe
public class JIMCacheProvider
{
    public static final String JIM_CACHE_NAME = "com.atlassian.confluence.extra.jira.JiraIssuesMacro";
    private static final String JIM_CHANNEL_RESPONSE_CACHE_NAME = JIM_CACHE_NAME + ".channel";
    private static final String JIM_STRING_RESPONSE_CACHE_NAME = JIM_CACHE_NAME + ".string";
    private static final Integer DEFAULT_JIM_CACHE_TIMEOUT =
            Integer.parseInt(System.getProperty("confluence.jim.cache.time", "5"));

    /**
     * Creates new or returns an existent cache
     *
     * @param vcacheFactory {@link com.atlassian.vcache.VCacheFactory}
     * @return new <code>DirectExternalCache</code> or existent one (if was already created)
     */
    public static DirectExternalCache<CompressingStringCache> getResponseCache(@Nonnull VCacheFactory vcacheFactory)
    {
        return requireNonNull(vcacheFactory).getDirectExternalCache(JIM_CACHE_NAME,
                JavaSerializationMarshalling.pair(CompressingStringCache.class),
                new ExternalCacheSettingsBuilder().build());
    }

    /**
     * Creates new or returns an existent cache
     *
     * @param vcacheFactory {@link com.atlassian.vcache.VCacheFactory}
     * @param cacheTimeoutInMinutes cache timeout want to set
     * @return new <code>DirectExternalCache</code> or existent one (if was already created)
     */
    public static DirectExternalCache<JiraChannelResponseHandler> getChannelResponseHandlersCache(@Nonnull VCacheFactory
            vcacheFactory, @Nonnull Optional<Integer> cacheTimeoutInMinutes)
    {
        Integer finalCacheTimeOutInMinutes;

        if (cacheTimeoutInMinutes.isPresent())
        {
            finalCacheTimeOutInMinutes = cacheTimeoutInMinutes.get();
        }
        else
        {
            finalCacheTimeOutInMinutes = DEFAULT_JIM_CACHE_TIMEOUT;
        }

        if (finalCacheTimeOutInMinutes <= 0)
        {
            return requireNonNull(vcacheFactory).getDirectExternalCache(JIM_CHANNEL_RESPONSE_CACHE_NAME,
                    JavaSerializationMarshalling.pair(JiraChannelResponseHandler.class),
                    new ExternalCacheSettingsBuilder().defaultTtl(Duration.ofSeconds(1)).build());
        }

        return requireNonNull(vcacheFactory).getDirectExternalCache(JIM_CHANNEL_RESPONSE_CACHE_NAME,
                JavaSerializationMarshalling.pair(JiraChannelResponseHandler.class),
                new ExternalCacheSettingsBuilder().defaultTtl(Duration.ofMinutes(finalCacheTimeOutInMinutes)).build());
    }

    /**
     * Creates new or returns an existent cache
     *
     * @param vcacheFactory {@link com.atlassian.vcache.VCacheFactory}
     * @param cacheTimeoutInMinutes cache timeout want to set
     * @return new <code>DirectExternalCache</code> or existent one (if was already created)
     */
    public static DirectExternalCache<JiraStringResponseHandler> getStringResponseHandlersCache(@Nonnull VCacheFactory
            vcacheFactory, @Nonnull Optional<Integer> cacheTimeoutInMinutes)
    {
        Integer finalCacheTimeOutInMinutes;

        if (cacheTimeoutInMinutes.isPresent())
        {
            finalCacheTimeOutInMinutes = cacheTimeoutInMinutes.get();
        }
        else
        {
            finalCacheTimeOutInMinutes = DEFAULT_JIM_CACHE_TIMEOUT;
        }

        if (finalCacheTimeOutInMinutes <= 0)
        {
            return requireNonNull(vcacheFactory).getDirectExternalCache(JIM_STRING_RESPONSE_CACHE_NAME,
                    JavaSerializationMarshalling.pair(JiraStringResponseHandler.class),
                    new ExternalCacheSettingsBuilder().defaultTtl(Duration.ofSeconds(1)).build());
        }

        return requireNonNull(vcacheFactory).getDirectExternalCache(JIM_STRING_RESPONSE_CACHE_NAME,
                JavaSerializationMarshalling.pair(JiraStringResponseHandler.class),
                new ExternalCacheSettingsBuilder().defaultTtl(Duration.ofMinutes(finalCacheTimeOutInMinutes)).build());
    }
}
