#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

if grep -R --include='AndroidManifest.xml' -n 'android.permission.INTERNET' "$ROOT/app" "$ROOT/engine-grammalecte" "$ROOT/spellchecker"; then
    echo "The project must remain offline-first: INTERNET permission detected." >&2
    exit 1
fi

echo "Privacy invariant OK: no INTERNET permission declared."
