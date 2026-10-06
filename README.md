# Grammalecte Android — unofficial

[![CI](https://github.com/zefutoff/grammalecte-android/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/zefutoff/grammalecte-android/actions/workflows/ci.yml)
[![Android instrumentation](https://github.com/zefutoff/grammalecte-android/actions/workflows/instrumentation.yml/badge.svg?branch=main)](https://github.com/zefutoff/grammalecte-android/actions/workflows/instrumentation.yml)
[![License: GPL-3.0-only](https://img.shields.io/badge/license-GPL--3.0--only-blue.svg)](LICENSE)

A local, open-source French spelling and grammar checker for Android, powered by [Grammalecte](https://grammalecte.net/).

All correction runs on-device. The application does not request the Android `INTERNET` permission and does not use a cloud API or LLM.

> **Project status:** early development, but the correction engine is functional on a physical Android device. Native spell-checking, `PROCESS_TEXT` correction and an IME-based fallback have all been validated during development.

## Current integration

Grammalecte Android currently exposes the same local correction engine through three Android integration paths:

1. **SpellCheckerService**
   - native Android spelling and grammar suggestions;
   - integrates automatically in applications that support the platform spell-checker framework.

2. **ACTION_PROCESS_TEXT**
   - adds a **Corriger avec Grammalecte** action to Android's selected-text menu;
   - opens a correction screen with explanations and individual suggestions;
   - can return corrected text to compatible applications;
   - falls back to copying corrected text when Android exposes the selection as read-only.

3. **Grammalecte IME**
   - a correction-oriented `InputMethodService`;
   - reads the currently selected text through `InputConnection`;
   - applies corrections directly to the active editable field;
   - provides a fallback for applications that bypass `SpellCheckerService` or ignore `PROCESS_TEXT` replacement results.

The current IME is intentionally focused on correcting selected text. It is not intended to replace a full-featured typing keyboard.

## Why several integration paths?

Android applications do not all expose text correction in the same way.

A standard `EditText` can use Android's native spell-checker service, while some custom editors and web fields never call it.

`PROCESS_TEXT` works in many applications, but some editors do not accept returned replacement text.

The IME fallback uses Android's normal `InputConnection` editing interface instead of an Accessibility Service. This keeps the replacement mechanism generic across editable applications while avoiding the broader permissions and privacy surface of accessibility automation.

Manual development tests have successfully applied real Grammalecte corrections in:

- Android native text fields;
- SMS editing;
- Samsung Notes;
- Firefox text fields.

Compatibility testing is documented in the reproducible [compatibility matrix](docs/compatibility.md), including application versions, Android environment details and the integration path tested.

## Architecture

The project is split into four Gradle modules:

| Module | Responsibility |
| --- | --- |
| `core` | Pure Kotlin domain API: `GrammarEngine`, issues, ranges and normalization. |
| `engine-grammalecte` | QuickJS runtime, Grammalecte JavaScript bridge, dictionaries and mapping to the internal correction model. |
| `spellchecker` | Android `SpellCheckerService` integration and mapping to Android suggestion objects. |
| `app` | Application UI, setup tools, `PROCESS_TEXT`, IME integration and final Android packaging. |

The correction engine remains isolated behind the `GrammarEngine` contract even though Android can reach it through several framework integrations.

See [`docs/architecture.md`](docs/architecture.md) and [`docs/adr/`](docs/adr/) for the detailed design decisions.

## Embedded Grammalecte engine

The repository vendors a pinned generated Grammalecte JavaScript runtime and Graphspell dictionaries.

The upstream revision is recorded in `tools/grammalecte.env`.

The current vendoring workflow is reproducible and tied to an immutable upstream commit.

Normal builds do **not** need to regenerate the engine.

Regeneration is only required when deliberately updating or rebuilding the vendored Grammalecte assets:

    ./tools/vendor-grammalecte.sh

## Build

Development requirements:

- JDK 17;
- Android SDK 36 with Android Build Tools;
- Git;
- GNU Make;
- ShellCheck;
- curl;
- Node.js 20+.

Python 3.11 is only required when regenerating the vendored Grammalecte assets.

The Gradle wrapper is committed to the repository and is the source of truth for the Gradle version.

Run the same quality checks used by CI:

```sh
make check
```

Build the debug APK and verify its final permissions:

```sh
make assemble
```

The debug APK is generated under:

```text
app/build/outputs/apk/debug/
```

For development on a connected Android device:

```sh
./tools/run-android.sh
```

The script builds the debug APK, installs it with ADB and launches the application.

See [`CONTRIBUTING.md`](CONTRIBUTING.md) for the complete contributor workflow.

## Activating the native spell checker

After installing the APK, open Android settings and select **Grammalecte Android** as the system spell checker.

The exact path depends on the Android distribution, but is generally similar to:

    Settings
    → System
    → Languages and input
    → Spell checker

The application also provides a shortcut to the relevant Android settings screen.

## Using PROCESS_TEXT

In a compatible application:

1. select some text;
2. open the Android text-selection menu;
3. choose **Corriger avec Grammalecte**;
4. review the detected issues and suggestions;
5. return the corrected text to the application, or copy it when the source selection is read-only.

## Using the Grammalecte IME

Android exposes the correction fallback as an input method named **Grammalecte**.

Once enabled in Android's keyboard/input-method settings:

1. select text in an editable field;
2. switch temporarily to the Grammalecte input method;
3. review and apply the proposed corrections;
4. switch back to the usual keyboard.

Basic activation and keyboard-return workflows are implemented. First-run onboarding and compatibility polish are still planned before public distribution.

## Privacy invariant

The application intentionally declares **no `android.permission.INTERNET` permission**.

CI checks both the source manifests and the final packaged APK, and fails if `android.permission.INTERNET` appears after manifest merging.

This is particularly important for a spell checker or input method because selected or typed text can contain private messages, emails, searches and form data.

The current design therefore keeps correction entirely local:

    Android text
        ↓
    local Kotlin code
        ↓
    embedded QuickJS runtime
        ↓
    embedded Grammalecte engine
        ↓
    local correction result

No text is sent to an external service.

## Updating Grammalecte

Engine updates should be isolated and reviewable:

1. update `GRAMMALECTE_COMMIT` and `GRAMMALECTE_VERSION` in `tools/grammalecte.env`;
2. run `./tools/vendor-grammalecte.sh`;
3. run the complete test suite;
4. inspect the generated asset diff;
5. verify upstream license metadata;
6. add or update regression tests when correction behavior changes.

Do not track an upstream branch dynamically at build time.

## Automated checks

The repository contains GitHub Actions workflows for:

- Kotlin formatting and unit tests;
- Android lint;
- debug APK builds;
- privacy checks;
- shell script validation;
- JavaScript bridge contract tests;
- vendored Grammalecte engine smoke tests;
- Android instrumentation tests.

Dependabot monitors project dependencies, while the Gradle wrapper is deliberately updated manually because its version and distribution checksum are kept in sync with the project's bootstrap and CI configuration.

## Contributing

Contributions are welcome.

Useful ways to help include:

- Android compatibility testing across devices, ROMs and applications;
- Kotlin and Android development;
- spelling and grammar regression fixtures using synthetic French text;
- automated tests and CI improvements;
- documentation and contributor tooling;
- build reproducibility and release engineering.

For non-trivial changes, open or reference an issue before starting significant work.

See [`CONTRIBUTING.md`](CONTRIBUTING.md) for development setup, project conventions and validation commands.

## Project documents

- [`ROADMAP.md`](ROADMAP.md)
- [`CHANGELOG.md`](CHANGELOG.md)
- [`CONTRIBUTING.md`](CONTRIBUTING.md)
- [`SECURITY.md`](SECURITY.md)
- [`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md)
- [`docs/architecture.md`](docs/architecture.md)
- [`docs/testing.md`](docs/testing.md)
- [`docs/compatibility.md`](docs/compatibility.md)
- [`docs/release.md`](docs/release.md)
- [`docs/adr/`](docs/adr/)

## Name and affiliation

This is an **unofficial** Android integration of Grammalecte.

It is not affiliated with or endorsed by the Grammalecte project or Algoo.

The Android package namespace is intentionally marked `unofficial` during the current development phase and should be reviewed before the first stable public release.

## License

This project is licensed under **GPL-3.0-only**.

Grammalecte is distributed under GPLv3 and its upstream license information is preserved with the vendored sources and metadata.

QuickJS-KT is distributed under the Apache License 2.0.

See [`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md).
