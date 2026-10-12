# Third-party notices

This document lists third-party components included in or used at runtime by
Grammalecte Android.

Test-only dependencies and build-only tools are not listed here.

## Grammalecte

- Project: Grammalecte
- Upstream: https://github.com/algoo/grammalecte
- Version: 2.3.0
- Commit: `47af2080202e647110e5199ebd5d4b51d2bd51db`
- License: GPL-3.0
- Purpose: French spelling and grammar engine and Graphspell dictionaries

The vendoring script builds the upstream JavaScript distribution from source
instead of downloading an opaque prebuilt artifact.

The exact vendored revision is recorded in:

- `tools/grammalecte.env`
- `third_party/grammalecte/UPSTREAM.json`
- `engine-grammalecte/src/main/assets/grammalecte/UPSTREAM.txt`

Upstream Grammalecte license files are preserved under:

    third_party/grammalecte/

## QuickJS-KT

- Project: quickjs-kt
- Upstream: https://github.com/dokar3/quickjs-kt
- Maven artifact: `io.github.dokar3:quickjs-kt:1.0.15`
- Android artifact: `io.github.dokar3:quickjs-kt-android:1.0.15`
- Version: 1.0.15
- Upstream revision reviewed: `a081755bcdb18ee6afa7ffee25b8fa976f06d31f`
- License: Apache-2.0
- Purpose: Kotlin/Android binding used to execute the embedded Grammalecte JavaScript engine

License text:

    third_party/licenses/quickjs-kt/LICENSE.txt

### Embedded QuickJS

QuickJS-KT embeds the QuickJS native engine.

- Project: QuickJS
- Upstream: https://github.com/bellard/quickjs
- Commit used by QuickJS-KT 1.0.15:
  `04be246001599f5995fa2f2d8c91a0f198d3f34c`
- License: MIT
- Copyright: Fabrice Bellard and Charlie Gordon

License text:

    third_party/licenses/quickjs/LICENSE

### Embedded c-vector

QuickJS-KT uses c-vector in its JNI/native binding layer.

- Project: c-vector
- Upstream: https://github.com/eteran/c-vector
- Commit used by QuickJS-KT 1.0.15:
  `774773d4cb1e66dd736e90e7f482d4f22af464c6`
- License: MIT
- Copyright: Evan Teran

License text:

    third_party/licenses/c-vector/LICENSE

## Kotlin standard library

- Project: Kotlin
- Upstream: https://github.com/JetBrains/kotlin
- Maven artifact: `org.jetbrains.kotlin:kotlin-stdlib:2.4.20`
- Version: 2.4.20
- License: Apache-2.0

License text:

    third_party/licenses/kotlin/LICENSE.txt

The Kotlin standard library contains code derived from additional
open-source projects. Their upstream notices are preserved under:

    third_party/licenses/kotlin/third_party/

This includes:

- GWT-derived collections code — Apache-2.0
- ThreeTenBP-derived time code — BSD-3-Clause
- Guava-derived unsigned-number code — Apache-2.0
- Boost-derived JVM math code — Boost Software License 1.0

## Kotlin Coroutines

- Project: kotlinx.coroutines
- Upstream: https://github.com/Kotlin/kotlinx.coroutines
- Maven artifact: `org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0`
- Version: 1.11.0
- License: Apache-2.0

License and NOTICE files:

    third_party/licenses/kotlinx-coroutines/LICENSE.txt
    third_party/licenses/kotlinx-coroutines/NOTICE.txt

## JetBrains annotations

- Project: JetBrains Java Annotations
- Upstream: https://github.com/JetBrains/java-annotations
- Resolved Maven artifact: `org.jetbrains:annotations:23.0.0`
- Version: 23.0.0
- License: Apache-2.0

Some dependency metadata requests version 13.0, but Gradle resolves the runtime
dependency to 23.0.0. Only the resolved 23.0.0 artifact is distributed.

License text:

    third_party/licenses/jetbrains-annotations/LICENSE.txt

## Maintenance

Review this document whenever:

- a runtime dependency is added, removed or upgraded;
- the embedded Grammalecte revision changes;
- QuickJS-KT changes version;
- QuickJS-KT changes its native QuickJS or c-vector revisions;
- the final packaged native libraries change.

The dependency versions used by the project are declared in:

    gradle/libs.versions.toml

The generated runtime dependency inventory is available as a CycloneDX SBOM.
