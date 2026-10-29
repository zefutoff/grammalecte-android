# Android application compatibility

This document records reproducible compatibility tests for the Android
integration paths exposed by Grammalecte Android.

The matrix is intentionally based on observed behavior for specific versions.
A successful result for one application version, Android version or vendor ROM
must not be assumed to apply to every environment.

## Integration paths

The project exposes three correction paths:

- **SpellCheckerService** — native Android spell-checker integration;
- **PROCESS_TEXT** — explicit correction of selected text;
- **IME / InputConnection** — selected-text correction through the Grammalecte input method.

## Standard test fixture

Use synthetic text only.

Spelling fixture:

> Je vais au magazin.

Expected relevant correction:

> Je vais au magasin.

Grammar fixture:

> Je suis aller au magasin hier.

Expected relevant correction:

> Je suis allé au magasin hier.

The purpose of the compatibility matrix is to validate Android integration,
not to exhaustively validate Grammalecte's linguistic behavior.

## Result values

- ✅ Works
- ❌ Does not work
- ⚠️ Partial or application-specific behavior
- — Not tested

## Recorded test environments

### Samsung Galaxy S21

| Field | Value |
| --- | --- |
| Manufacturer | Samsung |
| Model | SM-G998B |
| Android | 15 |
| API | 35 |
| Build ID | AP3A.240905.015.A2 |
| Security patch | 2026-01-01 |
| Android System WebView | 154.0.8037.57 (803705703) |

### Android 8.0 emulator

| Field | Value |
| --- | --- |
| Environment | Android Virtual Device (`grammalecte-api26`) |
| Manufacturer | Google |
| Model | Android SDK built for x86 |
| Android | 8.0.0 |
| API | 26 |
| Build ID | OSR1.180418.026 |
| Security patch | 2018-04-05 |
| WebView provider | Chrome 69.0.3497.100 (349710017) |

### Android 17 emulator

| Field | Value |
| --- | --- |
| Environment | Android Virtual Device (`grammalecte-api37`) |
| Android | 17 |
| API | 37 (`android-37.0`) |
| System image | Google APIs x86_64 |
| Native spellchecker tests | `OK (2 tests)` |

Manual A/B tests confirmed that automatic spellchecker language
selection (`0`) failed to provide a usable French session, while
explicit selection of the `fr_FR` subtype restored native suggestions.

The same language-selection behavior was reproduced on the Samsung
Galaxy S21 running Android 15.

These results validate the native correction path in the tested
diagnostics field. They do not establish compatibility with every
third-party application or with GrapheneOS.


### GrapheneOS Android 17 local emulator (2026-10-18)

