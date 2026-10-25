# F-Droid build recipe candidate

This directory contains a candidate F-Droid build recipe for
Grammalecte Android.

It is not the official fdroiddata repository.

## Target release

- Application ID: fr.zefut.grammalecte
- Version: 1.0.0-rc1
- Version code: 2
- Source commit: 4beb0c088745ea76c493e6e27ccc874f75713fdd
- Upstream signed APK: GitHub Release v1.0.0-rc1

## Reproducibility

The candidate recipe rebuilds the unsigned APK from the
exact release commit using Gradle and JDK 17.

F-Droid must compare the resulting unsigned APK against
the published upstream signed APK, using its own
reproducible-build verification process.

The upstream signing certificate is pinned in the recipe.

## Local validation

- `fdroid lint`: passed.
- `fdroid scanner`: zero problems.
- `fdroid build --test`: passed for versionCode 2.
- The rebuilt APK was successfully verified against
  the published developer-signed RC1 APK.

## Pending validation

- Build in the official F-Droid CI environment.
- Verify the build-server Java installation procedure.
- Complete the official inclusion review.

The recipe must not be considered ready for inclusion
until these checks have passed.

No published Git tag or release artifact is modified.
