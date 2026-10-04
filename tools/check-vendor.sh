#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
# shellcheck disable=SC1091
source "$ROOT/tools/grammalecte.env"

STAMP="$ROOT/engine-grammalecte/src/main/assets/grammalecte/UPSTREAM.txt"
METADATA="$ROOT/third_party/grammalecte/UPSTREAM.json"

if [[ ! -f "$STAMP" ]]; then
    echo "Grammalecte upstream stamp is missing: $STAMP" >&2
    exit 1
fi

if [[ ! -f "$METADATA" ]]; then
    echo "Grammalecte upstream metadata is missing: $METADATA" >&2
    exit 1
fi

grep -Fq "Grammalecte $GRAMMALECTE_VERSION" "$STAMP"
grep -Fq "Repository: $GRAMMALECTE_REPOSITORY" "$STAMP"
grep -Fq "Commit: $GRAMMALECTE_COMMIT" "$STAMP"
grep -Fq "Generated with: python3 make.py fr -js" "$STAMP"

METADATA="$METADATA" \
GRAMMALECTE_REPOSITORY="$GRAMMALECTE_REPOSITORY" \
GRAMMALECTE_COMMIT="$GRAMMALECTE_COMMIT" \
GRAMMALECTE_VERSION="$GRAMMALECTE_VERSION" \
python3 - <<'PY_JSON'
import json
import os
from pathlib import Path

metadata_path = Path(os.environ["METADATA"])
metadata = json.loads(metadata_path.read_text(encoding="utf-8"))

expected = {
    "repository": os.environ["GRAMMALECTE_REPOSITORY"],
    "commit": os.environ["GRAMMALECTE_COMMIT"],
    "version": os.environ["GRAMMALECTE_VERSION"],
    "buildCommand": "python3 make.py fr -js",
}

if metadata != expected:
    raise SystemExit(
        "Vendored Grammalecte metadata does not match tools/grammalecte.env.\n"
        f"Expected: {expected}\n"
        f"Actual:   {metadata}"
    )
PY_JSON

echo "Vendored Grammalecte metadata OK: $GRAMMALECTE_VERSION ($GRAMMALECTE_COMMIT)"
