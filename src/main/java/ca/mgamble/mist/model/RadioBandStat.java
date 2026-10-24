package ca.mgamble.mist.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/** Per-band radio statistics for an AP ({@code radio_stat.band_24 / band_5 / band_6}). Utilisation values are percentages. */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record RadioBandStat(
        String mac,
        Integer channel,
        Integer bandwidth,
        Integer power,
        Integer noiseFloor,
        Integer numClients,
        Boolean disabled,
        String usage,
        Integer utilAll,
        Integer utilTx,
        Integer utilRxInBss,
        Integer utilRxOtherBss,
        Integer utilNonWifi,
        Integer utilUnknownWifi,
        Integer utilUndecodableWifi,
        Long txBytes,
        Long rxBytes,
        Long txPkts,
        Long rxPkts) {

    @JsonIgnore
    public boolean isDisabled() {
        return Boolean.TRUE.equals(disabled);
    }
}
