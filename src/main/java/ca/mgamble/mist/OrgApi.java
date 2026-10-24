package ca.mgamble.mist;

import ca.mgamble.mist.model.Alarm;
import ca.mgamble.mist.model.DeviceSearchResult;
import ca.mgamble.mist.model.DeviceStats;
import ca.mgamble.mist.model.DeviceType;
import ca.mgamble.mist.model.DeviceVersion;
import ca.mgamble.mist.model.InventoryItem;
import ca.mgamble.mist.model.Org;
import ca.mgamble.mist.model.OrgStats;
import ca.mgamble.mist.model.Site;
import ca.mgamble.mist.model.SiteStats;
import ca.mgamble.mist.query.AlarmSearch;
import ca.mgamble.mist.query.DeviceSearch;
import ca.mgamble.mist.query.DeviceStatsQuery;
import ca.mgamble.mist.query.InventoryQuery;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.Collection;
import java.util.List;

/**
 * Organization-scoped endpoints. Obtain via {@link MistClient#org(String)}.
 * Method Javadoc names the Mist operation each call maps to.
 */
public final class OrgApi {

    private final Requests requests;
    private final String orgId;
    private final String base;

    OrgApi(Requests requests, String orgId) {
        this.requests = requests;
        this.orgId = orgId;
        this.base = Requests.path("/api/v1/orgs/{org_id}", orgId);
    }

    public String orgId() {
        return orgId;
    }

    /** {@code getOrg}. */
    public Org get() {
        return requests.get(base, Org.class);
    }

    /** {@code getOrgStats}: org-wide site, device and inventory counts. */
    public OrgStats stats() {
        return requests.get(base + "/stats", OrgStats.class);
    }

    /** {@code listOrgSites}. */
    public Paged<Site> listSites() {
        return requests.list(base + "/sites", null, requests.type(Site.class));
    }

    /** {@code listOrgSiteStats}: every site with its AP / switch / gateway connection counts. */
    public Paged<SiteStats> listSiteStats() {
        return requests.list(base + "/stats/sites", null, requests.type(SiteStats.class));
    }

    /** {@code listOrgDevicesStats} for every device type. */
    public Paged<DeviceStats> listDeviceStats() {
        return listDeviceStats(new DeviceStatsQuery());
    }

    /** {@code listOrgDevicesStats}. */
    public Paged<DeviceStats> listDeviceStats(DeviceStatsQuery query) {
        return requests.list(base + "/stats/devices", query.toQuery(), requests.type(DeviceStats.class));
    }

    /** {@code searchOrgDevices}. */
    public Paged<DeviceSearchResult> searchDevices(DeviceSearch search) {
        return requests.search(base + "/devices/search", search.toQuery(), requests.type(DeviceSearchResult.class));
    }

    /** {@code getOrgInventory}: every claimed device. */
    public Paged<InventoryItem> listInventory() {
        return listInventory(new InventoryQuery());
    }

    /** {@code getOrgInventory}. */
    public Paged<InventoryItem> listInventory(InventoryQuery query) {
        return requests.list(base + "/inventory", query.toQuery(), requests.type(InventoryItem.class));
    }

    /** {@code searchOrgAlarms}. */
    public Paged<Alarm> searchAlarms(AlarmSearch search) {
        return requests.search(base + "/alarms/search", search.toQuery(), requests.type(Alarm.class));
    }

    /** {@code ackOrgAlarm}. */
    public void ackAlarm(String alarmId, String note) {
        requests.json("POST", Requests.path(base + "/alarms/{alarm_id}/ack", alarmId), null, Alarms.noteBody(note));
    }

    /** {@code ackOrgMultipleAlarms}. */
    public void ackAlarms(Collection<String> alarmIds, String note) {
        requests.json("POST", base + "/alarms/ack", null, Alarms.idsBody(alarmIds, note));
    }

    /** {@code unackOrgMultipleAlarms}. */
    public void unackAlarms(Collection<String> alarmIds, String note) {
        requests.json("POST", base + "/alarms/unack", null, Alarms.idsBody(alarmIds, note));
    }

    /** {@code listOrgAvailableDeviceVersions}: firmware versions available for a device type. */
    public List<DeviceVersion> listAvailableDeviceVersions(DeviceType type) {
        return listAvailableDeviceVersions(type, null);
    }

    /** {@code listOrgAvailableDeviceVersions}, optionally restricted to one model. */
    public List<DeviceVersion> listAvailableDeviceVersions(DeviceType type, String model) {
        return requests.get(base + "/devices/versions", Query.of("type", type, "model", model),
                requests.type(new TypeReference<List<DeviceVersion>>() {
                }));
    }

    /** A {@link SiteApi} for one of this org's sites. */
    public SiteApi site(String siteId) {
        return new SiteApi(requests, siteId);
    }
}
