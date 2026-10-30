# RC1 to 1.0.0 upgrade acceptance

Date: 2026-10-30

## Environment

- Device: dedicated Android emulator
- AVD: grammalecte-upgrade-api26
- Android: 8.0, API 26
- Architecture: x86
- Package: fr.zefut.grammalecte

## Builds

- Baseline: v1.0.0-rc1 source, debug APK, versionCode 2
- Candidate: main source with temporary local version bump,
  debug APK, versionCode 3
- Both APKs used the same debug signing certificate.

No published release or Git tag was modified.

## Results

| Test | Result |
| --- | --- |
| Baseline APK installation | PASS |
| Upgrade using adb install -r | PASS |
| Installed versionCode increased from 2 to 3 | PASS |
| Application preferences preserved | PASS |
| Personal dictionary word preserved | PASS |
| Spelling correction after upgrade | PASS |

The user confirmed retained preferences and the personal
dictionary through the application interface.

The attempted before/after SharedPreferences SHA-256 collection
failed because of shell command quoting. No valid hash
comparison was obtained, so this report does not claim one.

## Limitations

This validates a debug-to-debug upgrade only.

Before stable publication, repeat the upgrade test using the
published signed RC1 APK and the candidate APK signed with
the official release certificate.

Stable release publication is not yet authorized.
