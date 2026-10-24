# mist-api-java

A lightweight Java 21 client for the [Juniper Mist](https://www.juniper.net/documentation/us/en/software/mist/api/http/getting-started/how-to-get-started) cloud API.

- Typed models and methods for the most-used endpoints: orgs, sites, site and device stats, device search, inventory, alarms, and firmware versions
- A raw API that reaches every other endpoint (the Mist API has 1,000+) with the same auth, retries and pagination
- Handles both Mist pagination styles, lazily
- Retries on rate limits (`429`, with `Retry-After`) and on server errors, with typed exceptions
- Optional response cache, pluggable
- Runtime dependencies: only `jackson-databind` and `slf4j-api`

Built against Mist API **2607.1.1** (August 2026).

> Not affiliated with or endorsed by Juniper Networks. "Juniper" and "Mist" are trademarks of Juniper Networks, Inc.

## Install

```xml
<dependency>
    <groupId>ca.mgamble</groupId>
    <artifactId>mist-api-java</artifactId>
    <version>0.1.0</version>
</dependency>
```

Until it is published, run `mvn install` and use `0.1.0-SNAPSHOT`.

## Quick start

```java
try (MistClient mist = MistClient.builder()
        .apiToken(System.getenv("MIST_API_TOKEN"))
        .region(MistRegion.GLOBAL_04)          // api.gc2.mist.com
        .build()) {

    OrgApi org = mist.org(orgId);

    for (SiteStats site : org.listSiteStats()) {
        System.out.printf("%s: %d/%d APs connected%n", site.name(), site.numApConnected(), site.numAp());
    }

    List<Alarm> open = org.searchAlarms(new AlarmSearch()
            .acked(false)
            .severity(AlarmSeverity.CRITICAL)
            .duration("1d")).toList();
}
```

You can also configure the client from the environment with `MistClient.fromEnvironment()`. It reads
`MIST_API_TOKEN` (or `MIST_APITOKEN`), plus an optional `MIST_HOST`, which takes a region name, a host or a URL.

To find the orgs a token can access, call `mist.self().get().orgIds()`.

### Regions

A token only works in the cloud where its org lives. Calling the wrong region returns `401`. The default is
`GLOBAL_01` (`api.mist.com`).

| Region | Host | Region | Host |
|---|---|---|---|
| `GLOBAL_01` | api.mist.com | `EMEA_01` | api.eu.mist.com |
| `GLOBAL_02` | api.gc1.mist.com | `EMEA_02` | api.gc3.mist.com |
| `GLOBAL_03` | api.ac2.mist.com | `EMEA_03` | api.ac6.mist.com |
| `GLOBAL_04` | api.gc2.mist.com | `EMEA_04` | api.gc6.mist.com |
| `GLOBAL_05` | api.gc4.mist.com | `APAC_01` | api.ac5.mist.com |
| | | `APAC_02` | api.gc5.mist.com |
| | | `APAC_03` | api.gc7.mist.com |

For any other host, use `.baseUrl("https://...")`.

## Typed API

| Call | Mist operation |
|---|---|
| `self().get()` / `self().apiUsage()` | `getSelf` / `getSelfApiUsage` |
| `org(id).get()` / `.stats()` | `getOrg` / `getOrgStats` |
| `org(id).listSites()` / `.listSiteStats()` | `listOrgSites` / `listOrgSiteStats` |
| `org(id).listDeviceStats(query)` | `listOrgDevicesStats` |
| `org(id).searchDevices(search)` | `searchOrgDevices` |
| `org(id).listInventory(query)` | `getOrgInventory` |
| `org(id).searchAlarms(search)` / `.ackAlarm(s)` / `.unackAlarms` | `searchOrgAlarms` / `ackOrgAlarm`, `ackOrgMultipleAlarms` / `unackOrgMultipleAlarms` |
| `org(id).listAvailableDeviceVersions(type)` | `listOrgAvailableDeviceVersions` |
| `site(id).get()` / `.stats()` | `getSiteInfo` / `getSiteStats` |
| `site(id).listDeviceStats(query)` / `.getDeviceStats(deviceId)` | `listSiteDevicesStats` / `getSiteDeviceStats` |
| `site(id).searchDevices(search)` | `searchSiteDevices` |
| `site(id).searchAlarms(search)` / ack / unack | `searchSiteAlarms` / `ackSiteAlarm`, … |
| `site(id).listAvailableDeviceVersions(type)` | `listSiteAvailableDeviceVersions` |

The query builders (`AlarmSearch`, `DeviceSearch`, `DeviceStatsQuery`, `InventoryQuery`) have methods for the
common filters. Any other filter goes through `.param("name", value)`.

`DeviceStatsQuery` sends `type=all` by default. If `type` is left out, Mist returns only APs.

### Models keep everything

Models are immutable records that map the commonly used fields. Every other field from the API lands in
`extra()`. Nothing is dropped, and those fields are written back out when you serialize the model:

```java
DeviceStats ap = mist.site(siteId).getDeviceStats(MistIds.deviceIdFromMac("5c:5b:35:d0:07:7b"));
int cpuTemp = ap.extra("env_stat").path("cpu_temp").asInt();
```

## Pagination

List methods return `Paged<T>`, which fetches pages only as you consume them:

```java
org.listSites().toList();                       // every page
org.listDeviceStats().stream().limit(20)...     // only as many pages as needed
org.searchAlarms(search).first();               // one request
for (Site s : org.listSites()) { ... }          // Iterable
```

Mist has two pagination styles, and the client handles both:

- **List endpoints** return an array, take `limit` and `page`, and report the count in the `X-Page-Total` header.
- **`/search` endpoints** return `{results, next}` and paginate with a `search_after` cursor carried in `next`.
  They ignore `page`, so a page loop gets the first page back again and again.

The default page size is 100. Change it with `.pageSize(n)` on the builder or on a query.

## Raw API for everything else

```java
RawApi raw = mist.raw();
Paged<JsonNode> wlans = raw.list(RawApi.path("/api/v1/orgs/{org_id}/wlans", orgId));
Paged<JsonNode> events = raw.search(RawApi.path("/api/v1/sites/{site_id}/devices/events/search", siteId),
        Query.of("duration", "1h"));
JsonNode hook = raw.post(RawApi.path("/api/v1/orgs/{org_id}/webhooks", orgId), Map.of("name", "ops", "topics", List.of("alarms")));
MyRecord typed = raw.get(path, null, MyRecord.class);
```

To find paths and parameters, use the [Mist API reference](https://www.juniper.net/documentation/us/en/software/mist/api/http/api/)
or the Postman collection.

## Errors, retries and rate limits

Every failure throws an unchecked `MistApiException`, which carries `statusCode()`, `method()`, `path()` and
`responseBody()`. Subclasses: `AuthException` (401/403), `NotFoundException` (404), and `RateLimitException`
(429, which also has `retryAfterSeconds()`).

`RetryPolicy.defaults()` works like this:
- **429:** retried up to 3 times, waiting for `Retry-After` when it is 60 seconds or less. A longer wait fails
  fast instead of blocking your thread for the rest of the hour.
- **5xx and I/O errors:** retried with backoff of 1s, 2s, then 4s, for GET, PUT and DELETE only. POSTs are
  never retried, so they can't be sent twice.

Mist allows each token 5,000 calls per hour. `self().apiUsage()` shows how many are left.

## Caching

```java
MistClient mist = MistClient.builder()
        .apiToken(token)
        .cache(new InMemoryResponseCache(Duration.ofMinutes(5)))
        .build();
mist.invalidateCache();   // force fresh data
```

Only successful GETs are cached. Cache keys include the full URL and a hash of the token, so one cache can
safely be shared by clients for different orgs. To use a shared store such as Redis or SQLite, implement
`ResponseCache`.

## Other notes

- `MistClient` is thread-safe. Create one per token and share it.
- If you pass your own `HttpClient` (for a proxy or custom TLS), the library will not close it.
- Logging goes through SLF4J. At `DEBUG` it logs each request; at `WARN`, each retry. The token is never logged.
- The library ships a `module-info` (module `ca.mgamble.mist`) and works on both the classpath and the module path.

## Development

```bash
mvn verify          # tests and javadoc
```

The model tests run against the example responses in Juniper's Postman collection, stored in
`src/test/resources/fixtures`. To refresh them after a new Mist release, download the collection from
[Juniper's Postman workspace](https://www.postman.com/juniper-mist/workspace/mist-systems-s-public-workspace)
as `mist-postman.json` and run:

```bash
python3 tools/extract_fixtures.py mist-postman.json src/test/resources/fixtures getSelf listOrgSites ...
```

To smoke-test against a real org (read-only, about 10 requests):

```bash
MIST_LIVE_TEST=true MIST_API_TOKEN=... MIST_HOST=api.gc2.mist.com mvn test -Dtest=LiveApiTest
```

## License

[Apache License 2.0](LICENSE)
