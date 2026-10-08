#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
COMMIT="${1:-$(git -C "$ROOT" rev-parse HEAD)}"

WORKTREE_A="$(mktemp -d /tmp/grammalecte-release-a.XXXXXX)"
WORKTREE_B="$(mktemp -d /tmp/grammalecte-release-b.XXXXXX)"

cleanup() {
    git -C "$ROOT" worktree remove --force "$WORKTREE_A" >/dev/null 2>&1 || true
    git -C "$ROOT" worktree remove --force "$WORKTREE_B" >/dev/null 2>&1 || true

    rm -rf "$WORKTREE_A" "$WORKTREE_B"
}

trap cleanup EXIT

for command in git cmp; do
    if ! command -v "$command" >/dev/null 2>&1; then
        echo "Required command not found: $command" >&2
        exit 1
    fi
done

hash_file() {
    local file="$1"

    if command -v sha256sum >/dev/null 2>&1; then
        sha256sum "$file" | awk '{print $1}'
        return
    fi

    if command -v shasum >/dev/null 2>&1; then
        shasum -a 256 "$file" | awk '{print $1}'
        return
    fi

    echo "Unable to find sha256sum or shasum." >&2
    exit 1
}

copy_local_properties() {
    local destination="$1"

    if [[ -f "$ROOT/local.properties" ]]; then
        cp "$ROOT/local.properties" "$destination/local.properties"
    fi
}

build_release() {
    local directory="$1"

    (
        cd "$directory"

        ./gradlew \
            :app:assembleRelease \
            --no-daemon \
            --no-build-cache \
            --no-configuration-cache
    )
}

echo "Testing unsigned release reproducibility"
echo "Commit: $COMMIT"

git -C "$ROOT" worktree add \
    --detach \
    "$WORKTREE_A" \
    "$COMMIT"

git -C "$ROOT" worktree add \
    --detach \
    "$WORKTREE_B" \
    "$COMMIT"

copy_local_properties "$WORKTREE_A"
copy_local_properties "$WORKTREE_B"

echo
echo "========== BUILD A =========="
build_release "$WORKTREE_A"

echo
echo "========== BUILD B =========="
build_release "$WORKTREE_B"

APK_A="$WORKTREE_A/app/build/outputs/apk/release/app-release-unsigned.apk"
APK_B="$WORKTREE_B/app/build/outputs/apk/release/app-release-unsigned.apk"

if [[ ! -f "$APK_A" ]]; then
    echo "Release APK A not found: $APK_A" >&2
    exit 1
fi

if [[ ! -f "$APK_B" ]]; then
    echo "Release APK B not found: $APK_B" >&2
    exit 1
fi

HASH_A="$(hash_file "$APK_A")"
HASH_B="$(hash_file "$APK_B")"

echo
echo "========== SHA-256 =========="
printf '%s  %s\n' "$HASH_A" "$APK_A"
printf '%s  %s\n' "$HASH_B" "$APK_B"

echo
echo "========== COMPARISON =========="

if [[ "$HASH_A" != "$HASH_B" ]] || ! cmp -s "$APK_A" "$APK_B"; then
    echo "NOT REPRODUCIBLE: unsigned release APK files differ." >&2
    exit 2
fi

echo "REPRODUCIBLE: unsigned release APK files are byte-for-byte identical."
