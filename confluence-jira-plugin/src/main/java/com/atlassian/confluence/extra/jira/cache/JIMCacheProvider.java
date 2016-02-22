package com.atlassian.confluence.extra.jira.cache;

import com.atlassian.annotations.Internal;
import com.atlassian.confluence.extra.jira.JiraResponseHandler;
import com.atlassian.vcache.*;
import com.atlassian.vcache.marshallers.MarshallerFactory;

import java.time.Duration;

/**
 *
 */
@Internal
public class JIMCacheProvider
{
    private static final String JIM_CACHE_NAME = "com.atlassian.confluence.extra.jira.JiraIssuesMacro";
    private static final String JIM_INSTANCE_CACHE_NAME = JIM_CACHE_NAME + ".instance";

    public static synchronized DirectExternalCache<CompressingStringCache> getCache(VCacheFactory vcacheFactory)
    {
        return vcacheFactory.getDirectExternalCache(JIM_CACHE_NAME,
                MarshallerFactory.serializableMarshaller(CompressingStringCache.class),
                new ExternalCacheSettingsBuilder().build());
    }

    public static synchronized JvmCache<CacheKey, JiraResponseHandler> getInstanceCache(VCacheFactory vcacheFactory)
    {
        return vcacheFactory.getJvmCache(JIM_INSTANCE_CACHE_NAME,
                new JvmCacheSettingsBuilder().defaultTtl(Duration.ofMinutes(5)).build());
    }
}
