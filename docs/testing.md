# Testing strategy

## Goals

The test suite protects several distinct boundaries:

1. pure correction-model behavior;
2. the JavaScript bridge contract;
3. the real embedded QuickJS/Grammalecte engine;
4. vendored-engine reproducibility;
5. Android framework integration;
6. real-world compatibility across editors.

Keeping these layers separate makes failures easier to diagnose.

## 1. Pure JVM unit tests

Fast JVM tests cover correction logic that does not require the Android framework or the real QuickJS runtime.

Current coverage includes:

- issue range validation;
- exact-range issue merging and priority;
- suggestion deduplication and limits;
- UTF-16 offset behavior, including emoji;
- asset path normalization;
- correction-result invariants.

These tests should remain fast and focused.

## 2. JavaScript bridge contract test

`tools/test-js-bridge.mjs` executes `android_bridge.js` with synthetic Grammalecte-compatible stubs.

It verifies the bridge contract, including:

- required data-file initialization;
- grammar issue conversion;
- spelling issue conversion;
- issue kinds;
- offsets;
- suggestion deduplication;
- word validation;
- suggestion limits.

This test intentionally does not prove that the real embedded Grammalecte engine can perform a correction. It protects the JavaScript/Kotlin integration contract independently from upstream engine data.

## 3. Real QuickJS / Grammalecte JVM integration tests

The `engine-grammalecte` module also runs tests against the actual vendored Grammalecte assets through QuickJS.

`RealEngineSmokeTest` verifies a minimal real-engine path, including:

- a spelling correction (`magazin` → `magasin`);
- a grammar correction (`aller` → `allé` / `allée`).

`RealEngineRegressionTest` extends this with representative French regression fixtures covering:

- spelling;
- grammar agreements;
- conjugation;
- apostrophes and typography-related corrections;
- multiple representative grammar rules;
- false-positive checks on correct sentences;
- validity of all returned issue ranges.

Regression fixtures must remain synthetic and should isolate the behavior being protected.

## 4. Vendored engine reproducibility

The **Vendored engine smoke test** GitHub Actions workflow:

1. checks out the project;
2. validates the committed upstream metadata;
3. regenerates Grammalecte from the pinned upstream commit;
4. validates the regenerated metadata;
5. verifies that regeneration produces no Git diff;
6. runs the real engine tests;
7. builds the debug Android application.

The vendoring script normalizes known non-deterministic upstream dictionary metadata:

- build timestamps are derived from the pinned upstream commit;
- `l2grams` values are sorted before serialization.

This protects against:

- upstream source-layout changes;
- stale or manually modified vendored assets;
- non-reproducible generated dictionaries;
- missing generated assets;
- engine-module regressions;
- APK packaging regressions.

## 5. Android instrumentation

Instrumentation tests cover Android behavior that cannot be reliably validated with plain JVM tests.

Current application-level instrumentation verifies that:

- the Grammalecte dictionary is packaged in the application;
- the real packaged QuickJS/Grammalecte engine performs spelling correction;
- the real packaged engine performs grammar correction;
- returned UTF-16 ranges remain correct with emoji before an issue;
- the spell-checker service is discoverable;
- the spell-checker service is protected by `android.permission.BIND_TEXT_SERVICE`.

The `spellchecker` instrumentation suite verifies Android result mapping, including:

- spelling suggestions and typo attributes;
- grammar suggestions and grammar-specific attributes where supported;
- UTF-16 sentence offsets with emoji;
- apostrophe ranges;
- multiple issues retaining their own ranges;
- identical-range issue merging and grammar priority;
- word suggestion limits;
- valid dictionary-word attributes.

The instrumentation workflow runs:

- API 35 for relevant pull requests;
- API 26, API 30 and API 35 on scheduled/manual runs.

API 26 is the current minimum supported Android API. Additional device, ROM and application coverage is still planned.

Future instrumentation should additionally cover:

- IME discovery and `android.permission.BIND_INPUT_METHOD`;
- IME metadata and lifecycle behavior;
- `ACTION_PROCESS_TEXT` exposure;
- end-to-end `SpellCheckerService` requests where practical;
- IME selected-text replacement where practical.

## 6. Build and privacy checks

`make check` is the canonical local quality entry point and is also used by the main CI workflow.

It currently runs:

- the source-manifest offline privacy check;
- ShellCheck;
- the JavaScript bridge contract test;
- ktlint;
- JVM tests;
- Android lint.

`make assemble` builds the debug APK and then runs `tools/check-apk-permissions.sh`.

The APK permission check inspects the final packaged application with Android `aapt2` and fails if `android.permission.INTERNET` appears after manifest merging.

This complements the faster source-manifest check and protects the project's offline-only invariant against permissions introduced through dependencies.

## 7. Physical-device validation

Some compatibility behavior depends on the target application's editor implementation and therefore requires real-device testing.

Development testing on a physical Android device has validated real local Grammalecte corrections through:

- native Android text fields with `SpellCheckerService`;
- `ACTION_PROCESS_TEXT`;
- the Grammalecte IME through `InputConnection`.

The IME replacement path has been manually validated in:

- Samsung Notes;
- Firefox;
- SMS editing.

These checks are development validation, not a complete compatibility matrix.

## Compatibility matrix

Before a stable public release, maintain a reproducible matrix that records for each tested application:

- Android version;
- application version;
- whether `SpellCheckerService` is invoked;
- whether `PROCESS_TEXT` is offered;
- whether returned `PROCESS_TEXT` text is applied;
- whether the IME receives a usable `InputConnection`;
- whether selected-text replacement succeeds.

Compatibility claims should identify the integration path actually tested.

## Fixture policy

Never use private messages, real emails, credentials or copied user content as test fixtures.

Use short synthetic French sentences designed to isolate one behavior.

## Performance tests

Performance coverage is still planned.

Useful measurements include:

- cold QuickJS/Grammalecte initialization;
- median sentence analysis latency;
- 95th percentile paragraph analysis latency;
- memory use after repeated corrections;
- rapid repeated IME analyses.

Performance tests should primarily detect regressions rather than enforce unrealistic device-independent timing thresholds.
