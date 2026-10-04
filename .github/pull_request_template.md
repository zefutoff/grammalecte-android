## What changed

<!-- Describe the user-visible, technical or architectural change. -->

## Why

<!-- Link the issue or ADR when relevant. -->

## Android integration paths

<!-- Remove paths that are not relevant. -->

- SpellCheckerService
- ACTION_PROCESS_TEXT
- IME / InputConnection
- Application UI / setup
- Engine only
- Build / CI / documentation only

## Validation

- [ ] Tests were added or updated where appropriate
- [ ] `make check` passes
- [ ] `make assemble` passes
- [ ] Android instrumentation was run when the change affects Android framework behavior
- [ ] No private or real user text was added as a fixture
- [ ] User-visible changes are reflected in documentation or `CHANGELOG.md`
- [ ] ADR added or updated if module boundaries, runtime, privacy or integration policy changed

## Privacy

- [ ] The change preserves the offline-only architecture
- [ ] The final APK still declares no `android.permission.INTERNET`
- [ ] No new sensitive Android permission or data flow was introduced without explicit review

## Engine changes

- [ ] Not applicable
- [ ] Grammalecte commit/version updated in `tools/grammalecte.env`
- [ ] Vendored assets regenerated from source
- [ ] `tools/check-vendor.sh` passes
- [ ] Regenerated assets are reproducible
- [ ] Regression fixtures cover relevant correction changes

## Additional notes

<!-- Compatibility observations, screenshots, follow-up work, known limitations, etc. -->
