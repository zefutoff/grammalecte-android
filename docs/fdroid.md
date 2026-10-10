# F-Droid readiness

Grammalecte Android is being prepared for eventual distribution through
F-Droid.

## Upstream store metadata

Localized application metadata is maintained directly in the source
repository using the Fastlane-compatible structure:

    fastlane/metadata/android/

The current locales are:

    en-US
    fr-FR

The metadata is validated as part of make check.

Screenshots, release-specific changelogs and other store graphics will be
added before the first stable public release.

## Reproducible APK verification

The project provides an F-Droid-style reproducibility check:

    make fdroid-reproducibility

The check:

1. starts from the current committed source revision;
2. creates a clean Git clone;
3. builds the unsigned release APK from that clone;
4. copies the signature from the upstream signed APK with apksigcopier;
5. compares the reconstructed APK byte-for-byte with the upstream APK.

A mismatch causes the check to fail.

## APK signing compatibility

Release APKs are signed with Android Build Tools 34.0.0.

The release signing script intentionally selects this apksigner version rather
than whichever Android Build Tools version happens to be newest on the build
machine.

The expected SHA-256 signing certificate fingerprint is stored in:

    config/release-signing-cert.sha256

Current fingerprint:

    0d8ac9f2341257482c8380be5758169f889e606691787d13edbbb9649a2f3144

## Android VCS metadata

Release builds disable Android Gradle Plugin VCS metadata:

    vcsInfo.include = false

Without this setting, the generated APK can differ according to how the Git
checkout is represented on disk. In particular, normal repositories and Git
worktrees can produce different META-INF/version-control-info.textproto
contents.

The source revision remains independently available through Git tags, release
metadata and the source repository.

## F-Droid build recipe

The final F-Droid build recipe is intentionally deferred until the Android
application ID has been reviewed and finalized.

The current development application ID is:

    fr.grammalecteandroid.unofficial

Changing an Android application ID after public distribution would create a
different application from Android's point of view, so this decision must be
made before the first stable release.

Once the namespace is final, the F-Droid recipe can define:

- the final application ID;
- the public Git repository;
- the exact full Git commit for each release;
- the version name and version code;
- the versioned upstream APK location;
- the expected APK signing certificate;
- the Gradle release build procedure.

The upstream signed APK should only be published after F-Droid successfully
verifies it against the independently rebuilt APK.

## Remaining work

Before an F-Droid submission:

1. finalize the Android package namespace;
2. finalize the first stable version name and version code;
3. publish a versioned upstream APK;
4. add release screenshots and changelog metadata;
5. prepare and validate the final F-Droid build recipe;
6. complete the third-party license review.
