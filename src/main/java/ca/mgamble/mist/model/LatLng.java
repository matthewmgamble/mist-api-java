package ca.mgamble.mist.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/** A site's geographic coordinates. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record LatLng(double lat, double lng) {
}
