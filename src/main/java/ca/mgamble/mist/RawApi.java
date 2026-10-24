package ca.mgamble.mist;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Untyped access to any Mist endpoint, with the same authentication, retries, caching and pagination as the
 * typed API. Use it for the endpoints that have no typed method yet. Obtain via {@link MistClient#raw()}.
 * <pre>{@code
 * Paged<JsonNode> wlans = client.raw().list(RawApi.path("/api/v1/orgs/{org_id}/wlans", orgId));
 * JsonNode created = client.raw().post(RawApi.path("/api/v1/orgs/{org_id}/webhooks", orgId), body);
 * }</pre>
 */
public final class RawApi {

    private final Requests requests;

    RawApi(Requests requests) {
        this.requests = requests;
    }

    /**
     * Fills {@code {placeholders}} in order with URL-encoded values:
     * {@code path("/api/v1/sites/{site_id}/devices/{device_id}", siteId, deviceId)}.
     */
    public static String path(String template, Object... values) {
        return Requests.path(template, values);
    }

    public JsonNode get(String path) {
        return get(path, null);
    }

    public JsonNode get(String path, Query query) {
        return requests.json("GET", path, query, null);
    }

    public <T> T get(String path, Query query, Class<T> type) {
        return requests.get(path, query, requests.type(type));
    }

    public <T> T get(String path, Query query, TypeReference<T> type) {
        return requests.get(path, query, requests.type(type));
    }

    /** POST with a JSON body (a String is sent verbatim; anything else is serialised). */
    public JsonNode post(String path, Object body) {
        return requests.json("POST", path, null, body);
    }

    public JsonNode put(String path, Object body) {
        return requests.json("PUT", path, null, body);
    }

    public JsonNode delete(String path) {
        return requests.json("DELETE", path, null, null);
    }

    /** Any method, with optional query and body. Returns a missing node for empty responses. */
    public JsonNode request(String method, String path, Query query, Object body) {
        return requests.json(method, path, query, body);
    }

    /** A page-based list endpoint (JSON array response, {@code limit}/{@code page} params). */
    public Paged<JsonNode> list(String path) {
        return list(path, null);
    }

    public Paged<JsonNode> list(String path, Query query) {
        return requests.list(path, query, requests.type(JsonNode.class));
    }

    public <T> Paged<T> list(String path, Query query, Class<T> elementType) {
        return requests.list(path, query, requests.type(elementType));
    }

    /** A cursor-based {@code /search} endpoint ({@code results} + {@code next} response). */
    public Paged<JsonNode> search(String path, Query query) {
        return requests.search(path, query, requests.type(JsonNode.class));
    }

    public <T> Paged<T> search(String path, Query query, Class<T> elementType) {
        return requests.search(path, query, requests.type(elementType));
    }
}
