#!/usr/bin/env python3
"""Verify that Android instrumentation tests actually executed."""

import argparse
import subprocess
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parent.parent
MODULES = {"app", "spellchecker"}


def results_dir(module: str) -> Path:
    return (
        PROJECT_ROOT
        / module
        / "build/outputs/androidTest-results/connected/debug"
    )


def preflight() -> None:
    output = subprocess.run(
        ["adb", "devices"],
        capture_output=True,
        text=True,
        check=True,
    ).stdout

    connected = [
        fields[0]
        for line in output.splitlines()[1:]
        if len(fields := line.split()) >= 2
        and fields[1] == "device"
    ]

    if len(connected) != 1:
        raise RuntimeError(
            "Exactly one connected Android device is required; "
            f"found {connected!r}."
        )

    print(f"Instrumentation target: {connected[0]}")


def prepare(path: Path) -> None:
    removed = 0

    for report in path.rglob("TEST-*.xml"):
        report.unlink()
        removed += 1

    print(
        f"Cleared {removed} previous instrumentation XML report(s)."
    )


def verify(path: Path) -> None:
    reports = sorted(path.rglob("TEST-*.xml"))

    if not reports:
        raise RuntimeError(
            f"No instrumentation XML reports in {path}; "
            "tests did not run."
        )

    total = skipped = failures = errors = 0

    for report in reports:
        root = ET.parse(report).getroot()
        cases = list(root.iter("testcase"))

        total += len(cases)
        skipped += sum(
            bool(case.findall("skipped")) for case in cases
        )
        failures += sum(
            bool(case.findall("failure")) for case in cases
        )
        errors += sum(
            bool(case.findall("error")) for case in cases
        )

        for suite in root.iter("testsuite"):
            failures = max(
                failures, int(suite.get("failures", "0"))
            )
            errors = max(
                errors, int(suite.get("errors", "0"))
            )

    executed = total - skipped

    print(
        f"Instrumentation: {total} test(s), "
        f"{executed} executed, "
        f"{failures} failure(s), "
        f"{errors} error(s), "
        f"{skipped} skipped."
    )

    if executed == 0 or failures or errors:
        raise RuntimeError(
            "Instrumentation results invalid or unsuccessful."
        )


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "action", choices=("preflight", "prepare", "verify")
    )
    parser.add_argument(
        "module", nargs="?", choices=sorted(MODULES)
    )
    args = parser.parse_args()

    if args.action != "preflight" and args.module is None:
        parser.error("module is required for prepare/verify")

    try:
        if args.action == "preflight":
            preflight()
        elif args.action == "prepare":
            prepare(results_dir(args.module))
        else:
            verify(results_dir(args.module))
    except (
        RuntimeError,
        OSError,
        ET.ParseError,
        ValueError,
        subprocess.CalledProcessError,
    ) as exc:
        print(
            f"Instrumentation check FAILED: {exc}",
            file=sys.stderr,
        )
        return 1

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
