# ADR 0002: Execute Grammalecte JavaScript with QuickJS-KT

- Status: Accepted for MVP
- Date: 2026-09-25

## Context

Grammalecte provides a JavaScript build used by browser integrations. Porting the linguistic engine to Kotlin would create a large, permanent fork and make upstream updates expensive.

## Decision

Embed the generated Grammalecte JavaScript and execute it with QuickJS-KT. Keep all runtime code in `engine-grammalecte`.

## Consequences

- Upstream rule and dictionary updates remain consumable.
- No Python runtime is required on Android.
- JNI/runtime behavior becomes a tested dependency, particularly for Unicode offsets and resource limits.
- A future native/Kotlin engine remains possible behind `GrammarEngine`.
