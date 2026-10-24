package ca.mgamble.mist.model;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.Map;

/** Organization-wide counts, as returned by {@code getOrgStats}. SLE data is available via {@code extra("sle")}. */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record OrgStats(
        String id,
        String name,
        String mspId,
        int numSites,
        int numDevices,
        int numDevicesConnected,
        int numDevicesDisconnected,
        int numInventory,
        Long createdTime,
        Long modifiedTime,
        @JsonAnySetter @JsonAnyGetter Map<String, JsonNode> extra) implements MistModel {

    public OrgStats {
        extra = Models.extra(extra);
    }
}
