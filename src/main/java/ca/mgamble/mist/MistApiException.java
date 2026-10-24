package ca.mgamble.mist;

/**
 * Thrown when a Mist API call fails.
 * <p>
 * {@link #statusCode()} is the HTTP status, or {@code 0} when the request never got a response
 * (connection failure, timeout, interrupted, unparseable body).
 */
public class MistApiException extends RuntimeException {

    private final int statusCode;
    private final String method;
    private final String path;
    private final String responseBody;

    public MistApiException(String message, Throwable cause) {
        this(message, 0, null, null, null, cause);
    }

    public MistApiException(String message, int statusCode, String method, String path, String responseBody) {
        this(message, statusCode, method, path, responseBody, null);
    }

    protected MistApiException(String message, int statusCode, String method, String path,
                               String responseBody, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
        this.method = method;
        this.path = path;
        this.responseBody = responseBody;
    }

    public int statusCode() {
        return statusCode;
    }

    /** HTTP method of the failed request, if known. */
    public String method() {
        return method;
    }

    /** Request path (without host), if known. */
    public String path() {
        return path;
    }

    /** Raw response body, if the server sent one. */
    public String responseBody() {
        return responseBody;
    }

    /** 401 or 403: the token is missing, invalid, for another region, or lacks privileges. */
    public static class AuthException extends MistApiException {
        public AuthException(String message, int statusCode, String method, String path, String responseBody) {
            super(message, statusCode, method, path, responseBody);
        }
    }

    /** 404: the org, site, device or other resource does not exist (or is not visible to this token). */
    public static class NotFoundException extends MistApiException {
        public NotFoundException(String message, String method, String path, String responseBody) {
            super(message, 404, method, path, responseBody);
        }
    }

    /**
     * 429: the token's hourly request budget (5,000 calls/hour by default) is exhausted and the client
     * gave up waiting.
     */
    public static class RateLimitException extends MistApiException {
        private final long retryAfterSeconds;

        public RateLimitException(String message, String method, String path, String responseBody,
                                  long retryAfterSeconds) {
            super(message, 429, method, path, responseBody);
            this.retryAfterSeconds = retryAfterSeconds;
        }

        /** Server-suggested wait in seconds, or {@code -1} if none was given. */
        public long retryAfterSeconds() {
            return retryAfterSeconds;
        }
    }
}
