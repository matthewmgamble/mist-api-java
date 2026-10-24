package ca.mgamble.mist;

import ca.mgamble.mist.FakeMist.Response;
import ca.mgamble.mist.cache.InMemoryResponseCache;
import ca.mgamble.mist.model.Alarm;
import ca.mgamble.mist.model.AlarmSeverity;
import ca.mgamble.mist.model.DeviceSearchResult;
import ca.mgamble.mist.model.DeviceStats;
import ca.mgamble.mist.model.DeviceType;
import ca.mgamble.mist.model.DeviceVersion;
import ca.mgamble.mist.model.Site;
import ca.mgamble.mist.query.AlarmSearch;
import ca.mgamble.mist.query.DeviceSearch;
import ca.mgamble.mist.query.DeviceStatsQuery;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MistClientTest {

    private static final String ORG = "8aaf4b40-0000-4000-8000-000000000001";
    private static final String SITE = "8aaf4b40-0000-4000-8000-000000000002";

    private final FakeMist mist = new FakeMist();
    private final List<Duration> sleeps = new ArrayList<>();
    private final ObjectMapper json = new ObjectMapper();

    @AfterEach
    void stop() {
        mist.close();
    }

    private static String sites(int from, int to) {
        return IntStream.range(from, to)
                .mapToObj(i -> "{\"id\":\"site-" + i + "\",\"name\":\"Site " + i + "\"}")
                .collect(Collectors.joining(",", "[", "]"));
    }

    @Test
    void sendsTokenAndUserAgent() {
        mist.respond(Response.json(FakeMist.fixture("getSelf")));
        try (MistClient client = mist.client(sleeps)) {
            assertEquals("user@example.com", client.self().get().email());
        }
        FakeMist.Request r = mist.lastRequest();
        assertEquals("GET", r.method());
        assertEquals("/api/v1/self", r.path());
        assertEquals("Token test-token", r.header("Authorization"));
        assertTrue(r.header("User-Agent").startsWith("mist-api-java/"));
    }

    @Test
    void pageBasedListFollowsXPageTotal() {
        mist.handle(r -> {
            int page = Integer.parseInt(r.query().get("page"));
            int from = (page - 1) * 100;
            return Response.json(sites(from, Math.min(from + 100, 250))).withHeader("X-Page-Total", "250");
        });
        try (MistClient client = mist.client(sleeps)) {
            List<Site> sites = client.org(ORG).listSites().toList();
            assertEquals(250, sites.size());
            assertEquals("site-249", sites.get(249).id());
        }
        assertEquals(3, mist.requests().size());
        assertEquals("/api/v1/orgs/" + ORG + "/sites", mist.requests().get(0).path());
        assertEquals(Map.of("limit", "100", "page", "3"), mist.lastRequest().query());
    }

    @Test
    void pageBasedListWithoutTotalStopsOnShortPage() {
        mist.respond(Response.json(sites(0, 100)), Response.json(sites(100, 130)));
        try (MistClient client = mist.client(sleeps)) {
            assertEquals(130, client.org(ORG).listSites().toList().size());
        }
        assertEquals(2, mist.requests().size());
    }

    @Test
    void pageBasedListStopsIfServerIgnoresPage() {
        mist.respond(Response.json(sites(0, 100)));
        try (MistClient client = mist.client(sleeps)) {
            assertEquals(100, client.org(ORG).listSites().toList().size());
        }
        assertEquals(2, mist.requests().size());
    }

    @Test
    void pagesAreFetchedLazily() {
        mist.handle(r -> Response.json(sites(0, 100)).withHeader("X-Page-Total", "1000"));
        try (MistClient client = mist.client(sleeps)) {
            assertEquals(5, client.org(ORG).listSites().stream().limit(5).count());
        }
        assertEquals(1, mist.requests().size());
    }

    @Test
    void searchFollowsNextCursorAndNeverSendsPage() {
        String next = "/api/v1/orgs/" + ORG + "/alarms/search?limit=2&search_after=%5B1711031354000%5D";
        mist.handle(r -> r.query().containsKey("search_after")
                ? Response.json("{\"results\":[{\"id\":\"a3\"}],\"total\":3}")
                : Response.json("{\"results\":[{\"id\":\"a1\"},{\"id\":\"a2\"}],\"total\":3,\"next\":\"" + next + "\"}"));

        List<Alarm> alarms;
        try (MistClient client = mist.clientBuilder(sleeps).pageSize(2).build()) {
            alarms = client.org(ORG).searchAlarms(new AlarmSearch()
                    .acked(false)
                    .severity(AlarmSeverity.CRITICAL, AlarmSeverity.WARN)
                    .duration("1d")).toList();
        }
        assertEquals(List.of("a1", "a2", "a3"), alarms.stream().map(Alarm::id).toList());
        assertEquals(2, mist.requests().size());
        Map<String, String> first = mist.requests().get(0).query();
        assertEquals("false", first.get("acked"));
        assertEquals("critical,warn", first.get("severity"));
        assertEquals("1d", first.get("duration"));
        assertEquals("2", first.get("limit"));
        assertFalse(first.containsKey("page"));
        assertEquals("[1711031354000]", mist.lastRequest().query().get("search_after"));
    }

    @Test
    void searchStopsWhenNextRepeats() {
        String next = "/api/v1/sites/" + SITE + "/devices/search?search_after=x";
        mist.respond(Response.json("{\"results\":[{\"mac\":\"aabbccddeeff\"}],\"next\":\"" + next + "\"}"));
        try (MistClient client = mist.client(sleeps)) {
            List<DeviceSearchResult> devices = client.site(SITE).searchDevices(new DeviceSearch()).toList();
            assertEquals(2, devices.size());
        }
        assertEquals(2, mist.requests().size());
    }

    @Test
    void nextLinkToAnotherHostIsRejected() {
        mist.respond(Response.json("{\"results\":[{\"id\":\"a1\"}],\"next\":\"https://evil.example/steal\"}"));
        try (MistClient client = mist.client(sleeps)) {
            Paged<Alarm> alarms = client.org(ORG).searchAlarms(new AlarmSearch());
            assertThrows(IllegalArgumentException.class, alarms::toList);
        }
    }

    @Test
    void deviceStatsDefaultsToAllTypes() {
        mist.respond(Response.json(FakeMist.fixture("listOrgDevicesStats")));
        try (MistClient client = mist.client(sleeps)) {
            List<DeviceStats> devices = client.org(ORG).listDeviceStats().toList();
            assertEquals(1, devices.size());
            client.site(SITE).listDeviceStats(new DeviceStatsQuery().type(DeviceType.SWITCH).connectedOnly()).toList();
        }
        assertEquals("all", mist.requests().get(0).query().get("type"));
        assertEquals("switch", mist.lastRequest().query().get("type"));
        assertEquals("connected", mist.lastRequest().query().get("status"));
        assertEquals("/api/v1/sites/" + SITE + "/stats/devices", mist.lastRequest().path());
    }

    @Test
    void availableVersionsIsASingleUnpagedCall() {
        mist.respond(Response.json(FakeMist.fixture("listOrgAvailableDeviceVersions")));
        try (MistClient client = mist.client(sleeps)) {
            List<DeviceVersion> versions = client.org(ORG).listAvailableDeviceVersions(DeviceType.AP);
            assertEquals(2, versions.size());
        }
        assertEquals(Map.of("type", "ap"), mist.lastRequest().query());
    }

    @Test
    void ackAlarmsPostsIdsAndNote() throws Exception {
        mist.respond(Response.json(""));
        try (MistClient client = mist.client(sleeps)) {
            client.org(ORG).ackAlarms(List.of("a1", "a2"), "maintenance window");
            client.site(SITE).ackAlarm("a3", null);
        }
        FakeMist.Request multi = mist.requests().get(0);
        assertEquals("POST", multi.method());
        assertEquals("/api/v1/orgs/" + ORG + "/alarms/ack", multi.path());
        assertEquals(json.readTree("{\"note\":\"maintenance window\",\"alarm_ids\":[\"a1\",\"a2\"]}"),
                json.readTree(multi.body()));
        assertEquals("application/json", multi.header("Content-Type"));
        assertEquals("/api/v1/sites/" + SITE + "/alarms/a3/ack", mist.lastRequest().path());
        assertEquals("{}", mist.lastRequest().body());
    }

    @Test
    void retriesRateLimitHonouringRetryAfter() {
        mist.respond(Response.status(429, "{\"detail\":\"Too Many Request\"}").withHeader("Retry-After", "7"),
                Response.json(FakeMist.fixture("getSelf")));
        try (MistClient client = mist.client(sleeps)) {
            client.self().get();
        }
        assertEquals(List.of(Duration.ofSeconds(7)), sleeps);
        assertEquals(2, mist.requests().size());
    }

    @Test
    void failsFastWhenRetryAfterIsTooLong() {
        mist.respond(Response.status(429, "{\"detail\":\"5000 API Calls per hour threshold\"}")
                .withHeader("Retry-After", "1800"));
        try (MistClient client = mist.client(sleeps)) {
            MistApiException.RateLimitException e =
                    assertThrows(MistApiException.RateLimitException.class, () -> client.self().get());
            assertEquals(1800, e.retryAfterSeconds());
            assertTrue(e.getMessage().contains("5000 API Calls per hour threshold"));
        }
        assertTrue(sleeps.isEmpty());
    }

    @Test
    void retriesServerErrorsForGetWithBackoff() {
        mist.respond(Response.status(503, ""), Response.status(502, ""), Response.json(FakeMist.fixture("getSelf")));
        try (MistClient client = mist.client(sleeps)) {
            client.self().get();
        }
        assertEquals(List.of(Duration.ofSeconds(1), Duration.ofSeconds(2)), sleeps);
    }

    @Test
    void doesNotRetryPostOnServerError() {
        mist.respond(Response.status(500, "{\"detail\":\"boom\"}"));
        try (MistClient client = mist.client(sleeps)) {
            MistApiException e = assertThrows(MistApiException.class,
                    () -> client.org(ORG).ackAlarm("a1", "note"));
            assertEquals(500, e.statusCode());
        }
        assertEquals(1, mist.requests().size());
    }

    @Test
    void givesUpAfterMaxRetries() {
        mist.respond(Response.status(503, ""));
        try (MistClient client = mist.client(sleeps)) {
            assertThrows(MistApiException.class, () -> client.self().get());
        }
        assertEquals(4, mist.requests().size());
        assertEquals(3, sleeps.size());
    }

    @Test
    void mapsErrorStatusesToExceptions() {
        mist.respond(Response.status(404, "{\"detail\":\"site not found\"}"),
                Response.status(401, "{\"detail\":\"Authentication credentials were not provided.\"}"));
        try (MistClient client = mist.client(sleeps)) {
            MistApiException.NotFoundException notFound =
                    assertThrows(MistApiException.NotFoundException.class, () -> client.site(SITE).get());
            assertEquals("GET /api/v1/sites/" + SITE + " failed with HTTP 404: site not found", notFound.getMessage());

            MistApiException.AuthException auth =
                    assertThrows(MistApiException.AuthException.class, () -> client.self().get());
            assertEquals(401, auth.statusCode());
            assertTrue(auth.getMessage().contains("region"));
            assertFalse(client.testConnection());
        }
    }

    @Test
    void cachesGetResponsesPerToken() {
        mist.respond(Response.json(FakeMist.fixture("getSelf")));
        InMemoryResponseCache cache = new InMemoryResponseCache(Duration.ofMinutes(5));
        try (MistClient a = mist.clientBuilder(sleeps).cache(cache).build();
             MistClient b = mist.clientBuilder(sleeps).apiToken("other-token").cache(cache).build()) {
            a.self().get();
            a.self().get();
            assertEquals(1, mist.requests().size());
            b.self().get();
            assertEquals(2, mist.requests().size());
            a.invalidateCache();
            a.self().get();
            assertEquals(3, mist.requests().size());
        }
    }

    @Test
    void rawApiReachesAnyEndpoint() {
        mist.handle(r -> r.method().equals("GET")
                ? Response.json("[{\"id\":\"w1\",\"ssid\":\"Corp\"}]")
                : Response.json("{\"id\":\"wh1\"}"));
        try (MistClient client = mist.client(sleeps)) {
            String wlans = RawApi.path("/api/v1/orgs/{org_id}/wlans", ORG);
            List<JsonNode> list = client.raw().list(wlans).toList();
            assertEquals("Corp", list.get(0).path("ssid").asText());

            JsonNode created = client.raw().post(RawApi.path("/api/v1/orgs/{org_id}/webhooks", ORG),
                    Map.of("name", "hook", "topics", List.of("alarms")));
            assertEquals("wh1", created.path("id").asText());
        }
        assertEquals("/api/v1/orgs/" + ORG + "/webhooks", mist.lastRequest().path());
    }

    @Test
    void emptyResponseBodyIsMissingNode() {
        mist.respond(Response.json(""));
        try (MistClient client = mist.client(sleeps)) {
            assertTrue(client.raw().delete("/api/v1/sites/" + SITE + "/webhooks/x").isMissingNode());
        }
    }

    @Test
    void builderRequiresToken() {
        assertThrows(IllegalStateException.class, () -> MistClient.builder().build());
    }

    @Test
    void fromEnvironmentReadsTokenAndHost() {
        try (MistClient client = MistClient.fromEnvironment(Map.of("MIST_API_TOKEN", "t", "MIST_HOST", "api.gc2.mist.com"))) {
            assertEquals(MistRegion.GLOBAL_04.baseUri(), client.baseUri());
        }
        try (MistClient client = MistClient.fromEnvironment(Map.of("MIST_APITOKEN", "t", "MIST_HOST", "EMEA_01"))) {
            assertEquals("https://api.eu.mist.com", client.baseUri().toString());
        }
        try (MistClient client = MistClient.fromEnvironment(Map.of("MIST_API_TOKEN", "t"))) {
            assertEquals("https://api.mist.com", client.baseUri().toString());
        }
        assertThrows(IllegalStateException.class, () -> MistClient.fromEnvironment(Map.of()));
    }

    @Test
    void absentOptionalFieldsAreNull() {
        mist.respond(Response.json("{\"id\":\"s\"}"));
        try (MistClient client = mist.client(sleeps)) {
            Site site = client.site(SITE).get();
            assertNull(site.name());
            assertTrue(site.sitegroupIds().isEmpty());
            assertTrue(site.extra().isEmpty());
        }
    }
}
