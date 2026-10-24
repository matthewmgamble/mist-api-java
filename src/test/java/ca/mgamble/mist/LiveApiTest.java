package ca.mgamble.mist;

import ca.mgamble.mist.model.SelfAccount;
import ca.mgamble.mist.model.Site;
import ca.mgamble.mist.query.AlarmSearch;
import ca.mgamble.mist.query.DeviceSearch;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Read-only smoke test against a real Mist org. Opt in with:
 * <pre>
 * MIST_LIVE_TEST=true MIST_API_TOKEN=... MIST_HOST=api.gc2.mist.com [MIST_ORG_ID=...] mvn test -Dtest=LiveApiTest
 * </pre>
 * Makes roughly ten GET requests; nothing is modified.
 */
@EnabledIfEnvironmentVariable(named = "MIST_LIVE_TEST", matches = "true")
class LiveApiTest {

    private static MistClient client;
    private static OrgApi org;

    @BeforeAll
    static void connect() {
        client = MistClient.fromEnvironment();
        SelfAccount self = client.self().get();
        String orgId = Optional.ofNullable(System.getenv("MIST_ORG_ID"))
                .orElseGet(() -> self.orgIds().stream().findFirst()
                        .orElseThrow(() -> new IllegalStateException("token has no org privileges; set MIST_ORG_ID")));
        org = client.org(orgId);
    }

    @AfterAll
    static void close() {
        client.close();
    }

    @Test
    void readsCoreEndpoints() {
        assertNotNull(org.get().name());
        assertTrue(org.stats().numSites() >= 0);
        assertTrue(client.self().apiUsage().requestLimit() > 0);

        Optional<Site> site = org.listSites().first();
        assertTrue(site.isPresent(), "org has no sites");
        SiteApi siteApi = client.site(site.get().id());
        assertNotNull(siteApi.get().id());
        assertNotNull(siteApi.stats().id());
        siteApi.listDeviceStats().first().ifPresent(d -> assertNotNull(d.mac()));

        org.listSiteStats().first().ifPresent(s -> assertNotNull(s.id()));
        org.listDeviceStats().first().ifPresent(d -> assertFalse(d.type().isBlank()));
        org.listInventory().first().ifPresent(i -> assertNotNull(i.mac()));
        org.searchDevices(new DeviceSearch().pageSize(5)).first().ifPresent(d -> assertNotNull(d.mac()));
        org.searchAlarms(new AlarmSearch().duration("7d").pageSize(5)).first().ifPresent(a -> assertNotNull(a.type()));
    }
}