| Field | Value |
| --- | --- |
| Environment | Locally built GrapheneOS emulator, x86_64 (`emu64x`) |
| GrapheneOS source tag | `2026100600` |
| Build target | `sdk_phone64_x86_64-cur-userdebug` |
| Android / API | Android 17 / API 37 |
| Build ID | `CP2A.260605.016` |
| Security patch | `2026-10-05` |
| Emulator-only build adjustment | Dynamic partition group size increased from 2800 MiB to 3000 MiB to fit `super.img`; no spellchecker source changes |
| Grammalecte commits | `22ce813` (original issue #34 report) and `fd2db61` (newer build) |
| Selected spellchecker subtype | Explicit French `fr_FR` (`-1266611582`) |
| Package at `22ce813` | `fr.grammalecteandroid.unofficial` |
| Package at `fd2db61` | `fr.zefut.grammalecte` |

**Both commits passed the native spellchecker checks in this emulator:**

- The app's direct `TextServicesManager` diagnostic returned a real `magazin` → `magasin` suggestion.
- After manually clearing and retyping `Je suis aller au magazin.` in the app's native diagnostic `EditText`, the editor displayed misspelling/grammar underlines and `SuggestionSpan`s.
- Tapping `magazin` and choosing `magasin` replaced the misspelled word in the editor.
- With `fd2db61`, the `SpellCheckerServiceEndToEndTest` XML reported **2 tests, 0 failures, 0 errors, 0 skipped**.
- With `22ce813`, a targeted `connectedDebugAndroidTest` invocation completed with `BUILD SUCCESSFUL` in 27 seconds. The historical test class defines one test, but its result XML was not provided for this run.

**Important test-fixture distinction:** the app's automatically prefilled
diagnostic `EditText` initially had no `SuggestionSpan`, whereas manually
entered text did. The initial absence of spans alone does not establish
that the selected spellchecker is broken; it can depend on when the editor
requests spellchecking.

These results **do not reproduce issue #34** in this x86_64 emulator, even
at the reporter's original commit. They do **not** establish compatibility
with a physical Pixel 9 running GrapheneOS. The original report remains
open pending device-specific investigation; `PROCESS_TEXT` and IME were
not tested as part of this emulator comparison.

### Pixel 9, GrapheneOS Android 17 (reported, 2026-10-26)

A reporter retested the signed `v1.0.0-rc1` release on a
physical Pixel 9 running GrapheneOS build `2026102101`.

Reported results:

- The in-app "Tester Grammalecte" diagnostic could not open
  an Android spell-checker session.
- No native `SuggestionSpan` corrections appeared.
- The issue persisted despite selecting French in Android settings.

These are external physical-device observations, not results
independently reproduced by the project maintainers.

The selected spell-checker component, subtype and installed
package state are being investigated in issue #34.
The reporter has been asked for read-only ADB diagnostics.

No conclusion about all GrapheneOS devices or Android 17
installations should be drawn from this report.

## Compatibility matrix

| Device / Android | Application | Version (code) | Grammalecte build | SpellCheckerService | PROCESS_TEXT | IME / InputConnection | Notes |
| --- | --- | --- | --- | :---: | :---: | :---: | --- |
| Pixel 9 / GrapheneOS 17 | Grammalecte diagnostics `EditText` | Built-in | `v1.0.0-rc1` | ❌ | — | — | Reported on build `2026102101`: native session not opened, no `SuggestionSpan`; read-only ADB diagnosis pending (issue #34). |
| GrapheneOS 17 emulator / API 37 | Grammalecte diagnostics `EditText` | Built-in | `22ce813` | ✅ | — | — | Local GrapheneOS `2026100600`; explicit `fr_FR` subtype; direct session correction, manual typing, `SuggestionSpan` inspection and native `magasin` replacement all worked. Targeted instrumentation: `BUILD SUCCESSFUL`; exact test count not independently confirmed by XML. Issue #34 not reproduced on emulator. |
| GrapheneOS 17 emulator / API 37 | Grammalecte diagnostics `EditText` | Built-in | `fd2db61` | ✅ | — | — | Same emulator and explicit French subtype; two `SuggestionSpan`s observed after manual typing, native suggestion/replacement worked; spellchecker instrumentation XML: 2 tests, 0 failures, 0 errors. Initially prefilled `EditText` had no spans. |
| SM-G998B / Android 15 | WhatsApp | 2.26.39.79 (263907900) | `735e893` (reported Git HEAD) | ✅ | ✅ | ✅ | Retest on 2026-10-14: native `magazin` suggestion and replacement, PROCESS_TEXT correction, IME replacement and late selection refresh were all reported working. Grammalecte 0.1.0-dev (1); explicit `fr_FR` spellchecker subtype (`-1266611582`). APK's exact source commit is not independently attested by package metadata. |
| SM-G998B / Android 15 | Samsung Messages | 16.0.10.49 (1601000049) | `735e893` (reported Git HEAD) | ⚠️ | ⚠️ | ✅ | Retest reported on 2026-10-14 with explicit `fr_FR` subtype (`-1266611582`). Native spelling feedback is intermittent: underlines may only appear after repeated spaces or leaving and returning to the editor. PROCESS_TEXT appends the corrected text instead of replacing the selection; observed result: `Je suis aller au magazin.Je suis allé au magasin.` IME replacement and selection made after opening the IME both work. APK exact source commit not independently attested. |
| SM-G998B / Android 15 | Samsung Notes | 4.4.45.37 (444537000) | `735e893` (reported Git HEAD) | ❌ | ❌ | ✅ | Retest reported on 2026-10-14 with explicit `fr_FR` subtype (`-1266611582`). Native spelling: no underline in the tested note editor. PROCESS_TEXT: corrected text does not replace the selected text (whether clipboard fallback occurs was not recorded). IME replacement and selection made after opening the IME both work. Underline refresh was reported as working after correction, but with no native underline visible initially the earlier stale-underline behavior is not conclusively ruled out. APK exact source commit not independently attested. |
| SM-G998B / Android 15 | Firefox | 157.0.1 (2016188623) | `735e893` (reported Git HEAD) | ❌ | ⚠️ | ✅ | Retest reported on 2026-10-15 using the Firefox HTML textarea on a Galaxy S21 (SM-G998B, Android 15/API 35), with the explicit `fr_FR` spellchecker subtype (`-1266611582`). Native correction: no misspelling underline. PROCESS_TEXT: correction suggestions appear and the corrected text is copied to the clipboard, but the original selected text is not replaced in the editor. IME correction and selection made after opening the IME both work. APK exact source commit not independently attested. |
| SM-G998B / Android 15 | Chrome | 155.0.8059.39 (805903904) | `735e893` (reported Git HEAD) | ❌ | ⚠️ | ✅ | Retest reported on 2026-10-15 in a Chrome HTML textarea on the Galaxy S21 (SM-G998B, Android 15/API 35), with explicit `fr_FR` spellchecker subtype (`-1266611582`). Native Grammalecte suggestions were not confirmed: a red underline appeared once and then stopped appearing; the option to add the word to the personal dictionary suggests Chrome/editor spellchecking rather than confirmed Grammalecte integration, but is not conclusive. PROCESS_TEXT generates correction and places it on the clipboard, without replacing text in the source editor. IME replacement and selection made after opening the IME both work. APK exact source commit not independently attested. |
| SM-G998B / Android 15 | Proton Mail | 7.11.10 (18325) | `735e893` (reported Git HEAD) | ❌ | ❌ | ✅ | Retest reported on 2026-10-15 in the mail composer on Galaxy S21 (SM-G998B, Android 15/API 35), with explicit `fr_FR` spellchecker subtype (`-1266611582`). Native spellchecking failed in this editor; the PROCESS_TEXT action was not offered in the text selection menu, so no direct replacement could be attempted. IME replacement and selection made after opening the IME both work. Package: `ch.protonmail.android`; Grammalecte 0.1.0-dev (1). APK exact source commit not independently attested. |
| SM-G998B / Android 15 | K-9 Mail | 23.1 (39045) | `735e893` (reported Git HEAD) | ✅ | ✅ | ✅ | Retest reported on 2026-10-15 on Samsung Galaxy S21 (SM-G998B, Android 15/API 35) with explicit French spellchecker subtype `fr_FR` (`-1266611582`). The user reports usable native spelling correction, a working PROCESS_TEXT action with replacement, IME correction and correction of text selected after the IME is already open. Earlier testing on the same K-9 Mail version reported partial native support, no PROCESS_TEXT action and failed late-selection refresh; results now differ with the newer Grammalecte build/configuration. Package: `com.fsck.k9`. Grammalecte 0.1.0-dev (1); APK exact source commit not independently attested. |
| Android 17 AVD / API 37 | Grammalecte diagnostics `EditText` | Built-in | `c713cf5` | ✅ | — | — | Native correction works with the explicit French subtype; automatic selection fails. Both spellchecker instrumentation tests passed. |
| SM-G998B / Android 15 | Grammalecte diagnostics `EditText` | Built-in | PR #42 test build | ✅ | — | — | Native correction works with the explicit French subtype; automatic selection fails when English is the primary system language. |
| Android SDK built for x86 / Android 8.0 (API 26, AVD) | Android System WebView (Chrome provider) | 69.0.3497.100 (349710017) | `a04dc56` | ❌ | ⚠️ | ✅ | The API 26 AVD exposed no native spell-checker feedback in the local WebView fixture; PROCESS_TEXT was offered and Grammalecte produced a correction, but WebView did not apply the returned replacement; IME correction works, including when the text is selected after the Grammalecte IME is already open. Returning from the Grammalecte IME opens Android's input-method picker on API 26. |
| SM-G998B / Android 15 | Samsung Notes | 4.4.45.37 (444537000) | `6c0dde2` | ❌ | ⚠️ | ⚠️ | Grammalecte was explicitly selected as the system spell checker, but Samsung Notes exposed no native suggestion UI; PROCESS_TEXT falls back to the clipboard instead of replacing the selection; IME replacement succeeds, but Samsung Notes keeps a stale red underline even after focus changes and further text edits. |
| SM-G998B / Android 15 | Firefox | 157.0 (2016186455) | `6c0dde2` | ❌ | ⚠️ | ✅ | Grammalecte was explicitly selected as the system spell checker, but Firefox did not invoke native spell-checker UI for the tested field; PROCESS_TEXT copies corrected text to the clipboard without replacing the source selection automatically; IME replacement works correctly. |
| SM-G998B / Android 15 | Samsung Messages | 16.0.10.49 (1601000049) | `6c0dde2` | ❌ | ⚠️ | ✅ | Grammalecte SpellCheckerService was explicitly selected but no native suggestion UI was exposed; PROCESS_TEXT appends corrected text after the original selection instead of replacing it; IME replacement works correctly. |
| SM-G998B / Android 15 | Chrome | 131.0.6778.260 (677826031) | `6c0dde2` | ❌ | ⚠️ | ✅ | Grammalecte was explicitly selected as the system spell checker, but the tested Chromium textarea exposed no native spell-checker UI; PROCESS_TEXT copies corrected text to the clipboard without replacing the source selection automatically; IME replacement works correctly. |
| SM-G998B / Android 15 | Android System WebView | 154.0.8037.57 (803705703) | `c69af20` | ❌ | ⚠️ | ⚠️ | The local WebView textarea showed no native spell-checker feedback; PROCESS_TEXT opens Grammalecte and produces corrected text, but WebView does not apply the returned replacement; the IME can analyze and commit corrected text successfully, but switching input methods causes WebView to lose focus/selection and requires the editor to be focused and the text selected again. |
| SM-G998B / Android 15 | Proton Mail | 7.11.9 (18324) | `5f724dd` | ❌ | ❌ | ✅ | Grammalecte was explicitly selected as the system spell checker, but the composer exposed no native spell-checker UI; the PROCESS_TEXT action was not offered for the tested selection; IME replacement works correctly, including text selected after the Grammalecte IME is already open. |
| SM-G998B / Android 15 | K-9 Mail | 23.1 (39045) | `5f724dd` | ⚠️ | ❌ | ⚠️ | Grammalecte is invoked by the editor and marks the misspelling, but K-9 exposes no usable native suggestion action when the marked word is tapped; PROCESS_TEXT is not offered; IME replacement works when text is selected before switching to Grammalecte, but a selection made after the IME is already open does not trigger a refresh. |
| SM-G998B / Android 15 | WhatsApp | 2.26.38.73 (263807322) | `5f724dd` | ⚠️ | ✅ | ⚠️ | Grammalecte is invoked and the misspelling is underlined, but WhatsApp exposes no usable native suggestion action when the marked word is tapped; PROCESS_TEXT replaces the selected text correctly; IME replacement works when text is selected before switching to Grammalecte, but a selection made after the IME is already open does not trigger a refresh. |

## October 2026 Galaxy S21 application retest summary

These manually reported results cover seven applications on one physical Samsung
Galaxy S21 (SM-G998B, Android 15/API 35), with Grammalecte selected as the
system spellchecker and the explicit French subtype `fr_FR` (`-1266611582`).
The reported Git HEAD was `735e893`, and the installed Grammalecte APK
identified itself as 0.1.0-dev (version code 1); package metadata does not
independently prove the APK's source commit.

The rows above preserve earlier test results and their application versions.
The following summary only counts the latest retest for each application:

| Application | Native spellchecking | PROCESS_TEXT | IME correction | Late IME selection |
| --- | :---: | :---: | :---: | :---: |
| WhatsApp 2.26.39.79 | ✅ | ✅ | ✅ | ✅ |
| Samsung Messages 16.0.10.49 | ⚠️ | ⚠️ | ✅ | ✅ |
| Samsung Notes 4.4.45.37 | ❌ | ❌ | ✅ | ✅ |
| Firefox 157.0.1 | ❌ | ⚠️ | ✅ | ✅ |
| Chrome 155.0.8059.39 | ❌ | ⚠️ | ✅ | ✅ |
| Proton Mail 7.11.10 | ❌ | ❌ | ✅ | ✅ |
| K-9 Mail 23.1 | ✅ | ✅ | ✅ | ✅ |
| **Works / partial / fails** | **2 / 1 / 4** | **2 / 3 / 2** | **7 / 0 / 0** | **7 / 0 / 0** |

- **Native spelling:** Samsung Messages updates the underline intermittently,
  while Samsung Notes, Firefox and Proton Mail expose no usable native
  Grammalecte suggestions. Chrome displayed an occasional underline whose
  origin was not confirmed as Grammalecte; it is not counted as a native success.
- **PROCESS_TEXT:** Samsung Messages appends the corrected phrase instead of
  replacing the source selection. Firefox and Chrome offer correction but return
  only a clipboard copy. Samsung Notes does not replace the source text; the
  precise fallback behavior was not captured. Proton Mail does not expose the
  PROCESS_TEXT menu action for the tested selection.
- **IME:** Correction and late selection worked in all seven tested editors.
  These results are specific to the listed application, Android and ROM builds.
- **Improvement:** WhatsApp and K-9 Mail now report all four checks as working.
  K-9 Mail's app version did not change since its earlier compatibility test,
  but the Grammalecte build/configuration did. The evidence does not isolate
  which change produced the improvement.
- **Follow-up:** Investigate PROCESS_TEXT result handling and read-only
  selection behavior without assuming that the host editor always supports
  replacement. Investigate whether editors request Android's selected
  SpellCheckerService before attributing missing underlines to Grammalecte.
  Preserve the working IME path as a fallback.

## Samsung Messages selection-end retest (2026-10-15)

A further manual retest on the Samsung Galaxy S21 (SM-G998B, Android 15)
confirmed that Samsung Messages inserts corrected text immediately **after
the original selection**, not necessarily at the end of the entire message.

The user selected the synthetic phrase inside a larger unsent message.
After using **Remplacer le texte sélectionné**, the visible result was:

```text
Avant : je vais au magazin.je vais au magasin. Après.
```

The original phrase remains, and the correction appears **before** the
trailing ` Après.`. This refines the earlier Samsung Messages compatibility
entry, which described the result only as appended text.

A separate metadata-only `GrammalectePT` trace showed:
- **Copier et revenir:** `opened` followed by `clipboard_copied`;
  the source editor remains unchanged and pasting is manual.
- **Remplacer le texte sélectionné:** `opened` followed by `result_ok`;
  the host editor inserted the returned correction after the selected text.
- Both invocations: `readOnly=false`, read-only extra present, calling
  activity present, and 19-character input/output lengths.

The installed application version and the exact APK source commit were not
freshly attested for this additional retest; do not reuse the previous test's
build metadata as proof of this run.

The controlled instrumentation host now models insertion at the selection
end while preserving surrounding text. **This reproduces the visible symptom,
not the internal Samsung implementation.** The Android `PROCESS_TEXT`
contract has already passed instrumentation in the controlled host; the
precise mechanism in Samsung Messages remains undetermined.

For Samsung Messages, **IME correction** was previously reported working.
The explicit **Copier et revenir** fallback only copies; it does not
automatically replace text.

An additional physical-device test used an independent, debug-only
`ACTION_PROCESS_TEXT` provider that returns a fixed synthetic correction
without invoking the Grammalecte engine. Samsung Messages again
inserted the correction without replacing the original selection:

```text
Je vais au magazin.Je vais au magasin.
```

This strongly supports a Samsung Messages host-side replacement
incompatibility independent of the grammar engine. The precise
internal cause remains unconfirmed.

The generic PROCESS_TEXT test issue #45 is closed. The Samsung
Messages bug remains tracked in issue #50. The independent probe
is preserved in the closed, unmerged PR #51 branch.

## Test procedure

### SpellCheckerService

1. Select Grammalecte Android as the system spell checker.
2. Open an editable field in the target application.
3. Enter the spelling fixture.
4. Verify whether Android invokes Grammalecte and exposes the expected correction.
5. Record the result without assuming that another editor in the same application behaves identically.

### PROCESS_TEXT

1. Enter the spelling fixture.
2. Select the complete text.
3. Open Android's text-selection actions.
4. Check whether **Corriger avec Grammalecte** is offered.
5. Apply the correction.
6. Record whether corrected text is returned to the source application or whether only the clipboard fallback is available.

### IME / InputConnection

1. Enter the spelling fixture in an editable field.
2. Select the complete text.
3. Switch temporarily to the Grammalecte IME.
4. Verify that the selected text is visible to Grammalecte.
5. Apply the correction.
6. Verify that the source editor now contains the corrected text.
7. Return to the normal keyboard.

## Recording environment information

Use:

    ./tools/collect-compatibility-env.sh PACKAGE [PACKAGE...]

For multiple ADB devices, select one with:

    ANDROID_SERIAL=<serial> ./tools/collect-compatibility-env.sh PACKAGE...

Record exact application versions whenever a compatibility result is added.

## Scope

A result describes only the tested combination of:

- physical device or emulator;
- Android version and vendor ROM;
- application and application version;
- editor or field tested;
- Grammalecte Android build.

Compatibility claims should identify the integration path that was actually
observed rather than infer support from another path.

Emulator results provide Android-version coverage but do not replace
physical-device validation. Roadmap items that explicitly require physical
devices remain open until they have been tested on real hardware.
