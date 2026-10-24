package ca.mgamble.mist.query;

import ca.mgamble.mist.model.DeviceType;

import java.util.List;

/**
 * Filters for {@code listOrgDevicesStats} / {@code listSiteDevicesStats}.
 * <p>
 * Defaults to {@code type=all}. Mist itself defaults to APs only when {@code type} is omitted, which is an
 * easy way to miss switches and gateways. The site-level endpoint supports only {@code type} and
 * {@code status}.
 */
public final class DeviceStatsQuery extends TimeRangeQuery<DeviceStatsQuery> {

    public DeviceStatsQuery() {
        param("type", "all");
    }

    /** A single device type; Mist does not accept a comma-separated list here. */
    public DeviceStatsQuery type(DeviceType type) {
        return param("type", type);
    }

    public DeviceStatsQuery allTypes() {
        return param("type", "all");
    }

    public DeviceStatsQuery connectedOnly() {
        return param("status", "connected");
    }

    public DeviceStatsQuery disconnectedOnly() {
        return param("status", "disconnected");
    }

    /** Org-level only. */
    public DeviceStatsQuery siteId(String siteId) {
        return param("site_id", siteId);
    }

    /** Org-level only. */
    public DeviceStatsQuery mac(String mac) {
        return param("mac", mac);
    }

    /** Org-level only: extra fields to include, e.g. {@code fields("power_budget", "radio_stat")}. */
    public DeviceStatsQuery fields(String... fields) {
        return param("fields", List.of(fields));
    }

    /** Org-level only: include every optional field ({@code fields=*}). */
    public DeviceStatsQuery allFields() {
        return param("fields", "*");
    }
}
