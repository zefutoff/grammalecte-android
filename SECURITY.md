# Security policy

## Reporting a vulnerability

Please use GitHub's **private security advisory** feature for vulnerabilities that could:

- expose typed or selected text;
- bypass the offline-only design;
- introduce unexpected network access;
- load untrusted JavaScript or dictionary data;
- escape the expected Android service boundaries;
- allow unintended text replacement through the IME;
- bypass the expected Grammalecte asset-loading restrictions.

Do not include real private text in reports.

Use a minimal synthetic reproduction whenever possible.

## Security model

Grammalecte Android is designed as a fully local correction tool.

Its main security and privacy assumptions are:

- Grammalecte assets are bundled at build time from a pinned upstream commit;
- no JavaScript is downloaded at runtime;
- no dictionaries are downloaded at runtime;
- the application does not request `android.permission.INTERNET`;
- cleartext network traffic is disabled in the Android application manifest;
- correction text is processed locally;
- asset paths passed through the JavaScript bridge reject parent-directory traversal;
- QuickJS execution is memory-bounded;
- QuickJS initialization and analysis are evaluation-time-bounded;
- access to a QuickJS runtime is serialized by the engine.

## Input method security

The application includes an Android `InputMethodService` used as a correction-oriented IME.

Input methods occupy a sensitive position because Android allows them to interact with editable text.

The Grammalecte IME is intentionally limited to correction workflows:

- it reads selected text through `InputConnection`;
- it analyzes that text locally;
- it presents correction suggestions;
- it replaces text only through the active editor's `InputConnection`.

Before applying a correction, the implementation verifies that the current selection still matches the text that was originally analyzed.

If the selection changed while analysis was running, the correction must not be applied automatically.

The project does not require an Android Accessibility Service for text replacement.

## Offline-only invariant

The absence of `android.permission.INTERNET` is a core project invariant.

CI should continue checking that the final Android manifest does not unexpectedly gain network permission.

Adding network access, telemetry, cloud correction, remote JavaScript, or remote dictionary loading would require:

1. an explicit architecture decision;
2. a security review;
3. a privacy review;
4. user-visible documentation;
5. corresponding release-note changes.

## Dependency and supply-chain considerations

Dependencies and GitHub Actions should be updated through reviewed changes.

The Gradle wrapper is deliberately updated manually so that:

- the Gradle version;
- the official distribution URL;
- the distribution SHA-256;
- the generated wrapper files;
- CI configuration

remain synchronized.

Grammalecte updates must continue to use the pinned vendoring workflow and should not silently track an upstream branch.

## Sensitive test data

Tests, bug reports and compatibility fixtures must not contain:

- private messages;
- real emails;
- passwords;
- API keys;
- authentication tokens;
- personal documents;
- copied user conversations.

Use synthetic French text that reproduces the behavior without exposing private information.
