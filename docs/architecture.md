# Architecture

## Overview

Grammalecte Android exposes one local correction engine through three Android integration paths:

    Android application
           │
           ├── SpellCheckerService
           │        │
           │        ▼
           │   Android suggestions
           │
           ├── ACTION_PROCESS_TEXT
           │        │
           │        ▼
           │   ProcessTextActivity
           │
           └── InputMethodService
                    │
                    ▼
              InputConnection
                    │
                    ▼
              selected text
                    │
                    ▼
              GrammarEngine
                    │
                    ▼
          GrammalecteQuickJsEngine
                    │
                    ▼
              QuickJS runtime
                    │
                    ▼
          Grammalecte JavaScript
          + Graphspell dictionaries

All three Android entry points use the same internal correction model.

## Module boundaries

### `core`

Pure Kotlin/JVM.

It owns the stable correction contract:

- `GrammarEngine`;
- `GrammarIssue`;
- `IssueKind`;
- range validation;
- suggestion normalization;
- correction-result invariants.

It must not depend on:

- Android framework classes;
- QuickJS;
- Grammalecte implementation details.

### `engine-grammalecte`

Owns the embedded Grammalecte runtime:

- QuickJS-KT;
- generated Grammalecte JavaScript;
- Graphspell dictionaries;
- Android asset loading;
- JavaScript bridge initialization;
- conversion of Grammalecte results to `GrammarIssue`.

The rest of the application reaches Grammalecte through the `GrammarEngine` abstraction.

The engine therefore remains replaceable without changing the correction model exposed to Android integrations.

### `spellchecker`

Owns Android's native spell-checker integration:

- `SpellCheckerService` registration;
- word correction requests;
- sentence correction requests;
- mapping `GrammarIssue` objects to `SuggestionsInfo`;
- mapping sentence results to `SentenceSuggestionsInfo`;
- Android suggestion attributes;
- compatibility behavior across supported Android versions.

The module does not parse Grammalecte JavaScript results directly.

### `app`

Owns application packaging and user-facing Android integrations that do not belong to the native spell-checker module:

- launcher/setup UI;
- diagnostic test UI;
- Android settings shortcuts;
- `ACTION_PROCESS_TEXT`;
- `ProcessTextActivity`;
- Grammalecte correction IME;
- `InputConnection` replacement.

The `app` module may instantiate the Grammalecte engine for these explicit correction workflows, but engine-specific implementation remains inside `engine-grammalecte`.

## Integration path 1 — SpellCheckerService

The native path is:

    Android text field
          │
          ▼
    TextServicesManager
          │
          ▼
    GrammalecteSpellCheckerService
          │
          ▼
    SpellCheckerService.Session
          │
          ▼
    GrammarEngine
          │
          ▼
    GrammarIssue
          │
          ▼
    SuggestionsInfo /
    SentenceSuggestionsInfo

This is the preferred transparent integration when an application supports Android's spell-checker framework.

Each spell-checker session lazily owns its Grammalecte engine instance.

The engine is closed when the Android session closes.

Some applications and custom editors never call the platform spell-checker service. This limitation cannot be solved from inside `SpellCheckerService`.

## Integration path 2 — ACTION_PROCESS_TEXT

The explicit selected-text path is:

    selected text
         │
         ▼
    ACTION_PROCESS_TEXT
         │
         ▼
    ProcessTextActivity
         │
         ▼
    GrammarEngine
         │
         ▼
    interactive correction review

The activity:

- receives selected text from Android;
- analyzes it locally;
- displays detected issues;
- exposes individual suggestions;
- maintains a corrected working copy;
- returns corrected text when the caller allows replacement;
- copies corrected text to the clipboard when Android marks the source as read-only.

Some editors accept `PROCESS_TEXT` but ignore the replacement result. This is why another fallback is required.

## Integration path 3 — IME / InputConnection

The generic editable-field fallback is:

    editable application
          │
          ▼
    Android InputConnection
          │
          ▼
    GrammalecteImeService
          │
          ▼
    selected text
          │
          ▼
    GrammarEngine
          │
          ▼
    corrected working text
          │
          ▼
    InputConnection.commitText()

The IME is correction-oriented rather than a full typing keyboard.

Its role is to:

- read the current selection;
- analyze it with Grammalecte;
- show issues and suggestions;
- let the user choose corrections;
- replace the selected text directly through Android's normal input-method API.

This path has been manually validated in:

- Samsung Notes;
- Firefox;
- SMS editing.

Using `InputConnection` avoids application-specific accessibility automation and works through the standard Android editable-text contract.

## Why Accessibility Service is not used

An Accessibility Service can inspect and manipulate a much broader portion of the user interface.

A prototype demonstrated that text replacement through accessibility actions can become application-specific and fragile.

For example, editor focus, selection state, context menus and paste behavior can differ between applications.

The project therefore prefers:

1. `SpellCheckerService` when supported;
2. `PROCESS_TEXT` for explicit selected-text correction;
3. IME / `InputConnection` for reliable replacement in editable fields.

Accessibility integration remains out of scope unless future compatibility evidence justifies reconsidering that decision.

See ADR 0004 and the IME architecture ADR.

## Engine lifecycle

`GrammalecteQuickJsEngine` lazily creates its QuickJS runtime.

Runtime initialization loads:

- Grammalecte helpers;
- conjugation data;
- phonetic data;
- morphology data;
- grammar engine data;
- Graphspell dictionaries;
- the Android JavaScript bridge.

The runtime remains local to the process and does not download JavaScript or dictionaries at runtime.

Calling `close()` shuts down the QuickJS runtime.

## Threading and serialization

QuickJS runtime access is serialized inside `GrammalecteQuickJsEngine`.

The engine protects runtime creation, evaluation and shutdown with an internal lock.

This means multiple calls using the same engine instance cannot evaluate JavaScript concurrently.

The Android integrations may perform correction work away from the main UI thread, while the engine lock preserves the single-runtime execution constraint.

The IME also discards stale UI results when a newer analysis supersedes an older one.

## Offset model

Android `String`, Kotlin/JVM `String` and JavaScript strings use UTF-16 code-unit indexing.

Grammalecte JavaScript offsets can therefore map directly to Android string offsets as long as the JavaScript bridge preserves Unicode correctly.

Regression tests cover non-BMP characters such as emoji appearing before an error.

## Failure behavior

### SpellCheckerService

If engine initialization or analysis fails, the spell-checker path should return no suggestions rather than crash the application requesting correction.

### PROCESS_TEXT

A correction failure remains isolated inside the correction activity and must not corrupt the source application's text.

### IME

Replacement is only applied after confirming that the current selected text still matches the selection that was originally analyzed.

This prevents applying a correction to unrelated text when the user changes the selection while analysis is running.

## Privacy boundary

The application intentionally declares no `android.permission.INTERNET` permission.

Correction data flows only between:

- the Android framework;
- application memory;
- the embedded QuickJS runtime;
- bundled Grammalecte assets.

No correction text is intentionally sent to a remote service.

Because the application can operate as an input method, preserving this offline-only boundary is a core architectural invariant.

## Compatibility target

- minimum Android API: 26;
- compile SDK: 36;
- French correction engine: Grammalecte 2.3.0;
- grammar-specific Android result attributes where supported by the platform;
- compatibility fallbacks for applications that bypass native spell checking.
