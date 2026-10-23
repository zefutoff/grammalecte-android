# Grammalecte Android 1.0.0-rc1 — Physical acceptance

Date: 2026-10-23

## Tested release

- Version: 1.0.0-rc1
- Version code: 2
- Source commit: 4beb0c088745ea76c493e6e27ccc874f75713fdd
- Signed APK workflow run: 39508577609
- Device: Samsung Galaxy S21 (SM-G998B)
- Android: 15

## Manual acceptance results

| Test | Result |
| --- | --- |
| Application launch and initial setup | PASS |
| Direct Grammalecte correction | PASS |
| Native Android spell checking | PASS |
| ACTION_PROCESS_TEXT | PASS |
| IME / InputConnection | PASS |
| Correction preferences | PASS |
| Offline operation | PASS |

## Observation

Native spelling underlines did not appear immediately in
an application that appeared to have remained open before
the spellchecker installation or activation.

Closing and reopening the host application restored native
spelling underlines and correction functionality.

This is consistent with a stale spellchecker session or
editor lifecycle state, but the exact cause is unconfirmed.

Recommended user guidance: reopen applications that were
already running when activating Grammalecte as the system
spellchecker.

## Scope and limitations

The seven acceptance checks passed on the tested physical
device. This does not guarantee identical behavior in all
Android applications or on other devices.

GrapheneOS issue #34 and Samsung Messages issue #50 remain
open and are not considered resolved by this acceptance test.

## Decision

RC1 accepted for further release preparation.

Public stable release is not yet authorized.
