# Release process

The repository now contains the committed Gradle wrapper and pinned Grammalecte assets required for normal builds.

A public release should still be produced only from a clean checkout and after all release checks below succeed.

## Release checklist

1. CI is green on the target commit.
2. The working tree is clean.
3. `tools/check-no-network-permission.sh` passes.
4. `tools/check-vendor.sh` passes.
5. JVM tests pass.
6. Android lint passes.
7. Android instrumentation tests pass.
8. The release APK or AAB is built from the exact tagged commit.
9. The packaged application is inspected for unexpected permissions.
10. The application still declares no `android.permission.INTERNET`.
11. Real spelling and grammar correction are smoke-tested on a physical device.
12. `SpellCheckerService`, `PROCESS_TEXT` and IME setup are checked on the target release build.
13. Third-party notices and licenses are reviewed.
14. The changelog contains all user-visible changes.
15. Release metadata records the Android app version and embedded Grammalecte revision.

## Build environment

Release documentation should record at least:

- Gradle version;
- Android Gradle Plugin version;
- Kotlin version;
- JDK version;
- compile SDK;
- minimum Android API;
- Grammalecte version;
- pinned Grammalecte commit.

The Gradle wrapper should be used for release builds.

## Gradle wrapper updates

The wrapper version is deliberately maintained manually.

When upgrading Gradle:

1. update `tools/bootstrap-gradle-wrapper.sh`;
2. update its official distribution SHA-256;
3. regenerate the wrapper;
4. review `gradlew`, `gradlew.bat` and `gradle/wrapper/*`;
5. run the complete CI suite;
6. commit the wrapper update as one reviewed change.

Dependabot does not automatically update the Gradle wrapper.

## Versioning

Use semantic versioning for the Android integration.

Grammalecte's version is tracked separately in `tools/grammalecte.env`.

Example:

- Android app `0.3.0`;
- embedded Grammalecte `2.3.0`.

An upstream engine update does not automatically require an Android major version bump unless it changes the integration contract or user-visible compatibility policy.

## Tagging

A release tag should identify exactly the commit used to build the published APK or AAB.

Do not rebuild a published version from a later commit under the same version number.

## Before the first stable release

The following project-level items are still expected before declaring a stable public release:

- application icon and final visual identity;
- polished first-run setup;
- compatibility matrix;
- broader Android-version testing;
- IME instrumentation coverage;
- automated real-engine correction smoke test;
- signed release pipeline;
- reproducibility review;
- final privacy and security review.
