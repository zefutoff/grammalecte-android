# ADR 0006: Use an IME and InputConnection as the editable-field fallback

- Status: Accepted
- Date: 2026-09-30

## Context

The native Android `SpellCheckerService` provides transparent spelling and grammar suggestions when an application participates in the platform spell-checker framework.

Compatibility testing showed that this is not universal.

Some applications and custom editors do not invoke `SpellCheckerService` at all.

`ACTION_PROCESS_TEXT` provides a useful explicit correction fallback for selected text, but replacement behavior also varies between applications. Some callers expose the selection as read-only, while others accept the action but ignore the corrected text returned by the processing activity.

An Accessibility Service prototype was investigated as another replacement mechanism.

The prototype demonstrated several application-specific problems:

- editable focus could be lost after returning from another activity;
- selection information could disappear;
- available accessibility actions varied by editor;
- paste behavior could differ from normal user-driven paste;
- reliable replacement could require application-specific UI interaction.

This would make accessibility-based replacement fragile and difficult to maintain across applications.

Android input methods already have a standard interface for interacting with active editable fields: `InputConnection`.

A minimal `InputMethodService` prototype successfully read and replaced selected text through `InputConnection` in several unrelated applications.

The real Grammalecte correction workflow was then validated through the same mechanism in Samsung Notes, Firefox and SMS editing.

## Decision

The project will use a correction-oriented Android `InputMethodService` as the preferred generic fallback for editable fields that cannot be corrected reliably through `SpellCheckerService` or `ACTION_PROCESS_TEXT`.

The IME will interact with editable text through Android's standard `InputConnection` API.

The integration priority is:

1. `SpellCheckerService` for transparent native correction where supported;
2. `ACTION_PROCESS_TEXT` for explicit selected-text correction;
3. IME / `InputConnection` for reliable correction and replacement in editable fields.

Accessibility-based text replacement is not part of the current architecture.

## IME scope

The Grammalecte IME is not intended to become a complete general-purpose keyboard.

Its current responsibility is limited to correction workflows:

- read selected text from the active editor;
- analyze it with the local `GrammarEngine`;
- display detected issues and suggestions;
- maintain a corrected working copy;
- replace the selected text through `InputConnection`.

The user can continue using their normal keyboard for ordinary typing.

## Privacy

An Android input method occupies a sensitive position because it can interact with user-entered text.

The project therefore keeps the existing offline-only privacy invariant:

- no `android.permission.INTERNET` permission;
- no cloud correction API;
- no analytics or telemetry;
- no remote JavaScript or dictionary loading;
- correction performed entirely by the embedded Grammalecte runtime.

Adding network access in the future would require an explicit architectural decision and privacy review.

## Safety of replacement

The IME must avoid replacing unrelated text if the user changes the selection while a correction is being analyzed.

Before applying a correction, the implementation verifies that the current selected text still matches the text that was originally analyzed.

If the selection has changed, replacement must not proceed automatically.

## Threading

A QuickJS runtime must not be evaluated concurrently.

`GrammalecteQuickJsEngine` serializes access to its runtime internally.

IME analysis may run away from the Android main thread, but all access to a shared engine instance remains serialized by the engine.

Stale analysis results must not overwrite newer UI state.

## Consequences

### Positive

- replacement uses a standard Android editable-text API;
- behavior is not tied to a specific application UI;
- Samsung Notes, Firefox and SMS have already accepted clean replacement through the mechanism;
- no Accessibility Service permission is required;
- the same `GrammarEngine` correction model remains shared with the other Android integrations;
- the offline privacy model is preserved.

### Negative

- the user must enable the Grammalecte input method;
- the user currently has to switch temporarily from their normal keyboard;
- the IME requires additional onboarding because Android presents strong warnings when enabling input methods;
- compatibility still depends on the target editor providing a usable `InputConnection`;
- additional automated testing is required.

## Follow-up work

Before public distribution:

- add a user-friendly IME activation flow;
- clearly explain why Android warns about enabling a keyboard;
- provide an easy way to return to the previous input method;
- expand compatibility testing;
- add automated IME integration tests where practical;
- review engine lifecycle and background execution under rapid repeated corrections.

## Relationship to ADR 0004

ADR 0004 rejected Accessibility Service integration for the original MVP because of its broad privacy and security surface.

This decision preserves that principle.

The project has expanded beyond a `SpellCheckerService`-only MVP by adding `PROCESS_TEXT` and IME-based fallbacks, but Accessibility Service replacement remains intentionally excluded.
