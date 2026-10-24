package ca.mgamble.mist.query;

import ca.mgamble.mist.model.DeviceType;

import java.time.Instant;
import java.util.List;

/** Filters for {@code getOrgInventory}. */
public final class InventoryQuery extends ApiQuery<InventoryQuery> {

    public InventoryQuery type(DeviceType... types) {
        return param("type", List.of(types));
    }

    public InventoryQuery siteId(String siteId) {
        return param("site_id", siteId);
    }

    public InventoryQuery model(String model) {
        return param("model", model);
    }

    public InventoryQuery serial(String serial) {
        return param("serial", serial);
    }

    public InventoryQuery mac(String mac) {
        return param("mac", mac);
    }

    /** Only devices not assigned to a site. */
    public InventoryQuery unassigned(Boolean unassigned) {
        return param("unassigned", unassigned);
    }

    /** Include virtual chassis members. */
    public InventoryQuery virtualChassis(Boolean vc) {
        return param("vc", vc);
    }

    public InventoryQuery modifiedAfter(Instant instant) {
        return param("modified_after", instant);
    }
}
