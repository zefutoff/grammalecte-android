# Contributing

Contributions are welcome, including code, tests, Android compatibility reports, documentation and build/CI improvements.

## Communication language

Code, commit messages, issues, pull requests and technical documentation should be written in English.

User-facing application strings may remain in French where appropriate.

## Development setup

Requirements:

- JDK 17;
- Android SDK 36 with Android Build Tools;
- Git;
- GNU Make;
- ShellCheck;
- curl;
- Node.js 20+.

Python 3.11 is additionally required when regenerating the vendored Grammalecte engine.

Clone the repository and run the same checks used by CI:

```sh
git clone https://github.com/zefutoff/grammalecte-android.git
cd grammalecte-android
make check
make assemble
```

`make check` is the canonical quality entry point. It runs the source privacy check, ShellCheck, the JavaScript bridge contract test, ktlint, JVM tests and Android lint.

`make assemble` builds the debug APK and verifies that the final packaged application does not request `android.permission.INTERNET`.

## Development workflow

1. Open or reference an issue for non-trivial changes.
2. Create a focused branch from `main`.
3. Keep the change scoped to one problem.
4. Add or update the narrowest useful tests.
5. Run `make check` and `make assemble`.
6. Update documentation or `CHANGELOG.md` when user-visible behavior changes.
7. Open a pull request and complete the pull-request template.

Small documentation and typo fixes do not require a prior issue.

## Good contribution areas

Useful contribution areas include:

- Android compatibility testing;
- `SpellCheckerService` integration;
- `PROCESS_TEXT` compatibility;
- IME integration and lifecycle testing;
- French correction regression fixtures;
- Android-version coverage;
- documentation;
- CI and reproducible builds;
- accessibility and usability improvements that preserve the project's privacy model.

See [`ROADMAP.md`](ROADMAP.md) for planned work.

## Design rules

- `core` must remain independent from Android and QuickJS.
- `spellchecker` depends on the `GrammarEngine` abstraction, not JavaScript details.
- Grammalecte-specific code belongs in `engine-grammalecte`.
- The `app` module owns Android-facing user workflows such as setup, `PROCESS_TEXT` and the correction IME, while engine-specific logic remains in `engine-grammalecte`.
- Avoid adding dependencies unless they clearly reduce maintenance risk.
- Do not add analytics, telemetry or network access without an explicit architecture decision and community review.
- Do not introduce Accessibility Service based text replacement without a new architecture decision.

## Tests

A change should normally land with the narrowest useful test:

- pure text/range behavior → `core` unit test;
- asset/runtime adapter behavior → `engine-grammalecte` unit test;
- real Grammalecte behavior → real-engine regression test;
- Android framework mapping → `spellchecker` instrumentation test;
- manifest/discovery or packaged-engine behavior → `app` instrumentation test.

French correction regressions should use short synthetic sentences.

Never commit private messages, real emails, credentials, personal documents or real user conversations as fixtures.

See [`docs/testing.md`](docs/testing.md) for the complete testing strategy.

## Grammalecte engine changes

The embedded Grammalecte revision is intentionally pinned.

When updating it:

1. update `GRAMMALECTE_COMMIT` and `GRAMMALECTE_VERSION` in `tools/grammalecte.env`;
2. run `./tools/vendor-grammalecte.sh`;
3. run `./tools/check-vendor.sh`;
4. inspect the generated diff;
5. run `make check` and `make assemble`;
6. add or update regression tests when correction behavior changes.

The vendored output must remain reproducible.

## Dependency updates

Gradle dependency verification is enabled and the trusted SHA-256 checksums
are stored in `gradle/verification-metadata.xml`.

When a Gradle dependency is added or updated:

1. use JDK 17;
2. update the dependency declaration or review the Dependabot change;
3. run `make refresh-verification-metadata`;
4. inspect the `gradle/verification-metadata.xml` diff and verify that every
   newly trusted artifact belongs to the intended dependency graph;
5. run `make check` and `make assemble`;
6. commit the dependency change and verification metadata together.

Do not blindly accept newly generated checksums. Dependency verification is a
supply-chain control, so additions to the trust metadata must be reviewed.

A Dependabot Gradle pull request may initially fail CI until its new artifacts
have been deliberately added to `gradle/verification-metadata.xml`.

The Gradle wrapper remains a separate manual update because its distribution
SHA-256 and bootstrap script must stay synchronized.

## Formatting

Kotlin formatting is enforced by ktlint.

Shell scripts are checked with ShellCheck.

Do not submit formatting-only changes mixed with unrelated functional changes unless the formatting is required by the modified code.

## Commits

Use concise imperative commit messages.

Conventional Commits are welcome but not required.

Examples:

- `feat: map grammar errors to Android 12 attributes`
- `fix: preserve UTF-16 offsets before emoji`
- `test: cover apostrophe suggestions`
- `docs: clarify compatibility testing`
- `ci: verify vendored engine reproducibility`
- `chore: update Grammalecte to 2.3.2`

## Architecture decisions

Create a new file under `docs/adr/` when a change affects:

- module boundaries;
- privacy or network access;
- runtime choice;
- upstream vendoring;
- Android integration strategy;
- compatibility policy.

Copy the style of the existing ADRs.

Significant architecture changes should be discussed in an issue before implementation.

## Security

Security-sensitive issues should follow [`SECURITY.md`](SECURITY.md).

Do not open a public issue containing private text, credentials or vulnerability details that could expose user data.
