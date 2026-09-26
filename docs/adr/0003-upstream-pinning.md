# ADR 0003: Pin Grammalecte to immutable source commits

- Status: Accepted
- Date: 2026-09-25

## Context

Fetching a moving branch during application builds is not reproducible and makes reviews unable to determine what linguistic code shipped.

## Decision

Pin the upstream repository, commit SHA and version in `tools/grammalecte.env`. Generate browser/JavaScript assets from that checkout using the upstream build script.

## Consequences

- Engine updates produce explicit, reviewable diffs.
- Builds do not silently change when upstream changes.
- Contributors need Python and Git when refreshing the vendored engine.
