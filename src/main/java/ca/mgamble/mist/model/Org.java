package ca.mgamble.mist.model;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.List;
import java.util.Map;

/** An organization, as returned by {@code getOrg}. */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record Org(
        String id,
        String name,
        String mspId,
        List<String> orggroupIds,
        String alarmtemplateId,
        Boolean allowMist,
        Integer sessionExpiry,
        Long createdTime,
        Long modifiedTime,
        @JsonAnySetter @JsonAnyGetter Map<String, JsonNode> extra) implements MistModel {

    public Org {
        orggroupIds = Models.list(orggroupIds);
        extra = Models.extra(extra);
    }
}
