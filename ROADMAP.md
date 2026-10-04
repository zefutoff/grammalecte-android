# Roadmap

## Phase 0 — Foundation

- [x] Modular Gradle project
- [x] Pure `GrammarEngine` contract
- [x] Native Android `SpellCheckerService`
- [x] QuickJS adapter boundary
- [x] JavaScript bridge contract test
- [x] UTF-16 / emoji regression coverage
- [x] Pinned Grammalecte vendoring workflow
- [x] Offline-only permission invariant
- [x] CI, lint, dependency updates, issue and PR templates
- [x] Architecture decision records
- [x] Committed Gradle wrapper

## Phase 1 — Functional local correction engine

- [x] Vendor Grammalecte 2.3.0 generated JavaScript assets
- [x] Embed Graphspell dictionaries
- [x] Initialize the full Grammalecte runtime in QuickJS
- [x] Validate real spelling corrections
- [x] Validate real grammar corrections
- [x] Validate the engine on a physical Android device
- [x] Validate native correction in a stock Android `EditText`
- [x] Validate Android `SuggestionSpan` integration
- [x] Confirm local operation without `INTERNET` permission
- [x] Add an automated packaged-engine smoke test performing real spelling and grammar corrections
- [x] Run automated instrumentation on API 26, API 30 and API 35
- [ ] Broaden physical-device compatibility validation across older Android versions
- [x] Measure cold engine initialization and correction latency

Exit criterion: an installable APK performs local French spelling and grammar correction without network access.

## Phase 2 — Coverage across Android applications

### Native SpellCheckerService

- [x] Validate native Android text fields
- [x] Validate SMS editing
- [x] Identify applications that bypass the Android spell-checker framework
- [x] Add an end-to-end Android framework test for `SpellCheckerService`
- [ ] Build and publish a reproducible compatibility matrix
- [ ] Test additional messaging and mail applications
- [ ] Test additional Chromium/WebView editors

### PROCESS_TEXT fallback

- [x] Add Android `ACTION_PROCESS_TEXT`
- [x] Add the **Corriger avec Grammalecte** selection action
- [x] Display detected issues and explanations
- [x] Allow individual suggestion replacement
- [x] Return corrected text to writable callers
- [x] Copy corrected text when the caller exposes a read-only selection
- [x] Validate the workflow in Firefox
- [x] Identify applications that ignore returned `PROCESS_TEXT` replacement
- [x] Add automated `ACTION_PROCESS_TEXT` exposure coverage

### IME / InputConnection fallback

- [x] Add a Grammalecte `InputMethodService`
- [x] Read selected text through `InputConnection`
- [x] Run the same local Grammalecte engine from the IME
- [x] Display issues and correction choices
- [x] Apply corrected text directly through `InputConnection`
- [x] Validate clean replacement in Samsung Notes
- [x] Validate clean replacement in Firefox
- [x] Validate clean replacement in SMS
- [x] Add a basic user-friendly IME activation and selection flow
- [x] Add an easy way to return to the previous keyboard
- [ ] Review QuickJS execution serialization inside the IME
- [ ] Add automated IME integration tests where practical
  - [x] Cover IME discovery, permission and metadata
  - [ ] Cover IME lifecycle and selected-text replacement through `InputConnection`

Accessibility-based replacement remains intentionally out of scope. The IME/InputConnection path is the preferred generic fallback for editable fields.

Exit criterion: correction remains usable in common editable applications even when `SpellCheckerService` or `PROCESS_TEXT` replacement is unavailable.

## Phase 3 — User controls

- [ ] Grammalecte rule-category settings
- [ ] Dictionary choice: all variants / classic / 1990 reform
- [ ] Personal dictionary
- [ ] Reset-to-default controls
- [ ] Import/export of non-sensitive preferences
- [ ] Clear status screen for native spell-checker and IME activation

## Phase 4 — Product polish

- [ ] Application icon and visual identity
- [ ] Improve first-run setup
- [ ] Explain the three correction modes in-app
- [ ] Improve accessibility of the application UI
- [ ] Review all user-facing French strings
- [ ] Add compatibility documentation
- [ ] Add performance regression tests
- [ ] Add memory/runtime stress tests

## Phase 5 — Public distribution

- [ ] Reproducible release build documentation
- [ ] Signed release pipeline
- [ ] SBOM / dependency inventory
- [ ] F-Droid metadata and reproducibility checks
- [ ] Review Android package namespace before stable release
- [ ] Final third-party license review
- [ ] First stable release
