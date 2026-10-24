package ca.mgamble.mist.cache;

import java.util.Optional;

/**
 * Pluggable cache for successful GET responses.
 * <p>
 * Keys already include the full request URL and a fingerprint of the API token, so one cache can be
 * shared across clients for different orgs or tokens. Implementations decide expiry and eviction, and
 * must be thread-safe. Use {@link InMemoryResponseCache} or back it with your own store (Redis, SQLite, ...).
 */
public interface ResponseCache {

    Optional<CachedResponse> get(String key);

    void put(String key, CachedResponse response);

    /** Drops every entry, e.g. to force a refresh. */
    void invalidateAll();
}
