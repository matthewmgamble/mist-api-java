package ca.mgamble.mist.query;

import ca.mgamble.mist.model.DeviceType;

/**
 * Filters for {@code searchOrgDevices} / {@code searchSiteDevices}. Mist supports many more filters
 * (radio channels, LLDP, EVPN, HA cluster nodes, ...); set those with {@link #param(String, Object)}.
 */
public final class DeviceSearch extends TimeRangeQuery<DeviceSearch> {

    public DeviceSearch type(DeviceType type) {
        return param("type", type);
    }

    /** Restricts an org-level search to one site (ignored by site-level search). */
    public DeviceSearch siteId(String siteId) {
        return param("site_id", siteId);
    }

    public DeviceSearch mac(String mac) {
        return param("mac", mac);
    }

    public DeviceSearch model(String model) {
        return param("model", model);
    }

    public DeviceSearch version(String version) {
        return param("version", version);
    }

    public DeviceSearch hostname(String hostname) {
        return param("hostname", hostname);
    }

    public DeviceSearch ip(String ip) {
        return param("ip", ip);
    }

    /** Switches and gateways only: last configuration status, e.g. {@code success} or {@code failed}. */
    public DeviceSearch lastConfigStatus(String status) {
        return param("last_config_status", status);
    }

    public DeviceSearch powerConstrained(Boolean powerConstrained) {
        return param("power_constrained", powerConstrained);
    }

    /** Include device stats in each result. */
    public DeviceSearch withStats(boolean stats) {
        return param("stats", stats);
    }

    /** One of {@code mac}, {@code model}, {@code sku}, {@code timestamp}; prefix {@code -} for descending. */
    public DeviceSearch sort(String sort) {
        return param("sort", sort);
    }
}
