# F-Droid inclusion status

Grammalecte Android has a signed release candidate and a submitted
F-Droid inclusion request. Official inclusion remains subject to
the upstream maintainers' review.

## Upstream store metadata

Localized application metadata is maintained directly in the source
repository using the Fastlane-compatible structure:

    fastlane/metadata/android/

The current locales are:

    en-US
    fr-FR

The metadata is validated as part of make check.

Both locales contain descriptions, changelogs, 512x512 PNG icons
and phone screenshots. The English listing currently uses authentic
screenshots of the French-language application interface.
These assets are available in the source repository for future tags.

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

The Android namespace and application ID are finalized as:

    fr.zefut.grammalecte

This identifier is considered stable and must not change after the first
public release.

The previous development identifier:

    fr.grammalecteandroid.unofficial

was used only by pre-release development builds. Android treats the new
application ID as a different application, so development installations using
the previous identifier do not upgrade in place.

The final F-Droid recipe can now define:

- the final application ID;
- the public Git repository;
- the exact full Git commit for each release;
- the version name and version code;
- the versioned upstream APK location;
- the expected APK signing certificate;
- the Gradle release build procedure.

The developer-signed `v1.0.0-rc1` APK is already published on GitHub.
F-Droid must independently rebuild and verify matching APK contents
before distributing that developer-signed binary.

## Upstream inclusion request

The submitted F-Droid recipe is tracked in:

https://gitlab.com/fdroid/fdroiddata/-/merge_requests/54224

The submission uses:

- application ID `fr.zefut.grammalecte`;
- release candidate `1.0.0-rc1`, versionCode 2;
- a pinned source commit and signing certificate;
- reproducible unsigned APK compilation;
- tag-based update checking.

The inclusion pipeline passed its build, APK, lint, source,
metadata formatting and update-check jobs.

## Remaining work

1. Address any feedback from the F-Droid maintainers.
2. Wait for inclusion approval and repository publication.
3. Verify the published APK and its signing certificate.
4. Verify subsequent updates preserve Android signature compatibility.
5. Keep future source tags, release artifacts and Fastlane metadata aligned.

Passing CI alone does not imply acceptance into the official catalog.
