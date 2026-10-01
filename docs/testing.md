# Testing strategy

## Goals

The test suite should protect four distinct areas:

1. the pure correction model;
2. the JavaScript bridge and vendored Grammalecte assets;
3. Android framework integration;
4. real-world compatibility across editors.

These layers should remain separate so a failure clearly identifies which boundary broke.

## 1. Pure JVM unit tests

Fast JVM tests cover logic that does not require Android or QuickJS.

Current coverage includes:

- issue range validation;
- overlapping-range handling;
- suggestion deduplication and limits;
- UTF-16 offset behavior;
- asset path normalization;
- correction-result invariants.

These tests should remain the majority of the suite.

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

## 3. Vendored engine smoke workflow

The **Vendored engine smoke test** GitHub Actions workflow:

1. checks out the project;
2. downloads and generates the pinned Grammalecte assets;
3. verifies the upstream vendoring stamp;
4. runs `:engine-grammalecte:test`;
5. builds the debug Android application.

This detects:

- upstream source-layout changes;
- broken vendoring;
- missing generated assets;
- engine-module build regressions;
- APK packaging regressions.

A future automated smoke test should instantiate the real embedded engine and assert at least one spelling correction and one grammar correction.

Useful real-engine fixtures should eventually include:

- a spelling error with suggestions;
- a grammar agreement error;
- accented characters;
- apostrophe or typography handling;
- an emoji before an error;
- a longer paragraph;
- timeout behavior.

## 4. Android instrumentation

Instrumentation tests cover Android framework behavior that cannot be reliably validated with JVM stubs.

Current automated instrumentation verifies that:

- the spell-checker service is discoverable through the Android spell-checker intent;
- the service is protected by `android.permission.BIND_TEXT_SERVICE`.

Future instrumentation should additionally verify:

- spell-checker metadata;
- IME discovery;
- `android.permission.BIND_INPUT_METHOD`;
- IME metadata;
- `ACTION_PROCESS_TEXT` exposure;
- sentence offsets and suggestions surviving Android parceling;
- grammar-specific attributes on supported Android versions.

## 5. Physical-device validation

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
