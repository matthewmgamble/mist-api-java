package ca.mgamble.mist.model;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.List;
import java.util.Map;

/**
 * A site, as returned by {@code listOrgSites} and {@code getSiteInfo}.
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record Site(
        String id,
        String name,
        String orgId,
        String timezone,
        String countryCode,
        String address,
        LatLng latlng,
        String notes,
        List<String> sitegroupIds,
        String alarmtemplateId,
        String aptemplateId,
        String gatewaytemplateId,
        String networktemplateId,
        String rftemplateId,
        String secpolicyId,
        String sitetemplateId,
        Long createdTime,
        Long modifiedTime,
        @JsonAnySetter @JsonAnyGetter Map<String, JsonNode> extra) implements MistModel {

    public Site {
        sitegroupIds = Models.list(sitegroupIds);
        extra = Models.extra(extra);
    }
}
