package ca.mgamble.mist.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.List;

/**
 * One privilege grant on the current admin or token.
 *
 * @param scope {@code msp}, {@code org}, {@code sitegroup} or {@code site}
 * @param role  e.g. {@code admin}, {@code write}, {@code read}, {@code helpdesk}
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record Privilege(
        String scope,
        String role,
        String name,
        String mspId,
        String mspName,
        String orgId,
        String orgName,
        String siteId,
        List<String> sitegroupIds,
        List<String> orggroupIds) {

    public Privilege {
        sitegroupIds = Models.list(sitegroupIds);
        orggroupIds = Models.list(orggroupIds);
    }
}
