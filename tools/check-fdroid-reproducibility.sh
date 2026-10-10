#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SIGNED_APK="${1:-$ROOT/app/build/outputs/apk/release/app-release-signed.apk}"
COMMIT="${2:-$(git -C "$ROOT" rev-parse HEAD)}"

CLONE_DIR="$(mktemp -d /tmp/grammalecte-fdroid-clone.XXXXXX)"
RECONSTRUCTED_APK="$(mktemp /tmp/grammalecte-fdroid-reconstructed.XXXXXX.apk)"

cleanup() {
    rm -rf "$CLONE_DIR"
    rm -f "$RECONSTRUCTED_APK"
}

trap cleanup EXIT

for command in git apksigcopier cmp sha256sum unzip; do
    if ! command -v "$command" >/dev/null 2>&1; then
        echo "Required command not found: $command" >&2
        exit 1
    fi
done

if [[ ! -f "$SIGNED_APK" ]]; then
    echo "Signed APK not found: $SIGNED_APK" >&2
    echo "Run make signed-release first for the same commit." >&2
    exit 1
fi

if ! git -C "$ROOT" cat-file -e "$COMMIT^{commit}" 2>/dev/null; then
    echo "Git commit not found: $COMMIT" >&2
    exit 1
fi

if ! git -C "$ROOT" diff --quiet ||
    ! git -C "$ROOT" diff --cached --quiet
then
    echo "Tracked working tree changes are present." >&2
    echo "Commit or restore them before checking F-Droid reproducibility." >&2
    exit 1
fi

SIGNED_APK="$(
    cd "$(dirname "$SIGNED_APK")"
    printf '%s/%s\n' "$PWD" "$(basename "$SIGNED_APK")"
)"

echo "Testing F-Droid-style reproducibility"
echo "Commit: $COMMIT"
echo "Signed APK: $SIGNED_APK"
echo

git clone \
    --quiet \
    --no-hardlinks \
    "$ROOT" \
    "$CLONE_DIR"

git -C "$CLONE_DIR" \
    checkout \
    --quiet \
    --detach \
    "$COMMIT"

if [[ -f "$ROOT/local.properties" ]]; then
    cp "$ROOT/local.properties" "$CLONE_DIR/local.properties"
fi

(
    cd "$CLONE_DIR"

    ./gradlew \
        :app:assembleRelease \
        --no-daemon \
        --no-build-cache \
        --no-configuration-cache
)

UNSIGNED_APK="$CLONE_DIR/app/build/outputs/apk/release/app-release-unsigned.apk"

if [[ ! -f "$UNSIGNED_APK" ]]; then
    echo "Rebuilt unsigned APK not found: $UNSIGNED_APK" >&2
    exit 1
fi

if unzip -l "$UNSIGNED_APK" |
    grep -q 'META-INF/version-control-info.textproto'
then
    echo "Unexpected VCS metadata found in rebuilt release APK." >&2
    exit 1
fi

apksigcopier copy \
    "$SIGNED_APK" \
    "$UNSIGNED_APK" \
    "$RECONSTRUCTED_APK"

SIGNED_HASH="$(sha256sum "$SIGNED_APK" | awk '{print $1}')"
RECONSTRUCTED_HASH="$(
    sha256sum "$RECONSTRUCTED_APK" |
        awk '{print $1}'
)"

echo
echo "========== SHA-256 =========="
printf '%s  upstream signed APK\n' "$SIGNED_HASH"
printf '%s  reconstructed APK\n' "$RECONSTRUCTED_HASH"

echo
echo "========== COMPARISON =========="

if [[ "$SIGNED_HASH" != "$RECONSTRUCTED_HASH" ]] ||
    ! cmp -s "$SIGNED_APK" "$RECONSTRUCTED_APK"
then
    echo "F-Droid reproducibility check FAILED." >&2
    exit 2
fi

echo "F-Droid reproducibility check: IDENTICAL"
