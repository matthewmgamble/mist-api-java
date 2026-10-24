package ca.mgamble.mist.model;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/** The admin (or API token) making requests, from {@code getSelf}. */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record SelfAccount(
        String adminId,
        String email,
        String firstName,
        String lastName,
        List<Privilege> privileges,
        @JsonAnySetter @JsonAnyGetter Map<String, JsonNode> extra) implements MistModel {

    public SelfAccount {
        privileges = Models.list(privileges);
        extra = Models.extra(extra);
    }

    /** Distinct org IDs this account has any privilege on, in the order returned. */
    public List<String> orgIds() {
        return privileges.stream().map(Privilege::orgId).filter(Objects::nonNull).distinct().toList();
    }
}
