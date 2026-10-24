package ca.mgamble.mist;

import java.net.URI;

/**
 * Mist cloud regions and their API hosts.
 * <p>
 * An API token only works against the cloud its org lives in. If every call returns 401,
 * check that the client is pointed at the right region.
 */
public enum MistRegion {
    GLOBAL_01("https://api.mist.com"),
    GLOBAL_02("https://api.gc1.mist.com"),
    GLOBAL_03("https://api.ac2.mist.com"),
    GLOBAL_04("https://api.gc2.mist.com"),
    GLOBAL_05("https://api.gc4.mist.com"),
    EMEA_01("https://api.eu.mist.com"),
    EMEA_02("https://api.gc3.mist.com"),
    EMEA_03("https://api.ac6.mist.com"),
    EMEA_04("https://api.gc6.mist.com"),
    APAC_01("https://api.ac5.mist.com"),
    APAC_02("https://api.gc5.mist.com"),
    APAC_03("https://api.gc7.mist.com");

    private final URI baseUri;

    MistRegion(String baseUrl) {
        this.baseUri = URI.create(baseUrl);
    }

    public URI baseUri() {
        return baseUri;
    }

    /**
     * Resolves a region from its enum name (e.g. {@code GLOBAL_04}, case-insensitive) or API host
     * (e.g. {@code api.gc2.mist.com}).
     *
     * @throws IllegalArgumentException if nothing matches
     */
    public static MistRegion from(String value) {
        String v = value.trim();
        for (MistRegion region : values()) {
            if (region.name().equalsIgnoreCase(v)
                    || region.baseUri.getHost().equalsIgnoreCase(v)
                    || region.baseUri.toString().equalsIgnoreCase(v)) {
                return region;
            }
        }
        throw new IllegalArgumentException("Unknown Mist region: " + value);
    }
}
