# Release process

The first public release should not happen until the project can build from a clean checkout with a committed Gradle wrapper and vendored/pinned Grammalecte assets.

## Release checklist

1. CI green on the target branch.
2. `tools/check-no-network-permission.sh` green.
3. `tools/check-vendor.sh` green.
4. Unit and emulator tests green.
5. Release APK/AAB built from a tagged commit.
6. Reproducibility metadata records Gradle, AGP, Kotlin, JDK and Grammalecte revisions.
7. Third-party notices reviewed.
8. Changelog documents user-visible correction and compatibility changes.
9. APK inspected to confirm no unexpected permissions.

## Versioning

Use semantic versioning for the Android integration. Grammalecte's version is tracked separately in `tools/grammalecte.env`.

Example:

- Android app `0.3.0`
- embedded Grammalecte `2.3.0`

An upstream engine update does not automatically require an Android major version bump unless it changes the integration contract or user-visible compatibility policy.
