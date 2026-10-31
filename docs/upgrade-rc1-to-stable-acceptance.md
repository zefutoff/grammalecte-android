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

The subsequent upgrade test using the published signed RC1
APK and the officially signed stable candidate is documented
in the following section.

Stable release publication is not yet authorized.

## Officially signed upgrade acceptance — 2026-10-31

### Environment

- Dedicated AVD: grammalecte-upgrade-signed-api26
- Android 8.0 / API 26 / x86
- Application ID: fr.zefut.grammalecte
- RC1: published v1.0.0-rc1 APK, versionCode 2
- Stable candidate: version 1.0.0, versionCode 3
- Candidate source: b13b85367a68af68c2686d7a1c409b1eeebd654f
- Signing workflow: 41212179409
- Signing certificate SHA-256:
  0d8ac9f2341257482c8380be5758169f889e606691787d13edbbb9649a2f3144

### Results

| Verification | Result |
| --- | --- |
| RC1 APK integrity and signature | PASS |
| Stable candidate integrity and signature | PASS |
| Identical release signing certificate | PASS |
| RC1 installation | PASS |
| Upgrade with adb install -r | PASS |
| Installed versionCode 3 | PASS |
| Personal dictionary preserved | PASS |
| Grammar preferences preserved | PASS |
| Dictionary selection preserved | PASS |
| Spelling correction after upgrade | PASS |

The six functional upgrade acceptance checks were confirmed
on the dedicated Android 8.0 emulator.

The signed candidate passed the release workflow, including
F-Droid-style reproducibility and CycloneDX SBOM validation.

### Remaining limitations

- GrapheneOS native spell-checker issue #34 remains open.
- Samsung Messages PROCESS_TEXT issue #50 remains open.
- The upgrade test does not establish compatibility across
  all Android devices and vendor ROMs.

Stable publication remains a separate release decision.
