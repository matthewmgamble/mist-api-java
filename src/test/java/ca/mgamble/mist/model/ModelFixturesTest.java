package ca.mgamble.mist.model;

import ca.mgamble.mist.MistClient;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Deserialises the example responses from the Mist Postman collection (see tools/extract_fixtures.py).
 * A plain ObjectMapper is used on purpose: models must not depend on the client's mapper configuration.
 */
class ModelFixturesTest {

    private final ObjectMapper mapper = new ObjectMapper();

    private JsonNode fixture(String operationId) throws IOException {
        return mapper.readTree(Files.readString(Path.of("src/test/resources/fixtures", operationId + ".json")));
    }

    private <T> T read(String operationId, Class<T> type) throws IOException {
        return mapper.treeToValue(fixture(operationId), type);
    }

    private <T> List<T> readList(JsonNode array, Class<T> type) {
        return mapper.convertValue(array, mapper.getTypeFactory().constructCollectionType(List.class, type));
    }

    @Test
    void self() throws IOException {
        SelfAccount self = read("getSelf", SelfAccount.class);
        assertEquals("user@example.com", self.email());
        assertEquals(1, self.privileges().size());
        Privilege p = self.privileges().get(0);
        assertEquals("org", p.scope());
        assertEquals("admin", p.role());
        assertEquals(List.of("b069b358-4c97-5319-1f8c-7c5ca64d6ab1"), self.orgIds());
        assertEquals(1656353525L, self.extra("password_modified_time").asLong());
    }

    @Test
    void apiUsage() throws IOException {
        ApiUsage usage = read("getSelfApiUsage", ApiUsage.class);
        assertEquals(5000, usage.requestLimit());
        assertEquals(4995, usage.remaining());
    }

    @Test
    void orgAndOrgStats() throws IOException {
        Org org = read("getOrg", Org.class);
        assertEquals(1440, org.sessionExpiry());
        assertTrue(org.allowMist());
        OrgStats stats = read("getOrgStats", OrgStats.class);
        assertEquals(0, stats.numSites());
        assertTrue(stats.extra("sle").isArray());
    }

    @Test
    void sites() throws IOException {
        List<Site> sites = readList(fixture("listOrgSites"), Site.class);
        Site site = sites.get(0);
        assertEquals("Mist Office", site.name());
        assertEquals("US", site.countryCode());
        assertEquals("America/Los_Angeles", site.timezone());
        assertEquals(37.295833, site.latlng().lat(), 1e-9);
        assertEquals("16bdf952-ade2-4491-80b0-85ce506c760b", site.aptemplateId());
        assertEquals(site.name(), read("getSiteInfo", Site.class).name());
    }

    @Test
    void siteStats() throws IOException {
        SiteStats s = readList(fixture("listOrgSiteStats"), SiteStats.class).get(0);
        assertEquals("Live-Demo", s.name());
        assertEquals(17, s.numAp());
        assertEquals(3, s.disconnectedAps());
        assertEquals(1, s.disconnectedSwitches());
        assertEquals(0, s.disconnectedGateways());
        assertFalse(s.isFullyConnected());
        assertEquals(960, s.tzoffset());
        // camelCase keys Mist mixes in are still reachable
        assertTrue(s.extra("analyticEnabled").asBoolean());
        assertEquals(0, read("getSiteStats", SiteStats.class).numAp());
    }

    @Test
    void deviceStats() throws IOException {
        for (String op : List.of("listOrgDevicesStats", "listSiteDevicesStats")) {
            DeviceStats d = readList(fixture(op), DeviceStats.class).get(0);
            assertEquals("5c5b35d0077b", d.mac());
            assertEquals(DeviceType.AP, d.deviceType());
            assertTrue(d.isConnected());
            assertEquals(8400, d.powerBudget());
            assertEquals("DC Input", d.powerSrc());
            assertFalse(d.isPowerConstrained());
            assertEquals("66.129.234.28", d.extIp());
            RadioStat radio = d.radioStat();
            assertEquals(11, radio.band24().channel());
            assertEquals(-80, radio.band24().noiseFloor());
            assertEquals(17, radio.band24().utilRxOtherBss());
            assertEquals(40, radio.band5().bandwidth());
            assertNull(radio.band6());
            assertEquals(53, d.extra("env_stat").path("cpu_temp").asInt());
        }
        DeviceStats single = read("getSiteDeviceStats", DeviceStats.class);
        assertEquals(DeviceType.AP, single.deviceType());
    }

    @Test
    void deviceSearch() throws IOException {
        for (String op : List.of("searchSiteDevices", "searchOrgDevices")) {
            DeviceSearchResult d = readList(fixture(op).path("results"), DeviceSearchResult.class).get(0);
            assertEquals("AP41", d.model());
            assertEquals(4, d.hostname().size());
            assertEquals("ME-DC-1-ACC-SW", d.lldpSystemName());
            assertEquals(1596588619L, d.timestamp()); // fractional epoch seconds are truncated
            assertEquals(2, d.extra("wlans").size());
        }
        // hostname is sometimes a plain string
        DeviceSearchResult single = mapper.readValue("{\"hostname\":\"sw1\"}", DeviceSearchResult.class);
        assertEquals(List.of("sw1"), single.hostname());
    }

    @Test
    void alarms() throws IOException {
        for (String op : List.of("searchOrgAlarms", "searchSiteAlarms")) {
            Alarm a = readList(fixture(op).path("results"), Alarm.class).get(0);
            assertEquals("rogue_client", a.type());
            assertEquals(AlarmSeverity.CRITICAL, a.severityLevel());
            assertTrue(a.isAcked());
            assertEquals("Joe", a.ackAdminName());
            assertEquals(150L, a.timestamp());
            assertEquals(List.of("ffeeddccbbaa", "ffeeddccbbab"), a.aps());
            assertEquals("open", a.status());
        }
    }

    @Test
    void inventoryAndVersions() throws IOException {
        InventoryItem item = readList(fixture("getOrgInventory"), InventoryItem.class).get(0);
        assertEquals("FXLH2015150025", item.serial());
        assertTrue(item.isConnected());
        assertEquals(DeviceType.AP, item.deviceType());

        List<DeviceVersion> versions = mapper.convertValue(fixture("listOrgAvailableDeviceVersions"),
                new TypeReference<>() {
                });
        assertEquals("suggested", versions.get(0).tag());
        assertNull(versions.get(1).tag());
    }

    @Test
    void unknownFieldsRoundTrip() throws IOException {
        JsonNode original = fixture("listOrgDevicesStats").get(0);
        DeviceStats device = mapper.treeToValue(original, DeviceStats.class);
        JsonNode written = MistClient.defaultObjectMapper().valueToTree(device);
        // every field from the API survives a read/write cycle, mapped or not
        original.fieldNames().forEachRemaining(field ->
                assertTrue(written.has(field), () -> "lost field " + field));
        assertFalse(written.has("extra"));
        assertFalse(written.has("connected"));
    }
}
