# Third-party notices

This document lists the main third-party components directly included in or
used at runtime by Grammalecte Android.

Test-only dependencies and build tools are not listed here.

## Grammalecte

- Project: Grammalecte
- Upstream: https://github.com/algoo/grammalecte
- Version: 2.3.0
- Commit: `47af2080202e647110e5199ebd5d4b51d2bd51db`
- License: GPL-3.0
- Purpose: French spelling and grammar engine and Graphspell dictionaries

The vendoring script builds the upstream JavaScript distribution from source
instead of downloading an opaque prebuilt artifact.

The exact vendored revision is also recorded in:

- `tools/grammalecte.env`
- `third_party/grammalecte/UPSTREAM.json`
- `engine-grammalecte/src/main/assets/grammalecte/UPSTREAM.txt`

Upstream license files are preserved under `third_party/grammalecte/`.

## QuickJS-KT

- Project: quickjs-kt
- Upstream: https://github.com/dokar3/quickjs-kt
- Maven artifact: `io.github.dokar3:quickjs-kt:1.0.15`
- Version: 1.0.15
- License: Apache-2.0
- Purpose: Kotlin/Android binding and native runtime used to execute the embedded Grammalecte JavaScript engine

QuickJS-KT includes native QuickJS sources as part of its distribution.
The native QuickJS revision is therefore controlled by the pinned QuickJS-KT
dependency rather than independently pinned by this project.

## Kotlin Coroutines

- Project: kotlinx.coroutines
- Upstream: https://github.com/Kotlin/kotlinx.coroutines
- Maven artifact: `org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0`
- Version: 1.11.0
- License: Apache-2.0
- Purpose: coroutine support used by the Grammalecte engine integration

## Maintenance

Review this file whenever:

- a runtime dependency is added, removed or upgraded;
- the embedded Grammalecte revision changes;
- the JavaScript runtime changes;
- the final packaged native libraries change.

The definitive direct dependency versions are declared in
`gradle/libs.versions.toml`.
