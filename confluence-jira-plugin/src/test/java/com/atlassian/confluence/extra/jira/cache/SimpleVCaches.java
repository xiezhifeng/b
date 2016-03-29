package com.atlassian.confluence.extra.jira.cache;

import com.atlassian.vcache.CasIdentifier;
import com.atlassian.vcache.DirectExternalCache;
import com.atlassian.vcache.IdentifiedValue;
import com.atlassian.vcache.JvmCache;
import com.atlassian.vcache.PutPolicy;
import com.atlassian.vcache.TransactionalExternalCache;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.atlassian.vcache.PutPolicy.PUT_ALWAYS;
import static java.util.Objects.requireNonNull;

/**
 * Provides trivial implementations of the various VCache cache types, backed by in-memory data structures.
 * These implementations are incomplete and should be fleshed out as required.
 */
@ParametersAreNonnullByDefault
public class SimpleVCaches {
    public static <V> SimpleDirectExternalCache<V> directExternalCache() {
        return new SimpleDirectExternalCache<>();
    }

    public static <V> TransactionalExternalCache<V> transactionalExternalCache() {
        return new SimpleTransactionalExternalCache<>();
    }

    public static <K, V> JvmCache<K, V> jvmCache() {
        return new SimpleJvmCache<>();
    }

    public static <T> CompletionStage<T> result(@Nullable T value) {
        CompletableFuture<T> result = new CompletableFuture<>();
        result.complete(value);
        return result;
    }

    public static class SimpleDirectExternalCache<V> implements DirectExternalCache<V> {
        final Map<String, V> map = new HashMap<>();

        @Nonnull
        @Override
        public CompletionStage<Optional<IdentifiedValue<V>>> getIdentified(final String key) {
            throw new UnsupportedOperationException();
        }

        @Nonnull
        @Override
        public CompletionStage<Map<String, Optional<IdentifiedValue<V>>>> getBulkIdentified(final Iterable<String> keys) {
            throw new UnsupportedOperationException();
        }

        @Nonnull
        @Override
        public CompletionStage<Boolean> removeIf(final String key, final CasIdentifier casId) {
            throw new UnsupportedOperationException();
        }

        @Nonnull
        @Override
        public CompletionStage<Boolean> replaceIf(final String key, final CasIdentifier casId, final V newValue) {
            throw new UnsupportedOperationException();
        }

        @Nonnull
        @Override
        public CompletionStage<Optional<V>> get(final String key) {
            V value = map.get(requireNonNull(key));
            return result(Optional.ofNullable(value));
        }

        @Nonnull
        @Override
        public CompletionStage<V> get(final String key, final Supplier<V> supplier) {
            V value = map.computeIfAbsent(requireNonNull(key), x -> supplier.get());
            return result(value);
        }

        @Nonnull
        @Override
        public CompletionStage<Map<String, Optional<V>>> getBulk(final Iterable<String> keys) {
            throw new UnsupportedOperationException();
        }

        @Nonnull
        @Override
        public CompletionStage<Map<String, V>> getBulk(final Function<Set<String>, Map<String, V>> factory, final Iterable<String> keys) {
            throw new UnsupportedOperationException();
        }

        @Nonnull
        @Override
        public CompletionStage<Boolean> put(final String key, final V value, final PutPolicy policy) {
            if (policy != PUT_ALWAYS) {
                throw new UnsupportedOperationException("Only PutPolicy.PUT_ALWAYS is supported");
            }
            map.put(requireNonNull(key), requireNonNull(value));
            return result(true);
        }

        @Nonnull
        @Override
        public CompletionStage<Void> remove(final Iterable<String> keys) {
            keys.forEach(map::remove);
            return result(null);
        }

        @Nonnull
        @Override
        public CompletionStage<Void> removeAll() {
            map.clear();
            return result(null);
        }

        @Nonnull
        @Override
        public CompletionStage<Void> remove(String... keys) {
            return remove(Arrays.asList(keys));
        }

        @Nonnull
        @Override
        public String getName() {
            throw new UnsupportedOperationException();
        }
    }

    public static class SimpleJvmCache<K, V> implements JvmCache<K, V> {
        final Map<K, V> map = new HashMap<>();

        @Nonnull
        @Override
        public Set<K> getKeys() {
            throw new UnsupportedOperationException();
        }

        @Nonnull
        @Override
        public Optional<V> get(K key) {
            return Optional.ofNullable(map.get(requireNonNull(key)));
        }

        @Nonnull
        @Override
        public V get(K key, Supplier<? extends V> supplier) {
            return map.computeIfAbsent(key, k -> supplier.get());
        }

        @Override
        public void put(K key, V value) {
            map.put(requireNonNull(key), requireNonNull(value));
        }

        @Nonnull
        @Override
        public Optional<V> putIfAbsent(K key, V value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean replaceIf(K key, V currentValue, V newValue) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean removeIf(K key, V value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void remove(K key) {
            map.remove(requireNonNull(key));
        }

        @Override
        public void removeAll() {
            map.clear();
        }

        @Nonnull
        @Override
        public String getName() {
            throw new UnsupportedOperationException();
        }
    }

    public static class SimpleTransactionalExternalCache<V> implements TransactionalExternalCache<V> {
        final Map<String, V> map = new HashMap<>();


        @Nonnull
        @Override
        public CompletionStage<Optional<V>> get(final String key) {
            V value = map.get(requireNonNull(key));
            return result(Optional.ofNullable(value));
        }

        @Nonnull
        @Override
        public CompletionStage<V> get(final String key, final Supplier<V> supplier) {
            V value = map.computeIfAbsent(requireNonNull(key), x -> supplier.get());
            return result(value);
        }

        @Nonnull
        @Override
        public CompletionStage<Map<String, Optional<V>>> getBulk(final Iterable<String> keys) {
            throw new UnsupportedOperationException();
        }

        @Nonnull
        @Override
        public CompletionStage<Map<String, V>> getBulk(final Function<Set<String>, Map<String, V>> factory, final Iterable<String> keys) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void put(final String key, final V value, final PutPolicy policy) {
            if (policy != PUT_ALWAYS) {
                throw new UnsupportedOperationException("Only PutPolicy.PUT_ALWAYS is supported");
            }
            map.put(requireNonNull(key), requireNonNull(value));
        }

        @Override
        public void remove(final Iterable<String> keys) {
            keys.forEach(map::remove);
        }

        @Override
        public void removeAll() {
            map.clear();
        }

        @Nonnull
        @Override
        public String getName() {
            throw new UnsupportedOperationException();
        }
    }
}