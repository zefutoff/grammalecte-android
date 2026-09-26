# Testing strategy

## Layers

### 1. Pure unit tests

Fast tests run on the JVM and cover:

- range validation;
- suggestion deduplication and limits;
- UTF-16 offsets;
- asset path normalization;
- data-model behavior.

These should form the majority of the test suite.

### 2. Engine smoke tests

A CI job vendors the pinned Grammalecte source, builds its JavaScript runtime and then executes engine-focused tests. This catches upstream layout changes and missing generated assets.

Future engine smoke fixtures should cover at least:

- spelling error with suggestions;
- subject/verb or noun/adjective agreement error;
- apostrophe/typography case;
- accented characters;
- emoji before an error;
- long paragraph timeout behavior.

### 3. Android instrumentation

Emulator tests verify platform integration that cannot be trusted to JVM stubs:

- service is discoverable by the spell-checker intent;
- service requires `BIND_TEXT_SERVICE`;
- metadata resource is present;
- sentence offsets/suggestions survive parceling;
- API 31+ grammar attributes are advertised and returned consistently.

## Fixture policy

Never use private messages, real emails or copied user content as test fixtures. Use short synthetic French sentences designed to isolate one rule.

## Performance gates

Once the engine integration stabilizes, add benchmark thresholds for:

- cold engine initialization;
- median sentence analysis;
- 95th percentile paragraph analysis;
- peak QuickJS memory after repeated corrections.

Performance tests should detect regressions, not enforce unrealistic device-independent timings.
