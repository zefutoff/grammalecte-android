# Release process

The repository now contains the committed Gradle wrapper and pinned Grammalecte assets required for normal builds.

A public release should still be produced only from a clean checkout and after all release checks below succeed.

## Reproducible unsigned release build

The current reproducibility guarantee covers the unsigned release APK.
Signing is handled separately and is not yet part of this guarantee.

Two builds are considered reproducible when they:

- start from the exact same Git commit;
- use separate source worktrees;
- use the committed Gradle wrapper;
- disable the Gradle build and configuration caches;
- produce byte-for-byte identical APK files.

Run:

    make release-reproducibility

The command builds the application twice from separate temporary Git
worktrees and compares both SHA-256 hashes and the final APK bytes.

The expected artifact is:

    app/build/outputs/apk/release/app-release-unsigned.apk

The initial validation produced identical unsigned APKs from two independent
worktrees.

The release build currently uses:

- Gradle 9.6.1;
- Android Gradle Plugin 9.4.1;
- Kotlin 2.4.21;
- JDK 17;
- compileSdk 36;
- targetSdk 36;
- minSdk 26;
- Grammalecte 2.3.0;
- Grammalecte commit 47af2080202e647110e5199ebd5d4b51d2bd51db.

Gradle dependency verification is enabled through
`gradle/verification-metadata.xml`.

The Gradle distribution checksum is pinned in
`gradle/wrapper/gradle-wrapper.properties`.

### Manual release build

Build the unsigned release APK with:

    ./gradlew :app:assembleRelease \
        --no-daemon \
        --no-build-cache \
        --no-configuration-cache

Verify the offline privacy invariant with:

    ./tools/check-apk-permissions.sh \
        app/build/outputs/apk/release/app-release-unsigned.apk

Record its SHA-256 with:

    sha256sum app/build/outputs/apk/release/app-release-unsigned.apk

The current release build may warn that `libquickjs.so` cannot be stripped.
This is currently non-fatal. The reproducibility check compares the final APK
bytes and therefore still detects differences in the packaged native library.

### Signed release pipeline

The reproducibility procedure above still applies to the unsigned APK.
Signing is performed as a separate step.

Private signing keys must never be committed to the repository.

The expected release signing certificate SHA-256 fingerprint is stored in:

    config/release-signing-cert.sha256

Local signing requires the following environment variables:

    GRAMMALECTE_SIGNING_KEYSTORE
    GRAMMALECTE_SIGNING_KEY_ALIAS
    GRAMMALECTE_SIGNING_STORE_PASSWORD
    GRAMMALECTE_SIGNING_KEY_PASSWORD

Run:

    make signed-release

This command:

1. builds the unsigned release APK with Gradle caches disabled;
2. verifies that the unsigned APK does not request INTERNET permission;
3. signs it with `tools/sign-release-apk.sh`;
4. verifies the APK signature;
5. verifies that the signer certificate matches the committed fingerprint;
6. verifies the privacy invariant again on the signed APK.

The resulting local artifact is:

    app/build/outputs/apk/release/app-release-signed.apk

GitHub Actions also provides the manually triggered workflow:

    .github/workflows/release.yml

The workflow uses the protected `release` environment and is restricted to
the `main` branch. Its signing material is stored only as GitHub environment
secrets.

The workflow currently uploads the signed APK and its SHA-256 checksum as
GitHub Actions artifacts. It does not create or publish a GitHub Release.

The workflow does not automatically publish a GitHub Release.
The first signed release candidate, `v1.0.0-rc1`, was published
separately with its APK, CycloneDX SBOM and `SHA256SUMS`.
Future releases must preserve the existing signing identity.

## Release checklist

1. CI is green on the target commit.
2. The working tree is clean.
3. `make check` passes.
4. `tools/check-vendor.sh` passes.
5. Regenerating the pinned Grammalecte assets produces no Git diff.
6. `make instrumented` passes on a dedicated Android device or emulator.
   Its XML report validator must confirm that both suites actually ran.
7. The release APK or AAB is built from the exact tagged commit.
8. The packaged application is inspected for unexpected permissions.
9. `tools/check-apk-permissions.sh` confirms that the final APK does not request `android.permission.INTERNET`.
10. The application still satisfies the documented offline-only invariant.
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

The signed `v1.0.0-rc1` release has been published.

The signing workflow, SBOM, F-Droid recipe, signing certificate pinning,
third-party license review and final application ID are in place.

The remaining release work is:

1. Complete the pre-stable acceptance checks on the intended release commit.
2. Investigate the GrapheneOS native spell-checker failure (issue #34).
3. Document the Samsung Messages `PROCESS_TEXT` limitation (issue #50).
4. Review supported-device compatibility claims and release notes.
5. Update `versionName` and increment `versionCode` beyond RC1's value of 2.
6. Build, sign and verify the release APK from the exact versioned source.
7. Publish the stable tag, APK, SBOM, checksums and localized changelogs.

F-Droid inclusion is tracked in upstream merge request !54224:
https://gitlab.com/fdroid/fdroiddata/-/merge_requests/54224

A passing F-Droid CI pipeline does not itself constitute maintainer
approval or publication in the official repository.
