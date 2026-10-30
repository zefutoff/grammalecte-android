# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and the Android integration follows semantic versioning.

## [Unreleased]

## [1.0.0] - DRAFT (not published)

### Release preparation

- Planned first stable release based on `1.0.0-rc1`.
- No production Android application or Grammalecte engine changes
  have been introduced since RC1 at the time of this draft.
- Hardened instrumentation validation to reject missing test results.
- Completed F-Droid metadata, release documentation and store assets.

### Known limitations

- GrapheneOS Android 17 native spell-checker issue on a physical
  Pixel 9 remains under investigation (#34).
- Samsung Messages can append PROCESS_TEXT results instead of
  replacing the selection (#50); IME replacement is the tested fallback.

## [1.0.0-rc1]

### Added

- CycloneDX runtime SBOM generation and validation, including the vendored Grammalecte engine and release-workflow artifacts.
- Manual signed-release pipeline with protected GitHub environment secrets, release-certificate fingerprint verification and signed APK artifacts.
- Reproducible unsigned-release build documentation and a two-worktree byte-for-byte APK verification command.
- Real-engine performance regression tests covering QuickJS runtime reuse and tolerant cold-start, sentence and paragraph timing budgets.
- Runtime stress coverage for prolonged Grammalecte analysis, concurrent engine calls, repeated QuickJS lifecycle cycles and retained QuickJS memory growth.
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
- Explicit **Copier et revenir** action for editable `PROCESS_TEXT` selections, copying corrected text without changing the source editor.
- Opt-in, metadata-only `GrammalectePT` diagnostics in debuggable builds, without logging selected or corrected text.
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
- Reproducible Android application compatibility matrix with a helper for collecting device, Android and application-version metadata.
- Grammalecte rule-option engine API exposing upstream rule groups, French labels, current/default states, option updates and reset support.
- Persistent in-app Grammalecte rule settings shared by the native spell checker, PROCESS_TEXT workflow and correction IME.
- Persistent dictionary selection between all variants, classic orthography and the 1990 reform, shared by all correction modes.
- Persistent personal dictionary with in-app word management, shared by all correction modes.
- Global reset-to-default control restoring the bundled dictionary, Grammalecte rule defaults and clearing the personal dictionary.
- Versioned JSON import/export for non-sensitive correction preferences, using Android's system document picker without storage permissions; the personal dictionary is intentionally excluded.
- Dedicated correction status screen showing the native Android spell-checker and Grammalecte IME activation/selection state, with direct access to the relevant system settings.
- First-run setup guide explaining the three correction modes, showing live activation state and providing direct access to the relevant Android settings; the guide is shown once automatically and remains available from the main screen.
- Application visual identity with adaptive and themed launcher icons, a matching Quick Settings icon, branded accent colors and a concise launcher label.

### Changed

- Finalized the Android namespace and application ID as `fr.zefut.grammalecte` before public distribution; development installs using `fr.grammalecteandroid.unofficial` are treated as a separate Android application.
- Reviewed and normalized user-facing French wording, typography and pluralization across the application, correction IME, setup screens and diagnostics.
- Improved Android UI accessibility with semantic headings, TalkBack live-region announcements, minimum IME touch targets and a large-text IME layout that avoids truncated actions.
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
- `PROCESS_TEXT` now serializes Grammalecte engine creation, analysis and shutdown on a dedicated worker. Superseded analysis results and callbacks after activity destruction are discarded, while engine shutdown no longer blocks the UI thread.

### Fixed

- `PROCESS_TEXT` now invalidates suggestions after manual edits, rejects obsolete analysis results and prevents stale correction actions from overwriting edited or subsequently restored text.

- Grammalecte JavaScript compatibility issues with QuickJS involving `RegExp.leftContext`.
- Grammalecte module initialization required for grammar suggestions.
- Asset packaging of Graphspell dictionary directories beginning with `_`.
- French spell-checker subtype configuration on Android devices whose primary system locale is not French.
- CI JavaScript bridge test stubs to match the real Grammalecte initialization contract.
- Android API 26 compatibility in the IME by avoiding the API 28-only `mainExecutor`.
- IME selected-text reads no longer block the UI thread on slow `InputConnection` implementations such as Android WebView.
- IME replacement now tracks and restores the analyzed selection range before committing corrected text, while rejecting stale selections.
- IME correction now verifies the current selected text asynchronously before replacement, preventing the reproduced stale-content overwrite when selection coordinates remain unchanged.
- IME correction is disabled for password, visible-password, web-password and numeric-password fields, with pending input work invalidated on editor changes and Android instrumentation regression coverage.

### Known limitations

- Samsung Messages on the tested Galaxy S21 / Android 15 can insert returned `PROCESS_TEXT` corrections immediately after the original selection instead of replacing it (issue #50). An independent debug provider reproduces the symptom; the precise host-side cause remains unconfirmed. IME replacement works in the tested configuration.
- Native spell checking on GrapheneOS Android 17 remains under investigation (issue #34). Successful tests on a standard Android 17 emulator do not establish GrapheneOS compatibility.

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

These results are development observations recorded in the compatibility matrix. They do not establish complete coverage across Android versions, ROMs or application editors.
