# ADR 0004: Do not use Android Accessibility Service for the MVP

- Status: Accepted
- Date: 2026-09-25

## Context

An accessibility service could inspect and replace text in applications that do not integrate Android's spell-checker framework, but it grants broad visibility into the user's interface and creates a substantially larger privacy/security surface.

## Decision

The MVP uses only Android's native `SpellCheckerService` framework. Accessibility-based fallback is out of scope until real-world compatibility data proves it necessary.

## Consequences

- Some custom editors and web views may not expose corrections.
- The application requests fewer sensitive capabilities.
- Compatibility gaps should be documented per application before reconsidering this decision.
