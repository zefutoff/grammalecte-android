# Contributing

## Development workflow

1. Open or reference an issue for non-trivial changes.
2. Create a focused branch.
3. Add tests with the change.
4. Run `make check` and `make assemble`.
5. Open a pull request and complete the template.

Small documentation and typo fixes do not require a prior issue.

## Design rules

- `core` must remain independent from Android and QuickJS.
- `spellchecker` depends on the `GrammarEngine` abstraction, not JavaScript details.
- Grammalecte-specific code belongs in `engine-grammalecte`.
- The `app` module owns Android-facing user workflows such as setup, `PROCESS_TEXT` and the correction IME, while engine-specific logic remains in `engine-grammalecte`.
- Avoid adding dependencies unless they clearly reduce maintenance risk.
- Do not add analytics, telemetry or network access without an explicit architecture decision and community review.

## Tests

A change should normally land with the narrowest useful test:

- pure text/range behavior → `core` unit test;
- asset/runtime adapter behavior → `engine-grammalecte` unit test;
- Android framework mapping → `spellchecker` unit/instrumentation test;
- manifest/discovery behavior → `app` instrumentation test.

French correction regressions should use short synthetic sentences. Do not commit private messages or real user text as fixtures.

## Formatting

Kotlin formatting is enforced by ktlint. CI runs `ktlintCheck` with warnings treated as errors by the Kotlin compiler.

## Commits

Use concise imperative commit messages. Conventional Commits are welcome but not required.

Examples:

- `feat: map grammar errors to Android 12 attributes`
- `fix: preserve UTF-16 offsets before emoji`
- `test: cover apostrophe suggestions`
- `chore: update Grammalecte to 2.3.2`

## Architecture decisions

Create a new file under `docs/adr/` when a change affects module boundaries, privacy, runtime choice, upstream vendoring or compatibility policy. Copy the style of existing ADRs.
