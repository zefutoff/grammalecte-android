# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and the Android integration follows semantic versioning.

## [Unreleased]

### Added

- Initial modular Android project foundation.
- Pure Kotlin `GrammarEngine` abstraction for correction results.
- Native Android `SpellCheckerService` integration.
- Sentence and word suggestion mapping to Android framework objects.
- Grammar and spelling suggestions backed by the real Grammalecte engine.
- Embedded Grammalecte 2.3.0 JavaScript runtime and Graphspell dictionaries.
- QuickJS-KT runtime integration.
- Reproducible, commit-pinned Grammalecte vendoring workflow.
- JavaScript bridge contract tests.
- UTF-16 and emoji offset regression coverage.
- Android `ACTION_PROCESS_TEXT` integration exposed as **Corriger avec Grammalecte**.
- Interactive `PROCESS_TEXT` correction screen with issue messages and individual suggestions.
- Writable `PROCESS_TEXT` replacement support.
- Clipboard fallback for read-only `PROCESS_TEXT` callers.
- Grammalecte correction IME based on Android `InputMethodService`.
- Selected-text reading and replacement through `InputConnection`.
- Interactive IME correction workflow with per-error suggestions.
- Local debug deployment helper through `tools/run-android.sh`.
- Android instrumentation test workflow.
- Vendored-engine smoke-test workflow.
- Offline-only privacy invariant with no `INTERNET` permission.
- CI checks for privacy, shell scripts, Kotlin formatting, unit tests, Android lint and APK builds.
- Dependabot configuration for Gradle and GitHub Actions dependencies.
- Issue templates, pull-request template and project documentation.
- Architecture decision records.
- Quick Settings access for switching to the Grammalecte correction IME.
- Apply-and-return workflow for restoring the previous keyboard after correction.
- Bulk correction actions in the Grammalecte IME.
- Dark theme support and redesigned application/IME interfaces.
- Real Grammalecte engine regression tests covering representative French corrections.
- Android instrumentation coverage for the packaged Grammalecte engine and spell-checker result mapping.
- Android framework integration tests covering IME discovery, `PROCESS_TEXT` exposure and end-to-end `SpellCheckerService` requests.
- End-to-end IME instrumentation covering Android IME selection, a real editable `InputConnection` and selected-text replacement.
- Opt-in real-engine performance characterization for cold start, sentence analysis and paragraph analysis.

### Changed

- Grammalecte engine initialization now loads conjugation, phonetic and morphology data required by the full grammar engine.
- Android spell-checker sessions now own their QuickJS engine lifecycle.
- Android correction is no longer limited to applications supporting `SpellCheckerService`.
- `PROCESS_TEXT` provides an explicit fallback for selected text.
- `InputMethodService` / `InputConnection` is now the preferred generic fallback for editable fields that bypass native spell checking or ignore `PROCESS_TEXT` replacement.
- Accessibility-based text replacement is not part of the current architecture.
- Gradle wrapper updates are maintained manually so the wrapper version, bootstrap script and verified distribution checksum stay synchronized.
- Local and CI checks now share the same `make check` and `make assemble` entry points.
- Grammalecte vendoring is normalized and checked for reproducible output.
- CI verifies the permissions of the final debug APK in addition to source manifests.
- IME analysis now uses a single serialized background worker so QuickJS engine creation, evaluation and shutdown cannot race during rapid repeated corrections or service destruction.
- The IME now reacts to selection changes while it is already open, allowing text selected after switching keyboards to be analyzed automatically.

### Fixed

- Grammalecte JavaScript compatibility issues with QuickJS involving `RegExp.leftContext`.
- Grammalecte module initialization required for grammar suggestions.
- Asset packaging of Graphspell dictionary directories beginning with `_`.
- French spell-checker subtype configuration on Android devices whose primary system locale is not French.
- CI JavaScript bridge test stubs to match the real Grammalecte initialization contract.
- Android API 26 compatibility in the IME by avoiding the API 28-only `mainExecutor`.

### Validated manually

The current development APK has successfully performed real local corrections on a physical Android device using:

- native Android `EditText` fields;
- SMS editing;
- Samsung Notes;
- Firefox text fields.

The following integration paths have been validated during development:

- `SpellCheckerService`;
- `ACTION_PROCESS_TEXT`;
- Grammalecte IME through `InputConnection`.

These tests are development validation only and do not yet constitute a complete public compatibility matrix.
