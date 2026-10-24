package ca.mgamble.mist.model;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.List;
import java.util.Map;

/** An alarm from {@code searchOrgAlarms} / {@code searchSiteAlarms}. Times are epoch seconds. */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record Alarm(
        String id,
        String type,
        String group,
        String severity,
        String status,
        Integer count,
        Long timestamp,
        Long lastSeen,
        String siteId,
        String orgId,
        Boolean acked,
        Long ackedTime,
        String ackAdminId,
        String ackAdminName,
        String note,
        Long resolvedTime,
        List<String> aps,
        List<String> switches,
        List<String> gateways,
        List<String> hostnames,
        List<String> ssids,
        List<String> bssids,
        @JsonAnySetter @JsonAnyGetter Map<String, JsonNode> extra) implements MistModel {

    public Alarm {
        aps = Models.list(aps);
        switches = Models.list(switches);
        gateways = Models.list(gateways);
        hostnames = Models.list(hostnames);
        ssids = Models.list(ssids);
        bssids = Models.list(bssids);
        extra = Models.extra(extra);
    }

    public AlarmSeverity severityLevel() {
        return AlarmSeverity.fromApi(severity);
    }

    @JsonIgnore
    public boolean isAcked() {
        return Boolean.TRUE.equals(acked);
    }
}
