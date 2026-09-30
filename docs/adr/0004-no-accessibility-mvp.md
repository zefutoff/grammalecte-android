# ADR 0004: Do not use Android Accessibility Service for text correction

- Status: Accepted
- Date: 2026-09-25
- Updated: 2026-09-30

## Context

An Accessibility Service could inspect and replace text in applications that do not integrate Android's spell-checker framework.

However, such a service grants broad visibility into the user's interface and creates a substantially larger privacy and security surface.

The original MVP therefore started with Android's native `SpellCheckerService` framework only.

Subsequent compatibility testing showed that some applications do not use `SpellCheckerService`, so additional standard Android integration paths were investigated.

An Accessibility Service prototype was also tested. It confirmed that reliable replacement could become application-specific because focus, selection state, available actions and paste behavior differed between editors.

## Decision

Accessibility-based text correction is not part of the current architecture.

The project prefers standard Android text APIs:

1. `SpellCheckerService` for native transparent correction;
2. `ACTION_PROCESS_TEXT` for explicit selected-text correction;
3. IME / `InputConnection` for correction and replacement in editable fields that bypass the previous mechanisms.

The IME decision is documented separately in ADR 0006.

Accessibility integration should only be reconsidered if future compatibility evidence demonstrates a requirement that cannot reasonably be handled through these standard Android APIs.

## Consequences

- The application avoids requesting Accessibility Service privileges.
- The privacy and security surface remains smaller.
- Some interfaces that expose neither usable text-processing actions nor a functional `InputConnection` may remain unsupported.
- Compatibility gaps should be documented rather than solved with application-specific accessibility automation.
- New fallback mechanisms should continue to prefer normal Android text APIs.

## Relationship to ADR 0006

ADR 0006 extends application coverage with an `InputMethodService` and `InputConnection`.

It does not reverse this decision: Accessibility Service replacement remains intentionally excluded.
