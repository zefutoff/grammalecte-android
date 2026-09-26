# ADR 0001: Isolate domain, engine and Android framework layers

- Status: Accepted
- Date: 2026-09-25

## Context

The project must support contributions without tying every test to an emulator or to the current JavaScript runtime choice.

## Decision

Use four Gradle modules: `core`, `engine-grammalecte`, `spellchecker` and `app`. All engine consumers depend on the `GrammarEngine` interface from `core`.

## Consequences

- Most behavior can be unit-tested on the JVM.
- QuickJS can be replaced without rewriting Android framework mapping.
- The module graph prevents UI code from becoming the integration layer.
