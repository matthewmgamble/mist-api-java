package ca.mgamble.mist;

import ca.mgamble.mist.model.AlarmSeverity;
import ca.mgamble.mist.model.DeviceType;
import ca.mgamble.mist.query.AlarmSearch;
import ca.mgamble.mist.query.DeviceStatsQuery;
import ca.mgamble.mist.query.InventoryQuery;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QueryAndIdsTest {

    @Test
    void queryConvertsValuesToWireForm() {
        Query q = Query.of(
                "type", DeviceType.SWITCH,
                "severity", List.of(AlarmSeverity.CRITICAL, AlarmSeverity.INFO),
                "start", Instant.ofEpochSecond(1700000000),
                "acked", false,
                "dropped", null,
                "text", "a b&c");
        assertEquals("type=switch&severity=critical,info&start=1700000000&acked=false&text=a%20b%26c",
                q.encode().replace("%2C", ","));
        assertEquals("/x?type=switch", Query.of("type", "switch").appendTo("/x"));
        assertEquals("/x?a=1&type=switch", Query.of("type", "switch").appendTo("/x?a=1"));
        assertTrue(Query.of("k", List.of()).isEmpty());
    }

    @Test
    void buildersProduceIndependentCopies() {
        AlarmSearch search = new AlarmSearch().siteIds("s1", "s2").type("device_down").acked(false);
        Query copy = search.toQuery();
        copy.set("acked", true);
        assertEquals("false", search.toQuery().get("acked"));
        assertEquals("s1,s2", search.toQuery().get("site_id"));
        assertEquals("all", new DeviceStatsQuery().toQuery().get("type"));
        assertEquals("ap,gateway", new InventoryQuery().type(DeviceType.AP, DeviceType.GATEWAY).toQuery().get("type"));
        assertEquals("x", new AlarmSearch().param("component", "x").toQuery().get("component"));
        assertThrows(IllegalArgumentException.class, () -> new AlarmSearch().pageSize(5000));
    }

    @Test
    void durationsUseLargestWholeMistUnit() {
        assertEquals("2w", new AlarmSearch().duration(Duration.ofDays(14)).toQuery().get("duration"));
        assertEquals("1d", new AlarmSearch().duration(Duration.ofHours(24)).toQuery().get("duration"));
        assertEquals("36h", new AlarmSearch().duration(Duration.ofHours(36)).toQuery().get("duration"));
        assertEquals("90m", new AlarmSearch().duration(Duration.ofMinutes(90)).toQuery().get("duration"));
        assertEquals("1m", new AlarmSearch().duration(Duration.ofSeconds(5)).toQuery().get("duration"));
    }

    @Test
    void pathTemplatesEncodeSegments() {
        assertEquals("/api/v1/sites/a%2Fb/devices/c", RawApi.path("/api/v1/sites/{site_id}/devices/{device_id}", "a/b", "c"));
        assertThrows(IllegalArgumentException.class, () -> RawApi.path("/api/v1/sites/{site_id}"));
        assertThrows(IllegalArgumentException.class, () -> RawApi.path("/api/v1/sites/{site_id}", "a", "b"));
        assertThrows(IllegalArgumentException.class, () -> RawApi.path("/api/v1/sites/{site_id}", ""));
    }

    @Test
    void deviceIdsEmbedMacs() {
        assertEquals("00000000-0000-0000-1000-5c5b35d0077b", MistIds.deviceIdFromMac("5C:5B:35:D0:07:7B"));
        assertEquals("5c5b35d0077b", MistIds.macFromDeviceId("00000000-0000-0000-1000-5c5b35d0077b"));
        assertThrows(IllegalArgumentException.class, () -> MistIds.deviceIdFromMac("not-a-mac"));
    }

    @Test
    void regionsResolveFromNameOrHost() {
        assertEquals(MistRegion.GLOBAL_04, MistRegion.from("global_04"));
        assertEquals(MistRegion.APAC_01, MistRegion.from("api.ac5.mist.com"));
        assertEquals(MistRegion.EMEA_01, MistRegion.from("https://api.eu.mist.com"));
        assertThrows(IllegalArgumentException.class, () -> MistRegion.from("api.example.com"));
    }
}
