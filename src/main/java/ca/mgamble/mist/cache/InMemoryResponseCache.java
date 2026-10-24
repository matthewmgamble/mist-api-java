package ca.mgamble.mist.cache;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Bounded in-memory cache with a fixed time-to-live, evicting the least recently used entry when full.
 */
public final class InMemoryResponseCache implements ResponseCache {

    private record CacheEntry(CachedResponse response, Instant expiresAt) {
    }

    private final Duration ttl;
    private final Clock clock;
    private final Map<String, CacheEntry> entries;

    public InMemoryResponseCache(Duration ttl) {
        this(ttl, 1_000);
    }

    public InMemoryResponseCache(Duration ttl, int maxEntries) {
        this(ttl, maxEntries, Clock.systemUTC());
    }

    InMemoryResponseCache(Duration ttl, int maxEntries, Clock clock) {
        if (ttl.isNegative() || ttl.isZero()) {
            throw new IllegalArgumentException("ttl must be positive");
        }
        if (maxEntries < 1) {
            throw new IllegalArgumentException("maxEntries must be at least 1");
        }
        this.ttl = ttl;
        this.clock = Objects.requireNonNull(clock);
        this.entries = new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, CacheEntry> eldest) {
                return size() > maxEntries;
            }
        };
    }

    @Override
    public synchronized Optional<CachedResponse> get(String key) {
        CacheEntry entry = entries.get(key);
        if (entry == null) {
            return Optional.empty();
        }
        if (!clock.instant().isBefore(entry.expiresAt())) {
            entries.remove(key);
            return Optional.empty();
        }
        return Optional.of(entry.response());
    }

    @Override
    public synchronized void put(String key, CachedResponse response) {
        entries.put(key, new CacheEntry(response, clock.instant().plus(ttl)));
    }

    @Override
    public synchronized void invalidateAll() {
        entries.clear();
    }

    public synchronized int size() {
        return entries.size();
    }
}
