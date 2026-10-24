package ca.mgamble.mist.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Per-band radio statistics for an AP. Bands the AP does not have are null. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RadioStat(
        @JsonProperty("band_24") RadioBandStat band24,
        @JsonProperty("band_5") RadioBandStat band5,
        @JsonProperty("band_6") RadioBandStat band6) {
}
