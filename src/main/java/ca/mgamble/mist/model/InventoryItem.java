package ca.mgamble.mist.model;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.Map;

/** A device in the org inventory, from {@code getOrgInventory}. {@code siteId} is null for unassigned devices. */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record InventoryItem(
        String id,
        String mac,
        String serial,
        String model,
        String sku,
        String type,
        String name,
        String siteId,
        String orgId,
        Boolean connected,
        String deviceprofileId,
        String vcMac,
        Long lastDisconnected,
        Long createdTime,
        Long modifiedTime,
        @JsonAnySetter @JsonAnyGetter Map<String, JsonNode> extra) implements MistModel {

    public InventoryItem {
        extra = Models.extra(extra);
    }

    public DeviceType deviceType() {
        return DeviceType.fromApi(type);
    }

    @JsonIgnore
    public boolean isConnected() {
        return Boolean.TRUE.equals(connected);
    }
}
