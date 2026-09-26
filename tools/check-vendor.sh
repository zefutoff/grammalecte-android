#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
# shellcheck disable=SC1091
source "$ROOT/tools/grammalecte.env"
STAMP="$ROOT/engine-grammalecte/src/main/assets/grammalecte/UPSTREAM.txt"

if [[ ! -f "$STAMP" ]]; then
    echo "Grammalecte assets are missing. Run ./tools/vendor-grammalecte.sh" >&2
    exit 1
fi

grep -Fq "Commit: $GRAMMALECTE_COMMIT" "$STAMP"
grep -Fq "Grammalecte $GRAMMALECTE_VERSION" "$STAMP"
