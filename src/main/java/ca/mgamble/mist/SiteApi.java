package ca.mgamble.mist;

import ca.mgamble.mist.model.Alarm;
import ca.mgamble.mist.model.DeviceSearchResult;
import ca.mgamble.mist.model.DeviceStats;
import ca.mgamble.mist.model.DeviceType;
import ca.mgamble.mist.model.DeviceVersion;
import ca.mgamble.mist.model.Site;
import ca.mgamble.mist.model.SiteStats;
import ca.mgamble.mist.query.AlarmSearch;
import ca.mgamble.mist.query.DeviceSearch;
import ca.mgamble.mist.query.DeviceStatsQuery;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.Collection;
import java.util.List;

/**
 * Site-scoped endpoints. Obtain via {@link MistClient#site(String)}.
 * Method Javadoc names the Mist operation each call maps to.
 */
public final class SiteApi {

    private final Requests requests;
    private final String siteId;
    private final String base;

    SiteApi(Requests requests, String siteId) {
        this.requests = requests;
        this.siteId = siteId;
        this.base = Requests.path("/api/v1/sites/{site_id}", siteId);
    }

    public String siteId() {
        return siteId;
    }

    /** {@code getSiteInfo}. */
    public Site get() {
        return requests.get(base, Site.class);
    }

    /** {@code getSiteStats}: the site with its device and client counts. */
    public SiteStats stats() {
        return requests.get(base + "/stats", SiteStats.class);
    }

    /**
     * {@code listSiteDevicesStats} for every device type. Site-level stats include fields the org-level
     * endpoint omits, such as {@code power_budget} and {@code power_src}.
     */
    public Paged<DeviceStats> listDeviceStats() {
        return listDeviceStats(new DeviceStatsQuery());
    }

    /** {@code listSiteDevicesStats}; only the query's {@code type} and {@code status} apply at site level. */
    public Paged<DeviceStats> listDeviceStats(DeviceStatsQuery query) {
        return requests.list(base + "/stats/devices", query.toQuery(), requests.type(DeviceStats.class));
    }

    /**
     * {@code getSiteDeviceStats}.
     *
     * @param deviceId device UUID; see {@link MistIds#deviceIdFromMac(String)}
     */
    public DeviceStats getDeviceStats(String deviceId) {
        return requests.get(Requests.path(base + "/stats/devices/{device_id}", deviceId), DeviceStats.class);
    }

    /** {@code searchSiteDevices}. */
    public Paged<DeviceSearchResult> searchDevices(DeviceSearch search) {
        return requests.search(base + "/devices/search", search.toQuery(), requests.type(DeviceSearchResult.class));
    }

    /** {@code searchSiteAlarms}. */
    public Paged<Alarm> searchAlarms(AlarmSearch search) {
        return requests.search(base + "/alarms/search", search.toQuery(), requests.type(Alarm.class));
    }

    /** {@code ackSiteAlarm}. */
    public void ackAlarm(String alarmId, String note) {
        requests.json("POST", Requests.path(base + "/alarms/{alarm_id}/ack", alarmId), null, Alarms.noteBody(note));
    }

    /** {@code unackSiteAlarm}. */
    public void unackAlarm(String alarmId, String note) {
        requests.json("POST", Requests.path(base + "/alarms/{alarm_id}/unack", alarmId), null, Alarms.noteBody(note));
    }

    /** {@code AckSiteMultipleAlarms}. */
    public void ackAlarms(Collection<String> alarmIds, String note) {
        requests.json("POST", base + "/alarms/ack", null, Alarms.idsBody(alarmIds, note));
    }

    /** {@code unackSiteMultipleAlarms}. */
    public void unackAlarms(Collection<String> alarmIds, String note) {
        requests.json("POST", base + "/alarms/unack", null, Alarms.idsBody(alarmIds, note));
    }

    /** {@code listSiteAvailableDeviceVersions}. */
    public List<DeviceVersion> listAvailableDeviceVersions(DeviceType type) {
        return requests.get(base + "/devices/versions", Query.of("type", type),
                requests.type(new TypeReference<List<DeviceVersion>>() {
                }));
    }
}
