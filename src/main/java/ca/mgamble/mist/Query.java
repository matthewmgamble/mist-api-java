package ca.mgamble.mist;

import ca.mgamble.mist.model.ApiValue;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.stream.Collectors;

/**
 * Mutable, ordered set of query parameters.
 * <p>
 * Values are converted to their wire form: collections and arrays become comma-separated lists (how Mist
 * accepts multiple values), {@link Instant}s become epoch seconds, {@link ApiValue} enums use their API
 * value, and a {@code null} value removes the parameter.
 */
public final class Query {

    private final LinkedHashMap<String, String> params = new LinkedHashMap<>();

    public static Query empty() {
        return new Query();
    }

    /** Builds a query from alternating key/value arguments: {@code Query.of("type", "ap", "limit", 100)}. */
    public static Query of(Object... keysAndValues) {
        if (keysAndValues.length % 2 != 0) {
            throw new IllegalArgumentException("Query.of requires key/value pairs");
        }
        Query query = new Query();
        for (int i = 0; i < keysAndValues.length; i += 2) {
            query.set(String.valueOf(keysAndValues[i]), keysAndValues[i + 1]);
        }
        return query;
    }

    public Query set(String key, Object value) {
        Objects.requireNonNull(key, "key");
        String wire = toWire(value);
        if (wire == null) {
            params.remove(key);
        } else {
            params.put(key, wire);
        }
        return this;
    }

    public Query setAll(Query other) {
        params.putAll(other.params);
        return this;
    }

    public Query copy() {
        return new Query().setAll(this);
    }

    public String get(String key) {
        return params.get(key);
    }

    public boolean isEmpty() {
        return params.isEmpty();
    }

    public Map<String, String> asMap() {
        return Collections.unmodifiableMap(params);
    }

    /** URL-encoded {@code a=1&b=2} form, without a leading {@code ?}. */
    public String encode() {
        StringJoiner joiner = new StringJoiner("&");
        params.forEach((k, v) -> joiner.add(encode(k) + "=" + encode(v)));
        return joiner.toString();
    }

    /** Appends this query to a path, respecting any query string the path already has. */
    String appendTo(String path) {
        if (params.isEmpty()) {
            return path;
        }
        return path + (path.contains("?") ? "&" : "?") + encode();
    }

    @Override
    public String toString() {
        return encode();
    }

    private static String encode(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static String toWire(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof ApiValue apiValue) {
            return apiValue.apiValue();
        }
        if (value instanceof Instant instant) {
            return Long.toString(instant.getEpochSecond());
        }
        if (value instanceof Collection<?> collection) {
            return collection.isEmpty() ? null
                    : collection.stream().map(Query::toWire).filter(Objects::nonNull).collect(Collectors.joining(","));
        }
        if (value instanceof Object[] array) {
            return toWire(Arrays.asList(array));
        }
        return value.toString();
    }
}
