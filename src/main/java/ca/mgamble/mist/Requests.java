package ca.mgamble.mist;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/** Shared request helpers for the API classes. */
final class Requests {

    private final Transport transport;
    private final int pageSize;

    Requests(Transport transport, int pageSize) {
        this.transport = transport;
        this.pageSize = pageSize;
    }

    Transport transport() {
        return transport;
    }

    JavaType type(Class<?> type) {
        return transport.mapper().constructType(type);
    }

    JavaType type(TypeReference<?> type) {
        return transport.mapper().getTypeFactory().constructType(type);
    }

    JsonNode json(String method, String path, Query query, Object body) {
        String target = query == null ? path : query.appendTo(path);
        return transport.readTree(transport.send(method, target, body), method, path);
    }

    <T> T get(String path, Query query, JavaType type) {
        return transport.convert(json("GET", path, query, null), type, "GET", path);
    }

    <T> T get(String path, Class<T> type) {
        return get(path, null, type(type));
    }

    <T> Paged<T> list(String path, Query query, JavaType elementType) {
        return Pagination.pageBased(transport, path, query == null ? Query.empty() : query, pageSize, elementType);
    }

    <T> Paged<T> search(String path, Query query, JavaType elementType) {
        return Pagination.cursorBased(transport, path, query == null ? Query.empty() : query, pageSize, elementType);
    }

    /** Expands {@code {name}} placeholders in order, URL-encoding each value as a path segment. */
    static String path(String template, Object... values) {
        StringBuilder out = new StringBuilder();
        int index = 0;
        int pos = 0;
        while (true) {
            int open = template.indexOf('{', pos);
            if (open < 0) {
                break;
            }
            int close = template.indexOf('}', open);
            if (close < 0) {
                throw new IllegalArgumentException("Unclosed '{' in path template: " + template);
            }
            if (index >= values.length) {
                throw new IllegalArgumentException("Not enough values for path template: " + template);
            }
            Object value = values[index++];
            if (value == null || value.toString().isEmpty()) {
                throw new IllegalArgumentException("Path parameter " + template.substring(open, close + 1) + " is empty");
            }
            out.append(template, pos, open)
                    .append(URLEncoder.encode(value.toString(), StandardCharsets.UTF_8).replace("+", "%20"));
            pos = close + 1;
        }
        if (index != values.length) {
            throw new IllegalArgumentException("Too many values for path template: " + template);
        }
        return out.append(template.substring(pos)).toString();
    }
}
