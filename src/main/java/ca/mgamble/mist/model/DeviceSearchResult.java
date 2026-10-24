package ca.mgamble.mist.model;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.List;
import java.util.Map;

/**
 * A device from {@code searchOrgDevices} / {@code searchSiteDevices}. Unlike the stats endpoints, search
 * returns config state ({@code last_config_status}) and LLDP neighbour details.
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record DeviceSearchResult(
        String mac,
        String type,
        String model,
        String sku,
        String version,
        @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY) List<String> hostname,
        String ip,
        String extIp,
        Long uptime,
        Long timestamp,
        String siteId,
        String orgId,
        String lastConfigStatus,
        Boolean powerConstrained,
        String powerOpmode,
        String lldpSystemName,
        String lldpPortId,
        String lldpMgmtAddr,
        String mxedgeId,
        String mxtunnelStatus,
        @JsonAnySetter @JsonAnyGetter Map<String, JsonNode> extra) implements MistModel {

    public DeviceSearchResult {
        hostname = Models.list(hostname);
        extra = Models.extra(extra);
    }

    public DeviceType deviceType() {
        return DeviceType.fromApi(type);
    }

    @JsonIgnore
    public boolean isConfigSuccess() {
        return "success".equalsIgnoreCase(lastConfigStatus);
    }

    @JsonIgnore
    public boolean isPowerConstrained() {
        return Boolean.TRUE.equals(powerConstrained);
    }
}
