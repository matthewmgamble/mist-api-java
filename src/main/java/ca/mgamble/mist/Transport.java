package ca.mgamble.mist;

import ca.mgamble.mist.cache.CachedResponse;
import ca.mgamble.mist.cache.ResponseCache;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.MissingNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HexFormat;
import java.util.Optional;
import java.util.Set;

/** Sends requests to the Mist API: authentication, retries, rate-limit handling, caching and error mapping. */
final class Transport {

    private static final Logger log = LoggerFactory.getLogger(Transport.class);
    private static final Set<String> IDEMPOTENT = Set.of("GET", "HEAD", "PUT", "DELETE");

    @FunctionalInterface
    interface Sleeper {
        void sleep(Duration duration) throws InterruptedException;
    }

    private final HttpClient http;
    private final URI baseUri;
    private final String authorization;
    private final String tokenFingerprint;
    private final Duration requestTimeout;
    private final RetryPolicy retryPolicy;
    private final ResponseCache cache;
    private final ObjectMapper mapper;
    private final String userAgent;
    private final Sleeper sleeper;

    Transport(HttpClient http, URI baseUri, String apiToken, Duration requestTimeout, RetryPolicy retryPolicy,
              ResponseCache cache, ObjectMapper mapper, String userAgent, Sleeper sleeper) {
        this.http = http;
        this.baseUri = baseUri;
        this.authorization = "Token " + apiToken;
        this.tokenFingerprint = fingerprint(apiToken);
        this.requestTimeout = requestTimeout;
        this.retryPolicy = retryPolicy;
        this.cache = cache;
        this.mapper = mapper;
        this.userAgent = userAgent;
        this.sleeper = sleeper;
    }

    URI baseUri() {
        return baseUri;
    }

    ObjectMapper mapper() {
        return mapper;
    }

    /**
     * Sends a request and returns the successful (2xx) response.
     *
     * @param pathAndQuery path beginning with {@code /}, optionally with a query string; an absolute URL is
     *                     accepted only if it points at this client's host (as {@code next} links do)
     * @param body         request body: a String is sent as-is, anything else is serialised to JSON
     */
    CachedResponse send(String method, String pathAndQuery, Object body) {
        URI uri = resolve(pathAndQuery);
        boolean cacheable = cache != null && "GET".equals(method);
        String cacheKey = cacheable ? "GET " + uri + " #" + tokenFingerprint : null;
        if (cacheable) {
            Optional<CachedResponse> hit = cache.get(cacheKey);
            if (hit.isPresent()) {
                log.debug("Cache hit: {} {}", method, pathAndQuery);
                return hit.get();
            }
        }

        HttpRequest request = buildRequest(method, uri, body);
        CachedResponse response = execute(request, method, pathAndQuery);
        if (cacheable) {
            cache.put(cacheKey, response);
        }
        return response;
    }

    /** Parses a response body; an empty body yields {@link MissingNode}. */
    JsonNode readTree(CachedResponse response, String method, String path) {
        String body = response.body();
        if (body == null || body.isBlank()) {
            return MissingNode.getInstance();
        }
        try {
            return mapper.readTree(body);
        } catch (JsonProcessingException e) {
            throw new MistApiException("Could not parse response from " + method + " " + path, e);
        }
    }

    <T> T convert(JsonNode node, JavaType type, String method, String path) {
        try {
            return mapper.treeToValue(node, type);
        } catch (JsonProcessingException | IllegalArgumentException e) {
            throw new MistApiException("Could not map response from " + method + " " + path + " to " + type, e);
        }
    }

    private URI resolve(String pathAndQuery) {
        if (pathAndQuery.startsWith("http://") || pathAndQuery.startsWith("https://")) {
            URI absolute = URI.create(pathAndQuery);
            if (!absolute.getHost().equalsIgnoreCase(baseUri.getHost())
                    || !absolute.getScheme().equalsIgnoreCase(baseUri.getScheme())
                    || absolute.getPort() != baseUri.getPort()) {
                // never send the API token to a host other than the configured one
                throw new IllegalArgumentException("URL " + pathAndQuery + " is not on " + baseUri);
            }
            return absolute;
        }
        if (!pathAndQuery.startsWith("/")) {
            throw new IllegalArgumentException("Path must start with '/': " + pathAndQuery);
        }
        return URI.create(baseUri + pathAndQuery);
    }

