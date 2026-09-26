# Architecture

## Data flow

```text
Android text field
      │
      ▼
SpellCheckerService.Session
      │ TextInfo
      ▼
spellchecker module
      │ plain String
      ▼
GrammarEngine (core)
      │
      ▼
GrammalecteQuickJsEngine
      │
      ▼
QuickJS runtime
      │
      ▼
Grammalecte JavaScript + Graphspell dictionary
      │
      ▼
GrammarIssue(start, endExclusive, suggestions, kind, ruleId)
      │
      ▼
SentenceSuggestionsInfo / SuggestionsInfo
```

## Module boundaries

### `core`

Pure Kotlin/JVM. It owns the stable internal contract that the rest of the project depends on.

It must not import:

- `android.*`
- QuickJS classes
- Grammalecte implementation details

This is where range validation, deduplication and correction-result invariants live.

### `engine-grammalecte`

Owns all knowledge about:

- QuickJS-KT;
- generated Grammalecte JS files;
- Graphspell dictionary asset loading;
- the JavaScript bridge;
- translation from Grammalecte result objects to `GrammarIssue`.

The Android service must be able to replace this module later without changing its public behavior.

### `spellchecker`

Owns Android's spell-checker framework integration:

- `SpellCheckerService` registration;
- sentence and word request handling;
- mapping issue kinds to Android suggestion attributes;
- compatibility behavior before Android 12/API 31.

It does not parse Grammalecte JSON or know how JavaScript is loaded.

### `app`

Owns only user-facing setup/status UI and application packaging.

## Threading

Android invokes spell-checker callbacks on incoming IPC threads. A session is called serially by the platform, but the engine still protects its single QuickJS runtime with a lock so future call sites cannot accidentally evaluate it concurrently.

QuickJS async jobs use `Dispatchers.Default`. The MVP bridge itself is synchronous and local.

## Offset model

Android `String`, Kotlin/JVM `String` and JavaScript strings all index strings in UTF-16 code units. Grammalecte's JavaScript offsets can therefore be mapped directly to Android offsets **provided the JNI bridge preserves Unicode correctly**.

Regression tests must cover text where non-BMP characters (for example emoji) occur before an error.

## Failure behavior

The system spell-checker service should fail closed: if the embedded engine cannot initialize or analyze text, the service logs the error and returns no suggestions rather than crashing the input application.

## Compatibility target

- minSdk: 26
- target/compile SDK: 36
- grammar-specific Android result flags: API 31+
- pre-API 31 grammar findings fall back to typo-style suggestion attributes
