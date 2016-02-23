package com.atlassian.confluence.extra.jira.cache;

import com.atlassian.vcache.DirectExternalCache;
import com.atlassian.vcache.ExternalCacheSettings;
import com.atlassian.vcache.JvmCache;
import com.atlassian.vcache.JvmCacheSettings;
import com.atlassian.vcache.Marshaller;
import com.atlassian.vcache.VCacheFactory;

import static com.atlassian.confluence.extra.jira.cache.SimpleVCaches.directExternalCache;
import static com.atlassian.confluence.extra.jira.cache.SimpleVCaches.jvmCache;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * Test helper provides functionality to help create appropriate vcache mocks for unit tests
 */
public class VCacheTestHelper
{
    public static VCacheFactory mockVCacheFactory()
    {
        return mock(VCacheFactory.class);
    }

    public static <T> DirectExternalCache<T> getExternalCacheOnCall(VCacheFactory mockVcacheFactory)
    {
        DirectExternalCache<T> desiredCache = spy(directExternalCache());
        when(mockVcacheFactory.<T>getDirectExternalCache(anyString(), any(Marshaller.class),
                any(ExternalCacheSettings.class)))
            .thenReturn(desiredCache);

        return desiredCache;
    }

    public static <U, V> JvmCache<U, V> getJvmCacheOnCall(VCacheFactory mockVcacheFactory)
    {
        JvmCache<U, V> desiredCache = spy(jvmCache());

        when(mockVcacheFactory.<U, V>getJvmCache(anyString(),
                any(JvmCacheSettings.class)))
                .thenReturn(desiredCache);

        return desiredCache;
    }
}
