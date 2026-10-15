# Samsung Messages PROCESS_TEXT retest (Android 15)

Related: unresolved Samsung Messages bug #50; completed contract-test issue #45;
compatibility campaign in `docs/compatibility.md`.

## Purpose

Determine whether the Samsung Messages editor duplicates a valid returned
`ACTION_PROCESS_TEXT` result and validate the explicit **Copier et revenir**
fallback. Do not use real conversations, contact names or identifiers.

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
./tools/collect-compatibility-env.sh com.samsung.android.messaging
adb -s "$ANDROID_SERIAL" shell pm list packages | grep -i samsung.*messag
```

The second command is only a fallback if the package identifier differs.

## Opt-in, metadata-only trace

Debug builds can emit three PROCESS_TEXT lifecycle markers: `opened`,
`result_ok`, `clipboard_copied`. The fields are
`readOnly`, `readOnlyExtraPresent`, `textLength`, and
`callingActivityPresent`. No selected text, corrected text, address,
recipient, package name, or contact details are logged.

Enable the tag only while conducting the test (no persistent setting):

```sh
adb -s "$ANDROID_SERIAL" shell setprop log.tag.GrammalectePT DEBUG
adb -s "$ANDROID_SERIAL" logcat -c
adb -s "$ANDROID_SERIAL" logcat -s GrammalectePT:D '*:S'
```

Stop the foreground logcat after the test. Disable the tag again:

```sh
adb -s "$ANDROID_SERIAL" shell setprop log.tag.GrammalectePT INFO
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
re-attested for this run. See issue #45 for the evidence.

## Differential diagnosis with the independent debug provider

PR for issue #50 adds an additional **debug-only** entry in the text-selection
menu: **Grammalecte : sonde (debug)**. It does not call the Grammalecte engine,
the IME, the clipboard or the network. It is **not present in release APKs**.
It offers a return button only for two exact, fictional French selections:
`Je vais au magazin.` and `je vais au magazin.`. All other selections,
read-only fields and unknown inputs have the return action disabled.

This comparison is more informative than simulating an editor that appends
text, because **Samsung Messages itself** receives two distinct
`ACTION_PROCESS_TEXT` results from independent providers.

1. Build/install the **debug APK from the PR branch**, not an older `main`
   APK. Use `git rev-parse --short HEAD` to record the commit.
2. Create a new **unsent** draft in Samsung Messages using exactly
   `Avant : Je vais au magazin. Après.`.
3. Select only `Je vais au magazin.`, choose the normal Grammalecte
   `PROCESS_TEXT` action, correct `magazin` to `magasin` and select
   **Remplacer le texte sélectionné**. Record the visible result.
4. **Reset** the draft to the original sentence; select the same phrase
   again. In the selection menu (possibly under overflow), choose
   **Grammalecte : sonde (debug)**, then **Renvoyer la correction de test**.
   Record the complete resulting sentence. The probe waits for user
   confirmation before returning a single hardcoded corrected phrase.
5. Optional control: repeat step 4 in WhatsApp or K-9 Mail using only a
   fresh synthetic **unsent** draft, to check normal host behavior.

If only the normal Grammalecte action appends, investigate the difference
between the two activities (lifecycle, UI, selection state, and timing).
If **both** append at the same selection boundary, that is strong evidence
of host-dependent behavior unrelated to the grammar engine. It still does
not reveal Samsung's implementation or prove that every `PROCESS_TEXT`
provider must fail.

Opt-in, metadata-only probe logs (no text, identifiers, or content lengths):

```sh
adb devices -l
adb -d shell setprop log.tag.GrammalectePT DEBUG
adb -d shell setprop log.tag.GrammalecteProbe DEBUG
adb -d logcat -s GrammalectePT:D GrammalecteProbe:D '*:S'
```

Stop logcat with Ctrl+C and restore the default tag levels:

```sh
adb -d shell setprop log.tag.GrammalectePT INFO
adb -d shell setprop log.tag.GrammalecteProbe INFO
```

The probe logs only `opened`, `result_ok`, or `cancelled`, plus
`fixtureAccepted`, `readOnly`, and `callingActivityPresent`.
An accepted fixture **must** still be manually confirmed by tapping the
return button.

The standard Android result contract is `RESULT_OK` with
`Intent.EXTRA_PROCESS_TEXT`. AOSP `Editor` explicitly preserves the
selection when starting a `PROCESS_TEXT` action. Losing the selection
and collapsing it at the end fits the Samsung symptom, but this is a
**hypothesis**, not an established Samsung source-code fact:

- https://developer.android.com/reference/kotlin/android/content/Intent#ACTION_PROCESS_TEXT
- https://android.googlesource.com/platform/frameworks/base/+/master/core/java/android/widget/Editor.java

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
