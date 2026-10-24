package ca.mgamble.mist;

import ca.mgamble.mist.cache.ResponseCache;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;

/**
 * Entry point for the Juniper Mist API.
 * <pre>{@code
 * try (MistClient mist = MistClient.builder()
 *         .apiToken(System.getenv("MIST_API_TOKEN"))
 *         .region(MistRegion.GLOBAL_04)
 *         .build()) {
 *     for (SiteStats site : mist.org(orgId).listSiteStats()) {
 *         System.out.println(site.name() + ": " + site.disconnectedAps() + " APs down");
 *     }
 * }
 * }</pre>
 * Instances are thread-safe and intended to be shared. One client holds one API token; a token that spans
 * several orgs can reach all of them through {@link #org(String)}.
 */
public final class MistClient implements AutoCloseable {

    /** Library version, sent in the User-Agent header. */
    public static final String VERSION = versionFromManifest();

    private final Transport transport;
    private final Requests requests;
    private final HttpClient ownedHttpClient;
    private final ResponseCache cache;

    private MistClient(Builder b) {
        HttpClient http = b.httpClient;
        this.ownedHttpClient = http == null
                ? HttpClient.newBuilder().connectTimeout(b.connectTimeout).followRedirects(HttpClient.Redirect.NEVER).build()
                : null;
        ObjectMapper mapper = b.objectMapper != null ? b.objectMapper : defaultObjectMapper();
        this.cache = b.cache;
        this.transport = new Transport(http != null ? http : ownedHttpClient, b.baseUri, b.apiToken, b.requestTimeout,
                b.retryPolicy, b.cache, mapper, b.userAgent, b.sleeper);
        this.requests = new Requests(transport, b.pageSize);
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builds a client from environment variables: {@code MIST_API_TOKEN} (or {@code MIST_APITOKEN}) and,
     * optionally, {@code MIST_HOST} as a region name ({@code GLOBAL_04}), host ({@code api.gc2.mist.com}) or URL.
     */
    public static MistClient fromEnvironment() {
        return fromEnvironment(System.getenv());
    }

    static MistClient fromEnvironment(Map<String, String> env) {
        String token = env.getOrDefault("MIST_API_TOKEN", env.get("MIST_APITOKEN"));
        if (token == null || token.isBlank()) {
            throw new IllegalStateException("MIST_API_TOKEN is not set");
        }
        Builder builder = builder().apiToken(token);
        String host = env.get("MIST_HOST");
        if (host != null && !host.isBlank()) {
            builder.host(host);
        }
        return builder.build();
    }

    /** The mapper used for requests and responses, configured to ignore unknown fields and omit nulls. */
    public static ObjectMapper defaultObjectMapper() {
        return new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    public SelfApi self() {
        return new SelfApi(requests);
    }

    public OrgApi org(String orgId) {
        return new OrgApi(requests, Objects.requireNonNull(orgId, "orgId"));
    }

    public SiteApi site(String siteId) {
        return new SiteApi(requests, Objects.requireNonNull(siteId, "siteId"));
    }

    /** Untyped access to every other endpoint. */
    public RawApi raw() {
        return new RawApi(requests);
    }

    /** Calls {@code getSelf}; returns false on any failure instead of throwing. */
    public boolean testConnection() {
        try {
            self().get();
            return true;
        } catch (MistApiException e) {
            return false;
        }
    }

    public URI baseUri() {
        return transport.baseUri();
    }

    public ObjectMapper objectMapper() {
        return transport.mapper();
    }

    /** Clears the response cache, if one is configured. */
    public void invalidateCache() {
        if (cache != null) {
            cache.invalidateAll();
        }
    }

    /** Closes the internal HTTP client. A client passed in through the builder is left open. */
    @Override
    public void close() {
        if (ownedHttpClient != null) {
            ownedHttpClient.close();
        }
    }

    private static String versionFromManifest() {
        String version = MistClient.class.getPackage().getImplementationVersion();
        return version != null ? version : "dev";
    }

    public static final class Builder {
        private URI baseUri = MistRegion.GLOBAL_01.baseUri();
        private String apiToken;
        private HttpClient httpClient;
        private ObjectMapper objectMapper;
        private Duration connectTimeout = Duration.ofSeconds(10);
        private Duration requestTimeout = Duration.ofSeconds(30);
        private RetryPolicy retryPolicy = RetryPolicy.defaults();
        private ResponseCache cache;
        private int pageSize = 100;
        private String userAgent = "mist-api-java/" + VERSION;
        private Transport.Sleeper sleeper = d -> Thread.sleep(d.toMillis());

        private Builder() {
        }

        /** Required. An org or user API token. */
        public Builder apiToken(String apiToken) {
            this.apiToken = apiToken;
            return this;
        }

        /** The Mist cloud the org lives in. Defaults to {@link MistRegion#GLOBAL_01} ({@code api.mist.com}). */
        public Builder region(MistRegion region) {
            this.baseUri = region.baseUri();
            return this;
        }

        /** A custom API base URL, e.g. {@code https://api.gc2.mist.com}. */
        public Builder baseUrl(String baseUrl) {
            String trimmed = baseUrl.trim();
            while (trimmed.endsWith("/")) {
                trimmed = trimmed.substring(0, trimmed.length() - 1);
            }
            URI uri = URI.create(trimmed);
            if (uri.getScheme() == null || uri.getHost() == null) {
                throw new IllegalArgumentException("baseUrl must be an absolute URL: " + baseUrl);
            }
            this.baseUri = uri;
            return this;
        }

        /** A region name ({@code GLOBAL_04}), bare host ({@code api.gc2.mist.com}) or URL. */
        public Builder host(String host) {
            String h = host.trim();
            if (h.startsWith("http://") || h.startsWith("https://")) {
                return baseUrl(h);
            }
            try {
                return region(MistRegion.from(h));
            } catch (IllegalArgumentException e) {
                return baseUrl("https://" + h);
            }
        }

        /** Use your own HttpClient (proxy, SSL, executor). It is not closed by {@link MistClient#close()}. */
        public Builder httpClient(HttpClient httpClient) {
            this.httpClient = httpClient;
            return this;
        }

        public Builder objectMapper(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
            return this;
        }

        /** Ignored when a custom {@link #httpClient(HttpClient)} is supplied. Default 10s. */
        public Builder connectTimeout(Duration connectTimeout) {
            this.connectTimeout = Objects.requireNonNull(connectTimeout);
            return this;
        }

        /** Per-request timeout. Default 30s. */
        public Builder requestTimeout(Duration requestTimeout) {
            this.requestTimeout = Objects.requireNonNull(requestTimeout);
            return this;
        }

        public Builder retryPolicy(RetryPolicy retryPolicy) {
            this.retryPolicy = Objects.requireNonNull(retryPolicy);
            return this;
        }

        /** Cache successful GET responses. Off by default. */
        public Builder cache(ResponseCache cache) {
            this.cache = cache;
            return this;
        }

        /** Default results per page (Mist's {@code limit}), 1-1000. Default 100. */
        public Builder pageSize(int pageSize) {
            if (pageSize < 1 || pageSize > 1000) {
                throw new IllegalArgumentException("pageSize must be between 1 and 1000");
            }
            this.pageSize = pageSize;
            return this;
        }

        public Builder userAgent(String userAgent) {
            this.userAgent = Objects.requireNonNull(userAgent);
            return this;
        }

        /** Test hook: replaces Thread.sleep between retries. */
        Builder sleeper(Transport.Sleeper sleeper) {
            this.sleeper = sleeper;
            return this;
        }

        public MistClient build() {
            if (apiToken == null || apiToken.isBlank()) {
                throw new IllegalStateException("apiToken is required");
            }
            return new MistClient(this);
        }
    }
}
