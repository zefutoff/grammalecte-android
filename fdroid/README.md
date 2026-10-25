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

## Pending validation

- Validate metadata with fdroidserver.
- Run the F-Droid source and binary scanner.
- Build in the official F-Droid environment.
- Verify byte-for-byte reproducibility against the
  published signed APK.
- Review build-server compatibility and dependencies.
- Review official submission requirements.

The recipe must not be considered ready for inclusion
until these checks have passed.

No published Git tag or release artifact is modified.
