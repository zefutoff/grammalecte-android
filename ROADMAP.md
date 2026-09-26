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

## Phase 1 — First functional APK

- [ ] Vendor Grammalecte 2.3.0 generated JS assets
- [ ] Run QuickJS engine against real Grammalecte fixtures on JVM/Android
- [ ] Validate cold startup and sentence analysis on a physical phone
- [ ] Confirm spelling and grammar underlines on API 31+
- [ ] Validate fallback behavior on API 26–30
- [ ] Add APK smoke test with at least one real correction request

Exit criterion: an installable APK performs local French spelling and grammar correction in a stock `EditText` without network permission.

## Phase 2 — Real-world compatibility

- [ ] Test AOSP Messages / common SMS app
- [ ] Test Signal / Element / Proton Mail where applicable
- [ ] Test Chromium/Firefox text fields and WebViews
- [ ] Document applications that bypass the Android spell-checker framework
- [ ] Add an in-app diagnostic screen showing whether the system selected the service
- [ ] Measure false positives and missing context caused by Android sentence segmentation

Exit criterion: maintain a public compatibility matrix based on reproducible tests.

## Phase 3 — User controls

- [ ] Grammalecte rule-category settings
- [ ] Dictionary choice: all variants / classic / 1990 reform
- [ ] Personal dictionary
- [ ] Reset-to-default controls
- [ ] Import/export of non-sensitive preferences

## Phase 4 — Coverage fallback without accessibility

- [ ] Add Android `PROCESS_TEXT` action: “Corriger avec Grammalecte”
- [ ] Paragraph review screen with explanations and per-error replacement
- [ ] Apply-all only when replacements do not overlap

Accessibility-service integration remains intentionally out of scope unless compatibility evidence justifies revisiting ADR 0004.

## Phase 5 — Public distribution

- [ ] Commit Gradle wrapper and validate it in CI
- [ ] Reproducible release build documentation
- [ ] Signed release pipeline
- [ ] SBOM / dependency inventory
- [ ] F-Droid metadata and reproducibility checks
- [ ] First stable release
