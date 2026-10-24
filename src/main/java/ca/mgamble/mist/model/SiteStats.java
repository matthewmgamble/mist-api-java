package ca.mgamble.mist.model;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.List;
import java.util.Map;

/**
 * A site with its device and client counts, as returned by {@code listOrgSiteStats} and {@code getSiteStats}.
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record SiteStats(
        String id,
        String name,
        String orgId,
        String mspId,
        String timezone,
        Integer tzoffset,
        String countryCode,
        String address,
        LatLng latlng,
        String notes,
        List<String> sitegroupIds,
        int numAp,
        int numApConnected,
        int numSwitch,
        int numSwitchConnected,
        int numGateway,
        int numGatewayConnected,
        int numDevices,
        int numDevicesConnected,
        int numClients,
        Long createdTime,
        Long modifiedTime,
        @JsonAnySetter @JsonAnyGetter Map<String, JsonNode> extra) implements MistModel {

    public SiteStats {
        sitegroupIds = Models.list(sitegroupIds);
        extra = Models.extra(extra);
    }

    public int disconnectedAps() {
        return Math.max(0, numAp - numApConnected);
    }

    public int disconnectedSwitches() {
        return Math.max(0, numSwitch - numSwitchConnected);
    }

    public int disconnectedGateways() {
        return Math.max(0, numGateway - numGatewayConnected);
    }

    /** True when every AP, switch and gateway at the site is connected. */
    @JsonIgnore
    public boolean isFullyConnected() {
        return disconnectedAps() == 0 && disconnectedSwitches() == 0 && disconnectedGateways() == 0;
    }
}
