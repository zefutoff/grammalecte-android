# ADR 0005: Keep text analysis fully offline by default

- Status: Accepted
- Date: 2026-09-25

## Context

A system spell checker can receive highly sensitive text from messaging, email and forms.

## Decision

Do not request `android.permission.INTERNET`. Do not load JavaScript, dictionaries, telemetry or configuration from the network at runtime.

## Consequences

- Analysis is private by construction at the Android permission layer.
- Engine updates require application updates.
- Any future network feature requires a new ADR and must not silently weaken this invariant.
