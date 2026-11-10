# Grammalecte Android - Project State

> **Latest verified checkpoint:** 2026-11-10 (Europe/Paris).
> **Repository:** https://github.com/zefutoff/grammalecte-android
> **Reference `main` commit:** `c62f1b5e373c0bc7759e081da4a8b3cf315ae935` (PR #96 squash merge, 2026-11-10).
> **Historical context:** The sections below the current checkpoint preserve the original 2026-11-07 audit. Dates, PR states, CI results and counts in that historical audit are not current.
> **Change policy:** Verify live GitHub state before operations; this documentation records evidence, not approval to merge, close issues, release builds or change CI.

## Current operational checkpoint — 2026-11-10

This checkpoint supersedes the dated 2026-11-09 operational snapshot. Historical sections below remain unchanged for traceability; their former open-PR states and counts are not current.

| Area | Verified state | Next action / gate |
| --- | --- | --- |
| **`main` / UI #89** | [PR #96](https://github.com/zefutoff/grammalecte-android/pull/96) **merged** via squash at `c62f1b5e` on 2026-11-10. The Material 3 **home dashboard** is now on `main`; secondary screens are not covered. | Keep [issue #89](https://github.com/zefutoff/grammalecte-android/issues/89) open and plan secondary-screen work separately. |
| **PR #96 validation** | PR head `70c9f9068f5a3174b421f92415757f58cce9247f` passed CI, CodeQL and Android instrumentation. Prior physical-device home-screen acceptance was reported as passed. Maintainer's local `make unsigned-release` succeeded (119 tasks), and the final APK permissions check found no `INTERNET` permission. | Check the **post-merge `main`** quality runs separately; PR-head green checks are not post-merge evidence. |
| **Runtime / licensing** | Audit reported 110 Maven coordinates, 109 Apache-2.0 POM license declarations and one undeclared Guava ListenableFuture POM. Existing third-party notices include QuickJS-KT (Apache-2.0), QuickJS and c-vector (MIT). Local Release APK inventory showed `libquickjs.so` and `libandroidx.graphics.path.so` for arm64-v8a, armeabi-v7a, x86 and x86_64. | No proven license defect. Before **publication**, verify binary provenance and how recipients obtain applicable licenses, notices and source; POM metadata alone is not legal sign-off. |
| **Dependencies** | [PR #97](https://github.com/zefutoff/grammalecte-android/pull/97) and [PR #99](https://github.com/zefutoff/grammalecte-android/pull/99) were merged before #96; AGP is 9.5.1 with strict dependency checksums. | Preserve verification gates. |
| **Other open work** | Issues #34 (GrapheneOS reporter follow-up), #50 (Samsung Messages host limitation), #85 (F-Droid submission) and #89 (remaining UI) remain the tracked open issues from the last verified register; no closure is implied here. F-Droid MR !54224 was reported closed, not merged; January revisit planned. | Check live issue/MR state before acting; do not reopen F-Droid prematurely. |
| **Published release** | Latest previously verified published version: signed `v1.0.0` (2026-10-31). The #96 merge is **not** a new published release. | New version, signed assets and full release acceptance required for publication. |

**Project invariants:** offline correction with no `android.permission.INTERNET`, no Accessibility Service replacement, pinned Grammalecte/QuickJS boundary, stable production identity/signing, separate `.dev` package. Distinguish merged code, device acceptance and published releases.

**Next priorities:** verify post-merge `main` checks; continue issue #89 secondary screens; preserve #34/#50 evidence; revisit F-Droid in January; complete distribution licensing review before release.

---

## Historical operational summary — audit of 2026-11-07 (not a live status)

**Snapshot:** 2026-11-07, referenced to `main` commit `ef44eb57aab45513e84379a970a9279df10c74d2`. Before an operation, compare this reference with the actual GitHub head, relevant issues/PRs and CI checks; this section is not a live dashboard.

| Area | Verified current position | Next responsible action / completion gate |
| --- | --- | --- |
| **Shipped baseline** | Signed `v1.0.0` is published (2026-10-31). Post-release fixes #92-#95 are merged into `main`, **not shipped** in that APK. | Do not republish changed code as `1.0.0`; prepare a distinct version, changelog and release validation when appropriate. |
| **Primary delivery: UI #89 / PR #96** | Compose Material 3 **home screen only** is on `feat/issue89-home-ui` (`b218562a`); no merge. Latest-head checks have no valid execution, and the earlier Android 17 emulator failed to boot. | Restore CI availability; require latest-head CI, CodeQL, instrumentation on API 26/35/37, theme/scale/a11y evidence, screenshots and dependency notices **before proposing a merge**. Keep #89 open for the secondary screens. |
| **P0: CI availability** | Several recent Actions jobs failed before running any steps; a billing/credit explanation is plausible but **not verified** through GitHub. Last verified green `main` push checks are from 2026-11-04. | Confirm runner/billing availability; rerun only missing relevant checks. Separate startup failures from actual test failures. |
| **P0: F-Droid #85** | The tracked `fdroiddata !54224` was reported **closed, not merged** on 2026-11-03. The closure reason and F-Droid catalog publication are unverified. | Read upstream closure feedback, agree whether to repair/reopen/resubmit, then correct the F-Droid docs and monitoring decision. Do not claim catalog inclusion. |
| **Other open work** | #34: physical Pixel 9 / GrapheneOS session failure not reproduced in an emulator. #50: Samsung Messages `PROCESS_TEXT` insertion issue reproduced, IME fallback works. #97/#99: dependency PRs await meaningful checks. | Preserve #34/#50 and existing test evidence; avoid speculative fixes or merges. Review dependency PRs after CI recovers. |
| **Acceptance debt** | #88 was merged/closed but PR #95 still lists some unconfirmed manual onboarding checks; #90/#91 have documented closure evidence. | Reconcile #88 checks without repeating already passed tests; reopen #90/#91 only for new regression evidence. |

**Non-negotiable constraints:** offline correction (no `INTERNET` permission), no Accessibility Service replacement, pinned vendored Grammalecte/QuickJS boundary, stable released app identity/signing certificate, and separate `.dev` package. A green CI result is not by itself physical-device acceptance or publication authorization.

**Next checkpoint:** record verified Actions recovery, a documented upstream F-Droid decision, a #96 acceptance decision, or any merge/release that materially changes the project state. This documentation-only PR must **remain unmerged until the maintainer explicitly approves it**.

## 1. Evidence standard and limits

Evidence categories used below:

- **Verified (GH):** repository content, GitHub issue/PR state, commits, release metadata, workflow runs or downloaded job logs queried on 2026-11-07.
- **Documented (repo):** claims recorded in the repository's tests, compatibility matrix, release documents or PR descriptions. These are not necessarily independently rerun during this audit.
- **Reported (conversation/device):** earlier project discussion or physical-device reports, not retested in this audit.
- **Pending/unverified:** requires another check or access to an unavailable source.

Scope limitations:

- Available project conversation excerpts were reviewed; a complete, independently searchable archive of every previous project chat was **not** available.
- The maintainer's local working trees, ADB devices and full current test environment were **not** inspected.
- No tests or builds were executed during this audit.
- Current GitHub Actions billing/credits and the platform-level reasons for unstarted jobs were **not** accessible.
- Direct access to GitLab merge request discussion and its closure reason was unavailable; the last recorded GitLab status is from GitHub issue #85, dated 2026-11-03.
- The initial audit was read-only. Follow-up authorization covers only a dedicated documentation PR for this file; it does not authorize an issue change, workflow rerun, PR merge or release.

Source-of-truth order: current GitHub code and metadata; concrete CI logs and test reports; dated repository documents; dated device reports; conversation recollections. Do not replace a device-specific failure with emulator success, or a successful build with unproven functional acceptance.

## 2. Repository snapshot

| Item | State on 2026-11-07 | Evidence |
| --- | --- | --- |
| Visibility / default branch | Public; `main` | [Repository](https://github.com/zefutoff/grammalecte-android) |
| `main` head | `ef44eb57aab45513e84379a970a9279df10c74d2` | [Commit](https://github.com/zefutoff/grammalecte-android/commit/ef44eb57aab45513e84379a970a9279df10c74d2) |
| Latest published stable release | `v1.0.0`, published 2026-10-31; signed APK, CycloneDX SBOM, SHA256SUMS | [Release](https://github.com/zefutoff/grammalecte-android/releases/tag/v1.0.0) |
| App identity | `fr.zefut.grammalecte`, versionName `1.0.0`, versionCode `3` on `main` | [`app/build.gradle.kts`](app/build.gradle.kts) |
| Permanent development variant | `fr.zefut.grammalecte.dev`; can coexist with production; not a store release | [PR #93](https://github.com/zefutoff/grammalecte-android/pull/93) |
| Android support | minSdk 26; targetSdk / compileSdk 36 | [`app/build.gradle.kts`](app/build.gradle.kts) |
| Core toolchain | Gradle 9.6.1; AGP 9.5.0; Kotlin 2.4.21; JDK 17 | [`gradle/`](gradle/), [`gradle/libs.versions.toml`](gradle/libs.versions.toml) |
| Embedded linguistic engine | Grammalecte 2.3.0, upstream commit `47af2080202e647110e5199ebd5d4b51d2bd51db` | [`tools/grammalecte.env`](tools/grammalecte.env) |
| Issues | 8 total: **4 open**, 4 closed | [Issue tracker](https://github.com/zefutoff/grammalecte-android/issues) |
| Pull requests | 90 total: **3 open**, 81 merged, 6 closed without merge | [PR tracker](https://github.com/zefutoff/grammalecte-android/pulls) |

**Existing remote branches (4, verified):**

| Branch | Head | Role |
| --- | --- | --- |
| `main` | `ef44eb57` | Default branch; newer than public v1.0.0 |
| `feat/issue89-home-ui` | `b218562a` | Open PR #96: home dashboard modernization |
| `dependabot/github_actions/github-actions-fffcf8739d` | `e8a2cf68` | Open PR #97 |
| `dependabot/gradle/gradle-minor-patch-a390ea5798` | `ef207661` | Open PR #99 |

The absence of old feature branches from this list does not imply their changes are absent; the associated PRs were merged.

## 3. Implemented and accepted product baseline

The public `v1.0.0` release is real and available. Its repository documentation records a signed APK acceptance, a reproducible build check, a signed RC1-to-stable upgrade test, and a released SBOM. The actual published asset was not re-signed or rehashed in this audit.

### 3.1 Correction functionality

- **Native `SpellCheckerService`:** spelling/grammar proposals through Android framework integration when host editors use that API. Validated in native fields, not universally across apps.
- **`ACTION_PROCESS_TEXT`:** selected-text correction with individual suggestions and explanations, returning corrected text to compatible editable hosts; explicit **Copy and return** fallback when replacement is unavailable.
- **Correction-focused IME / `InputConnection`:** explicit selected-text correction and direct replacement with a return-to-previous-keyboard workflow. The IME is intentionally **not** a general-purpose typing keyboard.
- All three consume the same `GrammarEngine` model and embedded QuickJS/Grammalecte runtime. No correction requests require network access.
- IME/PROCESS_TEXT engine access is serialized; stale selections/results are rejected. IME correction is disabled for sensitive/password input types.

### 3.2 User controls and distribution features

- Rule category options; persistent dictionary choice (all/classic/1990 reform); personal dictionary; reset; versioned preferences import/export. Personal dictionary content is intentionally excluded from preferences export.
- Activation/status UI for native spellchecker and IME, first-run help, French copy, accessibility adjustments, light/dark handling and launcher identity.
- Signed release process, reproducible unsigned builds, F-Droid metadata/Fastlane assets, third-party notices, CycloneDX SBOM, package/privacy checks.

### 3.3 Post-release changes merged into `main`

These changes have been merged **after** the published `v1.0.0` tag and must not be described as shipped in that specific APK:

| PR | Merged change | Evidence / caution |
| --- | --- | --- |
| [#92](https://github.com/zefutoff/grammalecte-android/pull/92) | Native suggestion popup contrast fix | Issue #90 closed after physical light/dark check and successful CI/CodeQL/instrumentation |
| [#93](https://github.com/zefutoff/grammalecte-android/pull/93) | Permanent side-by-side DEV variant | Builds, identity/privacy checks and Galaxy S21 activation documented |
| [#94](https://github.com/zefutoff/grammalecte-android/pull/94) | Compact IME actions | Issue #91 closed; Galaxy S21 manual and instrumentation checks recorded |
| [#95](https://github.com/zefutoff/grammalecte-android/pull/95) | Three-step, state-aware onboarding | Issue #88 closed; outstanding manual acceptance items remain noted in PR body |

Other major previously merged work includes engine/IME integration and safety, compatibility tests, Android 17 and Android 8 coverage, release reproducibility, licensing/SBOM, F-Droid submission preparation and v1.0.0 release documentation. Representative PRs: [#43](https://github.com/zefutoff/grammalecte-android/pull/43), [#53](https://github.com/zefutoff/grammalecte-android/pull/53), [#57](https://github.com/zefutoff/grammalecte-android/pull/57), [#58](https://github.com/zefutoff/grammalecte-android/pull/58), [#59](https://github.com/zefutoff/grammalecte-android/pull/59), [#60](https://github.com/zefutoff/grammalecte-android/pull/60), [#62](https://github.com/zefutoff/grammalecte-android/pull/62), [#63](https://github.com/zefutoff/grammalecte-android/pull/63), [#74](https://github.com/zefutoff/grammalecte-android/pull/74), [#78](https://github.com/zefutoff/grammalecte-android/pull/78), [#82](https://github.com/zefutoff/grammalecte-android/pull/82).

## 4. Established architecture and project decisions

These are accepted decisions, not proposals to revisit without new evidence:

1. **Four modules:** `core`, `engine-grammalecte`, `spellchecker`, `app`, with a pure Kotlin `GrammarEngine` contract ([ADR 0001](docs/adr/0001-modular-boundaries.md)).
2. **Embed QuickJS-KT** to run generated upstream Grammalecte JavaScript; no separate Python runtime on Android ([ADR 0002](docs/adr/0002-quickjs-runtime.md)).
3. **Pin the upstream engine to an immutable commit** and vendor assets reproducibly ([ADR 0003](docs/adr/0003-upstream-pinning.md)).
4. **No Accessibility Service** for automatic text replacement; use Android standard text APIs and document host limitations ([ADR 0004](docs/adr/0004-no-accessibility-mvp.md), [ADR 0006](docs/adr/0006-ime-inputconnection-fallback.md)).
5. **Strict offline/privacy invariant:** no `android.permission.INTERNET`, cloud correction API or runtime downloads; inspect the final packaged APK ([ADR 0005](docs/adr/0005-offline-privacy-invariant.md)).
6. **IME safety:** serialized runtime, content/selection revalidation before commits, no edits to sensitive fields; maintain synthetic-only test fixtures.
7. **Distribution identity:** release package `fr.zefut.grammalecte` is stable; isolated `fr.zefut.grammalecte.dev` for ongoing development; keep release signing certificate stable.
8. **Build/release process:** Gradle wrapper updated manually; CI and local checks share `make` targets; publish only a versioned, validated and signed artifact from an immutable tag. A green CI run alone does not satisfy functional acceptance.

References: [`docs/architecture.md`](docs/architecture.md), [`docs/testing.md`](docs/testing.md), [`docs/release.md`](docs/release.md), [`docs/adr/`](docs/adr/).

## 5. Issue register (all eight issues)

| Issue | State | Evidence and next action |
| --- | --- | --- |
| [#34](https://github.com/zefutoff/grammalecte-android/issues/34) | **OPEN**; reopened | Reporter: physical Pixel 9/GrapheneOS 17 (`2026102101`), RC1 cannot open native French spellchecker session. Maintainer's GrapheneOS 17 x86_64 emulator **passes** with both original `22ce813` and newer `fd2db61`. Device/ROM discrepancy unresolved. Reporter has already confirmed explicit French selection; do not ask for it again. Await requested read-only service/package diagnostic or new physical evidence. |
| [#45](https://github.com/zefutoff/grammalecte-android/issues/45) | CLOSED | Generic PROCESS_TEXT result-contract and host fixture coverage completed. |
| [#50](https://github.com/zefutoff/grammalecte-android/issues/50) | **OPEN** | Samsung Messages on Galaxy S21/Android 15 inserts correction after selected text. Independent debug-only provider reproduced it; strongly supports host-side incompatibility, not a proven Samsung internal root cause. IME direct replacement works. Avoid speculative production change; track documented limitation. |
| [#85](https://github.com/zefutoff/grammalecte-android/issues/85) | **OPEN** | F-Droid upstream MR !54224 was last recorded as **closed** (not merged) on 2026-11-03. Root cause and later changes not directly verifiable; determine why it closed before deciding on a new submission. |
| [#88](https://github.com/zefutoff/grammalecte-android/issues/88) | CLOSED | Guided first-run onboarding merged in #95. PR still lists fresh-install, return-from-settings, light theme, enlarged font and API-matrix acceptance checks as outstanding; reconcile this with existing green instrumentation before asserting full acceptance. |
| [#89](https://github.com/zefutoff/grammalecte-android/issues/89) | **OPEN** | App-wide modern visual design. PR #96 is **only the home screen**; setup/status/rules/diagnostics, cross-theme visuals and screenshots remain required. Do not close with #96. |
| [#90](https://github.com/zefutoff/grammalecte-android/issues/90) | CLOSED | Contrast defect resolved by #92; closure comment cites physical light/dark validation and CI/CodeQL/instrumentation passes at API 26/35/37. No basis to reopen without new regression evidence. |
| [#91](https://github.com/zefutoff/grammalecte-android/issues/91) | CLOSED | Oversized IME actions resolved by #94 with manual/CI/instrumentation evidence, including default and enlarged layouts. |

## 6. Pull request register

### Open PRs (all three)

| PR | Head | State and merge gate |
| --- | --- | --- |
| [#96 - Compose Material 3 home](https://github.com/zefutoff/grammalecte-android/pull/96) | `b218562a7633d310071717815687397de7ebf82f` | **Not merged.** CI/CodeQL/instrumentation on latest head did not run successfully. Last substantive API 37 attempt failed to boot the emulator; API 26 and 35 passed on earlier `d104ce59`. Manual visual evidence and dependency provenance review outstanding. |
| [#97 - GitHub Actions updates](https://github.com/zefutoff/grammalecte-android/pull/97) | `e8a2cf68af9f97f61beaff39ea4da5ce16867e3f` | **Not merged.** `setup-java` 6.1.0 -> 6.2.0 and CodeQL action 4.39.1 -> 4.39.3. Revalidate checks after Actions availability is restored. |
| [#99 - Gradle plugin update](https://github.com/zefutoff/grammalecte-android/pull/99) | `ef2076615ad04377be6adb55a2614af2295c9735` | **Not merged.** AGP 9.5.0 -> 9.5.1 for app/library. Revalidate builds, instrumentation and dependency metadata before merge. |

### Closed PRs

- **81 merged:** historical feature, test, CI, hardening, documentation and release changes; the latest four post-release merges are #92-#95 as listed in Section 3.
- **6 closed without merge:** [#1](https://github.com/zefutoff/grammalecte-android/pull/1), [#2](https://github.com/zefutoff/grammalecte-android/pull/2), [#3](https://github.com/zefutoff/grammalecte-android/pull/3), [#4](https://github.com/zefutoff/grammalecte-android/pull/4), [#51](https://github.com/zefutoff/grammalecte-android/pull/51), [#79](https://github.com/zefutoff/grammalecte-android/pull/79). **Important:** #51's independent Samsung Messages probe was tested physically but deliberately not merged; its implementation is not part of `main`.

## 7. Testing and continuous integration evidence

### Coverage and methods

- JVM unit tests: domain model, suggestion/range normalization and Unicode/UTF-16 behavior.
- JavaScript bridge tests with synthetic stubs; real QuickJS/Grammalecte smoke and regression tests for French spelling and grammar.
- Performance regression targets (`make performance-check`) and memory/runtime stress targets (`make stress-check`); documented thresholds are JVM regression budgets, not device performance guarantees.
- Instrumentation suites for spellchecker and application: Android framework registration, real `SpellCheckerService`, `PROCESS_TEXT`, IME selection/replacement, packaged engine and first-run behavior.
- CI Android API 26/35/37.0 on relevant PRs; 26/30/35/37.0 on scheduled/manual runs. `make instrumented` checks that tests actually ran and XML reports contain neither failures nor errors.
- Physical Galaxy S21/Android 15 functional/compatibility checks, including IME, native spellchecker and `PROCESS_TEXT`; separate local GrapheneOS emulator experiments; external Pixel 9 report remains unresolved.
- Build/privacy: `make check`, `make assemble`, `make unsigned-release`, reproducibility, SBOM and packaged APK INTERNET-permission assertions.

### Dated results - do not treat all green runs as latest-head proof

| Date (UTC) | Scope | Result and qualification |
| --- | --- | --- |
| 2026-10-31 | Public v1.0.0 | Signed release published; repo records signed RC1 -> stable upgrade PASS, personal dictionary/settings preserved, F-Droid-style reproducibility and SBOM validation. [Release](https://github.com/zefutoff/grammalecte-android/releases/tag/v1.0.0), [`docs/upgrade-rc1-to-stable-acceptance.md`](docs/upgrade-rc1-to-stable-acceptance.md). |
| 2026-11-03 | PR #92 / issue #90 | Physical light/dark native popup validation plus CI, CodeQL and API 26/35/37 instrumentation passed according to issue closure comment. |
| 2026-11-03 | PR #93 and #94 | CI, CodeQL and instrumentation passed at PR heads; PR descriptions record physical DEV setup and IME sizing validation. |
| 2026-11-03 | PR #95 | [CI](https://github.com/zefutoff/grammalecte-android/actions/runs/41734820788), [CodeQL](https://github.com/zefutoff/grammalecte-android/actions/runs/41734820588), [instrumentation](https://github.com/zefutoff/grammalecte-android/actions/runs/41734820747) successful on `d4acf62f`; remaining hands-on onboarding checks were still listed in the PR. |
| 2026-11-04 | `main` at `ef44eb57` | [CI](https://github.com/zefutoff/grammalecte-android/actions/runs/41889057141) and [CodeQL](https://github.com/zefutoff/grammalecte-android/actions/runs/41889056685) successful. This is the last observed `main` push quality run, **not** proof that later scheduled runs succeeded. |
| 2026-11-04 | PR #96 at `a501b05` | CI and CodeQL successful, instrumentation unsuccessful. [Runs](https://github.com/zefutoff/grammalecte-android/pull/96/checks). |
| 2026-11-04 | PR #96 at `d104ce59` | [CI](https://github.com/zefutoff/grammalecte-android/actions/runs/41928673511) and [CodeQL](https://github.com/zefutoff/grammalecte-android/actions/runs/41928673463) successful; [instrumentation](https://github.com/zefutoff/grammalecte-android/actions/runs/41928673547) succeeded on API 26 and 35, **failed on API 37 before tests** (`Timeout waiting for emulator to boot`, ADB connection refused). |
| 2026-11-05 | PR #96 at `b218562a` | Latest head changes Android 17 emulator userdata size. [CI](https://github.com/zefutoff/grammalecte-android/actions/runs/42086089632), [CodeQL](https://github.com/zefutoff/grammalecte-android/actions/runs/42086089642) and [instrumentation](https://github.com/zefutoff/grammalecte-android/actions/runs/42086089659) all reported failure but jobs expose no executed steps; no valid test conclusion for this head. |
| 2026-11-07 | Scheduled `main` and Dependabot | Scheduled [instrumentation](https://github.com/zefutoff/grammalecte-android/actions/runs/42367794903) and [F-Droid monitor](https://github.com/zefutoff/grammalecte-android/actions/runs/42376207634), plus dependency PR checks, failed without visible steps. Cause **not directly verified**. |

The #96 PR description reports local Samsung Galaxy S21 `make instrumented` success: **8/8 spellchecker** and **46/46 app tests**, plus checks, DEV/debug/release builds, SBOM (117 components) and two identical unsigned release builds. These are **maintainer-documented PR assertions**; XML output and fresh latest-head CI were not independently reproduced in this audit.

**Actions diagnosis:** unstarted jobs and absent job logs are consistent with the previously reported Actions-credit problem, but GitHub billing settings were not available. Do not label these failures as code regressions or mark them resolved until checks on the relevant heads actually execute.

## 8. F-Droid and external distribution

- Github stable release: **published and verified in release metadata**; this does **not** imply F-Droid catalog publication.
- F-Droid upstream submission: [fdroiddata !54224](https://gitlab.com/fdroid/fdroiddata/-/merge_requests/54224), tracked by [issue #85](https://github.com/zefutoff/grammalecte-android/issues/85).
- Latest recorded GitLab MR snapshot in issue #85: state `closed`, merge status `not_open`, successful prior pipeline, last update `2026-11-03T17:19:26Z`. **This is not a merged/approved inclusion.**
- Direct GitLab discussion, closure explanation, present MR state and current F-Droid catalog status could **not** be verified in this audit.
- [`docs/fdroid.md`](docs/fdroid.md) still describes upstream review as pending and the recipe as RC1-based (versionCode 2), despite the subsequent v1.0.0 stable release (versionCode 3) and closed MR.
- The [F-Droid monitor](.github/workflows/monitor-fdroid.yml) remains scheduled every six hours. Review whether continued polling of this closed MR is useful after learning why it closed; avoid consuming scarce Actions capacity without a clear next step.

## 9. Discrepancies and consistency risks

| Finding | Status | Required decision or correction |
| --- | --- | --- |
| F-Droid documentation says review pending, while issue #85 records closed-not-merged MR | **Verified discrepancy** | Read closure feedback; choose corrected submission path; update docs/monitor only after decision. |
| `docs/release.md` lists AGP **9.4.1** although current `main` uses **9.5.0** | **Verified discrepancy** | Update toolchain snapshot at the next documentation change. Gradle remains 9.6.1. |
| `main` has code changes after `v1.0.0`, while build still names **1.0.0** | **Verified release-process risk** | Never republish modified code under the already published 1.0.0 identity. Plan a new versionName/versionCode and release notes. |
| `[Unreleased]` in `main` CHANGELOG is empty despite post-release merges #92-#95 | **Verified documentation gap** | Record shipped-vs-unreleased changes before the next release. |
| #88 closed although PR #95 still lists manual acceptance tasks | **Traceability gap** | Reconcile with actual device/test evidence; create targeted follow-up only if criteria remain unmet. Do not automatically reopen. |
| #96 has earlier green runs, but latest head has no valid CI/Android 17 results and screenshots are absent | **Merge blocker** | Validate latest head, API matrix, themes, accessibility, screenshots, third-party notices and provenance. |
| GrapheneOS 17 emulator succeeds but physical Pixel 9 user reports failure | **Real environment difference, not contradiction** | Keep #34 open; follow device-specific diagnostics rather than repeating French-locale checks. |
| Samsung Messages failure persists despite general PROCESS_TEXT tests | **Host-specific limitation, not a failed contract** | Keep IME fallback and document supported/unsupported editors; avoid speculative production fix. |
| Multiple recent Actions workflows fail without starting | **Operational blocker, cause unverified** | Verify credits/runner entitlement and rerun only necessary checks after availability returns. |

## 10. Prioritized work queue

| Priority | Work / exit gate | Status |
| --- | --- | --- |
| **P0** | Restore reliable Actions execution (billing/credits check outside GitHub read API); run missing validation on #96 latest head; separate quota failures from real test failures. | **Blocked on external execution availability** |
| **P0** | Investigate upstream F-Droid MR !54224 closure and feedback; choose whether to repair/reopen/submit a replacement; remove stale monitoring and document the true inclusion status. | **External evidence needed** |
| **P1** | Complete #96 home-phase acceptance: green latest-head CI/CodeQL, instrumentation API 26/35/37, real-device checks, light/dark screenshots, enlarged font, a11y, dependency notices; merge only after evidence. Keep #89 open for remaining screens. | **In progress; PR open** |
| **P1** | Reconcile #88 onboarding post-merge manual checks (fresh install, settings return, light/dark, large font and compact/API 26); record evidence or create a narrowly scoped regression issue. | **Evidence gap** |
| **P1** | Review #97/#99 dependency changes, verification metadata, security and API matrix after CI is functional; avoid merging merely because changes are automated. | **PRs open** |
| **P1** | Correct AGP and F-Droid documentation; populate `[Unreleased]`; agree next versionCode/versionName and release gates before publishing new code. | **Pending maintainer review** |
| **P2** | Continue #34 only when reporter/device-side diagnostic or new reproducing evidence arrives; preserve distinction between emulator and physical GrapheneOS. | **Waiting on external evidence** |
| **P2** | Maintain #50 as a documented Samsung Messages host limitation; reconsider issue lifecycle only with evidence and explicit acceptance of the workaround. | **Open, understood limitation** |

Do not perform an automatic release or close #89 based solely on a green PR. Do not repeat already completed #90 or #91 diagnostics unless new regression evidence appears.

## 11. Maintenance protocol for this file

- **Update triggers:** any PR merge/closure, issue status or acceptance change, release/tag publication, F-Droid upstream status change, meaningful device regression report, or resolution of the Actions execution blocker.
- **At each update:** record date/time and exact `main` SHA, current branches and open items, latest run *on each relevant head*, actual test results, decisions, blockers and next action.
- **Evidence:** link to GitHub issue/PR/commit/run and to a test artifact or dated document when available. Distinguish reported, verified and still-pending outcomes.
- **Release discipline:** do not move published tags, replace signed v1.0.0 assets, silently change app identity, introduce INTERNET permission, or close an issue without satisfying its material acceptance criteria.
- **Approval:** the maintainer approved this snapshot for a dedicated documentation PR on 2026-11-07. Check that the PR changes only `PROJECT_STATE.md`; do not merge until a separate, explicit maintainer decision. CI availability and PR creation are not merge authorization.

## 12. Primary reference links

- [Repository / main](https://github.com/zefutoff/grammalecte-android/tree/main)
- [Issues](https://github.com/zefutoff/grammalecte-android/issues), [pull requests](https://github.com/zefutoff/grammalecte-android/pulls), [Actions runs](https://github.com/zefutoff/grammalecte-android/actions)
- [Stable release v1.0.0](https://github.com/zefutoff/grammalecte-android/releases/tag/v1.0.0)
- [`ROADMAP.md`](ROADMAP.md), [`CHANGELOG.md`](CHANGELOG.md), [`docs/testing.md`](docs/testing.md)
- [`docs/compatibility.md`](docs/compatibility.md), [`docs/release.md`](docs/release.md), [`docs/fdroid.md`](docs/fdroid.md)
- [Open UI PR #96](https://github.com/zefutoff/grammalecte-android/pull/96), [F-Droid tracker #85](https://github.com/zefutoff/grammalecte-android/issues/85)