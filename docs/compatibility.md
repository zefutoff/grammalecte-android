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

## Compatibility matrix

| Device / Android | Application | Version (code) | Grammalecte build | SpellCheckerService | PROCESS_TEXT | IME / InputConnection | Notes |
| --- | --- | --- | --- | :---: | :---: | :---: | --- |
| SM-G998B / Android 15 | Samsung Notes | 4.4.45.37 (444537000) | `6c0dde2` | ❌ | ⚠️ | ⚠️ | Grammalecte was explicitly selected as the system spell checker, but Samsung Notes exposed no native suggestion UI; PROCESS_TEXT falls back to the clipboard instead of replacing the selection; IME replacement succeeds, but Samsung Notes keeps a stale red underline even after focus changes and further text edits. |
| SM-G998B / Android 15 | Firefox | 157.0 (2016186455) | `6c0dde2` | ❌ | ⚠️ | ✅ | Grammalecte was explicitly selected as the system spell checker, but Firefox did not invoke native spell-checker UI for the tested field; PROCESS_TEXT copies corrected text to the clipboard without replacing the source selection automatically; IME replacement works correctly. |
| SM-G998B / Android 15 | Samsung Messages | 16.0.10.49 (1601000049) | `6c0dde2` | ❌ | ⚠️ | ✅ | Grammalecte SpellCheckerService was explicitly selected but no native suggestion UI was exposed; PROCESS_TEXT appends corrected text after the original selection instead of replacing it; IME replacement works correctly. |
| SM-G998B / Android 15 | Chrome | 131.0.6778.260 (677826031) | `6c0dde2` | ❌ | ⚠️ | ✅ | Grammalecte was explicitly selected as the system spell checker, but the tested Chromium textarea exposed no native spell-checker UI; PROCESS_TEXT copies corrected text to the clipboard without replacing the source selection automatically; IME replacement works correctly. |
| SM-G998B / Android 15 | Android System WebView | 154.0.8037.57 (803705703) | `c69af20` | ❌ | ⚠️ | ⚠️ | The local WebView textarea showed no native spell-checker feedback; PROCESS_TEXT opens Grammalecte and produces corrected text, but WebView does not apply the returned replacement; the IME can analyze and commit corrected text successfully, but switching input methods causes WebView to lose focus/selection and requires the editor to be focused and the text selected again. |
| SM-G998B / Android 15 | Proton Mail | 7.11.9 (18324) | `5f724dd` | ❌ | ❌ | ✅ | Grammalecte was explicitly selected as the system spell checker, but the composer exposed no native spell-checker UI; the PROCESS_TEXT action was not offered for the tested selection; IME replacement works correctly, including text selected after the Grammalecte IME is already open. |
| SM-G998B / Android 15 | K-9 Mail | 23.1 (39045) | `5f724dd` | ⚠️ | ❌ | ⚠️ | Grammalecte is invoked by the editor and marks the misspelling, but K-9 exposes no usable native suggestion action when the marked word is tapped; PROCESS_TEXT is not offered; IME replacement works when text is selected before switching to Grammalecte, but a selection made after the IME is already open does not trigger a refresh. |
| SM-G998B / Android 15 | WhatsApp | 2.26.38.73 (263807322) | `5f724dd` | ⚠️ | ✅ | ⚠️ | Grammalecte is invoked and the misspelling is underlined, but WhatsApp exposes no usable native suggestion action when the marked word is tapped; PROCESS_TEXT replaces the selected text correctly; IME replacement works when text is selected before switching to Grammalecte, but a selection made after the IME is already open does not trigger a refresh. |

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
