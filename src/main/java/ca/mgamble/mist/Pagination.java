package ca.mgamble.mist;

import ca.mgamble.mist.cache.CachedResponse;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * The two pagination styles used by Mist.
 * <ul>
 *   <li><b>Page-based</b> list endpoints return a JSON array and take {@code limit} + {@code page}; the total
 *       count is in the {@code X-Page-Total} header.</li>
 *   <li><b>Cursor-based</b> {@code /search} endpoints return {@code {"results": [...], "next": "..."}}; the
 *       {@code next} URL carries a {@code search_after} cursor. They do not accept {@code page}.</li>
 * </ul>
 */
final class Pagination {

    private Pagination() {
    }

    static <T> Paged<T> pageBased(Transport transport, String path, Query query, int pageSize, JavaType elementType) {
        return new Paged<>(() -> new PageIterator<>(transport, path, query.copy(), pageSize, elementType));
    }

    static <T> Paged<T> cursorBased(Transport transport, String path, Query query, int pageSize, JavaType elementType) {
        return new Paged<>(() -> new CursorIterator<>(transport, path, query.copy(), pageSize, elementType));
    }

    private abstract static class BufferedIterator<T> implements Iterator<T> {
        private Iterator<T> buffer = List.<T>of().iterator();
        protected boolean done;

        /** Fetches the next page; sets {@link #done} after the last one. */
        protected abstract List<T> fetch();

        @Override
        public boolean hasNext() {
            while (!buffer.hasNext() && !done) {
                buffer = fetch().iterator();
            }
            return buffer.hasNext();
        }

        @Override
        public T next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            return buffer.next();
        }
    }

    private static final class PageIterator<T> extends BufferedIterator<T> {
        private final Transport transport;
        private final String path;
        private final Query query;
        private final JavaType elementType;
        private final int limit;
        private int page = 1;
        private long fetched;
        private String previousBody;

        PageIterator(Transport transport, String path, Query query, int pageSize, JavaType elementType) {
            this.transport = transport;
            this.path = path;
            this.query = query;
            this.elementType = elementType;
            this.limit = query.get("limit") != null ? Integer.parseInt(query.get("limit")) : pageSize;
            query.set("limit", limit);
        }

        @Override
        protected List<T> fetch() {
            query.set("page", page);
            CachedResponse response = transport.send("GET", query.appendTo(path), null);
            // a server that ignores `page` would return the same page forever
            if (Objects.equals(response.body(), previousBody)) {
                done = true;
                return List.of();
            }
            previousBody = response.body();

            JsonNode root = transport.readTree(response, "GET", path);
            JsonNode array = root.isArray() ? root : root.path("results");
            List<T> items = toList(transport, array, elementType, path);
            fetched += items.size();
            page++;

            Long total = header(response, "X-Page-Total");
            if (items.isEmpty() || !root.isArray()) {
                done = true;
            } else if (total != null) {
                done = fetched >= total;
            } else {
                Long pageLimit = header(response, "X-Page-Limit");
                done = items.size() < (pageLimit != null ? pageLimit : limit);
            }
            return items;
        }

        private static Long header(CachedResponse response, String name) {
            return response.firstHeader(name).map(v -> {
                try {
                    return Long.parseLong(v.trim());
                } catch (NumberFormatException e) {
                    return null;
                }
            }).orElse(null);
        }
    }

    private static final class CursorIterator<T> extends BufferedIterator<T> {
        private final Transport transport;
        private final String path;
        private final Query query;
        private final JavaType elementType;
        private String next;
        private long fetched;

        CursorIterator(Transport transport, String path, Query query, int pageSize, JavaType elementType) {
            this.transport = transport;
            this.path = path;
            this.query = query;
            this.elementType = elementType;
            if (query.get("limit") == null) {
                query.set("limit", pageSize);
            }
        }

        @Override
        protected List<T> fetch() {
            String target = next != null ? next : query.appendTo(path);
            CachedResponse response = transport.send("GET", target, null);
            JsonNode root = transport.readTree(response, "GET", path);
            List<T> items = toList(transport, root.path("results"), elementType, path);
            fetched += items.size();

            JsonNode nextNode = root.path("next");
            String nextLink = nextNode.isTextual() && !nextNode.asText().isBlank() ? nextNode.asText() : null;
            long total = root.path("total").asLong(0);
            done = items.isEmpty()
                    || nextLink == null
                    || nextLink.equals(next)
                    || (total > 0 && fetched >= total);
            next = nextLink;
            return items;
        }
    }

    private static <T> List<T> toList(Transport transport, JsonNode array, JavaType elementType, String path) {
        if (!array.isArray()) {
            return List.of();
        }
        List<T> items = new ArrayList<>(array.size());
        for (JsonNode node : array) {
            items.add(transport.convert(node, elementType, "GET", path));
        }
        return items;
    }
}
