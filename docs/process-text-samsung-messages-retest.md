# Samsung Messages PROCESS_TEXT retest (Android 15)

Related: Samsung Messages issue #50 (open), completed contract-test issue #45 (closed), and `docs/compatibility.md`.

## Purpose

Document the observed Samsung Messages replacement failure,
the independent-provider comparison and the explicit
**Copier et revenir** fallback. Do not use real conversations,
contact names or identifiers.

The existing synthetic host integration tests prove that Grammalecte sends
one correction and host callback behavior can replace, append or ignore it.
They **do not prove** Samsung Messages implements any particular callback.

## Preconditions

- Galaxy S21 (SM-G998B), Android 15, Samsung Messages
  `com.samsung.android.messaging` (confirm package/version via ADB)
- Grammalecte debug APK built from the branch under test; preserve exact APK
  commit and app version
- Explicit `fr_FR` system spellchecker subtype
- A new **unsent** message containing only synthetic text
- No other logcat capture of private message text

Check installed versions and build environment with:

```sh
adb devices -l
./tools/collect-compatibility-env.sh com.samsung.android.messaging
adb -d shell pm list packages | grep -i 'samsung.*messag'
```

The `adb -d` commands target a USB-connected physical device.
For an emulator, use `adb -s SERIAL` instead, replacing `SERIAL`
with the identifier shown by `adb devices -l`.

The environment collection helper selects the only connected device
automatically. When several devices are connected, set `ANDROID_SERIAL`
to the intended device serial before running the helper.

The package-listing command is only a fallback if the package identifier differs.

## Opt-in, metadata-only trace

Debug builds can emit three PROCESS_TEXT lifecycle markers: `opened`,
`result_ok`, `clipboard_copied`. The fields are
`readOnly`, `readOnlyExtraPresent`, `textLength`, and
`callingActivityPresent`. No selected text, corrected text, address,
recipient, package name, or contact details are logged.

Enable the tag only while conducting the test (no persistent setting):

```sh
adb -d shell setprop log.tag.GrammalectePT DEBUG
adb -d logcat -s GrammalectePT:D '*:S'
```

Stop the foreground logcat after the test. Disable the tag again:

```sh
adb -d shell setprop log.tag.GrammalectePT INFO
```

**Privacy:** do not collect a complete logcat dump. Use only synthetic messages.
The trace can establish that Grammalecte returned or copied text, but
**cannot determine whether Samsung Messages applied the returned selection
correctly**. That requires comparing the visible editor result.

## Manual checks

| Case | Procedure | Expected evidence |
| --- | --- | --- |
| Replacement | Type `Je vais au magazin.`, select the full sentence, open **Corriger avec Grammalecte**, choose `magasin` and tap **Remplacer le texte sélectionné** | One `opened` and one `result_ok` marker; report visible result: replaced / appended / unchanged |
| Copy fallback | Reset the unsent message to the original synthetic sentence, select it, correct in Grammalecte and tap **Copier et revenir** | One `opened` and one `clipboard_copied`; Samsung selection should not be automatically replaced by Grammalecte. Manually select original and paste corrected text if desired |
| Partial selection | Type `Avant : Je vais au magazin. Après.`, select only `Je vais au magazin.`, correct via replacement | Record exact synthetic visible result and whether surrounding text remains intact |
| Selection lost | Before launching correction, select the phrase. Repeat after changing cursor focus or collapsing the selection (if action remains offered) | Record whether Samsung offers PROCESS_TEXT and the resulting text. Do not infer Android's selection state from a screenshot alone |
| Repeated invocation | Reset text; repeat the first case twice, each time with a fresh selection | Record if duplication is reproducible or intermittent |

For each result, record: application version/code, device build, debug APK commit,
test case, whether the action appeared, which button was used,
whether a result was replaced/appended/ignored, and only the metadata trace
for that synthetic test.

## Observed physical-device result (2026-10-15)

The partial-selection experiment was completed on the Galaxy S21.
After selecting the synthetic misspelling inside a longer unsent message,
**Remplacer le texte sélectionné** produced:

```text
Avant : je vais au magazin.je vais au magasin. Après.
```

The correction was inserted at the **end of the original selection**, before
the surrounding suffix, without deleting the original text. The
`GrammalectePT` trace recorded `opened` followed by `result_ok`.
This does not establish how Samsung Messages internally handles the callback.

**Copier et revenir** recorded `opened` then `clipboard_copied`.
It copied the correction and returned to the conversation, but made
**no automatic change** to the source text. The user must paste manually.

Exact installed APK commit and current Samsung Messages version were not
re-attested for this run. See issue #50 for the Samsung Messages
evidence. Issue #45 contains the completed generic contract tests.

## Independent provider comparison (PR #51)

An additional physical-device test used an independent debug-only
`ACTION_PROCESS_TEXT` provider. It returned a fixed synthetic
correction without invoking the Grammalecte engine.

Observed Samsung Messages result:

```text
Je vais au magazin.Je vais au magasin.
```

The original selection remained intact, with the correction
inserted immediately after it.

This reproduces the behavior observed with the normal
Grammalecte PROCESS_TEXT activity.

**Conclusion:** The results strongly support a Samsung Messages
host-side replacement incompatibility independent of the
Grammalecte engine.

The exact internal Samsung implementation remains unknown.

The diagnostic probe is preserved in the branch associated
with PR #51, which was closed without merging into `main`.

Issue #50 remains open. Grammalecte IME replacement is the
previously validated alternative.

## Interpretation

- `result_ok` with one corrected payload on our controlled host, but append
  only in Samsung Messages: compatible with **host-side result handling
  difference**; it does not prove the exact Samsung implementation.
- `clipboard_copied` without `result_ok`: confirms fallback bypassed the
  PROCESS_TEXT replacement callback. **Copying does not insert or paste.**
- No `opened`: selection action may not have been offered or the activity did
  not start. Check app-specific editor behavior first.
- Unexpected result in the controlled host or failing instrumentation tests:
  investigate Grammalecte before attributing the problem to Samsung.
