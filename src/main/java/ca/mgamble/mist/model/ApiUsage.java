package ca.mgamble.mist.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/** API calls made in the current hour against the hourly limit, from {@code getSelfApiUsage}. */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record ApiUsage(int requests, int requestLimit) {

    public int remaining() {
        return Math.max(0, requestLimit - requests);
    }
}
