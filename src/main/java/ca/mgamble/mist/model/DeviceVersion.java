package ca.mgamble.mist.model;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;

/**
 * A firmware version available for a device model, from {@code listOrgAvailableDeviceVersions}.
 *
 * @param tag release tag such as {@code suggested}; empty or null for untagged versions
 */
public record DeviceVersion(
        String model,
        String version,
        String tag,
        @JsonAnySetter @JsonAnyGetter Map<String, JsonNode> extra) implements MistModel {

    public DeviceVersion {
        extra = Models.extra(extra);
    }
}