    private HttpRequest buildRequest(String method, URI uri, Object body) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
                .timeout(requestTimeout)
                .header("Authorization", authorization)
                .header("Accept", "application/json")
                .header("User-Agent", userAgent);
        if (body == null) {
            builder.method(method, HttpRequest.BodyPublishers.noBody());
        } else {
            builder.header("Content-Type", "application/json")
                    .method(method, HttpRequest.BodyPublishers.ofString(toJson(body), StandardCharsets.UTF_8));
        }
        return builder.build();
    }

    private String toJson(Object body) {
        if (body instanceof String s) {
            return s;
        }
        try {
            return mapper.writeValueAsString(body);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Could not serialise request body", e);
        }
    }

    private CachedResponse execute(HttpRequest request, String method, String path) {
        boolean idempotent = IDEMPOTENT.contains(method);
        for (int attempt = 0; ; attempt++) {
            boolean canRetry = attempt < retryPolicy.maxRetries();
            log.debug("{} {}", method, path);

            HttpResponse<String> response;
            try {
                response = http.send(request, HttpResponse.BodyHandlers.ofString());
            } catch (IOException e) {
                if (canRetry && idempotent) {
                    Duration wait = retryPolicy.backoff(attempt);
                    log.warn("{} {} failed ({}), retrying in {}ms", method, path, e.toString(), wait.toMillis());
                    sleep(wait);
                    continue;
                }
                throw new MistApiException(method + " " + path + " failed: " + e, e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new MistApiException(method + " " + path + " was interrupted", e);
            }

            int status = response.statusCode();
            if (status >= 200 && status < 300) {
                return new CachedResponse(status, response.headers().map(), response.body());
            }

            if (status == 429) {
                long retryAfter = retryAfterSeconds(response);
                Duration wait = retryAfter >= 0 ? Duration.ofSeconds(retryAfter) : retryPolicy.backoff(attempt);
                if (canRetry && wait.compareTo(retryPolicy.maxRateLimitWait()) <= 0) {
                    log.warn("{} {} rate limited, retrying in {}ms", method, path, wait.toMillis());
                    sleep(wait);
                    continue;
                }
                throw new MistApiException.RateLimitException(
                        message(method, path, status, response.body()), method, path, response.body(), retryAfter);
            }

            if (status >= 500 && canRetry && idempotent) {
                Duration wait = retryPolicy.backoff(attempt);
                log.warn("{} {} returned {}, retrying in {}ms", method, path, status, wait.toMillis());
                sleep(wait);
                continue;
            }

            throw error(method, path, status, response.body());
        }
    }

    private MistApiException error(String method, String path, int status, String body) {
        String message = message(method, path, status, body);
        return switch (status) {
            case 401 -> new MistApiException.AuthException(
                    message + " (check the API token, and that the client targets the org's cloud region; currently "
                            + baseUri + ")", status, method, path, body);
            case 403 -> new MistApiException.AuthException(message, status, method, path, body);
            case 404 -> new MistApiException.NotFoundException(message, method, path, body);
            default -> new MistApiException(message, status, method, path, body);
        };
    }

    private String message(String method, String path, int status, String body) {
        String detail = detail(body);
        return method + " " + path + " failed with HTTP " + status + (detail.isEmpty() ? "" : ": " + detail);
    }

    /** Mist errors are usually {@code {"detail": "..."}}; fall back to a truncated raw body. */
    private String detail(String body) {
        if (body == null || body.isBlank()) {
            return "";
        }
        try {
            JsonNode node = mapper.readTree(body);
            for (String field : new String[]{"detail", "message", "error"}) {
                if (node.path(field).isTextual()) {
                    return node.path(field).asText();
                }
            }
        } catch (JsonProcessingException ignored) {
            // not JSON
        }
        String trimmed = body.strip();
        return trimmed.length() > 300 ? trimmed.substring(0, 300) + "..." : trimmed;
    }

    private static long retryAfterSeconds(HttpResponse<?> response) {
        Optional<String> header = response.headers().firstValue("Retry-After");
        if (header.isEmpty()) {
            return -1;
        }
        String value = header.get().trim();
        try {
            return Math.max(0, Long.parseLong(value));
        } catch (NumberFormatException e) {
            try {
                ZonedDateTime when = ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME);
                return Math.max(0, Duration.between(ZonedDateTime.now(when.getZone()), when).toSeconds());
            } catch (DateTimeParseException ignored) {
                return -1;
            }
        }
    }

    private void sleep(Duration duration) {
        try {
            sleeper.sleep(duration);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MistApiException("Interrupted while waiting to retry", e);
        }
    }

    private static String fingerprint(String token) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash, 0, 8);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
