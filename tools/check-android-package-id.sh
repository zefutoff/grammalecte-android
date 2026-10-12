#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BUILD_FILE="$ROOT/app/build.gradle.kts"
EXPECTED="fr.zefut.grammalecte"

NAMESPACE="$(
    sed -nE \
        's/^[[:space:]]*namespace = "([^"]+)".*/\1/p' \
        "$BUILD_FILE" |
        head -n 1
)"

APPLICATION_ID="$(
    sed -nE \
        's/^[[:space:]]*applicationId = "([^"]+)".*/\1/p' \
        "$BUILD_FILE" |
        head -n 1
)"

if [[ "$NAMESPACE" != "$EXPECTED" ]]; then
    echo "Unexpected Android namespace: ${NAMESPACE:-missing}" >&2
    echo "Expected: $EXPECTED" >&2
    exit 1
fi

if [[ "$APPLICATION_ID" != "$EXPECTED" ]]; then
    echo "Unexpected Android applicationId: ${APPLICATION_ID:-missing}" >&2
    echo "Expected: $EXPECTED" >&2
    exit 1
fi

echo "Android package identity OK: $EXPECTED"
