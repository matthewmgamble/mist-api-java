# Changelog

## 0.1.0 (unreleased)

Initial release, extracted from an internal Mist reporting tool and updated for Mist API 2607.1.1 (August 2026).

- `MistClient` with region support, token auth, retries (429 `Retry-After`, 5xx backoff for idempotent
  methods), typed exceptions and an optional response cache.
- Both Mist pagination styles: page-based lists (`X-Page-Total`) and cursor-based `/search` (`next` /
  `search_after`), fetched lazily through `Paged<T>`.
- Typed APIs for self, orgs, sites, site/device stats, device search, inventory, alarms (search, ack/unack)
  and available firmware versions.
- `RawApi` for every other endpoint.
