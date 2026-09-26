#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
# shellcheck disable=SC1091
source "$ROOT/tools/grammalecte.env"

if ! python3 -c 'from distutils import dir_util, file_util' >/dev/null 2>&1; then
    echo "The pinned Grammalecte builder requires Python distutils compatibility." >&2
    echo "Use Python 3.11 or install setuptools for your Python version." >&2
    exit 1
fi

UPSTREAM_ROOT="$ROOT/.upstream"
SOURCE_DIR="$UPSTREAM_ROOT/grammalecte"

cleanup() {
    rm -rf "$SOURCE_DIR"
    rmdir "$UPSTREAM_ROOT" 2>/dev/null || true
}
trap cleanup EXIT

ASSET_DIR="$ROOT/engine-grammalecte/src/main/assets/grammalecte"
NOTICE_DIR="$ROOT/third_party/grammalecte"

rm -rf "$SOURCE_DIR"
mkdir -p "$UPSTREAM_ROOT"

git init -q "$SOURCE_DIR"
git -C "$SOURCE_DIR" remote add origin "$GRAMMALECTE_REPOSITORY"
git -C "$SOURCE_DIR" fetch -q --depth 1 origin "$GRAMMALECTE_COMMIT"
git -C "$SOURCE_DIR" checkout -q --detach FETCH_HEAD

(
    cd "$SOURCE_DIR"
    python3 make.py fr -js
)

rm -rf "$ASSET_DIR"
mkdir -p "$ASSET_DIR"
cp -a "$SOURCE_DIR/grammalecte-js/." "$ASSET_DIR/"

# QuickJS does not implement the deprecated non-standard RegExp.leftContext
# property used by Grammalecte's generated JavaScript. Replace it with the
# standard equivalent based on the current match index.
ASSET_DIR="$ASSET_DIR" python3 - <<'PY_PATCH'
import os
from pathlib import Path

root = Path(os.environ["ASSET_DIR"])
old = "RegExp.leftContext.search(sRegEx)"
new = "sText.slice(0, m.index).search(sRegEx)"

patched = []

for path in root.rglob("*.js"):
    text = path.read_text(encoding="utf-8")
    count = text.count(old)
    if count:
        path.write_text(text.replace(old, new), encoding="utf-8")
        patched.append((path.relative_to(root), count))

total = sum(count for _, count in patched)

# This is intentionally strict for the pinned Grammalecte revision.
# If upstream changes, review the generated JavaScript before updating this.
if total != 3:
    raise SystemExit(
        f"Unexpected RegExp.leftContext occurrence count: {total} (expected 3)"
    )

for path, count in patched:
    print(f"QuickJS compatibility patch: {path} ({count} replacement(s))")
PY_PATCH

mkdir -p "$NOTICE_DIR"
cp "$SOURCE_DIR/LICENSE.txt" "$NOTICE_DIR/LICENSE.txt"
if [[ -f "$SOURCE_DIR/LICENSE.fr.txt" ]]; then
    cp "$SOURCE_DIR/LICENSE.fr.txt" "$NOTICE_DIR/LICENSE.fr.txt"
fi

cat > "$NOTICE_DIR/UPSTREAM.json" <<JSON
{
  "repository": "$GRAMMALECTE_REPOSITORY",
  "commit": "$GRAMMALECTE_COMMIT",
  "version": "$GRAMMALECTE_VERSION",
  "buildCommand": "python3 make.py fr -js"
}
JSON

cat > "$ASSET_DIR/UPSTREAM.txt" <<TXT
Grammalecte $GRAMMALECTE_VERSION
Repository: $GRAMMALECTE_REPOSITORY
Commit: $GRAMMALECTE_COMMIT
Generated with: python3 make.py fr -js
TXT

printf 'Vendored Grammalecte %s (%s)\n' "$GRAMMALECTE_VERSION" "$GRAMMALECTE_COMMIT"
