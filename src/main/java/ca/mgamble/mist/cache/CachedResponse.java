package ca.mgamble.mist.cache;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

/**
 * A cached HTTP response. Headers are kept because page-based pagination relies on
 * {@code X-Page-Total}.
 *
 * @param statusCode HTTP status
 * @param headers    response headers (names are matched case-insensitively)
 * @param body       response body
 */
public record CachedResponse(int statusCode, Map<String, List<String>> headers, String body) {

    public CachedResponse {
        TreeMap<String, List<String>> copy = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        if (headers != null) {
            headers.forEach((k, v) -> {
                if (k != null) {
                    copy.put(k, List.copyOf(v));
                }
            });
        }
        headers = Collections.unmodifiableMap(copy);
    }

    public Optional<String> firstHeader(String name) {
        List<String> values = headers.get(name);
        return values == null || values.isEmpty() ? Optional.empty() : Optional.of(values.get(0));
    }
}
