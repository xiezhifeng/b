package com.atlassian.confluence.extra.jira.cache;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;

import com.atlassian.vcache.DirectExternalCache;
import com.atlassian.vcache.ExternalCacheSettings;
import com.atlassian.vcache.JvmCache;
import com.atlassian.vcache.JvmCacheSettings;
import com.atlassian.vcache.Marshaller;
import com.atlassian.vcache.PutPolicy;
import com.atlassian.vcache.VCacheFactory;

import static java.util.Optional.ofNullable;
import static org.mockito.Mockito.*;

/**
 * Test helper provides functionality to help create appropriate vcache mocks for unit tests
 */
public class VCacheTestHelper
{
    public static VCacheFactory mockVCacheFactory()
    {
        return mock(VCacheFactory.class);
    }

    public static <T> DirectExternalCache<T> getExternalCacheOnCall(VCacheFactory mockVcacheFactory, DirectExternalCache<T> desiredCache)
    {
        when(mockVcacheFactory.<T>getDirectExternalCache(anyString(), any(Marshaller.class),
                any(ExternalCacheSettings.class)))
            .thenReturn(desiredCache);

        return desiredCache;
    }

    public static <U, V> JvmCache<U, V> getJvmCacheOnCall(VCacheFactory mockVcacheFactory, JvmCache<U, V> desiredCache)
    {
        when(mockVcacheFactory.<U, V>getJvmCache(anyString(),
                any(JvmCacheSettings.class)))
                .thenReturn(desiredCache);

        return desiredCache;
    }

    public static <U, V> JvmCache<U, V> mockJvmCache(final Map<U, V> cacheMap)
    {
        cacheMap.clear();
        JvmCache<U, V> cache = mock(JvmCache.class);

        when(cache.get(anyObject(), any(Supplier.class)))
                .thenAnswer(invocation -> {
                    if (cacheMap.containsKey(invocation.getArguments()[0]))
                    {
                        return completion(cacheMap.get(invocation.getArguments()[0]));
                    }
                    else
                    {
                        Object result = ((Supplier) invocation.getArguments()[1]).get();
                        cacheMap.put((U)invocation.getArguments()[0], (V)result);
                        return completion(result);
                    }
                });

        when(cache.get(anyObject())).thenAnswer(invocation -> ofNullable(cacheMap.get(invocation.getArguments()[0])));

        doAnswer(invocation -> cacheMap.put((U) invocation.getArguments()[0], (V)invocation.getArguments()[1]))
                .when(cache).put(anyObject(), anyObject());

        return cache;
    }

    public static  <T> DirectExternalCache<T> mockExternalCache(final Map<String, T> cacheMap)
    {
        cacheMap.clear();
        DirectExternalCache<T> cache = mock(DirectExternalCache.class);

        when(cache.get(anyString(), any(Supplier.class)))
                .thenAnswer(invocation -> {
                    if (cacheMap.containsKey(invocation.getArguments()[0]))
                    {
                        return completion(cacheMap.get(invocation.getArguments()[0]));
                    }
                    else
                    {
                        Object result = ((Supplier) invocation.getArguments()[1]).get();
                        cacheMap.put((String)invocation.getArguments()[0], (T)result);
                        return completion(result);
                    }
                });

        when(cache.get(anyString())).thenAnswer(invocation -> completion(ofNullable(cacheMap.get(invocation.getArguments()[0]))));

        when(cache.put(anyString(), anyObject(), any(PutPolicy.class))).thenAnswer(invocation -> {
            cacheMap.put((String)invocation.getArguments()[0], (T)invocation.getArguments()[1]);
            return completion(true);
        });

        when(cache.remove(any(String[].class))).thenAnswer(invocation -> {
            cacheMap.remove(invocation.getArguments()[0]);
            return completion(null);
        });

        return cache;
    }

    private static <T> CompletionStage<T> completion(T val)
    {
        CompletableFuture<T> future = new CompletableFuture<>();
        future.complete(val);
        return future;
    }
}
