package ca.mgamble.mist.model;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.Map;

/**
 * Device statistics, as returned by {@code listOrgDevicesStats}, {@code listSiteDevicesStats} and
 * {@code getSiteDeviceStats}.
 * <p>
 * Org-level stats omit some fields (notably the power fields); query the site-level endpoint when you need
 * them. The many type-specific blocks ({@code env_stat}, {@code port_stat}, {@code if_stat}, ...) are
 * available through {@link #extra(String)}.
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record DeviceStats(
        String id,
        String mac,
        String name,
        String type,
        String model,
        String serial,
        String hwRev,
        String status,
        String version,
        String ip,
        String extIp,
        Long uptime,
        Long lastSeen,
        String siteId,
        String orgId,
        Integer numClients,
        Integer cpuUtil,
        Long memTotalKb,
        Long memUsedKb,
        Long txBps,
        Long rxBps,
        Boolean powerConstrained,
        Integer powerBudget,
        String powerSrc,
        String powerOpmode,
        RadioStat radioStat,
        String notes,
        Long createdTime,
        Long modifiedTime,
        @JsonAnySetter @JsonAnyGetter Map<String, JsonNode> extra) implements MistModel {

    public DeviceStats {
        extra = Models.extra(extra);
    }

    public DeviceType deviceType() {
        return DeviceType.fromApi(type);
    }

    @JsonIgnore
    public boolean isConnected() {
        return "connected".equalsIgnoreCase(status);
    }

    @JsonIgnore
    public boolean isPowerConstrained() {
        return Boolean.TRUE.equals(powerConstrained);
    }
}
