# Grammalecte Android — unofficial

A local, open-source French spelling and grammar checker for Android, powered by [Grammalecte](https://grammalecte.net/) and exposed through Android's native `SpellCheckerService` framework.

> **Project status:** early development. The Android service, modular architecture, QuickJS bridge, upstream vendoring workflow, tests and CI scaffolding are present. The repository must vendor the pinned Grammalecte JavaScript build before an APK can perform corrections.

## Goals

- Correct French spelling **and grammar** in Android text fields that support the platform spell-checker framework.
- Run fully on-device: no cloud API, no LLM and no `INTERNET` permission.
- Keep the Android integration independent from the embedded Grammalecte runtime.
- Make upstream engine updates reproducible and reviewable.
- Treat automated tests, static analysis and contributor documentation as first-class project code.

## Architecture

```text
app
 └─ spellchecker
     ├─ core
     └─ engine-grammalecte
         └─ QuickJS-KT
             └─ vendored Grammalecte JavaScript
```

| Module | Responsibility |
| --- | --- |
| `core` | Pure Kotlin domain API (`GrammarEngine`, issues, normalization). No Android dependency. |
| `engine-grammalecte` | QuickJS runtime, Android asset loading and Grammalecte JS bridge. |
| `spellchecker` | Android `SpellCheckerService` and mapping to `SuggestionsInfo` / `SentenceSuggestionsInfo`. |
| `app` | Thin launcher/setup UI. No correction logic. |

The dependency direction is deliberate: Android UI and framework code never know how Grammalecte is executed internally.

## First build

Requirements:

- JDK 17
- Android SDK 36
- Python 3.11 recommended for the upstream Grammalecte builder (newer versions need a `distutils` compatibility layer such as setuptools)
- Git
- Node.js 20+ (bridge contract test)
- `curl`, `unzip` and `sha256sum` for the one-time Gradle Wrapper bootstrap

Vendor the exact pinned Grammalecte source and generate its JavaScript runtime:

```bash
./tools/vendor-grammalecte.sh
```

Then bootstrap the Gradle wrapper once. The script verifies the official Gradle 9.6.1 binary ZIP checksum before running the wrapper task:

```bash
./tools/bootstrap-gradle-wrapper.sh
```

Build and test:

```bash
./gradlew ktlintCheck :core:test :engine-grammalecte:test :spellchecker:test :app:lintDebug :app:assembleDebug
```

The debug APK will be under `app/build/outputs/apk/debug/`.

## Activating on Android

After installing the APK, open Android settings and select **Grammalecte Android** as the spell checker. The exact path varies by ROM, generally under:

`System > Languages & input > Spell checker`

The app itself exposes a button that opens the broader input-method settings screen.

## Privacy invariant

The project intentionally declares **no `android.permission.INTERNET` permission**. A CI check fails if that permission appears in one of the project manifests.

This matters because text submitted to a system spell checker can contain private messages, emails, searches or form data. The default architecture therefore keeps all analysis on-device.

## Updating Grammalecte

The upstream revision is pinned in `tools/grammalecte.env`.

An engine update should be a dedicated pull request:

1. Change `GRAMMALECTE_COMMIT` and `GRAMMALECTE_VERSION`.
2. Run `./tools/vendor-grammalecte.sh`.
3. Run the complete test suite.
4. Review the generated asset diff and upstream license metadata.
5. Add/adjust regression tests for any changed correction behavior.

Do not silently track an upstream branch at build time.

## Contribution model

See [`CONTRIBUTING.md`](CONTRIBUTING.md), [`CHANGELOG.md`](CHANGELOG.md), [`ROADMAP.md`](ROADMAP.md), [`docs/architecture.md`](docs/architecture.md) and the ADRs under [`docs/adr/`](docs/adr/).

Important rules:

- feature work goes through pull requests;
- public APIs between modules require tests;
- architecture changes require an ADR;
- bug fixes should include a regression test where feasible;
- generated Grammalecte updates must remain pinned to an immutable commit;
- new network permissions are considered a breaking privacy change.

## Name and affiliation

This is an **unofficial** Android integration. It is not affiliated with or endorsed by the Grammalecte project or Algoo. The package namespace is intentionally marked `unofficial` for the initial development phase and should be reviewed before the first public release.

## License

This project is licensed under **GPL-3.0-only**. Grammalecte is also distributed under GPLv3; its upstream license is preserved when vendored. QuickJS-KT is an Apache-2.0 dependency. See [`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md).
