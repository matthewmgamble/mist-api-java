package ca.mgamble.mist.query;

import ca.mgamble.mist.model.AlarmSeverity;

import java.util.Collection;
import java.util.List;

/**
 * Filters for {@code searchOrgAlarms} / {@code searchSiteAlarms}.
 * <pre>{@code
 * // unacknowledged critical alarms from the last day
 * new AlarmSearch().acked(false).severity(AlarmSeverity.CRITICAL).duration("1d")
 * }</pre>
 * Without a time range Mist searches only a recent default window, so set {@code duration} or
 * {@code start}/{@code end} explicitly.
 */
public final class AlarmSearch extends TimeRangeQuery<AlarmSearch> {

    /** Restricts an org-level search to these sites (ignored by site-level search). */
    public AlarmSearch siteIds(Collection<String> siteIds) {
        return param("site_id", siteIds);
    }

    public AlarmSearch siteIds(String... siteIds) {
        return siteIds(List.of(siteIds));
    }

    public AlarmSearch severity(AlarmSeverity... severities) {
        return param("severity", List.of(severities));
    }

    /** Alarm groups: {@code infrastructure}, {@code security}, {@code marvis}, {@code certificate_expiry}. */
    public AlarmSearch group(String... groups) {
        return param("group", List.of(groups));
    }

    /** Alarm types, e.g. {@code device_down}; see Mist's alarm definitions for the full list. */
    public AlarmSearch type(String... types) {
        return param("type", List.of(types));
    }

    /** {@code false} for open (unacknowledged) alarms only, {@code true} for acknowledged only. */
    public AlarmSearch acked(Boolean acked) {
        return param("acked", acked);
    }

    public AlarmSearch ackAdminName(String... names) {
        return param("ack_admin_name", List.of(names));
    }

    /** Sort field; prefix with {@code -} for descending, e.g. {@code -timestamp}. */
    public AlarmSearch sort(String sort) {
        return param("sort", sort);
    }
}
